from __future__ import annotations

from collections import defaultdict
from datetime import datetime, timezone, timedelta
from typing import Any

from fastapi import APIRouter, Depends, Query, Request

from app.api.dependencies.trusted_context import require_admin_context
from app.core.config import get_settings
from app.core.responses import success_response
from app.schemas.admin import FileUploadedEventRequest, ProviderConfigRequest
from app.services.data_contract import data_contract_registry, summarize_data_contract, validate_data_contract
from app.services.draft_service import draft_service
from app.services.file_context_service import file_context_service
from app.services.langfuse_service import langfuse_service
from app.services.observability_service import runtime_observability_service
from app.services.provider_config import provider_config_service
from app.services.release_readiness_service import release_readiness_service
from app.services.vector_service import vector_service

router = APIRouter(dependencies=[Depends(require_admin_context)])


@router.post("/reindex-courses")
async def reindex_courses(request: Request) -> dict:
    data = await vector_service.reindex_courses()
    return success_response(message="Reindexed courses", data=data, path=request.url.path, status="REINDEX_COMPLETED")


@router.post("/reindex-lessons")
async def reindex_lessons(request: Request) -> dict:
    data = await vector_service.reindex_lessons()
    return success_response(message="Reindexed lessons", data=data, path=request.url.path, status="REINDEX_COMPLETED")


@router.post("/reindex-blogs")
async def reindex_blogs(request: Request) -> dict:
    data = await vector_service.reindex_blogs()
    return success_response(message="Reindexed blogs", data=data, path=request.url.path, status="REINDEX_COMPLETED")


@router.post("/reindex-data-contract")
async def reindex_data_contract(request: Request) -> dict:
    data = await vector_service.reindex_data_contract()
    return success_response(
        message="Reindexed AI data contract",
        data=data,
        path=request.url.path,
        status="REINDEX_COMPLETED",
    )


@router.post("/reindex-all")
async def reindex_all(request: Request) -> dict:
    data = await vector_service.reindex_all()
    return success_response(
        message="Full system reindexing completed successfully",
        data=data,
        path=request.url.path,
        status="REINDEX_COMPLETED",
    )


@router.get("/qdrant-stats")
async def get_qdrant_stats(request: Request) -> dict:
    data = await vector_service.get_collection_stats()
    return success_response(message="Qdrant statistics retrieved", data=data, path=request.url.path)


@router.get("/runtime-stats")
async def get_runtime_stats(request: Request) -> dict:
    data = await runtime_observability_service.snapshot()
    return success_response(message="Runtime AI statistics retrieved", data=data, path=request.url.path)


@router.post("/sweep-stuck-publishing")
async def sweep_stuck_publishing(request: Request, olderThanMinutes: int = 10) -> dict:
    """Reset draft rows stuck at PUBLISHING (orphan after AI service crash mid-publish)."""
    bounded = max(1, min(int(olderThanMinutes), 1440))
    count = await draft_service.sweep_stuck_publishing(older_than_minutes=bounded)
    return success_response(
        message=f"Sweeper reset {count} stuck PUBLISHING row(s).",
        data={"reset": count, "olderThanMinutes": bounded},
        path=request.url.path,
        status="DRAFT_SWEEP_COMPLETED",
    )


@router.get("/release-readiness")
async def get_release_readiness(request: Request) -> dict:
    """Aggregated per-capability status for release decisions (Step 10).

    Combines Qdrant feature readiness, publish counters/timestamps, data-contract
    validation, and ai_generation_tasks audit. Returns a `capabilities` map keyed
    by AI capability with status in:
    CODE_WIRED | FALLBACK_DEMO | REAL_DATA_READY | E2E_VERIFIED | DEGRADED | FAILED.
    """
    data = await release_readiness_service.snapshot()
    return success_response(message="Release readiness snapshot", data=data, path=request.url.path)


@router.get("/data-contract")
async def get_data_contract(request: Request) -> dict:
    data = await summarize_data_contract()
    return success_response(message="AI data contract retrieved", data=data, path=request.url.path)


