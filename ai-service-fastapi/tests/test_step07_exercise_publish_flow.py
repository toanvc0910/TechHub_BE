"""
Step 07 - Exercise Publish Flow E2E smoke test.

Validator cases hit the live PostgreSQL via a temporary chapter+lesson seeded
under a real course (cleaned up at teardown) so we exercise the real lesson-
ownership join. Publisher cases mock httpx so we exercise the wire format
against the Course Service `POST /api/courses/{courseId}/lessons/{lessonId}/exercises`
contract. The approve-flow test writes a real `ai_generation_tasks` row and
reads it back to confirm DB status transitions DRAFT -> PUBLISHING ->
PUBLISHED / PUBLISH_FAILED.

Run from the service root:

    python -m tests.test_step07_exercise_publish_flow
"""

from __future__ import annotations

import asyncio
import sys
import traceback
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from unittest.mock import AsyncMock, patch
from uuid import UUID

SERVICE_ROOT = Path(__file__).resolve().parent.parent
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

import httpx  # noqa: E402
from sqlalchemy import delete, select, text  # noqa: E402

from app.core.config import get_settings  # noqa: E402
from app.core.enums import AiTaskStatus, AiTaskType  # noqa: E402
from app.db.models import AiGenerationTaskModel  # noqa: E402
from app.db.session import get_db_session  # noqa: E402
from app.services.draft_service import draft_service  # noqa: E402
from app.services.publishers import (  # noqa: E402
    ExercisePublishStatus,
    ExerciseValidationError,
    exercise_publisher,
    exercise_validator,
)


# Seeded test fixtures (course is real & PUBLISHED; chapter+lesson are
# inserted by setup() and removed by teardown()).
REAL_COURSE_ID = "ad0ac9c6-a60d-4895-8310-49b7b38e5009"
TEST_CHAPTER_ID = str(uuid.uuid4())
TEST_LESSON_ID = str(uuid.uuid4())
TRUSTED_USER = str(uuid.uuid4())


@dataclass
class CaseResult:
    name: str
    passed: bool
    detail: str = ""


# ---------------------------------------------------------------------------
# Fixture management (real chapters + lessons rows on live DB).
# ---------------------------------------------------------------------------


async def setup_lesson_fixture() -> None:
    async with get_db_session() as session:
        await session.execute(
            text(
                """
                INSERT INTO chapters (id, title, "order", course_id, is_active, created, updated)
                VALUES (CAST(:id AS uuid), :title, 1, CAST(:course_id AS uuid), 'Y', now(), now())
                """
            ),
            {"id": TEST_CHAPTER_ID, "title": "step07-test chapter", "course_id": REAL_COURSE_ID},
        )
        await session.execute(
            text(
                """
                INSERT INTO lessons (
                    id, title, description, "order", chapter_id, content_type, content,
                    mandatory, completion_weight, is_free, workspace_enabled, is_active,
                    created, updated
                )
                VALUES (
                    CAST(:id AS uuid), :title, :description, 1, CAST(:chapter_id AS uuid),
                    'TEXT'::content_type, :content,
                    true, 1.0, true, false, 'Y',
                    now(), now()
                )
                """
            ),
            {
                "id": TEST_LESSON_ID,
                "title": "step07-test lesson",
                "description": "test lesson description for exercise generation",
                "chapter_id": TEST_CHAPTER_ID,
                "content": "This lesson covers Python list comprehensions and how to use them effectively.",
            },
        )


async def teardown_lesson_fixture() -> None:
    async with get_db_session() as session:
        await session.execute(
            text("DELETE FROM lessons WHERE id = CAST(:id AS uuid)"),
            {"id": TEST_LESSON_ID},
        )
        await session.execute(
            text("DELETE FROM chapters WHERE id = CAST(:id AS uuid)"),
            {"id": TEST_CHAPTER_ID},
        )


