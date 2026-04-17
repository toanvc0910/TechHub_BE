"""Langfuse v4 observability service.

Langfuse v4 API:
  span = client.start_observation(name=..., as_type='span', input=..., metadata=...)
  gen  = span.start_observation(name=..., as_type='generation', model=..., input=...)
  gen.update(output=..., usage_details={'input': N, 'output': M})
  gen.end()
  span.update(output=...)
  span.end()
  client.flush()
"""
from __future__ import annotations

import logging
from typing import Any

from app.core.config import get_settings

logger = logging.getLogger(__name__)


class LangfuseService:
    def __init__(self) -> None:
        self._client: Any = None
        self._enabled = False

    def init(self) -> None:
        settings = get_settings()
        if not settings.langfuse_enabled or not settings.langfuse_public_key:
            logger.info("Langfuse disabled or no public key configured.")
            return
        try:
            from langfuse import Langfuse
            self._client = Langfuse(
                public_key=settings.langfuse_public_key,
                secret_key=settings.langfuse_secret_key,
                host=settings.langfuse_host,
            )
            self._enabled = True
            logger.info("Langfuse connected to %s", settings.langfuse_host)
        except Exception as exc:
            logger.warning("Langfuse init failed: %s", exc)

    @property
    def enabled(self) -> bool:
        return self._enabled and self._client is not None

    def create_trace(
        self,
        *,
        name: str,
        user_id: str | None = None,
        session_id: str | None = None,
        input: Any = None,
        metadata: dict | None = None,
    ) -> Any:
        """Create a top-level trace span. Returns a LangfuseSpan or NoOp stub."""
        if not self.enabled:
            return _NoOp()
        try:
            meta = dict(metadata or {})
            if user_id:
                meta["user_id"] = user_id
            if session_id:
                meta["session_id"] = session_id
            return self._client.start_observation(
                name=name,
                as_type="span",
                input=input,
                metadata=meta,
            )
        except Exception as exc:
            logger.debug("Langfuse create_trace failed: %s", exc)
            return _NoOp()

    def flush(self) -> None:
        if self._client:
            try:
                self._client.flush()
            except Exception:
                pass

    def shutdown(self) -> None:
        if self._client:
            try:
                self._client.flush()
                self._client.shutdown()
            except Exception:
                pass


class _NoOp:
    """Stub when Langfuse is disabled — all calls are silent no-ops."""
    def start_observation(self, **_: Any) -> "_NoOp":
        return self
    def update(self, **_: Any) -> None:
        pass
    def end(self, **_: Any) -> None:
        pass
    def score(self, **_: Any) -> None:
        pass
    @property
    def trace_id(self) -> str:
        return ""


langfuse_service = LangfuseService()