@router.get("/data-contract/validate")
async def validate_current_data_contract(request: Request) -> dict:
    data = await validate_data_contract()
    return success_response(
        message="AI data contract validation completed",
        data=data,
        path=request.url.path,
        status=data.get("status", "DATA_CONTRACT_VALIDATED"),
    )


@router.post("/data-contract/sync")
async def sync_data_contract(request: Request) -> dict:
    data = await data_contract_registry.sync_static_contract()
    return success_response(
        message="AI data contract synced into PostgreSQL",
        data=data,
        path=request.url.path,
        status=data.get("status", "DATA_CONTRACT_SYNCED"),
    )


@router.get("/provider-config")
async def get_provider_config(request: Request) -> dict:
    provider = await provider_config_service.get_provider()
    metadata = await provider_config_service.get_metadata()
    data = {
        "provider": provider,
        "activeChatModel": await provider_config_service.get_active_chat_model(),
        "activeEmbeddingModel": await provider_config_service.get_active_embedding_model(),
        "models": await provider_config_service.get_current_models(),
        "supportedProviders": list((await provider_config_service.get_supported_chat_models()).keys()),
        "supportedChatModels": await provider_config_service.get_supported_chat_models(),
        "metadata": metadata,
    }
    return success_response(message="AI provider configuration retrieved", data=data, path=request.url.path)


@router.post("/provider-config")
async def update_provider_config(payload: ProviderConfigRequest, request: Request) -> dict:
    if payload.provider and payload.provider not in ("openai", "gemini"):
        raise ValueError("Invalid provider. Supported values: openai, gemini")
    # Accept any model name — real models are fetched from /available-models
    await provider_config_service.update(
        provider=payload.provider,
        chat_model=payload.chatModel,
        embedding_model=payload.embeddingModel,
    )
    return await get_provider_config(request)


@router.get("/provider-health")
async def check_provider_health(request: Request) -> dict:
    """Check if configured LLM providers are reachable (free ping, no quota)."""
    import httpx
    settings = get_settings()
    results: dict[str, Any] = {}

    # Gemini: list 1 model (free)
    if settings.gemini_api_key:
        try:
            async with httpx.AsyncClient(timeout=5) as client:
                resp = await client.get(
                    f"{settings.gemini_base_url}/models",
                    params={"key": settings.gemini_api_key, "pageSize": 1},
                )
                results["gemini"] = {"alive": resp.status_code == 200, "status": resp.status_code}
        except Exception as exc:
            results["gemini"] = {"alive": False, "error": str(exc)}
    else:
        results["gemini"] = {"alive": False, "error": "no_api_key"}

    # OpenAI: list 1 model (free)
    if settings.openai_api_key:
        try:
            async with httpx.AsyncClient(timeout=5) as client:
                resp = await client.get(
                    f"{settings.openai_base_url}/models",
                    headers={"Authorization": f"Bearer {settings.openai_api_key}"},
                )
                results["openai"] = {"alive": resp.status_code == 200, "status": resp.status_code}
        except Exception as exc:
            results["openai"] = {"alive": False, "error": str(exc)}
    else:
        results["openai"] = {"alive": False, "error": "no_api_key"}

    return success_response(message="Provider health check", data=results, path=request.url.path)


@router.post("/ingest-file-uploaded")
async def ingest_file_uploaded(payload: FileUploadedEventRequest, request: Request) -> dict:
    data = await file_context_service.ingest_uploaded_event(payload.model_dump())
    return success_response(message="File upload event ingested", data=data, path=request.url.path)


# ─── Langfuse proxy endpoints ───

