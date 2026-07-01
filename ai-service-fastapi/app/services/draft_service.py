from __future__ import annotations

import logging
from datetime import datetime, timedelta, timezone
from typing import Any
from uuid import UUID

from sqlalchemy import select, update

from app.core.enums import AiTaskStatus, AiTaskType
from app.db.models import AiGenerationTaskModel
from app.db.session import get_db_session
from app.schemas.drafts import (
    ApproveExerciseDraftResponse,
    ApproveLearningPathDraftResponse,
    DraftListItem,
)
from app.services.publishers import (
    ExercisePublishStatus,
    LearningPathPublishStatus,
    exercise_publisher,
    learning_path_publisher,
)

logger = logging.getLogger(__name__)


class DraftConcurrencyError(ValueError):
    """Raised when atomic claim of a draft fails (already being published)."""


class DraftService:
    async def get_exercise_drafts(self, lesson_id: UUID) -> list[DraftListItem]:
        return await self._list_by_target(str(lesson_id), AiTaskType.EXERCISE_GENERATION)

    async def get_exercise_drafts_batch(self, lesson_ids: list[str]) -> list[DraftListItem]:
        async with get_db_session() as session:
            result = await session.execute(
                select(AiGenerationTaskModel)
                .where(AiGenerationTaskModel.target_reference.in_(lesson_ids))
                .where(AiGenerationTaskModel.status == AiTaskStatus.DRAFT.value)
                .where(AiGenerationTaskModel.task_type == AiTaskType.EXERCISE_GENERATION.value)
                .order_by(AiGenerationTaskModel.created.desc())
            )
            return [self._to_item(item) for item in result.scalars().all()]

    async def get_latest_exercise_draft(self, lesson_id: UUID) -> DraftListItem:
        items = await self.get_exercise_drafts(lesson_id)
        if not items:
            raise ValueError("No draft found for lesson.")
        return items[0]

    async def get_learning_path_drafts(self) -> list[DraftListItem]:
        async with get_db_session() as session:
            result = await session.execute(
                select(AiGenerationTaskModel)
                .where(AiGenerationTaskModel.task_type == AiTaskType.LEARNING_PATH_GENERATION.value)
                .where(
                    AiGenerationTaskModel.status.in_(
                        [
                            AiTaskStatus.DRAFT.value,
                            AiTaskStatus.APPROVED.value,
                        ]
                    )
                )
                .order_by(AiGenerationTaskModel.created.desc())
            )
            return [self._to_item(item) for item in result.scalars().all()]

    async def get_draft_by_id(self, task_id: UUID) -> DraftListItem:
        async with get_db_session() as session:
            result = await session.execute(select(AiGenerationTaskModel).where(AiGenerationTaskModel.id == task_id))
            task = result.scalar_one_or_none()
            if task is None:
                raise ValueError("Draft not found.")
            return self._to_item(task)

    async def approve_exercise(
        self,
        task_id: UUID,
        *,
        trusted_user_id: str | None = None,
        trusted_user_email: str | None = None,
        trusted_roles: list[str] | None = None,
    ) -> ApproveExerciseDraftResponse:
        # Atomic claim: UPDATE … WHERE status='DRAFT' RETURNING. If two
        # admins approve concurrently, only one row UPDATE succeeds — the
        # other gets None and bails out without ever calling the publisher.
        claimed = await self._claim_draft(task_id, AiTaskType.EXERCISE_GENERATION)
        if claimed is None:
            raise DraftConcurrencyError(
                "Draft is not in DRAFT status (already approved, published, or being processed)."
            )
        draft_payload = claimed["result_payload"] if isinstance(claimed["result_payload"], dict) else {}
        lesson_id = claimed["target_reference"] or ""
        request_payload = claimed["request_payload"] if isinstance(claimed["request_payload"], dict) else {}
        course_id = str(request_payload.get("courseId") or "")

        if not course_id or not lesson_id:
            await self._mark_failed(
                task_id,
                draft_payload,
                error="Draft missing courseId/lessonId; cannot publish.",
                publish_meta=None,
            )
            return ApproveExerciseDraftResponse(
                taskId=str(task_id),
                lessonId=lesson_id,
                success=False,
                message="Publish failed: missing courseId/lessonId in original request.",
            )

        # Publisher exceptions must still mark PUBLISH_FAILED — otherwise the
        # row stays stuck at PUBLISHING forever.
        try:
            outcome = await exercise_publisher.publish(
                course_id=course_id,
                lesson_id=lesson_id,
                payload=draft_payload,
                trusted_user_id=trusted_user_id,
                trusted_user_email=trusted_user_email,
                trusted_roles=trusted_roles,
            )
        except Exception as exc:  # noqa: BLE001
            logger.exception("Exercise publisher raised; recording PUBLISH_FAILED for task %s", task_id)
            await self._mark_failed(
                task_id,
                draft_payload,
                error=f"publisher exception: {type(exc).__name__}: {exc}",
                publish_meta=None,
            )
            return ApproveExerciseDraftResponse(
                taskId=str(task_id),
                lessonId=lesson_id,
                success=False,
                message=f"Publish failed (exception): {type(exc).__name__}",
            )

        if outcome.status == ExercisePublishStatus.PUBLISHED:
            await self._mark_status(
                task_id,
                AiTaskStatus.PUBLISHED,
                draft_payload,
                publish_meta=outcome.to_dict(),
            )
            return ApproveExerciseDraftResponse(
                taskId=str(task_id),
                lessonId=lesson_id,
                success=True,
                message=f"Exercises published to Course Service. createdExerciseIds={outcome.created_exercise_ids}",
            )
        if outcome.status == ExercisePublishStatus.SKIPPED_NO_BASE_URL:
            await self._mark_status(
                task_id,
                AiTaskStatus.APPROVED,
                draft_payload,
                publish_meta=outcome.to_dict(),
            )
            return ApproveExerciseDraftResponse(
                taskId=str(task_id),
                lessonId=lesson_id,
                success=True,
                message="Draft approved. COURSE_SERVICE_BASE_URL not configured, skipped remote publish.",
            )
        await self._mark_failed(
            task_id,
            draft_payload,
            error=outcome.error or "publish failed",
            publish_meta=outcome.to_dict(),
        )
        return ApproveExerciseDraftResponse(
            taskId=str(task_id),
            lessonId=lesson_id,
            success=False,
            message=f"Publish failed: {outcome.error}",
        )

    async def approve_learning_path(
        self,
        task_id: UUID,
        *,
        trusted_user_id: str | None = None,
        trusted_user_email: str | None = None,
        trusted_roles: list[str] | None = None,
    ) -> ApproveLearningPathDraftResponse:
        claimed = await self._claim_draft(task_id, AiTaskType.LEARNING_PATH_GENERATION)
        if claimed is None:
            raise DraftConcurrencyError(
                "Draft is not in DRAFT status (already approved, published, or being processed)."
            )
        draft_payload = claimed["result_payload"] if isinstance(claimed["result_payload"], dict) else {}

        try:
            outcome = await learning_path_publisher.publish(
                draft_payload,
                trusted_user_id=trusted_user_id,
                trusted_user_email=trusted_user_email,
                trusted_roles=trusted_roles,
            )
        except Exception as exc:  # noqa: BLE001
            logger.exception("Learning path publisher raised; recording PUBLISH_FAILED for task %s", task_id)
            await self._mark_failed(
                task_id,
                draft_payload,
                error=f"publisher exception: {type(exc).__name__}: {exc}",
                publish_meta=None,
            )
            return ApproveLearningPathDraftResponse(
                taskId=str(task_id),
                success=False,
                message=f"Publish failed (exception): {type(exc).__name__}",
                learningPathData=draft_payload,
            )

        updated_payload = dict(draft_payload)
        updated_payload["publish"] = outcome.to_dict()

        if outcome.status == LearningPathPublishStatus.PUBLISHED:
            await self._mark_status(
                task_id,
                AiTaskStatus.PUBLISHED,
                draft_payload,
                publish_meta=outcome.to_dict(),
            )
            return ApproveLearningPathDraftResponse(
                taskId=str(task_id),
                success=True,
                message=f"Draft published to Learning Path Service. learningPathId={outcome.learning_path_id}",
                learningPathData=updated_payload,
            )
        if outcome.status == LearningPathPublishStatus.SKIPPED_NO_BASE_URL:
            await self._mark_status(
                task_id,
                AiTaskStatus.APPROVED,
                draft_payload,
                publish_meta=outcome.to_dict(),
            )
            return ApproveLearningPathDraftResponse(
                taskId=str(task_id),
                success=True,
                message=(
                    "Draft approved. LEARNING_PATH_SERVICE_BASE_URL not configured, "
                    "skipped remote publish. Frontend can still call proxy directly."
                ),
                learningPathData=updated_payload,
            )
        await self._mark_failed(
            task_id,
            draft_payload,
            error=outcome.error or "publish failed",
            publish_meta=outcome.to_dict(),
        )
        return ApproveLearningPathDraftResponse(
            taskId=str(task_id),
            success=False,
            message=f"Publish failed: {outcome.error}",
            learningPathData=updated_payload,
        )

    async def _claim_draft(self, task_id: UUID, task_type: AiTaskType) -> dict[str, Any] | None:
        """Atomically transition DRAFT -> PUBLISHING for this task.

        Returns the row's current `result_payload` / `request_payload` /
        `target_reference` snapshot, or None if another approver beat us to
        it (status != DRAFT). The UPDATE-WHERE-RETURNING pattern is the
        race-safe equivalent of `SELECT FOR UPDATE` + `SET`.
        """
        stmt = (
            update(AiGenerationTaskModel)
            .where(AiGenerationTaskModel.id == task_id)
            .where(AiGenerationTaskModel.task_type == task_type.value)
            .where(AiGenerationTaskModel.status == AiTaskStatus.DRAFT.value)
            .values(status=AiTaskStatus.PUBLISHING.value)
            .returning(
                AiGenerationTaskModel.id,
                AiGenerationTaskModel.target_reference,
                AiGenerationTaskModel.request_payload,
                AiGenerationTaskModel.result_payload,
            )
        )
        async with get_db_session() as session:
            result = await session.execute(stmt)
            row = result.first()
            if row is None:
                return None
            return {
                "id": row.id,
                "target_reference": row.target_reference,
                "request_payload": row.request_payload,
                "result_payload": row.result_payload,
            }

    async def _mark_status(
        self,
        task_id: UUID,
        new_status: AiTaskStatus,
        draft_payload: dict[str, Any],
        *,
        publish_meta: dict[str, Any] | None,
    ) -> None:
        updated_payload = dict(draft_payload)
        if publish_meta is not None:
            updated_payload["publish"] = publish_meta
        stmt = (
            update(AiGenerationTaskModel)
            .where(AiGenerationTaskModel.id == task_id)
            .values(status=new_status.value, result_payload=updated_payload, error_message=None)
        )
        async with get_db_session() as session:
            await session.execute(stmt)

    async def _mark_failed(
        self,
        task_id: UUID,
        draft_payload: dict[str, Any],
        *,
        error: str,
        publish_meta: dict[str, Any] | None,
    ) -> None:
        updated_payload = dict(draft_payload)
        if publish_meta is not None:
            updated_payload["publish"] = publish_meta
        stmt = (
            update(AiGenerationTaskModel)
            .where(AiGenerationTaskModel.id == task_id)
            .values(
                status=AiTaskStatus.PUBLISH_FAILED.value,
                result_payload=updated_payload,
                error_message=str(error)[:512],
            )
        )
        async with get_db_session() as session:
            await session.execute(stmt)

    async def sweep_stuck_publishing(self, *, older_than_minutes: int = 10) -> int:
        """Reset rows stuck in PUBLISHING longer than `older_than_minutes` to
        PUBLISH_FAILED. Use case: AI service crashed between atomic claim and
        outcome write — without the sweep that row would be unrecoverable
        (DRAFT-only loader rejects PUBLISHING). Returns the number of rows
        reset; admins should run this every ~5 minutes via cron / admin route.
        """
        cutoff = datetime.now(timezone.utc) - timedelta(minutes=max(1, older_than_minutes))
        stmt = (
            update(AiGenerationTaskModel)
            .where(AiGenerationTaskModel.status == AiTaskStatus.PUBLISHING.value)
            .where(AiGenerationTaskModel.updated < cutoff)
            .values(
                status=AiTaskStatus.PUBLISH_FAILED.value,
                error_message=(
                    "Sweeper reset: task stuck in PUBLISHING beyond timeout; "
                    "AI service likely crashed mid-publish. Inspect downstream domain "
                    "service for orphan records before retrying."
                ),
            )
            .returning(AiGenerationTaskModel.id)
        )
        async with get_db_session() as session:
            result = await session.execute(stmt)
            ids = list(result.scalars().all())
        if ids:
            logger.warning(
                "Draft sweeper reset %d stuck PUBLISHING row(s): %s",
                len(ids),
                [str(i) for i in ids[:20]],
            )
        return len(ids)

    async def reject_draft(self, task_id: UUID, reason: str) -> None:
        async with get_db_session() as session:
            result = await session.execute(select(AiGenerationTaskModel).where(AiGenerationTaskModel.id == task_id))
            task = result.scalar_one_or_none()
            if task is None:
                raise ValueError("Draft not found.")
            task.status = AiTaskStatus.REJECTED.value
            task.error_message = f"Rejected by admin: {reason}"

    async def _list_by_target(self, target: str, task_type: AiTaskType) -> list[DraftListItem]:
        async with get_db_session() as session:
            result = await session.execute(
                select(AiGenerationTaskModel)
                .where(AiGenerationTaskModel.target_reference == target)
                .where(AiGenerationTaskModel.status == AiTaskStatus.DRAFT.value)
                .where(AiGenerationTaskModel.task_type == task_type.value)
                .order_by(AiGenerationTaskModel.created.desc())
            )
            return [self._to_item(item) for item in result.scalars().all()]

    async def _load_task(self, session, task_id: UUID, task_type: AiTaskType) -> AiGenerationTaskModel:
        result = await session.execute(
            select(AiGenerationTaskModel)
            .where(AiGenerationTaskModel.id == task_id)
            .where(AiGenerationTaskModel.task_type == task_type.value)
        )
        task = result.scalar_one_or_none()
        if task is None:
            raise ValueError("Draft not found or task type mismatch.")
        if task.status != AiTaskStatus.DRAFT.value:
            raise ValueError(f"Task is not in DRAFT status, current: {task.status}")
        return task

    @staticmethod
    def _to_item(task: AiGenerationTaskModel) -> DraftListItem:
        return DraftListItem(
            taskId=str(task.id),
            taskType=task.task_type,
            status=task.status,
            targetReference=task.target_reference,
            resultPayload=task.result_payload,
            requestPayload=task.request_payload,
            prompt=task.prompt,
            createdAt=task.created,
        )


draft_service = DraftService()
