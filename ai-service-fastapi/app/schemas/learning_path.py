from __future__ import annotations

from typing import Any
from uuid import UUID

from pydantic import BaseModel

from app.core.enums import AiTaskStatus


class LearningPathGenerateRequest(BaseModel):
    goal: str
    timeframe: str
    language: str = "vi"
    currentLevel: str
    targetLevel: str
    userId: UUID
    preferredCourseIds: list[UUID] | None = None
    includePositions: bool = True
    includeProjects: bool = True
    duration: str = "1 month"
    level: str = "BEGINNER"


class LearningPathNode(BaseModel):
    id: str
    type: str = "course"
    data: dict[str, Any]
    position: dict[str, float]


class LearningPathEdge(BaseModel):
    id: str
    source: str
    target: str
    type: str | None = None


class LearningPathDraftResponse(BaseModel):
    taskId: str
    status: AiTaskStatus
    title: str | None = None
    path: dict[str, Any] | None = None
    nodes: list[LearningPathNode] | None = None
    edges: list[LearningPathEdge] | None = None
