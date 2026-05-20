from __future__ import annotations

from fastapi import APIRouter, Depends, Request

from app.api.dependencies.trusted_context import (
    TrustedContext,
    require_ai_content_operator,
    require_trusted_user,
)
from app.core.responses import success_response
from app.schemas.exercise import AiExerciseGenerateRequest, QuizFeedbackRequest
from app.services.exercise_service import exercise_service

router = APIRouter()


@router.post("/generate")
async def generate_exercises(
    payload: AiExerciseGenerateRequest,
    request: Request,
    trusted: TrustedContext = Depends(require_ai_content_operator),
) -> dict:
    del trusted
    response = await exercise_service.generate(payload)
    return success_response(
        message="AI exercise drafts created",
        data=response.model_dump(mode="json"),
        path=request.url.path,
        status="AI_EXERCISE_DRAFT",
    )


@router.post("/feedback")
async def generate_quiz_feedback(
    payload: QuizFeedbackRequest,
    request: Request,
    trusted: TrustedContext = Depends(require_trusted_user),
) -> dict:
    del trusted
    response = await exercise_service.generate_feedback(payload)
    return success_response(
        message="AI quiz feedback generated",
        data=response.model_dump(mode="json"),
        path=request.url.path,
        status="AI_QUIZ_FEEDBACK",
    )