def _good_mcq() -> dict[str, Any]:
    return {
        "question": "What does Python's list comprehension primarily do?",
        "options": [
            {"text": "Generate a new list by transforming items", "correct": True},
            {"text": "Delete items from a list", "correct": False},
            {"text": "Sort a list alphabetically", "correct": False},
            {"text": "Convert a list to a dict", "correct": False},
        ],
        "difficulty": "BEGINNER",
        "explanation": "List comprehension builds new lists.",
    }


def _good_essay() -> dict[str, Any]:
    return {
        "question": "Explain when list comprehension is preferable to a for-loop in Python.",
        "rubric": ["Mentions readability", "Mentions performance", "Gives an example"],
        "difficulty": "INTERMEDIATE",
        "explanation": "Tests understanding of idiomatic Python.",
    }


def _good_coding() -> dict[str, Any]:
    return {
        "question": "Write a function that squares each element of a list using list comprehension.",
        "starterCode": "def square_all(nums):\n    return []\n",
        "testCases": [
            {"input": "[1, 2, 3]", "expectedOutput": "[1, 4, 9]"},
            {"input": "[0, -2]", "expectedOutput": "[0, 4]"},
        ],
        "difficulty": "INTERMEDIATE",
        "explanation": "Direct application of list comprehension.",
    }


def _good_payload() -> dict[str, Any]:
    return {"mcq": [_good_mcq()], "essay": [_good_essay()], "coding": [_good_coding()]}


# ---------------------------------------------------------------------------
# Validator cases
# ---------------------------------------------------------------------------


async def case_validator_happy_path() -> CaseResult:
    name = "V1 - valid MCQ + essay + coding payload passes validator"
    batch = await exercise_validator.validate(
        course_id=REAL_COURSE_ID,
        lesson_id=TEST_LESSON_ID,
        payload=_good_payload(),
    )
    if len(batch.exercises) != 3:
        return CaseResult(name, False, f"expected 3 exercises, got {len(batch.exercises)}")
    types = sorted(ex.type for ex in batch.exercises)
    if types != ["CODING", "MULTIPLE_CHOICE", "OPEN_ENDED"]:
        return CaseResult(name, False, f"unexpected types after mapping: {types}")
    orders = [ex.order_index for ex in batch.exercises]
    if orders != [1, 2, 3]:
        return CaseResult(name, False, f"orderIndex should be 1..3 sequential, got {orders}")
    return CaseResult(name, True, f"types={types} orders={orders}")


async def case_validator_lesson_not_in_course() -> CaseResult:
    name = "V2 - lesson_id does not belong to course_id"
    other_course = str(uuid.uuid4())
    try:
        await exercise_validator.validate(
            course_id=other_course,
            lesson_id=TEST_LESSON_ID,
            payload=_good_payload(),
        )
    except ExerciseValidationError as exc:
        if "not found" in str(exc) or "does not belong" in str(exc):
            return CaseResult(name, True, str(exc)[:120])
        return CaseResult(name, False, f"wrong error: {exc}")
    return CaseResult(name, False, "did not raise on lesson/course mismatch")


async def case_validator_unknown_lesson() -> CaseResult:
    name = "V3 - unknown lesson rejected"
    try:
        await exercise_validator.validate(
            course_id=REAL_COURSE_ID,
            lesson_id=str(uuid.uuid4()),
            payload=_good_payload(),
        )
    except ExerciseValidationError as exc:
        if "not found" in str(exc):
            return CaseResult(name, True, str(exc)[:120])
        return CaseResult(name, False, f"wrong error: {exc}")
    return CaseResult(name, False, "did not raise on unknown lesson")


async def case_validator_mcq_zero_correct() -> CaseResult:
    name = "V4 - MCQ with 0 correct option rejected"
    payload = _good_payload()
    for opt in payload["mcq"][0]["options"]:
        opt["correct"] = False
    try:
        await exercise_validator.validate(
            course_id=REAL_COURSE_ID, lesson_id=TEST_LESSON_ID, payload=payload
        )
    except ExerciseValidationError as exc:
        if "exactly 1 correct" in str(exc):
            return CaseResult(name, True, str(exc)[:120])
        return CaseResult(name, False, f"wrong error: {exc}")
    return CaseResult(name, False, "did not raise on MCQ with no correct option")