@router.get("/langfuse-traces")
async def get_langfuse_traces(
    request: Request,
    page: int = Query(1, ge=1),
    limit: int = Query(50, ge=1, le=200),
) -> dict:
    if not langfuse_service.enabled:
        return success_response(message="Langfuse not configured", data={"traces": [], "total": 0}, path=request.url.path)
    try:
        start_index = (page - 1) * limit
        try:
            result = langfuse_service._client.api.trace.list(page=page, limit=limit)
            page_items = result.data
        except TypeError:
            fetch_limit = min(max(page * limit, limit), 200)
            result = langfuse_service._client.api.trace.list(limit=fetch_limit)
            page_items = result.data[start_index : start_index + limit]

        traces = []
        for t in page_items:
            obs_list = t.observations if isinstance(t.observations, list) else []
            obs_count = len(obs_list)
            models_used = list({str(o.model) for o in obs_list if hasattr(o, "model") and o.model} if isinstance(obs_list, list) and obs_list and hasattr(obs_list[0], "model") else set())
            traces.append({
                "id": t.id,
                "name": t.name,
                "timestamp": str(t.timestamp),
                "latency": t.latency,
                "totalCost": t.total_cost,
                "userId": t.user_id,
                "sessionId": t.session_id,
                "metadata": t.metadata if isinstance(t.metadata, dict) else {},
                "tags": t.tags or [],
                "observationsCount": obs_count,
                "modelsUsed": models_used,
                "input": str(t.input or "")[:200],
                "output": str(t.output or "")[:200],
                "htmlPath": t.html_path,
            })
        return success_response(
            message="Langfuse traces retrieved",
            data={
                "traces": traces,
                "total": result.meta.total_items if hasattr(result.meta, "total_items") else len(traces),
                "page": page,
                "limit": limit,
            },
            path=request.url.path,
        )
    except Exception as exc:
        return success_response(message=f"Langfuse query failed: {exc}", data={"traces": [], "total": 0}, path=request.url.path)


@router.get("/langfuse-trace/{trace_id}")
async def get_langfuse_trace_detail(trace_id: str, request: Request) -> dict:
    if not langfuse_service.enabled:
        raise ValueError("Langfuse not configured")
    detail = langfuse_service._client.api.trace.get(trace_id)
    d = detail.model_dump()
    # Serialize observations
    obs_list = d.get("observations") or []
    observations = []
    for o in obs_list:
        if isinstance(o, dict):
            observations.append(o)
        elif hasattr(o, "model_dump"):
            observations.append(o.model_dump())
        else:
            observations.append({"raw": str(o)})
    d["observations"] = observations
    # Ensure timestamps are strings
    for key in ("timestamp", "createdAt", "updatedAt"):
        if key in d and d[key] is not None:
            d[key] = str(d[key])
    return success_response(message="Langfuse trace detail", data=d, path=request.url.path)


@router.get("/langfuse-analytics")
async def get_langfuse_analytics(request: Request, days: int = Query(7, ge=1, le=30)) -> dict:
    if not langfuse_service.enabled:
        return success_response(message="Langfuse not configured", data=_empty_analytics(), path=request.url.path)
    try:
        result = langfuse_service._client.api.trace.list(limit=100)
        traces = result.data
        cutoff = datetime.now(timezone.utc) - timedelta(days=days)
        filtered = [t for t in traces if t.timestamp and t.timestamp >= cutoff]

        total_cost = sum(t.total_cost or 0 for t in filtered)
        latencies = [t.latency for t in filtered if t.latency is not None]
        avg_latency = round(sum(latencies) / len(latencies), 3) if latencies else 0

        # Cost by day
        cost_by_day: dict[str, dict[str, Any]] = defaultdict(lambda: {"cost": 0, "traces": 0, "tokens": 0})
        intent_dist: dict[str, int] = defaultdict(int)
        model_usage: dict[str, dict[str, Any]] = defaultdict(lambda: {"count": 0, "cost": 0})
        user_usage: dict[str, dict[str, Any]] = defaultdict(lambda: {"traces": 0, "tokens": 0, "cost": 0})
        total_tokens = 0
        errors = 0

        for t in filtered:
            day = str(t.timestamp.date()) if t.timestamp else "unknown"
            cost_by_day[day]["cost"] += t.total_cost or 0
            cost_by_day[day]["traces"] += 1

            meta = t.metadata if isinstance(t.metadata, dict) else {}
            intent = meta.get("intent") or meta.get("name") or t.name or "unknown"
            intent_dist[intent] += 1

            uid = t.user_id or "anonymous"
            user_usage[uid]["traces"] += 1
            user_usage[uid]["cost"] += t.total_cost or 0

            if meta.get("level") == "ERROR":
                errors += 1

            obs_list = t.observations if isinstance(t.observations, list) else []
            for o in obs_list:
                if not hasattr(o, "model"):
                    continue
                if o.model:
                    model_usage[o.model]["count"] += 1
                    model_usage[o.model]["cost"] += o.total_cost or 0
                if hasattr(o, "usage") and o.usage:
                    tokens = getattr(o.usage, "total", 0) or 0
                    total_tokens += tokens
                    cost_by_day[day]["tokens"] += tokens
                    user_usage[uid]["tokens"] += tokens

        data = {
            "totalTraces": len(filtered),
            "totalCost": round(total_cost, 6),
            "avgLatency": avg_latency,
            "totalTokens": total_tokens,
            "errorRate": round(errors / max(len(filtered), 1), 4),
            "costByDay": [{"date": k, **v} for k, v in sorted(cost_by_day.items())],
            "intentDistribution": [{"intent": k, "count": v} for k, v in sorted(intent_dist.items(), key=lambda x: -x[1])],
            "modelUsage": [{"model": k, **v} for k, v in sorted(model_usage.items(), key=lambda x: -x[1]["count"])],
            "topUsers": sorted(
                [{"userId": k, **v} for k, v in user_usage.items()],
                key=lambda x: -x["traces"],
            )[:20],
            "days": days,
        }
        return success_response(message="Langfuse analytics", data=data, path=request.url.path)
    except Exception as exc:
        return success_response(message=f"Langfuse analytics failed: {exc}", data=_empty_analytics(), path=request.url.path)


