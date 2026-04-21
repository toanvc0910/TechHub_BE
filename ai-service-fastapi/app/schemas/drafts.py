from __future__ import annotations

from datetime import datetime
from typing import Any

from pydantic import BaseModel

from app.core.enums import AiTaskStatus, AiTaskType


class DraftListItem(BaseModel):
    taskId: str
    taskType: AiTaskType
    status: AiTaskStatus
    targetReference: str | None = None
    resultPayload: Any | None = None
    requestPayload: Any | None = None
    prompt: str | None = None
    createdAt: datetime


class ApproveExerciseDraftResponse(BaseModel):
    taskId: str
    lessonId: str
    success: bool
    message: str


class ApproveLearningPathDraftResponse(BaseModel):
    taskId: str
    success: bool
    message: str
    learningPathData: Any | None = None