async def case_validator_mcq_multi_correct() -> CaseResult:
    name = "V5 - MCQ with 2+ correct options rejected"
    payload = _good_payload()
    payload["mcq"][0]["options"][1]["correct"] = True
    try:
        await exercise_validator.validate(
            course_id=REAL_COURSE_ID, lesson_id=TEST_LESSON_ID, payload=payload
        )
    except ExerciseValidationError as exc:
        if "exactly 1 correct" in str(exc):
            return CaseResult(name, True, str(exc)[:120])
        return CaseResult(name, False, f"wrong error: {exc}")
    return CaseResult(name, False, "did not raise on multi-correct MCQ")


async def case_validator_essay_no_rubric() -> CaseResult:
    name = "V6 - Essay without rubric rejected"
    payload = _good_payload()
    payload["essay"][0]["rubric"] = []
    try:
        await exercise_validator.validate(
            course_id=REAL_COURSE_ID, lesson_id=TEST_LESSON_ID, payload=payload
        )
    except ExerciseValidationError as exc:
        if "rubric" in str(exc).lower():
            return CaseResult(name, True, str(exc)[:120])
        return CaseResult(name, False, f"wrong error: {exc}")
    return CaseResult(name, False, "did not raise on essay without rubric")


async def case_validator_coding_no_starter() -> CaseResult:
    name = "V7 - Coding without starterCode rejected"
    payload = _good_payload()
    payload["coding"][0]["starterCode"] = ""
    try:
        await exercise_validator.validate(
            course_id=REAL_COURSE_ID, lesson_id=TEST_LESSON_ID, payload=payload
        )
    except ExerciseValidationError as exc:
        if "starterCode" in str(exc):
            return CaseResult(name, True, str(exc)[:120])
        return CaseResult(name, False, f"wrong error: {exc}")
    return CaseResult(name, False, "did not raise on coding without starterCode")


async def case_validator_coding_placeholder_test_case() -> CaseResult:
    name = "V8 - Coding test case with placeholder strings rejected"
    payload = _good_payload()
    payload["coding"][0]["testCases"] = [
        {"input": "sample input for X", "expectedOutput": "expected behavior demonstrating X"}
    ]
    try:
        await exercise_validator.validate(
            course_id=REAL_COURSE_ID, lesson_id=TEST_LESSON_ID, payload=payload
        )
    except ExerciseValidationError as exc:
        if "placeholder" in str(exc).lower():
            return CaseResult(name, True, str(exc)[:120])
        return CaseResult(name, False, f"wrong error: {exc}")
    return CaseResult(name, False, "did not raise on placeholder test case")


async def case_validator_duplicate_questions() -> CaseResult:
    name = "V9 - duplicate question text in batch rejected"
    payload = _good_payload()
    payload["mcq"].append({**_good_mcq(), "question": _good_mcq()["question"]})
    try:
        await exercise_validator.validate(
            course_id=REAL_COURSE_ID, lesson_id=TEST_LESSON_ID, payload=payload
        )
    except ExerciseValidationError as exc:
        if "Duplicate" in str(exc):
            return CaseResult(name, True, str(exc)[:120])
        return CaseResult(name, False, f"wrong error: {exc}")
    return CaseResult(name, False, "did not raise on duplicate question")


async def case_validator_empty_batch() -> CaseResult:
    name = "V10 - empty payload rejected"
    try:
        await exercise_validator.validate(
            course_id=REAL_COURSE_ID, lesson_id=TEST_LESSON_ID, payload={}
        )
    except ExerciseValidationError as exc:
        if "empty" in str(exc).lower():
            return CaseResult(name, True, str(exc)[:120])
        return CaseResult(name, False, f"wrong error: {exc}")
    return CaseResult(name, False, "did not raise on empty payload")


# ---------------------------------------------------------------------------
# Publisher cases (mock httpx)
# ---------------------------------------------------------------------------


