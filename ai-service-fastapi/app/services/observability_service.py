from __future__ import annotations

import asyncio
import json
import logging
from collections import Counter, deque
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from statistics import mean
from typing import Any

from app.core.config import get_settings

# Structured logger for metrics — can be collected by Langfuse, OpenTelemetry, or any JSON log sink
metrics_logger = logging.getLogger("ai.metrics")
metrics_logger.setLevel(logging.INFO)


@dataclass(slots=True)
class ChatRunMetric:
    request_id: str | None
    pipeline: str
    intent: str
    success: bool
    duration_ms: float
    awaiting_approval: bool
    fallback_reason: str | None = None
    tokens_used: int = 0


@dataclass(slots=True)
class ProviderMetric:
    kind: str
    requested_provider: str
    effective_provider: str
    model: str
    used_mock: bool
    fallback_used: bool


@dataclass(slots=True)
class VectorMetric:
    operation: str
    collection: str | None
    success: bool
    duration_ms: float
    count: int = 0


class RuntimeObservabilityService:
    def __init__(self) -> None:
        settings = get_settings()
        self._lock = asyncio.Lock()
        self._chat_runs: deque[ChatRunMetric] = deque(maxlen=settings.runtime_metrics_window)
        self._provider_events: deque[ProviderMetric] = deque(maxlen=settings.runtime_metrics_window * 2)
        self._vector_events: deque[VectorMetric] = deque(maxlen=settings.runtime_metrics_window * 2)
        self._counters: Counter[str] = Counter()

    async def record_chat_run(
        self,
        *,
        request_id: str | None,
        pipeline: str,
        intent: str,
        success: bool,
        duration_ms: float,
        awaiting_approval: bool,
        fallback_reason: str | None = None,
        tokens_used: int = 0,
    ) -> None:
        async with self._lock:
            self._chat_runs.append(
                ChatRunMetric(
                    request_id=request_id,
                    pipeline=pipeline,
                    intent=intent,
                    success=success,
                    duration_ms=duration_ms,
                    awaiting_approval=awaiting_approval,
                    fallback_reason=fallback_reason,
                    tokens_used=max(tokens_used, 0),
                )
            )
            self._counters["chat_total"] += 1
            if success:
                self._counters["chat_success"] += 1
            else:
                self._counters["chat_failed"] += 1
            if pipeline == "legacy":
                self._counters["chat_legacy_total"] += 1
            if awaiting_approval:
                self._counters["chat_hitl_total"] += 1
            if fallback_reason:
                self._counters[f"chat_fallback:{fallback_reason}"] += 1
            if tokens_used:
                self._counters["tokens:chat_total"] += max(tokens_used, 0)
        # Structured log for external collection
        metrics_logger.info(json.dumps({
            "type": "chat_run",
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "request_id": request_id,
            "pipeline": pipeline,
            "intent": intent,
            "success": success,
            "duration_ms": round(duration_ms, 2),
            "awaiting_approval": awaiting_approval,
            "fallback_reason": fallback_reason,
            "tokens_used": tokens_used,
        }))

    async def record_provider_event(
        self,
        *,
        kind: str,
        requested_provider: str,
        effective_provider: str,
        model: str,
        used_mock: bool,
    ) -> None:
        fallback_used = requested_provider != effective_provider or used_mock
        async with self._lock:
            self._provider_events.append(
                ProviderMetric(
                    kind=kind,
                    requested_provider=requested_provider,
                    effective_provider=effective_provider,
                    model=model,
                    used_mock=used_mock,
                    fallback_used=fallback_used,
                )
            )
            self._counters[f"provider:{kind}:requested:{requested_provider}"] += 1
            self._counters[f"provider:{kind}:effective:{effective_provider}"] += 1
            if used_mock:
                self._counters[f"provider:{kind}:mock"] += 1
            if fallback_used:
                self._counters[f"provider:{kind}:fallback"] += 1

    async def record_token_usage(
        self,
        *,
        request_id: str | None,
        scope: str,
        provider: str,
        model: str,
        prompt_tokens: int,
        completion_tokens: int,
        embedding_tokens: int = 0,
    ) -> None:
        total = max(prompt_tokens, 0) + max(completion_tokens, 0) + max(embedding_tokens, 0)
        async with self._lock:
            self._counters["tokens:prompt_total"] += max(prompt_tokens, 0)
            self._counters["tokens:completion_total"] += max(completion_tokens, 0)
            self._counters["tokens:embedding_total"] += max(embedding_tokens, 0)
            self._counters["tokens:total"] += total
            self._counters[f"tokens:scope:{scope}"] += total
            self._counters[f"tokens:provider:{provider}"] += total
            self._counters[f"tokens:model:{model}"] += total
            if request_id:
                self._counters[f"tokens:request:{request_id}"] += total
        metrics_logger.info(json.dumps({
            "type": "token_usage",
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "request_id": request_id,
            "scope": scope,
            "provider": provider,
            "model": model,
            "prompt_tokens": prompt_tokens,
            "completion_tokens": completion_tokens,
            "embedding_tokens": embedding_tokens,
            "total": total,
        }))

    async def record_vector_operation(
        self,
        *,
        operation: str,
        duration_ms: float,
        success: bool,
        collection: str | None = None,
        count: int = 0,
    ) -> None:
        async with self._lock:
            self._vector_events.append(
                VectorMetric(
                    operation=operation,
                    collection=collection,
                    success=success,
                    duration_ms=duration_ms,
                    count=count,
                )
            )
            self._counters[f"vector:{operation}:total"] += 1
            if success:
                self._counters[f"vector:{operation}:success"] += 1
            else:
                self._counters[f"vector:{operation}:failed"] += 1
            if count:
                self._counters[f"vector:{operation}:count"] += count
        metrics_logger.info(json.dumps({
            "type": "vector_operation",
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "operation": operation,
            "collection": collection,
            "success": success,
            "duration_ms": round(duration_ms, 2),
            "count": count,
        }))

    async def record_file_ingestion(
        self,
        *,
        files_total: int,
        hydrated: int,
        chunks_indexed: int,
        unsupported: int,
    ) -> None:
        async with self._lock:
            self._counters["files:seen"] += files_total
            self._counters["files:hydrated"] += hydrated
            self._counters["files:chunks_indexed"] += chunks_indexed
            self._counters["files:unsupported"] += unsupported

    async def record_error(self, *, scope: str) -> None:
        async with self._lock:
            self._counters[f"errors:{scope}"] += 1

    async def snapshot(self) -> dict[str, Any]:
        async with self._lock:
            chat_runs = list(self._chat_runs)
            provider_events = list(self._provider_events)
            vector_events = list(self._vector_events)
            counters = dict(self._counters)

        chat_latencies = [item.duration_ms for item in chat_runs]
        vector_latencies = [item.duration_ms for item in vector_events]
        return {
            "overview": {
                "chatTotal": counters.get("chat_total", 0),
                "chatSuccess": counters.get("chat_success", 0),
                "chatFailed": counters.get("chat_failed", 0),
                "chatLegacyTotal": counters.get("chat_legacy_total", 0),
                "chatHitlTotal": counters.get("chat_hitl_total", 0),
                "mockChatResponses": counters.get("provider:chat:mock", 0),
                "mockEmbeddingCalls": counters.get("provider:embedding:mock", 0),
                "totalTokens": counters.get("tokens:total", 0),
            },
            "latency": {
                "chatAverageMs": round(mean(chat_latencies), 2) if chat_latencies else 0.0,
                "chatP95Ms": round(self._percentile(chat_latencies, 95), 2) if chat_latencies else 0.0,
                "vectorAverageMs": round(mean(vector_latencies), 2) if vector_latencies else 0.0,
            },
            "tokens": {
                "promptTotal": counters.get("tokens:prompt_total", 0),
                "completionTotal": counters.get("tokens:completion_total", 0),
                "embeddingTotal": counters.get("tokens:embedding_total", 0),
                "averagePerChat": round(
                    counters.get("tokens:chat_total", 0) / max(counters.get("chat_total", 1), 1),
                    2,
                ),
            },
            "providers": {
                "chatFallbacks": counters.get("provider:chat:fallback", 0),
                "embeddingFallbacks": counters.get("provider:embedding:fallback", 0),
                "recent": [asdict(item) for item in provider_events[-10:]],
            },
            "files": {
                "filesSeen": counters.get("files:seen", 0),
                "filesHydrated": counters.get("files:hydrated", 0),
                "chunksIndexed": counters.get("files:chunks_indexed", 0),
                "unsupportedFiles": counters.get("files:unsupported", 0),
            },
            "vectorOps": {
                "recent": [asdict(item) for item in vector_events[-10:]],
            },
            "chatRuns": {
                "recent": [asdict(item) for item in chat_runs[-10:]],
            },
            "counters": counters,
        }

    @staticmethod
    def _percentile(values: list[float], percentile: int) -> float:
        if not values:
            return 0.0
        ordered = sorted(values)
        index = max(0, min(len(ordered) - 1, round((percentile / 100) * (len(ordered) - 1))))
        return ordered[index]


runtime_observability_service = RuntimeObservabilityService()
