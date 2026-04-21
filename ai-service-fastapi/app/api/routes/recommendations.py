from __future__ import annotations

from uuid import UUID

from fastapi import APIRouter, Request

from app.core.responses import success_response
from app.core.enums import RecommendationMode
from app.schemas.recommendation import RecommendationRequest
from app.services.recommendation_service import recommendation_service

router = APIRouter()


@router.post("/realtime")
async def realtime(payload: RecommendationRequest, request: Request) -> dict:
    response = await recommendation_service.generate(payload)
    return success_response(
        message="Realtime recommendations generated",
        data=response.model_dump(mode="json"),
        path=request.url.path,
        status="AI_RECOMMENDATION_REALTIME",
    )


@router.post("/scheduled")
async def scheduled(payload: RecommendationRequest, request: Request) -> dict:
    response = await recommendation_service.generate(payload)
    return success_response(
        message="Scheduled recommendations generated",
        data=response.model_dump(mode="json"),
        path=request.url.path,
        status="AI_RECOMMENDATION_SCHEDULED",
    )


@router.get("/history")
async def history(
    userId: UUID,
    request: Request,
    mode: RecommendationMode | None = None,
    limit: int = 20,
) -> dict:
    items = await recommendation_service.get_history(userId, mode=mode, limit=limit)
    return success_response(
        message=f"Found {len(items)} recommendation history item(s)",
        data=[item.model_dump(mode="json") for item in items],
        path=request.url.path,
        status="AI_RECOMMENDATION_HISTORY",
    )
