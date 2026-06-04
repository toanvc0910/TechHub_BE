from __future__ import annotations

from fastapi import APIRouter, Depends, Request

from app.api.dependencies.trusted_context import (
    ADMIN_ROLES,
    TrustedContext,
    copy_model_with_updates,
    get_trusted_context,
    require_user_match,
)
from app.core.responses import success_response
from app.schemas.learning_path import LearningPathGenerateRequest
from app.services.learning_path_service import learning_path_service

router = APIRouter()


@router.post("/generate")
async def generate_learning_path(
    payload: LearningPathGenerateRequest,
    request: Request,
    trusted: TrustedContext = Depends(get_trusted_context),
) -> dict:
    trusted_user_id = require_user_match(payload.userId, trusted)
    trusted_payload = copy_model_with_updates(payload, userId=trusted_user_id)
    instructor_scoped = trusted.has_any_role({"INSTRUCTOR"}) and not trusted.has_any_role(ADMIN_ROLES)
    response = await learning_path_service.generate(
        trusted_payload,
        course_owner_id=str(trusted_user_id) if instructor_scoped else None,
        limit_to_user_courses=instructor_scoped,
    )
    return success_response(
        message="Learning path draft generated",
        data=response.model_dump(mode="json"),
        path=request.url.path,
        status="AI_LEARNING_PATH_DRAFT",
    )
