from __future__ import annotations

from uuid import UUID

from sqlalchemy import select

from app.core.enums import AiTaskStatus, AiTaskType
from app.db.models import AiGenerationTaskModel
from app.db.session import get_db_session
from app.schemas.drafts import (
    ApproveExerciseDraftResponse,
    ApproveLearningPathDraftResponse,
    DraftListItem,
)


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

    async def approve_exercise(self, task_id: UUID) -> ApproveExerciseDraftResponse:
        async with get_db_session() as session:
            task = await self._load_task(session, task_id, AiTaskType.EXERCISE_GENERATION)
            task.status = AiTaskStatus.APPROVED.value
            return ApproveExerciseDraftResponse(
                taskId=str(task.id),
                lessonId=task.target_reference or "",
                success=True,
                message="Draft approved. Result payload is ready for Course Service API.",
            )

    async def approve_learning_path(self, task_id: UUID) -> ApproveLearningPathDraftResponse:
        async with get_db_session() as session:
            task = await self._load_task(session, task_id, AiTaskType.LEARNING_PATH_GENERATION)
            task.status = AiTaskStatus.APPROVED.value
            return ApproveLearningPathDraftResponse(
                taskId=str(task.id),
                success=True,
                message="Draft approved. Frontend can now create learning path via proxy API.",
                learningPathData=task.result_payload,
            )

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
