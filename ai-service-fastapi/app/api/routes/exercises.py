from __future__ import annotations

from fastapi import APIRouter, Request

from app.core.responses import success_response
from app.schemas.exercise import AiExerciseGenerateRequest
from app.services.exercise_service import exercise_service

router = APIRouter()


@router.post("/generate")
async def generate_exercises(payload: AiExerciseGenerateRequest, request: Request) -> dict:
    response = await exercise_service.generate(payload)
    return success_response(
        message="AI exercise drafts created",
        data=response.model_dump(mode="json"),
        path=request.url.path,
        status="AI_EXERCISE_DRAFT",
    )