def _mock_response(status_code: int, body: dict | list | str | None = None) -> httpx.Response:
    if isinstance(body, (dict, list)):
        return httpx.Response(status_code, json=body, request=httpx.Request("POST", "http://mock"))
    return httpx.Response(
        status_code, text=body or "", request=httpx.Request("POST", "http://mock")
    )


async def case_publisher_happy_path() -> CaseResult:
    name = "P1 - publisher happy path: POST creates 3 exercises -> PUBLISHED"
    exercise_ids = [str(uuid.uuid4()) for _ in range(3)]
    client = AsyncMock(spec=httpx.AsyncClient)
    client.post = AsyncMock(return_value=_mock_response(200, {"data": [{"id": eid} for eid in exercise_ids]}))
    with patch.object(get_settings(), "course_service_base_url", "http://course-mock"):
        outcome = await exercise_publisher.publish(
            course_id=REAL_COURSE_ID,
            lesson_id=TEST_LESSON_ID,
            payload=_good_payload(),
            trusted_user_id=TRUSTED_USER,
            http_client=client,
        )
    if outcome.status != ExercisePublishStatus.PUBLISHED:
        return CaseResult(name, False, f"expected PUBLISHED, got {outcome.status}: {outcome.error}")
    if set(outcome.created_exercise_ids) != set(exercise_ids):
        return CaseResult(name, False, f"created ids mismatch: {outcome.created_exercise_ids}")
    if client.post.await_count != 1:
        return CaseResult(name, False, f"expected 1 POST call, got {client.post.await_count}")
    # Verify URL uses course/lesson path
    actual_url = client.post.call_args.args[0] if client.post.call_args.args else client.post.call_args.kwargs.get("url", "")
    if f"/api/courses/{REAL_COURSE_ID}/lessons/{TEST_LESSON_ID}/exercises" not in str(actual_url):
        return CaseResult(name, False, f"unexpected URL: {actual_url}")
    return CaseResult(name, True, f"createdIds={outcome.created_exercise_ids}")


async def case_publisher_create_failed() -> CaseResult:
    name = "P2 - POST returns 500 -> PUBLISH_FAILED"
    client = AsyncMock(spec=httpx.AsyncClient)
    client.post = AsyncMock(return_value=_mock_response(500, "boom"))
    with patch.object(get_settings(), "course_service_base_url", "http://course-mock"):
        outcome = await exercise_publisher.publish(
            course_id=REAL_COURSE_ID,
            lesson_id=TEST_LESSON_ID,
            payload=_good_payload(),
            trusted_user_id=TRUSTED_USER,
            http_client=client,
        )
    if outcome.status != ExercisePublishStatus.PUBLISH_FAILED:
        return CaseResult(name, False, f"expected PUBLISH_FAILED, got {outcome.status}")
    if "HTTP 500" not in (outcome.error or ""):
        return CaseResult(name, False, f"error missing HTTP 500: {outcome.error}")
    return CaseResult(name, True, outcome.error[:120])


async def case_publisher_missing_identity() -> CaseResult:
    name = "P3 - missing trusted_user_id with base URL -> PUBLISH_FAILED, no HTTP call"
    client = AsyncMock(spec=httpx.AsyncClient)
    client.post = AsyncMock()
    with patch.object(get_settings(), "course_service_base_url", "http://course-mock"):
        outcome = await exercise_publisher.publish(
            course_id=REAL_COURSE_ID,
            lesson_id=TEST_LESSON_ID,
            payload=_good_payload(),
            trusted_user_id=None,
            http_client=client,
        )
    if outcome.status != ExercisePublishStatus.PUBLISH_FAILED:
        return CaseResult(name, False, f"expected PUBLISH_FAILED, got {outcome.status}")
    if client.post.await_count != 0:
        return CaseResult(name, False, "publisher must not call HTTP without trusted identity")
    return CaseResult(name, True, outcome.error[:120] if outcome.error else "")


