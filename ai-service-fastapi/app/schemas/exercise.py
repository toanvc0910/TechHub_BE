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


class QuizFeedbackRequest(BaseModel):
    courseId: str | None = None
    courseTitle: str | None = None
    lessonId: str | None = None
    lessonTitle: str | None = None
    question: str
    options: list[dict[str, Any]] = Field(default_factory=list)
    selectedAnswers: list[str] = Field(default_factory=list)
    correctAnswers: list[str] = Field(default_factory=list)
    isCorrect: bool = False
    explanation: str | None = None
    language: str = "vi"


class QuizReviewSuggestion(BaseModel):
    lessonId: str | None = None
    title: str
    reason: str
    action: str


class QuizFeedbackResponse(BaseModel):
    correct: bool
    summary: str
    explanation: str
    selectedAnswers: list[str] = Field(default_factory=list)
    correctAnswers: list[str] = Field(default_factory=list)
    weakConcepts: list[str] = Field(default_factory=list)
    reviewSuggestions: list[QuizReviewSuggestion] = Field(default_factory=list)
    nextAction: str
    source: str = "AI_SERVICE"


class DraftMetadata(BaseModel):
    courseId: str
    lessonId: str
    generatedAt: datetime
    totalCount: int
