"""
JSON structured logging + correlation ID propagation.

Standard library `logging` configured with a JSON formatter so log aggregators
(ELK, Loki, Cloud Logging) can parse fields without regex. Every log record
carries a `request_id` field if available, sourced from a `ContextVar` set by
the request-ID middleware. Background tasks (Kafka consumer, sweepers) emit
records with `request_id=None`.

Public surface:

  - `configure_logging(level)` — call once at startup before any logger.info.
  - `request_id_ctx` — ContextVar holding the current request's ID.
  - `RequestIdMiddleware` — ASGI middleware: reads `X-Request-Id` from
    incoming request (or generates a new UUID), binds it to `request_id_ctx`,
    and echoes it in the response so the gateway can stitch traces.
"""

from __future__ import annotations

import contextvars
import json
import logging
import sys
import uuid
from datetime import datetime, timezone
from typing import Any

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request

request_id_ctx: contextvars.ContextVar[str | None] = contextvars.ContextVar(
    "request_id", default=None
)


class JsonFormatter(logging.Formatter):
    """Minimal JSON formatter; never raises on weird record attrs."""

    _RESERVED = {
        "args", "asctime", "created", "exc_info", "exc_text", "filename",
        "funcName", "levelname", "levelno", "lineno", "message", "module",
        "msecs", "msg", "name", "pathname", "process", "processName",
        "relativeCreated", "stack_info", "thread", "threadName",
    }

    def format(self, record: logging.LogRecord) -> str:  # noqa: D401
        payload: dict[str, Any] = {
            "ts": datetime.now(timezone.utc).isoformat(),
            "level": record.levelname,
            "logger": record.name,
            "msg": record.getMessage(),
        }
        try:
            req_id = request_id_ctx.get()
        except LookupError:
            req_id = None
        if req_id:
            payload["request_id"] = req_id

        if record.exc_info:
            payload["exc"] = self.formatException(record.exc_info)

        # Surface any `logger.info("...", extra={...})` keys.
        for key, value in record.__dict__.items():
            if key in self._RESERVED or key.startswith("_"):
                continue
            try:
                json.dumps(value)
                payload[key] = value
            except (TypeError, ValueError):
                payload[key] = repr(value)
        try:
            return json.dumps(payload, ensure_ascii=False)
        except Exception:
            return json.dumps({"ts": payload["ts"], "msg": str(payload.get("msg"))})


def configure_logging(level: str = "INFO") -> None:
    """Replace the root logger handlers with a single JSON-emitting stream
    handler. Idempotent — safe to call from app startup AND from tests.
    """
    root = logging.getLogger()
    # Reset handlers so basicConfig invoked elsewhere doesn't leave a plain
    # formatter in front of ours.
    for handler in list(root.handlers):
        root.removeHandler(handler)
    handler = logging.StreamHandler(stream=sys.stdout)
    handler.setFormatter(JsonFormatter())
    root.addHandler(handler)
    try:
        root.setLevel(getattr(logging, level.upper(), logging.INFO))
    except Exception:
        root.setLevel(logging.INFO)


class RequestIdMiddleware(BaseHTTPMiddleware):
    """Bind a request ID for every HTTP request.

    - If the caller sent `X-Request-Id`, reuse it (so upstream gateway traces
      are preserved). Length-capped to 64 chars to prevent log injection.
    - Otherwise generate a fresh UUID4.
    - Echoed back as `X-Request-Id` response header.
    """

    async def dispatch(self, request: Request, call_next):
        incoming = request.headers.get("x-request-id") or ""
        request_id = incoming.strip()[:64] if incoming.strip() else uuid.uuid4().hex
        token = request_id_ctx.set(request_id)
        try:
            response = await call_next(request)
        finally:
            request_id_ctx.reset(token)
        response.headers["X-Request-Id"] = request_id
        return response