async def case_publisher_no_base_url() -> CaseResult:
    name = "P4 - COURSE_SERVICE_BASE_URL not set -> SKIPPED_NO_BASE_URL"
    client = AsyncMock(spec=httpx.AsyncClient)
    client.post = AsyncMock()
    with patch.object(get_settings(), "course_service_base_url", None):
        outcome = await exercise_publisher.publish(
            course_id=REAL_COURSE_ID,
            lesson_id=TEST_LESSON_ID,
            payload=_good_payload(),
            trusted_user_id=TRUSTED_USER,
            http_client=client,
        )
    if outcome.status != ExercisePublishStatus.SKIPPED_NO_BASE_URL:
        return CaseResult(name, False, f"expected SKIPPED_NO_BASE_URL, got {outcome.status}")
    if client.post.await_count != 0:
        return CaseResult(name, False, "publisher must not call HTTP when base URL missing")
    return CaseResult(name, True, "skipped cleanly")


async def case_publisher_request_shape() -> CaseResult:
    name = "P5 - publisher converts AI draft to Course Service ExerciseRequest shape"
    client = AsyncMock(spec=httpx.AsyncClient)
    client.post = AsyncMock(return_value=_mock_response(200, {"data": [{"id": str(uuid.uuid4())}]}))
    with patch.object(get_settings(), "course_service_base_url", "http://course-mock"):
        await exercise_publisher.publish(
            course_id=REAL_COURSE_ID,
            lesson_id=TEST_LESSON_ID,
            payload=_good_payload(),
            trusted_user_id=TRUSTED_USER,
            http_client=client,
        )
    sent = client.post.call_args.kwargs.get("json")
    if not isinstance(sent, list) or len(sent) != 3:
        return CaseResult(name, False, f"expected list of 3 ExerciseRequest, got {type(sent).__name__} len={len(sent) if sent else 0}")
    # MCQ entry
    mcq = next((e for e in sent if e["type"] == "MULTIPLE_CHOICE"), None)
    if not mcq or "options" not in mcq:
        return CaseResult(name, False, f"MCQ entry missing or no options: {mcq}")
    if not isinstance(mcq["options"], list):
        return CaseResult(name, False, f"MCQ options should be list, got {type(mcq['options']).__name__}")
    # Coding entry
    coding = next((e for e in sent if e["type"] == "CODING"), None)
    if not coding or "testCases" not in coding:
        return CaseResult(name, False, f"Coding entry missing testCases: {coding}")
    if not coding["testCases"][0].get("expectedOutput"):
        return CaseResult(name, False, f"Coding testCase missing expectedOutput: {coding['testCases'][0]}")
    # Essay entry
    essay = next((e for e in sent if e["type"] == "OPEN_ENDED"), None)
    if not essay or "options" not in essay or "rubric" not in essay["options"]:
        return CaseResult(name, False, f"Essay options.rubric missing: {essay}")
    # Trusted identity headers
    headers = client.post.call_args.kwargs.get("headers") or {}
    if headers.get("X-User-Id") != TRUSTED_USER:
        return CaseResult(name, False, f"trusted X-User-Id not forwarded: {headers}")
    return CaseResult(name, True, f"types={[e['type'] for e in sent]} headers.X-User-Id ok")


async def case_publisher_validation_failure() -> CaseResult:
    name = "P6 - validation failure inside publish -> PUBLISH_FAILED with prefix"
    bad = _good_payload()
    bad["mcq"][0]["options"][0]["correct"] = False  # zero correct
    client = AsyncMock(spec=httpx.AsyncClient)
    client.post = AsyncMock()
    with patch.object(get_settings(), "course_service_base_url", "http://course-mock"):
        outcome = await exercise_publisher.publish(
            course_id=REAL_COURSE_ID,
            lesson_id=TEST_LESSON_ID,
            payload=bad,
            trusted_user_id=TRUSTED_USER,
            http_client=client,
        )
    if outcome.status != ExercisePublishStatus.PUBLISH_FAILED:
        return CaseResult(name, False, f"expected PUBLISH_FAILED, got {outcome.status}")
    if not outcome.error or not outcome.error.startswith("validation_failed"):
        return CaseResult(name, False, f"expected validation_failed prefix, got: {outcome.error}")
    if client.post.await_count != 0:
        return CaseResult(name, False, "publisher must not call HTTP when validation fails")
    return CaseResult(name, True, outcome.error[:120])


