"""
Publishes a validated AI exercise batch to Course Service.

HTTP flow:

  POST {base}/api/courses/{courseId}/lessons/{lessonId}/exercises
    body: List<ExerciseRequest>
    field shape:
      - type: ExerciseType enum (MULTIPLE_CHOICE | CODING | OPEN_ENDED)
      - question: String
      - options: Object (MCQ -> list of {text, correct}; ESSAY -> {rubric: [...]})
      - orderIndex: Integer
      - testCases: List<ExerciseTestCaseDto> (CODING only)

Trusted identity flows as `X-User-Id` / `X-User-Email` / `X-User-Roles` so
Course Service can attribute createdBy correctly. When no trusted user id is
provided AND a base URL is configured, the publisher refuses to publish
rather than letting Course Service attach the exercise to an unknown actor.
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
from app.services.publishers.exercise_validator import (
    ExerciseValidationError,
    ValidatedExerciseBatch,
    exercise_validator,
)

logger = logging.getLogger(__name__)


class ExercisePublishStatus(str, Enum):
    PUBLISHED = "PUBLISHED"
    PUBLISH_FAILED = "PUBLISH_FAILED"
    SKIPPED_NO_BASE_URL = "SKIPPED_NO_BASE_URL"


@dataclass
class ExercisePublishOutcome:
    status: ExercisePublishStatus
    created_exercise_ids: list[str]
    error: str | None
    request_payload: list[dict[str, Any]] | None
    response_payload: Any | None
    started_at: str
    finished_at: str

    def to_dict(self) -> dict[str, Any]:
        return {
            "status": self.status.value,
            "createdExerciseIds": list(self.created_exercise_ids),
            "error": self.error,
            "requestPayload": self.request_payload,
            "responsePayload": self.response_payload,
            "startedAt": self.started_at,
            "finishedAt": self.finished_at,
        }


class ExercisePublisher:
    def __init__(self) -> None:
        self._settings = get_settings()

    async def publish(
        self,
        *,
        course_id: str,
        lesson_id: str,
        payload: dict[str, Any],
        trusted_user_id: str | None,
        trusted_user_email: str | None = None,
        trusted_roles: list[str] | None = None,
        http_client: httpx.AsyncClient | None = None,
    ) -> ExercisePublishOutcome:
        started = datetime.now(timezone.utc).isoformat()

        try:
            batch = await exercise_validator.validate(
                course_id=course_id,
                lesson_id=lesson_id,
                payload=payload,
            )
        except ExerciseValidationError as exc:
            await runtime_observability_service.record_error(scope="exercise_publish_validation")
            return await self._failure(
                started_at=started,
                error=f"validation_failed: {exc}",
                created_exercise_ids=[],
                request_payload=None,
                response_payload=None,
            )

        request_payload = self._build_request(batch)
        if not self._settings.course_service_base_url:
            logger.warning("COURSE_SERVICE_BASE_URL not configured; skipping publish.")
            return ExercisePublishOutcome(
                status=ExercisePublishStatus.SKIPPED_NO_BASE_URL,
                created_exercise_ids=[],
                error=None,
                request_payload=request_payload,
                response_payload=None,
                started_at=started,
                finished_at=datetime.now(timezone.utc).isoformat(),
            )

        if not trusted_user_id:
            await runtime_observability_service.record_error(scope="exercise_publish_no_identity")
            return await self._failure(
                started_at=started,
                error="missing trusted user identity; refusing to attribute exercises to unknown actor",
                created_exercise_ids=[],
                request_payload=request_payload,
                response_payload=None,
            )

        owns_client = http_client is None
        client = http_client or httpx.AsyncClient(
            timeout=self._settings.course_service_timeout_seconds,
        )
        try:
            headers = self._auth_headers(
                trusted_user_id=trusted_user_id,
                trusted_user_email=trusted_user_email,
                trusted_roles=trusted_roles,
            )
            base = self._settings.course_service_base_url.rstrip("/")
            url = f"{base}/api/courses/{batch.course_id}/lessons/{batch.lesson_id}/exercises"
            response = await client.post(url, headers=headers, json=request_payload)
            if response.status_code >= 400:
                return await self._failure(
                    started_at=started,
                    error=f"HTTP {response.status_code}: {response.text[:300]}",
                    created_exercise_ids=[],
                    request_payload=request_payload,
                    response_payload=response.text,
                )
            body = self._parse_json(response)
            created_ids = self._extract_ids(body)
            await runtime_observability_service.record_vector_operation(
                operation="publish_exercises",
                duration_ms=0.0,
                success=True,
                collection=None,
                count=len(created_ids) or len(request_payload),
            )
            await runtime_observability_service.record_publish_event(
                kind="exercise",
                status="PUBLISHED",
                target_id=",".join(created_ids) if created_ids else None,
            )
            return ExercisePublishOutcome(
                status=ExercisePublishStatus.PUBLISHED,
                created_exercise_ids=created_ids,
                error=None,
                request_payload=request_payload,
                response_payload=body,
                started_at=started,
                finished_at=datetime.now(timezone.utc).isoformat(),
            )
        except httpx.HTTPError as exc:
            await runtime_observability_service.record_error(scope="exercise_publish_http")
            return await self._failure(
                started_at=started,
                error=f"http error: {type(exc).__name__}: {exc}",
                created_exercise_ids=[],
                request_payload=request_payload,
                response_payload=None,
            )
        finally:
            if owns_client:
                await client.aclose()

    @staticmethod
    def _build_request(batch: ValidatedExerciseBatch) -> list[dict[str, Any]]:
        out: list[dict[str, Any]] = []
        for ex in batch.exercises:
            entry: dict[str, Any] = {
                "type": ex.type,
                "question": ex.question,
                "orderIndex": ex.order_index,
            }
            if ex.options is not None:
                entry["options"] = ex.options
            if ex.test_cases:
                entry["testCases"] = ex.test_cases
            out.append(entry)
        return out

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
    def _parse_json(response: httpx.Response) -> Any | None:
        try:
            return response.json()
        except Exception:
            return None

    @staticmethod
    def _extract_ids(body: Any) -> list[str]:
        # Course Service wraps in GlobalResponse: {data: [{id: ...}, ...]}
        if isinstance(body, dict):
            data = body.get("data") if "data" in body else body
        else:
            data = body
        ids: list[str] = []
        if isinstance(data, list):
            for item in data:
                if isinstance(item, dict) and item.get("id"):
                    ids.append(str(item["id"]))
        elif isinstance(data, dict) and data.get("id"):
            ids.append(str(data["id"]))
        return ids

    async def _failure(
        self,
        *,
        started_at: str,
        error: str,
        created_exercise_ids: list[str],
        request_payload: list[dict[str, Any]] | None,
        response_payload: Any | None,
    ) -> ExercisePublishOutcome:
        await runtime_observability_service.record_publish_event(
            kind="exercise",
            status="PUBLISH_FAILED",
            target_id=",".join(created_exercise_ids) if created_exercise_ids else None,
            error=error,
        )
        return ExercisePublishOutcome(
            status=ExercisePublishStatus.PUBLISH_FAILED,
            created_exercise_ids=created_exercise_ids,
            error=error,
            request_payload=request_payload,
            response_payload=response_payload,
            started_at=started_at,
            finished_at=datetime.now(timezone.utc).isoformat(),
        )


exercise_publisher = ExercisePublisher()
