"""
Prometheus `/metrics` endpoint + lightweight HTTP request instrumentation.

Counters exposed:
  ai_http_requests_total{method,path_template,status_code}
  ai_http_request_duration_seconds{method,path_template} (histogram)

Counter names that already live in `observability_service` (publish events,
vector ops, fallbacks) are not duplicated here — production deployments
should keep that data in their own dashboards via `/api/ai/admin/runtime-stats`.
What this module adds is the standard /metrics format Prometheus expects.
"""

from __future__ import annotations

import time

from fastapi import APIRouter, Request, Response
from prometheus_client import CONTENT_TYPE_LATEST, Counter, Histogram, generate_latest
from starlette.middleware.base import BaseHTTPMiddleware

router = APIRouter()

HTTP_REQUESTS = Counter(
    "ai_http_requests_total",
    "Total HTTP requests handled by AI service.",
    labelnames=("method", "path", "status"),
)

HTTP_DURATION = Histogram(
    "ai_http_request_duration_seconds",
    "Latency of HTTP requests handled by AI service.",
    labelnames=("method", "path"),
    buckets=(0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0, 30.0),
)


class PrometheusMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        start = time.perf_counter()
        # Use the matched route path so we don't explode label cardinality
        # with every dynamic UUID. Fallback to the raw path if no route
        # matched (e.g. 404).
        route = request.scope.get("route")
        path_template = getattr(route, "path", request.url.path)
        method = request.method
        try:
            response = await call_next(request)
            status_code = str(response.status_code)
            return response
        except Exception:
            status_code = "500"
            raise
        finally:
            HTTP_DURATION.labels(method=method, path=path_template).observe(
                time.perf_counter() - start
            )
            HTTP_REQUESTS.labels(method=method, path=path_template, status=status_code).inc()


@router.get("/metrics", include_in_schema=False)
async def metrics() -> Response:
    return Response(content=generate_latest(), media_type=CONTENT_TYPE_LATEST)