# ---------------------------------------------------------------------------
# Approve flow E2E (DB live + mocked httpx)
# ---------------------------------------------------------------------------


async def _create_exercise_draft(course_id: str, lesson_id: str, payload: dict[str, Any]) -> UUID:
    async with get_db_session() as session:
        task = AiGenerationTaskModel(
            task_type=AiTaskType.EXERCISE_GENERATION.value,
            status=AiTaskStatus.DRAFT.value,
            target_reference=lesson_id,
            request_payload={"courseId": course_id, "lessonId": lesson_id, "source": "step07-test"},
            result_payload=payload,
            prompt="test",
            model_used="step07-test",
        )
        session.add(task)
        await session.flush()
        return task.id


async def _cleanup_draft(task_id: UUID) -> None:
    async with get_db_session() as session:
        await session.execute(delete(AiGenerationTaskModel).where(AiGenerationTaskModel.id == task_id))


async def _load_task(task_id: UUID) -> AiGenerationTaskModel:
    async with get_db_session() as session:
        result = await session.execute(
            select(AiGenerationTaskModel).where(AiGenerationTaskModel.id == task_id)
        )
        return result.scalar_one()


async def case_approve_flow_publish_success() -> CaseResult:
    name = "A1 - approve flow happy path: DRAFT -> PUBLISHED in DB"
    task_id = await _create_exercise_draft(REAL_COURSE_ID, TEST_LESSON_ID, _good_payload())
    try:
        exercise_ids = [str(uuid.uuid4()) for _ in range(3)]
        client = AsyncMock(spec=httpx.AsyncClient)
        client.post = AsyncMock(return_value=_mock_response(200, {"data": [{"id": eid} for eid in exercise_ids]}))
        client.aclose = AsyncMock()
        with patch.object(get_settings(), "course_service_base_url", "http://course-mock"), \
             patch("httpx.AsyncClient", return_value=client):
            response = await draft_service.approve_exercise(
                task_id, trusted_user_id=TRUSTED_USER
            )
        if not response.success:
            return CaseResult(name, False, f"approve returned success=False: {response.message}")
        task = await _load_task(task_id)
        if task.status != AiTaskStatus.PUBLISHED.value:
            return CaseResult(name, False, f"DB status should be PUBLISHED, got {task.status}")
        publish_meta = (task.result_payload or {}).get("publish") if isinstance(task.result_payload, dict) else None
        if not publish_meta or set(publish_meta.get("createdExerciseIds") or []) != set(exercise_ids):
            return CaseResult(name, False, f"publish metadata not persisted correctly: {publish_meta}")
        return CaseResult(name, True, f"DB status={task.status}, created {len(exercise_ids)} exercises")
    finally:
        await _cleanup_draft(task_id)


async def case_approve_flow_publish_failed() -> CaseResult:
    name = "A2 - approve flow with Course Service 500: DB status = PUBLISH_FAILED"
    task_id = await _create_exercise_draft(REAL_COURSE_ID, TEST_LESSON_ID, _good_payload())
    try:
        client = AsyncMock(spec=httpx.AsyncClient)
        client.post = AsyncMock(return_value=_mock_response(500, "boom"))
        client.aclose = AsyncMock()
        with patch.object(get_settings(), "course_service_base_url", "http://course-mock"), \
             patch("httpx.AsyncClient", return_value=client):
            response = await draft_service.approve_exercise(
                task_id, trusted_user_id=TRUSTED_USER
            )
        if response.success:
            return CaseResult(name, False, "approve should report success=False on publish failure")
        task = await _load_task(task_id)
        if task.status != AiTaskStatus.PUBLISH_FAILED.value:
            return CaseResult(name, False, f"DB status should be PUBLISH_FAILED, got {task.status}")
        if not task.error_message or "HTTP 500" not in (task.error_message or ""):
            return CaseResult(name, False, f"error_message missing HTTP 500: {task.error_message}")
        return CaseResult(name, True, f"DB status={task.status}, error captured")
    finally:
        await _cleanup_draft(task_id)


