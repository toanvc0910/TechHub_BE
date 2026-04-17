from __future__ import annotations

from typing import Any
from uuid import UUID

from pydantic import BaseModel

from app.core.enums import RecommendationMode


class RecommendationRequest(BaseModel):
    userId: UUID
    mode: RecommendationMode = RecommendationMode.REALTIME
    language: str = "vi"
    excludeCourseIds: list[UUID] | None = None
    preferredLanguages: list[str] | None = None


class RecommendationItem(BaseModel):
    courseId: str
    title: str
    description: str | None = None
    score: float
    reason: str
    tags: list[str] | None = None
    estimatedDuration: str | None = None


class RecommendationResponse(BaseModel):
    taskId: str | None = None
    recommendations: list[RecommendationItem]
    metadata: dict[str, Any] | None = None


class RecommendationHistoryItem(BaseModel):
    taskId: str
    mode: RecommendationMode
    status: str
    createdAt: str
    recommendations: list[RecommendationItem]
    metadata: dict[str, Any] | None = None
