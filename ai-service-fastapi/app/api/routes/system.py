"""
Liveness vs readiness endpoints.

`/health` and `/actuator/health` remain process-only liveness probes — they
always return UP as long as the FastAPI event loop is responsive. Kubernetes /
Docker / Eureka use these to decide whether to restart the container.

`/actuator/health/readiness` performs short-timeout pings to every downstream
the service depends on (PostgreSQL, Qdrant, optional Redis, optional LLM
providers). Any downstream that AI service genuinely cannot function without
gates traffic by returning HTTP 503 + a structured component breakdown.

Liveness MUST NOT depend on downstream health (otherwise a Qdrant outage would
restart the whole AI service in a loop). Readiness MUST, so the load balancer
stops sending traffic until the dependency comes back.
"""

from __future__ import annotations

import asyncio
from typing import Any

import httpx
from fastapi import APIRouter, Response, status
from sqlalchemy import text

from app.core.config import get_settings
from app.db.session import engine

router = APIRouter()


@router.get("/health")
async def health() -> dict[str, str]:
    return {"status": "UP"}


@router.get("/actuator/health")
async def actuator_health() -> dict[str, str]:
    return {"status": "UP"}


@router.get("/actuator/health/liveness")
async def liveness() -> dict[str, str]:
    return {"status": "UP"}


@router.get("/actuator/health/readiness")
async def readiness(response: Response) -> dict[str, Any]:
    settings = get_settings()

    # Run downstream probes concurrently so total latency = max(slowest) and
    # not sum-of-all. Cold remote connections (Postgres TLS handshake to a
    # cloud instance) can take 5–8 s on first hit.
    postgres_check, qdrant_check, llm_check = await asyncio.gather(
        _check_postgres(),
        _check_qdrant(settings),
        _check_llm_provider(settings),
    )
    components = {
        "postgres": postgres_check,
        "qdrant": qdrant_check,
        "llm_provider": llm_check,
    }

    hard_required = ("postgres", "qdrant")
    overall_ok = all(components[name].get("status") == "UP" for name in hard_required)
    if not overall_ok:
        response.status_code = status.HTTP_503_SERVICE_UNAVAILABLE
    return {
        "status": "UP" if overall_ok else "DOWN",
        "components": components,
        "hardRequired": list(hard_required),
    }


async def _check_postgres() -> dict[str, Any]:
    try:
        async def _ping() -> None:
            async with engine.connect() as conn:
                await conn.execute(text("SELECT 1"))

        # 10s budget covers a cold remote TLS handshake; the connection pool
        # caches the channel afterwards so subsequent probes return in <100ms.
        await asyncio.wait_for(_ping(), timeout=10.0)
        return {"status": "UP"}
    except asyncio.TimeoutError:
        return {"status": "DOWN", "error": "timeout"}
    except Exception as exc:  # noqa: BLE001
        return {"status": "DOWN", "error": f"{type(exc).__name__}: {exc}"[:200]}


async def _check_qdrant(settings) -> dict[str, Any]:
    if not settings.qdrant_host:
        return {"status": "DOWN", "error": "qdrant_host not configured"}
    headers = {"Content-Type": "application/json"}
    if settings.qdrant_api_key:
        headers["api-key"] = settings.qdrant_api_key
    try:
        async with httpx.AsyncClient(timeout=3.0) as client:
            resp = await client.get(f"{settings.qdrant_host.rstrip('/')}/", headers=headers)
        if 200 <= resp.status_code < 400:
            return {"status": "UP", "version": resp.json().get("version")}
        return {"status": "DOWN", "error": f"HTTP {resp.status_code}"}
    except Exception as exc:  # noqa: BLE001
        return {"status": "DOWN", "error": f"{type(exc).__name__}: {exc}"[:200]}


async def _check_llm_provider(settings) -> dict[str, Any]:
    # Best effort — both providers are optional. If neither is configured we
    # report DOWN-but-not-hard-required so admin can see it; readiness still
    # passes because postgres + qdrant are the only hard prereqs.
    provider_results: dict[str, Any] = {}
    if settings.gemini_api_key:
        try:
            async with httpx.AsyncClient(timeout=3.0) as client:
                resp = await client.get(
                    f"{settings.gemini_base_url}/models",
                    params={"key": settings.gemini_api_key, "pageSize": 1},
                )
            provider_results["gemini"] = {
                "status": "UP" if resp.status_code == 200 else "DOWN",
                "httpStatus": resp.status_code,
            }
        except Exception as exc:  # noqa: BLE001
            provider_results["gemini"] = {"status": "DOWN", "error": str(exc)[:200]}
    if settings.openai_api_key:
        try:
            async with httpx.AsyncClient(timeout=3.0) as client:
                resp = await client.get(
                    f"{settings.openai_base_url}/models",
                    headers={"Authorization": f"Bearer {settings.openai_api_key}"},
                )
            provider_results["openai"] = {
                "status": "UP" if resp.status_code == 200 else "DOWN",
                "httpStatus": resp.status_code,
            }
        except Exception as exc:  # noqa: BLE001
            provider_results["openai"] = {"status": "DOWN", "error": str(exc)[:200]}
    if not provider_results:
        return {"status": "DOWN", "error": "no_provider_configured"}
    overall = "UP" if any(p.get("status") == "UP" for p in provider_results.values()) else "DOWN"
    return {"status": overall, "providers": provider_results}
