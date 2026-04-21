from __future__ import annotations

from datetime import datetime
from typing import Any
from uuid import UUID

from pydantic import BaseModel, Field

from app.core.enums import AiTaskStatus, DifficultyLevel, ExerciseFormat


class AiExerciseGenerateRequest(BaseModel):
    courseId: UUID
    lessonId: UUID
    language: str = "vi"
    difficulties: list[DifficultyLevel] = Field(default_factory=lambda: [DifficultyLevel.BEGINNER])
    formats: list[ExerciseFormat] = Field(default_factory=lambda: [ExerciseFormat.MCQ])
    variants: int = 1
    includeExplanations: bool = True
    includeTestCases: bool = True
    customInstruction: str | None = None
    count: int = 5
    type: str = "MCQ"
    difficulty: str = "BEGINNER"


class AiExerciseGenerationResponse(BaseModel):
    taskId: str
    status: AiTaskStatus
    exercises: dict[str, list[dict[str, Any]]] | None = None
    metadata: dict[str, Any] | None = None
    message: str | None = None
    drafts: Any | None = None


class DraftMetadata(BaseModel):
    courseId: str
    lessonId: str
    generatedAt: datetime
    totalCount: int
