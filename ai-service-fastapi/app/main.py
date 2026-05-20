from __future__ import annotations

import asyncio
import contextlib
import logging

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from redis.asyncio import Redis

from app.api.router import api_router
from app.api.routes.metrics import PrometheusMiddleware
from app.core.config import get_settings
from app.core.logging_config import RequestIdMiddleware, configure_logging
from app.core.responses import error_response
from app.db.session import engine, maybe_create_schema
from app.orchestration.memory.redis_memory_service import redis_memory_service
from app.services.file_context_service import file_context_service
from app.services.indexing_event_consumer import indexing_event_consumer
from app.services.langfuse_service import langfuse_service
from app.services.llm_gateway import switchable_ai_gateway
from app.services.provider_config import provider_config_service
from app.services.rate_limit import rate_limiting_service
from app.services.vector_service import vector_service

settings = get_settings()

# Replace the standard `logging.basicConfig` plain-text formatter with the
# structured JSON formatter so log aggregators get a parseable stream and
# every record carries the request correlation ID.
configure_logging(settings.log_level)
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

    # Graceful shutdown — every resource is closed best-effort with a timeout
    # so a hung dependency cannot stall SIGTERM > 30s in production.
    async def _safe_close(label: str, coro, timeout: float = 10.0) -> None:
        try:
            await asyncio.wait_for(coro, timeout=timeout)
        except Exception as exc:  # noqa: BLE001
            logger.warning("Shutdown: %s failed: %s", label, exc)

    await _safe_close("kafka_consumer", indexing_event_consumer.stop(), timeout=10.0)

    try:
        langfuse_service.shutdown()
    except Exception as exc:  # noqa: BLE001
        logger.warning("Shutdown: langfuse failed: %s", exc)

    # Close every long-lived httpx.AsyncClient held by service singletons.
    for label, client in (
        ("llm_gateway_client", getattr(switchable_ai_gateway, "_client", None)),
        ("vector_service_client", getattr(vector_service, "_client", None)),
        ("file_context_client", getattr(file_context_service, "_client", None)),
    ):
        if client is not None:
            await _safe_close(label, client.aclose(), timeout=5.0)

    if redis_client is not None:
        await _safe_close("redis", redis_client.aclose(), timeout=5.0)

    # SQLAlchemy async engine — disposes the connection pool.
    await _safe_close("db_engine", engine.dispose(), timeout=10.0)


app = FastAPI(title=settings.app_name, version=settings.app_version, lifespan=lifespan)
# Middleware ordering: PrometheusMiddleware first so it can observe the final
# response status that RequestIdMiddleware injects. Starlette processes
# middleware bottom-up, so the last `add_middleware` is outermost.
app.add_middleware(PrometheusMiddleware)
app.add_middleware(RequestIdMiddleware)
app.include_router(api_router)


@app.exception_handler(ValueError)
async def handle_value_error(request: Request, exc: ValueError) -> JSONResponse:
    return JSONResponse(
        status_code=400,
        content=error_response(message=str(exc), path=request.url.path, code=400),
    )


@app.exception_handler(Exception)
async def handle_generic_error(request: Request, exc: Exception) -> JSONResponse:
    # Log full exception server-side; never return raw exception text to the
    # client because it can leak DB schema, internal stack traces, file paths,
    # provider error messages, etc.
    logger.exception("Unhandled FastAPI AI service error: %s", exc)
    dev_mode = settings.environment.lower() in {"dev", "development", "local", "test"}
    safe_message = (
        f"{type(exc).__name__}: {exc}"
        if dev_mode
        else "Internal server error. The incident has been logged; please contact support if it persists."
    )
    return JSONResponse(
        status_code=500,
        content=error_response(message=safe_message, path=request.url.path, code=500, status="INTERNAL_ERROR"),
    )
