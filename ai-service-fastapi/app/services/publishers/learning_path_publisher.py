"""
Publishes a validated AI learning-path draft to Learning Path Service.

Two-step HTTP flow against the Java service:

  1. POST {base}/api/v1/learning-paths      (LearningPathRequestDTO)
  2. POST {base}/api/v1/learning-paths/{id}/courses  (AddCoursesToPathRequestDTO)

The publisher records the outcome on the draft so callers can distinguish
APPROVED (no publish attempted) from PUBLISHED / PUBLISH_FAILED, including
the partial path ID when step 2 fails after step 1 already created the
record (so an admin can manually reconcile or retry).

Trusted identity flows as `X-User-Id` / `X-User-Email` / `X-User-Roles`
headers so Learning Path Service can attach createdBy correctly. The
publisher never invents a user — when no trusted ID is provided we return
PUBLISH_FAILED rather than letting the upstream attribute the path to
the wrong actor.
"""

from __future__ import annotations

import logging
from dataclasses import dataclass
from datetime import datetime, timezone
from enum import Enum
from typing import Any

import httpx

from app.core.config import get_settings
from app.services.observability_service import runtime_observability_service
from app.services.publishers.learning_path_validator import (
    LearningPathValidationError,
    ValidatedPath,
    learning_path_validator,
)

logger = logging.getLogger(__name__)


class LearningPathPublishStatus(str, Enum):
    PUBLISHED = "PUBLISHED"
    PUBLISH_FAILED = "PUBLISH_FAILED"
    SKIPPED_NO_BASE_URL = "SKIPPED_NO_BASE_URL"


@dataclass
class LearningPathPublishOutcome:
    status: LearningPathPublishStatus
    learning_path_id: str | None
    error: str | None
    request_payload: dict[str, Any]
    response_payload: dict[str, Any] | None
    started_at: str
    finished_at: str

    def to_dict(self) -> dict[str, Any]:
        return {
            "status": self.status.value,
            "learningPathId": self.learning_path_id,
            "error": self.error,
            "requestPayload": self.request_payload,
            "responsePayload": self.response_payload,
            "startedAt": self.started_at,
            "finishedAt": self.finished_at,
        }