@router.get("/available-models")
async def get_available_models(request: Request) -> dict:
    """Fetch real available models from Gemini and OpenAI APIs.
    Uses list-models endpoints which are FREE (no quota consumed)."""
    settings = get_settings()
    models: dict[str, list[dict]] = {"gemini": [], "openai": []}

    # Gemini: GET /v1beta/models?key=...
    if settings.gemini_api_key:
        try:
            import httpx
            async with httpx.AsyncClient(timeout=10) as client:
                resp = await client.get(
                    f"{settings.gemini_base_url}/models",
                    params={"key": settings.gemini_api_key},
                )
                if resp.status_code == 200:
                    data = resp.json()
                    for m in data.get("models", []):
                        name = m.get("name", "").replace("models/", "")
                        # Only include generateContent-capable models
                        methods = m.get("supportedGenerationMethods", [])
                        if "generateContent" in methods or "embedContent" in methods:
                            models["gemini"].append({
                                "id": name,
                                "name": m.get("displayName", name),
                                "type": "embedding" if "embedContent" in methods and "generateContent" not in methods else "chat",
                                "inputTokenLimit": m.get("inputTokenLimit"),
                                "outputTokenLimit": m.get("outputTokenLimit"),
                            })
        except Exception:
            pass

    # OpenAI: GET /v1/models
    if settings.openai_api_key:
        try:
            import httpx
            async with httpx.AsyncClient(timeout=10) as client:
                resp = await client.get(
                    f"{settings.openai_base_url}/models",
                    headers={"Authorization": f"Bearer {settings.openai_api_key}"},
                )
                if resp.status_code == 200:
                    data = resp.json()
                    for m in data.get("data", []):
                        model_id = m.get("id", "")
                        if not model_id:
                            continue
                        is_embedding = "embedding" in model_id or "bge" in model_id or "e5" in model_id
                        models["openai"].append({
                            "id": model_id,
                            "name": model_id,
                            "type": "embedding" if is_embedding else "chat",
                        })
        except Exception:
            pass

    # Sort: chat models first, then embedding
    for provider in models:
        models[provider].sort(key=lambda x: (0 if x["type"] == "chat" else 1, x["id"]))

    return success_response(
        message="Available models from configured API keys",
        data={
            "providers": {
                p: {"available": bool(models[p]), "models": models[p]}
                for p in models
            }
        },
        path=request.url.path,
    )


def _empty_analytics() -> dict:
    return {
        "totalTraces": 0, "totalCost": 0, "avgLatency": 0, "totalTokens": 0,
        "errorRate": 0, "costByDay": [], "intentDistribution": [],
        "modelUsage": [], "topUsers": [], "days": 0,
    }
