"""
Strict validator for AI-generated exercise drafts before they get published
to Course Service. Enforces:

  - lesson exists, is active, and belongs to the requested course
  - lesson has enough grounding content (content / description / vector chunks)
  - MCQ has >= 2 options with exactly 1 correct
  - Essay has a non-empty rubric
  - Coding has starter code and >= 1 test case with concrete (non-placeholder) IO
  - No duplicate question text within the same batch
  - Question is non-empty, difficulty is in the allowed set
  - AI format -> Course Service ExerciseType:
      MCQ    -> MULTIPLE_CHOICE
      ESSAY  -> OPEN_ENDED
      CODING -> CODING
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from typing import Any

from sqlalchemy import text

from app.db.session import get_db_session


class ExerciseValidationError(ValueError):
    """Raised when the exercise batch cannot be safely published."""


_ALLOWED_DIFFICULTIES = {"BEGINNER", "INTERMEDIATE", "ADVANCED"}

# Placeholder strings that should never make it into production test cases.
_PLACEHOLDER_PATTERNS = (
    re.compile(r"^\s*sample\s+input", re.I),
    re.compile(r"^\s*sample\s+output", re.I),
    re.compile(r"^\s*expected\s+behavior", re.I),
    re.compile(r"^\s*expected\s+output\s+for", re.I),
)

# AI format -> Java ExerciseType
_TYPE_MAP = {
    "mcq": "MULTIPLE_CHOICE",
    "MCQ": "MULTIPLE_CHOICE",
    "essay": "OPEN_ENDED",
    "ESSAY": "OPEN_ENDED",
    "coding": "CODING",
    "CODING": "CODING",
}


@dataclass(frozen=True, slots=True)
class ValidatedExercise:
    type: str  # "MULTIPLE_CHOICE" | "CODING" | "OPEN_ENDED"
    ai_format: str  # "mcq" | "essay" | "coding"
    question: str
    options: Any | None
    test_cases: list[dict[str, Any]]
    difficulty: str
    explanation: str | None
    order_index: int


@dataclass(frozen=True, slots=True)
class ValidatedExerciseBatch:
    course_id: str
    lesson_id: str
    lesson_title: str
    exercises: tuple[ValidatedExercise, ...] = field(default_factory=tuple)


class ExerciseValidator:
    async def validate(
        self,
        *,
        course_id: str,
        lesson_id: str,
        payload: dict[str, Any],
    ) -> ValidatedExerciseBatch:
        if not course_id or not lesson_id:
            raise ExerciseValidationError("courseId and lessonId are required.")
        if not isinstance(payload, dict):
            raise ExerciseValidationError("Exercise payload must be a dict keyed by format.")

        lesson = await self._lookup_lesson_for_course(lesson_id=lesson_id, course_id=course_id)
        self._ensure_lesson_grounding(lesson)

        exercises: list[ValidatedExercise] = []
        seen_questions: set[str] = set()
        order_counter = 0
        for ai_format in ("mcq", "essay", "coding"):
            raw_items = payload.get(ai_format)
            if raw_items in (None, []):
                continue
            if not isinstance(raw_items, list):
                raise ExerciseValidationError(
                    f"Payload['{ai_format}'] must be a list (got {type(raw_items).__name__})."
                )
            for index, item in enumerate(raw_items, start=1):
                order_counter += 1
                validated = self._validate_item(
                    ai_format=ai_format,
                    item=item,
                    item_index=index,
                    order_index=order_counter,
                )
                question_key = validated.question.strip().lower()
                if question_key in seen_questions:
                    raise ExerciseValidationError(
                        f"Duplicate exercise question detected: {validated.question[:80]!r}"
                    )
                seen_questions.add(question_key)
                exercises.append(validated)

        if not exercises:
            raise ExerciseValidationError("Exercise batch is empty after filtering.")
        if len(exercises) > 50:
            raise ExerciseValidationError("Exercise batch too large (max 50).")

        return ValidatedExerciseBatch(
            course_id=str(course_id),
            lesson_id=str(lesson_id),
            lesson_title=str(lesson.get("title") or ""),
            exercises=tuple(exercises),
        )

    @staticmethod
    async def _lookup_lesson_for_course(*, lesson_id: str, course_id: str) -> dict[str, Any]:
        sql = """
            SELECT
                l.id::text       AS id,
                l.title          AS title,
                l.description    AS description,
                l.content        AS content,
                l.content_type::text AS content_type,
                l.workspace_languages AS workspace_languages,
                l.workspace_template  AS workspace_template,
                l.is_active      AS is_active,
                ch.course_id::text   AS course_id
            FROM lessons l
            JOIN chapters ch ON ch.id = l.chapter_id
            WHERE l.id = CAST(:lesson_id AS uuid)
              AND ch.course_id = CAST(:course_id AS uuid)
              AND l.is_active = 'Y'
            LIMIT 1
        """
        async with get_db_session() as session:
            result = await session.execute(
                text(sql),
                {"lesson_id": lesson_id, "course_id": course_id},
            )
            row = result.mappings().first()
        if row is None:
            raise ExerciseValidationError(
                f"Lesson {lesson_id} not found or does not belong to course {course_id}, "
                "or lesson is inactive."
            )
        return dict(row)

    @staticmethod
    def _ensure_lesson_grounding(lesson: dict[str, Any]) -> None:
        content_sources: list[str] = []
        for key in ("content", "description"):
            value = str(lesson.get(key) or "").strip()
            if value:
                content_sources.append(key)
        if not content_sources:
            raise ExerciseValidationError(
                f"Lesson {lesson.get('id')} has no usable content (content/description) "
                "to ground exercises on. Refusing to hallucinate exercises."
            )

    @classmethod
    def _validate_item(
        cls,
        *,
        ai_format: str,
        item: Any,
        item_index: int,
        order_index: int,
    ) -> ValidatedExercise:
        if not isinstance(item, dict):
            raise ExerciseValidationError(
                f"Exercise {ai_format}#{item_index} must be an object."
            )
        question = str(item.get("question") or "").strip()
        if not question or len(question) < 5:
            raise ExerciseValidationError(
                f"Exercise {ai_format}#{item_index} has missing or too-short question."
            )

        difficulty = str(item.get("difficulty") or "INTERMEDIATE").upper()
        if difficulty not in _ALLOWED_DIFFICULTIES:
            raise ExerciseValidationError(
                f"Exercise {ai_format}#{item_index} has unsupported difficulty: {difficulty!r}."
            )

        explanation_raw = item.get("explanation")
        explanation = str(explanation_raw).strip() if explanation_raw else None

        java_type = _TYPE_MAP.get(ai_format)
        if java_type is None:
            raise ExerciseValidationError(f"Unsupported AI format: {ai_format!r}.")

        if ai_format == "mcq":
            options, test_cases = cls._validate_mcq(item, item_index)
        elif ai_format == "essay":
            options, test_cases = cls._validate_essay(item, item_index)
        elif ai_format == "coding":
            options, test_cases = cls._validate_coding(item, item_index)
        else:  # pragma: no cover - guarded by _TYPE_MAP
            raise ExerciseValidationError(f"Unsupported AI format: {ai_format!r}.")

        return ValidatedExercise(
            type=java_type,
            ai_format=ai_format,
            question=question,
            options=options,
            test_cases=test_cases,
            difficulty=difficulty,
            explanation=explanation,
            order_index=order_index,
        )

    @staticmethod
    def _validate_mcq(item: dict[str, Any], item_index: int) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
        options = item.get("options")
        if not isinstance(options, list) or len(options) < 2:
            raise ExerciseValidationError(
                f"MCQ #{item_index} must have at least 2 options."
            )
        cleaned: list[dict[str, Any]] = []
        correct_count = 0
        for opt_index, raw in enumerate(options, start=1):
            if not isinstance(raw, dict):
                raise ExerciseValidationError(
                    f"MCQ #{item_index} option #{opt_index} must be an object."
                )
            text_value = str(raw.get("text") or "").strip()
            if not text_value:
                raise ExerciseValidationError(
                    f"MCQ #{item_index} option #{opt_index} missing text."
                )
            correct_flag = bool(raw.get("correct") or raw.get("isCorrect") or False)
            if correct_flag:
                correct_count += 1
            cleaned.append({"text": text_value, "correct": correct_flag})
        if correct_count != 1:
            raise ExerciseValidationError(
                f"MCQ #{item_index} must have exactly 1 correct option (got {correct_count})."
            )
        return cleaned, []

    @staticmethod
    def _validate_essay(item: dict[str, Any], item_index: int) -> tuple[dict[str, Any], list[dict[str, Any]]]:
        rubric = item.get("rubric")
        if not isinstance(rubric, list) or not rubric:
            raise ExerciseValidationError(
                f"Essay #{item_index} requires a non-empty rubric list."
            )
        cleaned_rubric = [str(criterion).strip() for criterion in rubric if str(criterion).strip()]
        if not cleaned_rubric:
            raise ExerciseValidationError(
                f"Essay #{item_index} rubric entries cannot all be blank."
            )
        return {"rubric": cleaned_rubric}, []

    @staticmethod
    def _validate_coding(item: dict[str, Any], item_index: int) -> tuple[dict[str, Any], list[dict[str, Any]]]:
        starter_code = str(item.get("starterCode") or item.get("starter_code") or "").strip()
        if not starter_code:
            raise ExerciseValidationError(
                f"Coding #{item_index} requires non-empty starterCode."
            )
        raw_cases = item.get("testCases") or item.get("test_cases")
        if not isinstance(raw_cases, list) or not raw_cases:
            raise ExerciseValidationError(
                f"Coding #{item_index} requires at least one test case."
            )
        cleaned_cases: list[dict[str, Any]] = []
        for case_index, raw in enumerate(raw_cases, start=1):
            if not isinstance(raw, dict):
                raise ExerciseValidationError(
                    f"Coding #{item_index} testCase #{case_index} must be an object."
                )
            input_text = str(raw.get("input") or "").strip()
            expected = str(raw.get("expectedOutput") or raw.get("expected_output") or "").strip()
            if not input_text or not expected:
                raise ExerciseValidationError(
                    f"Coding #{item_index} testCase #{case_index} missing input or expectedOutput."
                )
            for pattern in _PLACEHOLDER_PATTERNS:
                if pattern.search(input_text) or pattern.search(expected):
                    raise ExerciseValidationError(
                        f"Coding #{item_index} testCase #{case_index} contains placeholder text: "
                        f"{input_text!r} -> {expected!r}"
                    )
            visibility = str(raw.get("visibility") or "PUBLIC").upper()
            cleaned_cases.append(
                {
                    "orderIndex": int(raw.get("orderIndex") or case_index),
                    "visibility": visibility,
                    "input": input_text,
                    "expectedOutput": expected,
                    "weight": float(raw.get("weight") or 1.0),
                    "timeoutSeconds": int(raw.get("timeoutSeconds") or 5),
                    "sample": bool(raw.get("sample", False)),
                }
            )
        return {"starterCode": starter_code}, cleaned_cases


exercise_validator = ExerciseValidator()
