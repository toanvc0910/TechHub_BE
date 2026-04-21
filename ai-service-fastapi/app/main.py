from __future__ import annotations

import contextlib
import logging

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from redis.asyncio import Redis

from app.api.router import api_router
from app.core.config import get_settings
from app.core.responses import error_response
from app.db.session import maybe_create_schema
from app.orchestration.memory.redis_memory_service import redis_memory_service
from app.services.indexing_event_consumer import indexing_event_consumer
from app.services.langfuse_service import langfuse_service
from app.services.provider_config import provider_config_service
from app.services.rate_limit import rate_limiting_service

settings = get_settings()

logging.basicConfig(level=settings.log_level)
logger = logging.getLogger(__name__)


@contextlib.asynccontextmanager
async def lifespan(app: FastAPI):
    await maybe_create_schema()

    redis_client: Redis | None = None
    try:
        redis_client = Redis(
            host=settings.redis_host,
            port=settings.redis_port,
            password=settings.redis_password,
            db=settings.redis_db,
            socket_connect_timeout=2.0,
            socket_timeout=2.0,
        )
        await redis_client.ping()
        redis_memory_service.set_client(redis_client)
        rate_limiting_service.set_client(redis_client)
        provider_config_service.set_redis(redis_client)
        await provider_config_service.load_persisted()
        logger.info("Connected to Redis for AI service hot path.")
    except Exception as exc:  # pragma: no cover - infra dependent
        logger.warning("Redis unavailable, using in-process fallback: %s", exc)
        redis_memory_service.set_client(None)
        rate_limiting_service.set_client(None)

    if settings.eureka_enabled:
        try:
            import py_eureka_client.eureka_client as eureka_client

            await eureka_client.init_async(
                eureka_server=settings.eureka_url,
                app_name=settings.app_name,
                instance_port=settings.port,
                instance_host=settings.eureka_hostname,
            )
            logger.info("Registered FastAPI AI service with Eureka as %s.", settings.app_name)
        except Exception as exc:  # pragma: no cover - infra dependent
            logger.warning("Eureka registration skipped: %s", exc)

    try:
        langfuse_service.init()
    except Exception as exc:
        logger.warning("Langfuse init skipped: %s", exc)

    try:
        await indexing_event_consumer.start()
    except Exception as exc:  # pragma: no cover - infra dependent
        logger.warning("Kafka indexing consumer not started: %s", exc)

    yield

    await indexing_event_consumer.stop()
    langfuse_service.shutdown()

    if redis_client is not None:
        await redis_client.aclose()


app = FastAPI(title=settings.app_name, version=settings.app_version, lifespan=lifespan)
app.include_router(api_router)


@app.exception_handler(ValueError)
async def handle_value_error(request: Request, exc: ValueError) -> JSONResponse:
    return JSONResponse(
        status_code=400,
        content=error_response(message=str(exc), path=request.url.path, code=400),
    )


@app.exception_handler(Exception)
async def handle_generic_error(request: Request, exc: Exception) -> JSONResponse:
    logger.exception("Unhandled FastAPI AI service error: %s", exc)
    return JSONResponse(
        status_code=500,
        content=error_response(message=str(exc), path=request.url.path, code=500, status="UNKNOWN_ERROR"),
    )