class LearningPathPublisher:
    def __init__(self) -> None:
        self._settings = get_settings()

    async def publish(
        self,
        draft: dict[str, Any],
        *,
        trusted_user_id: str | None,
        trusted_user_email: str | None = None,
        trusted_roles: list[str] | None = None,
        http_client: httpx.AsyncClient | None = None,
    ) -> LearningPathPublishOutcome:
        started = datetime.now(timezone.utc).isoformat()

        try:
            validated = await learning_path_validator.validate(draft)
        except LearningPathValidationError as exc:
            await runtime_observability_service.record_error(scope="learning_path_publish_validation")
            return await self._failure(
                started_at=started,
                error=f"validation_failed: {exc}",
                learning_path_id=None,
                request_payload={"raw_title": draft.get("title")},
                response_payload=None,
            )

        request_payload = self._build_request(validated, trusted_user_id=trusted_user_id)
        if not self._settings.learning_path_service_base_url:
            logger.warning("LEARNING_PATH_SERVICE_BASE_URL not configured; skipping publish.")
            return LearningPathPublishOutcome(
                status=LearningPathPublishStatus.SKIPPED_NO_BASE_URL,
                learning_path_id=None,
                error=None,
                request_payload=request_payload,
                response_payload=None,
                started_at=started,
                finished_at=datetime.now(timezone.utc).isoformat(),
            )

        if not trusted_user_id:
            await runtime_observability_service.record_error(scope="learning_path_publish_no_identity")
            return await self._failure(
                started_at=started,
                error="missing trusted user identity; refusing to attribute path to unknown actor",
                learning_path_id=None,
                request_payload=request_payload,
                response_payload=None,
            )

        owns_client = http_client is None
        client = http_client or httpx.AsyncClient(
            timeout=self._settings.learning_path_service_timeout_seconds,
        )
        try:
            headers = self._auth_headers(
                trusted_user_id=trusted_user_id,
                trusted_user_email=trusted_user_email,
                trusted_roles=trusted_roles,
            )
            create_url = f"{self._settings.learning_path_service_base_url.rstrip('/')}/api/v1/learning-paths"
            create_response = await client.post(create_url, headers=headers, json=request_payload)
            if create_response.status_code >= 400:
                return await self._failure(
                    started_at=started,
                    error=f"create failed: HTTP {create_response.status_code} {create_response.text[:300]}",
                    learning_path_id=None,
                    request_payload=request_payload,
                    response_payload={"create": create_response.text},
                )
            create_body = self._parse_json(create_response)
            learning_path_id = self._extract_id(create_body)
            if not learning_path_id:
                return await self._failure(
                    started_at=started,
                    error="create returned no learning path id",
                    learning_path_id=None,
                    request_payload=request_payload,
                    response_payload={"create": create_body},
                )

            add_courses_payload = {"courses": self._courses_for_add(validated)}
            add_url = f"{create_url}/{learning_path_id}/courses"
            add_response = await client.post(add_url, headers=headers, json=add_courses_payload)
            if add_response.status_code >= 400:
                return await self._failure(
                    started_at=started,
                    error=(
                        f"addCourses failed: HTTP {add_response.status_code} {add_response.text[:300]}; "
                        "path was created but courses were not attached"
                    ),
                    learning_path_id=learning_path_id,
                    request_payload={"create": request_payload, "addCourses": add_courses_payload},
                    response_payload={
                        "create": create_body,
                        "addCourses": add_response.text,
                    },
                )
            add_body = self._parse_json(add_response)
            finished = datetime.now(timezone.utc).isoformat()
            await runtime_observability_service.record_vector_operation(
                operation="publish_learning_path",
                duration_ms=0.0,
                success=True,
                collection=None,
                count=len(validated.courses),
            )
            await runtime_observability_service.record_publish_event(
                kind="learning_path",
                status="PUBLISHED",
                target_id=learning_path_id,
            )
            return LearningPathPublishOutcome(
                status=LearningPathPublishStatus.PUBLISHED,
                learning_path_id=learning_path_id,
                error=None,
                request_payload={"create": request_payload, "addCourses": add_courses_payload},
                response_payload={"create": create_body, "addCourses": add_body},
                started_at=started,
                finished_at=finished,
            )
        except httpx.HTTPError as exc:
            await runtime_observability_service.record_error(scope="learning_path_publish_http")
            return await self._failure(
                started_at=started,
                error=f"http error: {type(exc).__name__}: {exc}",
                learning_path_id=None,
                request_payload=request_payload,
                response_payload=None,
            )
        finally:
            if owns_client:
                await client.aclose()

    @staticmethod
    def _build_request(validated: ValidatedPath, *, trusted_user_id: str | None) -> dict[str, Any]:
        return {
            "title": validated.title,
            "description": validated.description,
            "skills": list(validated.skills),
            "layoutEdges": [
                {"source": edge["source"], "target": edge["target"]}
                for edge in validated.layout_edges
            ],
            "createdBy": trusted_user_id,
            "updatedBy": trusted_user_id,
        }

    @staticmethod
    def _courses_for_add(validated: ValidatedPath) -> list[dict[str, Any]]:
        return [
            {
                "courseId": course["courseId"],
                "order": course["order"],
                "positionX": course["positionX"],
                "positionY": course["positionY"],
                "isOptional": course["isOptional"],
                "title": course["title"],
                "description": course["description"],
                "thumbnail": course.get("thumbnail"),
            }
            for course in validated.courses
        ]

    @staticmethod
    def _auth_headers(
        *,
        trusted_user_id: str | None,
        trusted_user_email: str | None,
        trusted_roles: list[str] | None,
    ) -> dict[str, str]:
        headers: dict[str, str] = {
            "Content-Type": "application/json",
            "X-Request-Source": "ai-service",
        }
        if trusted_user_id:
            headers["X-User-Id"] = trusted_user_id
        if trusted_user_email:
            headers["X-User-Email"] = trusted_user_email
        if trusted_roles:
            headers["X-User-Roles"] = ",".join(role for role in trusted_roles if role)
        return headers

    @staticmethod
    def _parse_json(response: httpx.Response) -> dict[str, Any] | None:
        try:
            return response.json()
        except Exception:
            return None

    @staticmethod
    def _extract_id(body: dict[str, Any] | None) -> str | None:
        if not isinstance(body, dict):
            return None
        data = body.get("data") if isinstance(body.get("data"), dict) else body
        if isinstance(data, dict):
            for key in ("id", "learningPathId", "pathId"):
                value = data.get(key)
                if value:
                    return str(value)
        return None

    async def _failure(
        self,
        *,
        started_at: str,
        error: str,
        learning_path_id: str | None,
        request_payload: dict[str, Any],
        response_payload: dict[str, Any] | None,
    ) -> LearningPathPublishOutcome:
        finished = datetime.now(timezone.utc).isoformat()
        await runtime_observability_service.record_publish_event(
            kind="learning_path",
            status="PUBLISH_FAILED",
            target_id=learning_path_id,
            error=error,
        )
        return LearningPathPublishOutcome(
            status=LearningPathPublishStatus.PUBLISH_FAILED,
            learning_path_id=learning_path_id,
            error=error,
            request_payload=request_payload,
            response_payload=response_payload,
            started_at=started_at,
            finished_at=finished,
        )


learning_path_publisher = LearningPathPublisher()
