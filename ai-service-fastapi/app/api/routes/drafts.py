from __future__ import annotations

from uuid import UUID

from fastapi import APIRouter, Request

from app.core.responses import success_response
from app.services.draft_service import draft_service

router = APIRouter()


@router.get("/exercises")
async def get_exercise_drafts(lessonId: UUID, request: Request) -> dict:
    drafts = await draft_service.get_exercise_drafts(lessonId)
    return success_response(
        message=f"Found {len(drafts)} draft(s)",
        data=[item.model_dump(mode="json") for item in drafts],
        path=request.url.path,
    )


@router.post("/exercises/batch")
async def get_exercise_drafts_batch(payload: dict, request: Request) -> dict:
    lesson_ids = payload.get("lessonIds", [])
    drafts = await draft_service.get_exercise_drafts_batch(lesson_ids)
    return success_response(
        message=f"Found {len(drafts)} draft(s)",
        data=[item.model_dump(mode="json") for item in drafts],
        path=request.url.path,
    )


@router.get("/exercises/latest")
async def get_latest_exercise_draft(lessonId: UUID, request: Request) -> dict:
    draft = await draft_service.get_latest_exercise_draft(lessonId)
    return success_response(message="Latest draft retrieved", data=draft.model_dump(mode="json"), path=request.url.path)


@router.get("/learning-paths")
async def get_learning_path_drafts(request: Request) -> dict:
    drafts = await draft_service.get_learning_path_drafts()
    return success_response(
        message=f"Found {len(drafts)} draft(s)",
        data=[item.model_dump(mode="json") for item in drafts],
        path=request.url.path,
    )


@router.get("/{task_id}")
async def get_draft_by_id(task_id: UUID, request: Request) -> dict:
    draft = await draft_service.get_draft_by_id(task_id)
    return success_response(message="Draft retrieved", data=draft.model_dump(mode="json"), path=request.url.path)


@router.post("/{task_id}/approve-exercise")
async def approve_exercise(task_id: UUID, request: Request) -> dict:
    response = await draft_service.approve_exercise(task_id)
    return success_response(
        message="Exercise draft approved successfully",
        data=response.model_dump(mode="json"),
        path=request.url.path,
        status="DRAFT_APPROVED",
    )


@router.post("/{task_id}/approve-learning-path")
async def approve_learning_path(task_id: UUID, request: Request) -> dict:
    response = await draft_service.approve_learning_path(task_id)
    return success_response(
        message="Learning path draft approved successfully",
        data=response.model_dump(mode="json"),
        path=request.url.path,
        status="DRAFT_APPROVED",
    )


@router.post("/{task_id}/reject")
async def reject_draft(task_id: UUID, request: Request, reason: str = "No reason provided") -> dict:
    await draft_service.reject_draft(task_id, reason)
    return success_response(
        message="Draft rejected successfully",
        data=None,
        path=request.url.path,
        status="DRAFT_REJECTED",
    )
