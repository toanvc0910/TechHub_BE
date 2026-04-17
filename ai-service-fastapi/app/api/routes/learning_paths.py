from __future__ import annotations

from fastapi import APIRouter, Request

from app.core.responses import success_response
from app.schemas.learning_path import LearningPathGenerateRequest
from app.services.learning_path_service import learning_path_service

router = APIRouter()


@router.post("/generate")
async def generate_learning_path(payload: LearningPathGenerateRequest, request: Request) -> dict:
    response = await learning_path_service.generate(payload)
    return success_response(
        message="Learning path draft generated",
        data=response.model_dump(mode="json"),
        path=request.url.path,
        status="AI_LEARNING_PATH_DRAFT",
    )