async def case_approve_flow_no_base_url() -> CaseResult:
    name = "A3 - approve flow without COURSE_SERVICE_BASE_URL: DB status = APPROVED"
    task_id = await _create_exercise_draft(REAL_COURSE_ID, TEST_LESSON_ID, _good_payload())
    try:
        with patch.object(get_settings(), "course_service_base_url", None):
            response = await draft_service.approve_exercise(
                task_id, trusted_user_id=TRUSTED_USER
            )
        if not response.success:
            return CaseResult(name, False, f"expected success when skipped: {response.message}")
        task = await _load_task(task_id)
        if task.status != AiTaskStatus.APPROVED.value:
            return CaseResult(name, False, f"expected APPROVED, got {task.status}")
        return CaseResult(name, True, f"DB status={task.status}, skipped without error")
    finally:
        await _cleanup_draft(task_id)


async def case_approve_flow_missing_course_id() -> CaseResult:
    name = "A4 - draft missing courseId in request_payload -> PUBLISH_FAILED at draft_service"
    async with get_db_session() as session:
        task = AiGenerationTaskModel(
            task_type=AiTaskType.EXERCISE_GENERATION.value,
            status=AiTaskStatus.DRAFT.value,
            target_reference=TEST_LESSON_ID,
            request_payload={"lessonId": TEST_LESSON_ID},  # no courseId
            result_payload=_good_payload(),
            prompt="test",
            model_used="step07-test",
        )
        session.add(task)
        await session.flush()
        task_id = task.id
    try:
        with patch.object(get_settings(), "course_service_base_url", "http://course-mock"):
            response = await draft_service.approve_exercise(task_id, trusted_user_id=TRUSTED_USER)
        if response.success:
            return CaseResult(name, False, "approve should fail when courseId is missing")
        task = await _load_task(task_id)
        if task.status != AiTaskStatus.PUBLISH_FAILED.value:
            return CaseResult(name, False, f"expected PUBLISH_FAILED, got {task.status}")
        return CaseResult(name, True, f"failed correctly: {task.error_message}")
    finally:
        await _cleanup_draft(task_id)


# ---------------------------------------------------------------------------
# Runner
# ---------------------------------------------------------------------------


async def main() -> int:
    await setup_lesson_fixture()
    results: list[CaseResult] = []
    cases = [
        case_validator_happy_path,
        case_validator_lesson_not_in_course,
        case_validator_unknown_lesson,
        case_validator_mcq_zero_correct,
        case_validator_mcq_multi_correct,
        case_validator_essay_no_rubric,
        case_validator_coding_no_starter,
        case_validator_coding_placeholder_test_case,
        case_validator_duplicate_questions,
        case_validator_empty_batch,
        case_publisher_happy_path,
        case_publisher_create_failed,
        case_publisher_missing_identity,
        case_publisher_no_base_url,
        case_publisher_request_shape,
        case_publisher_validation_failure,
        case_approve_flow_publish_success,
        case_approve_flow_publish_failed,
        case_approve_flow_no_base_url,
        case_approve_flow_missing_course_id,
    ]
    try:
        for func in cases:
            print(f"\n--- {func.__name__}")
            try:
                result = await func()
            except Exception as exc:  # noqa: BLE001
                tb = "".join(traceback.format_exception_only(type(exc), exc)).strip()
                result = CaseResult(func.__name__, False, f"EXC: {tb}")
            print(("PASS " if result.passed else "FAIL ") + result.name + " :: " + result.detail)
            results.append(result)
    finally:
        await teardown_lesson_fixture()

    passed = [r for r in results if r.passed]
    failed = [r for r in results if not r.passed]
    print("\n=================================================")
    print(f"PASSED: {len(passed)} / {len(results)}")
    if failed:
        print("FAILED:")
        for r in failed:
            print(f"  - {r.name}: {r.detail}")
    return 0 if not failed else 1


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
