"""
Step 06 - Learning Path Publish Flow E2E smoke test.

Validator cases hit the live PostgreSQL `courses` table so unknown / unpublished
ids fail at the boundary. Publisher cases mock httpx so we exercise the wire
format and status transitions without needing the Java Learning Path Service to
be reachable. The approve-flow test writes a real `ai_generation_tasks` row
and reads it back to confirm DB status transitions DRAFT -> PUBLISHING ->
PUBLISHED / PUBLISH_FAILED.

Run from the service root:

    python -m tests.test_step06_learning_path_publish_flow
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
from sqlalchemy import delete, select  # noqa: E402

from app.core.config import get_settings  # noqa: E402
from app.core.enums import AiTaskStatus, AiTaskType  # noqa: E402
from app.db.models import AiGenerationTaskModel  # noqa: E402
from app.db.session import get_db_session  # noqa: E402
from app.services.draft_service import draft_service  # noqa: E402
from app.services.publishers import (  # noqa: E402
    LearningPathPublishStatus,
    LearningPathValidationError,
    learning_path_publisher,
    learning_path_validator,
)


# Live published course ids on the techhub schema (verified at test time).
COURSE_IDS = [
    "ad0ac9c6-a60d-4895-8310-49b7b38e5009",
    "96fbbfcf-fb6f-4c92-bfb9-6a11c69c1033",
    "eb0b2e00-67d2-48a0-a249-9863ecf709fb",
]
TRUSTED_USER = str(uuid.uuid4())


@dataclass
class CaseResult:
    name: str
    passed: bool
    detail: str = ""


def _build_valid_draft(course_ids: list[str] | None = None) -> dict[str, Any]:
    cids = course_ids or COURSE_IDS
    courses = []
    nodes = []
    edges = []
    for index, cid in enumerate(cids, start=1):
        position_x = 120 + (index - 1) * 280
        position_y = 220
        courses.append(
            {
                "courseId": cid,
                "title": f"Course {index}",
                "description": "test desc",
                "thumbnail": None,
                "order": index,
                "positionX": position_x,
                "positionY": position_y,
                "isOptional": "N",
            }
        )
        nodes.append(
            {
                "id": cid,
                "type": "courseNode",
                "data": {"label": f"Course {index}", "courseId": cid},
                "position": {"x": float(position_x), "y": float(position_y)},
            }
        )
        if index > 1:
            edges.append(
                {"id": f"{cids[index - 2]}->{cid}", "source": cids[index - 2], "target": cid}
            )
    return {
        "title": "Test learning path",
        "description": "step 06 e2e",
        "skills": ["python", "react"],
        "courses": courses,
        "nodes": nodes,
        "edges": edges,
        "metadata": {"goal": "test", "duration": "4w"},
    }


# ---------------------------------------------------------------------------
# Validator cases
# ---------------------------------------------------------------------------


async def case_validator_happy_path() -> CaseResult:
    name = "V1 - valid draft with 3 real PUBLISHED courses passes validator"
    validated = await learning_path_validator.validate(_build_valid_draft())
    if len(validated.courses) != 3:
        return CaseResult(name, False, f"expected 3 courses, got {len(validated.courses)}")
    if len(validated.layout_edges) != 2:
        return CaseResult(name, False, f"expected 2 edges, got {len(validated.layout_edges)}")
    return CaseResult(name, True, f"courses={len(validated.courses)} edges={len(validated.layout_edges)} skills={list(validated.skills)}")


async def case_validator_unknown_course() -> CaseResult:
    name = "V2 - unknown courseId rejected"
    draft = _build_valid_draft()
    bogus = str(uuid.uuid4())
    draft["courses"][1]["courseId"] = bogus
    draft["nodes"][1]["id"] = bogus
    draft["edges"] = []
    try:
        await learning_path_validator.validate(draft)
    except LearningPathValidationError as exc:
        if bogus in str(exc) or "Unknown courseIds" in str(exc):
            return CaseResult(name, True, str(exc)[:120])
        return CaseResult(name, False, f"wrong error message: {exc}")
    return CaseResult(name, False, "did not raise on unknown courseId")


async def case_validator_duplicate_course() -> CaseResult:
    name = "V3 - duplicate courseId rejected"
    draft = _build_valid_draft()
    draft["courses"][1]["courseId"] = draft["courses"][0]["courseId"]
    draft["nodes"][1]["id"] = draft["nodes"][0]["id"]
    try:
        await learning_path_validator.validate(draft)
    except LearningPathValidationError as exc:
        if "Duplicate" in str(exc) or "duplicate" in str(exc):
            return CaseResult(name, True, str(exc)[:120])
        return CaseResult(name, False, f"wrong message: {exc}")
    return CaseResult(name, False, "did not raise on duplicate courseId")


async def case_validator_duplicate_order() -> CaseResult:
    name = "V4 - duplicate order rejected"
    draft = _build_valid_draft()
    draft["courses"][1]["order"] = draft["courses"][0]["order"]
    try:
        await learning_path_validator.validate(draft)
    except LearningPathValidationError as exc:
        if "order" in str(exc).lower():
            return CaseResult(name, True, str(exc)[:120])
        return CaseResult(name, False, f"wrong message: {exc}")
    return CaseResult(name, False, "did not raise on duplicate order")


async def case_validator_bad_is_optional() -> CaseResult:
    name = "V5 - isOptional bad value rejected"
    draft = _build_valid_draft()
    draft["courses"][1]["isOptional"] = "MAYBE"
    try:
        await learning_path_validator.validate(draft)
    except LearningPathValidationError as exc:
        if "isOptional" in str(exc):
            return CaseResult(name, True, str(exc)[:120])
        return CaseResult(name, False, f"wrong message: {exc}")
    return CaseResult(name, False, "did not raise on bad isOptional")


async def case_validator_dangling_edge() -> CaseResult:
    name = "V6 - edge to non-existent node rejected"
    draft = _build_valid_draft()
    draft["edges"].append(
        {"id": "ghost", "source": draft["courses"][0]["courseId"], "target": str(uuid.uuid4())}
    )
    try:
        await learning_path_validator.validate(draft)
    except LearningPathValidationError as exc:
        if "target" in str(exc).lower() and "not in courses" in str(exc):
            return CaseResult(name, True, str(exc)[:120])
        return CaseResult(name, False, f"wrong message: {exc}")
    return CaseResult(name, False, "did not raise on dangling edge")


async def case_validator_self_loop() -> CaseResult:
    name = "V7 - self-loop edge rejected"
    draft = _build_valid_draft()
    cid = draft["courses"][0]["courseId"]
    draft["edges"].append({"id": "self", "source": cid, "target": cid})
    try:
        await learning_path_validator.validate(draft)
    except LearningPathValidationError as exc:
        if "self-loop" in str(exc).lower():
            return CaseResult(name, True, str(exc)[:120])
        return CaseResult(name, False, f"wrong message: {exc}")
    return CaseResult(name, False, "did not raise on self-loop")


async def case_validator_cycle() -> CaseResult:
    name = "V8 - cycle in graph rejected"
    draft = _build_valid_draft()
    a, b, c = draft["courses"][0]["courseId"], draft["courses"][1]["courseId"], draft["courses"][2]["courseId"]
    draft["edges"] = [
        {"id": "ab", "source": a, "target": b},
        {"id": "bc", "source": b, "target": c},
        {"id": "ca", "source": c, "target": a},
    ]
    try:
        await learning_path_validator.validate(draft)
    except LearningPathValidationError as exc:
        if "Cycle" in str(exc) or "cycle" in str(exc):
            return CaseResult(name, True, str(exc)[:120])
        return CaseResult(name, False, f"wrong message: {exc}")
    return CaseResult(name, False, "did not raise on cycle")


# ---------------------------------------------------------------------------
# Publisher cases (mocked httpx)
# ---------------------------------------------------------------------------


def _mock_response(status_code: int, body: dict[str, Any] | str | None = None) -> httpx.Response:
    if isinstance(body, dict):
        return httpx.Response(status_code, json=body, request=httpx.Request("POST", "http://mock"))
    return httpx.Response(
        status_code,
        text=body or "",
        request=httpx.Request("POST", "http://mock"),
    )


async def case_publisher_happy_path() -> CaseResult:
    name = "P1 - publisher happy path: POST create + POST addCourses -> PUBLISHED"
    draft = _build_valid_draft()
    path_id = str(uuid.uuid4())
    client = AsyncMock(spec=httpx.AsyncClient)
    client.post = AsyncMock(side_effect=[
        _mock_response(201, {"data": {"id": path_id}}),
        _mock_response(200, {"data": {"courses": []}}),
    ])
    with patch.object(get_settings(), "learning_path_service_base_url", "http://lp-mock"):
        outcome = await learning_path_publisher.publish(
            draft, trusted_user_id=TRUSTED_USER, http_client=client
        )
    if outcome.status != LearningPathPublishStatus.PUBLISHED:
        return CaseResult(name, False, f"expected PUBLISHED, got {outcome.status}: {outcome.error}")
    if outcome.learning_path_id != path_id:
        return CaseResult(name, False, f"expected pathId={path_id}, got {outcome.learning_path_id}")
    if client.post.await_count != 2:
        return CaseResult(name, False, f"expected 2 POST calls, got {client.post.await_count}")
    return CaseResult(name, True, f"learningPathId={path_id}")


async def case_publisher_create_failed() -> CaseResult:
    name = "P2 - create returns 500 -> PUBLISH_FAILED, no addCourses call"
    draft = _build_valid_draft()
    client = AsyncMock(spec=httpx.AsyncClient)
    client.post = AsyncMock(side_effect=[
        _mock_response(500, "boom"),
    ])
    with patch.object(get_settings(), "learning_path_service_base_url", "http://lp-mock"):
        outcome = await learning_path_publisher.publish(
            draft, trusted_user_id=TRUSTED_USER, http_client=client
        )
    if outcome.status != LearningPathPublishStatus.PUBLISH_FAILED:
        return CaseResult(name, False, f"expected PUBLISH_FAILED, got {outcome.status}")
    if outcome.learning_path_id is not None:
        return CaseResult(name, False, "should not have learningPathId when create failed")
    if client.post.await_count != 1:
        return CaseResult(name, False, f"expected 1 POST call, got {client.post.await_count}")
    return CaseResult(name, True, outcome.error[:120] if outcome.error else "no error msg")


async def case_publisher_add_courses_failed() -> CaseResult:
    name = "P3 - addCourses fails after create succeeds -> PUBLISH_FAILED with partial pathId"
    draft = _build_valid_draft()
    path_id = str(uuid.uuid4())
    client = AsyncMock(spec=httpx.AsyncClient)
    client.post = AsyncMock(side_effect=[
        _mock_response(201, {"data": {"id": path_id}}),
        _mock_response(400, "bad request"),
    ])
    with patch.object(get_settings(), "learning_path_service_base_url", "http://lp-mock"):
        outcome = await learning_path_publisher.publish(
            draft, trusted_user_id=TRUSTED_USER, http_client=client
        )
    if outcome.status != LearningPathPublishStatus.PUBLISH_FAILED:
        return CaseResult(name, False, f"expected PUBLISH_FAILED, got {outcome.status}")
    if outcome.learning_path_id != path_id:
        return CaseResult(name, False, f"expected partial pathId={path_id}, got {outcome.learning_path_id}")
    return CaseResult(name, True, f"partial pathId={path_id}, error includes 'addCourses': {('addCourses' in (outcome.error or ''))}")


async def case_publisher_missing_identity() -> CaseResult:
    name = "P4 - missing trusted_user_id -> PUBLISH_FAILED, no HTTP call"
    draft = _build_valid_draft()
    client = AsyncMock(spec=httpx.AsyncClient)
    client.post = AsyncMock()
    with patch.object(get_settings(), "learning_path_service_base_url", "http://lp-mock"):
        outcome = await learning_path_publisher.publish(
            draft, trusted_user_id=None, http_client=client
        )
    if outcome.status != LearningPathPublishStatus.PUBLISH_FAILED:
        return CaseResult(name, False, f"expected PUBLISH_FAILED, got {outcome.status}")
    if client.post.await_count != 0:
        return CaseResult(name, False, "publisher must not make HTTP call without trusted identity")
    return CaseResult(name, True, outcome.error[:120] if outcome.error else "")


async def case_publisher_no_base_url() -> CaseResult:
    name = "P5 - no base URL configured -> SKIPPED_NO_BASE_URL"
    draft = _build_valid_draft()
    client = AsyncMock(spec=httpx.AsyncClient)
    client.post = AsyncMock()
    with patch.object(get_settings(), "learning_path_service_base_url", None):
        outcome = await learning_path_publisher.publish(
            draft, trusted_user_id=TRUSTED_USER, http_client=client
        )
    if outcome.status != LearningPathPublishStatus.SKIPPED_NO_BASE_URL:
        return CaseResult(name, False, f"expected SKIPPED_NO_BASE_URL, got {outcome.status}")
    if client.post.await_count != 0:
        return CaseResult(name, False, "publisher must not make HTTP call when base URL is missing")
    return CaseResult(name, True, "skipped cleanly")


async def case_publisher_validation_failure() -> CaseResult:
    name = "P6 - validation failure inside publish -> PUBLISH_FAILED (validation_failed prefix)"
    draft = _build_valid_draft()
    draft["courses"][1]["courseId"] = str(uuid.uuid4())
    client = AsyncMock(spec=httpx.AsyncClient)
    client.post = AsyncMock()
    with patch.object(get_settings(), "learning_path_service_base_url", "http://lp-mock"):
        outcome = await learning_path_publisher.publish(
            draft, trusted_user_id=TRUSTED_USER, http_client=client
        )
    if outcome.status != LearningPathPublishStatus.PUBLISH_FAILED:
        return CaseResult(name, False, f"expected PUBLISH_FAILED, got {outcome.status}")
    if not outcome.error or not outcome.error.startswith("validation_failed"):
        return CaseResult(name, False, f"expected validation_failed prefix, got: {outcome.error}")
    if client.post.await_count != 0:
        return CaseResult(name, False, "publisher must not call HTTP when validation fails")
    return CaseResult(name, True, outcome.error[:120])


# ---------------------------------------------------------------------------
# Approve flow E2E (DB live + mocked httpx)
# ---------------------------------------------------------------------------


async def _create_draft_row(payload: dict[str, Any]) -> UUID:
    async with get_db_session() as session:
        task = AiGenerationTaskModel(
            task_type=AiTaskType.LEARNING_PATH_GENERATION.value,
            status=AiTaskStatus.DRAFT.value,
            target_reference="step06-e2e",
            request_payload={"source": "step06-test"},
            result_payload=payload,
            prompt="test",
            model_used="step06-test",
        )
        session.add(task)
        await session.flush()
        return task.id


async def _cleanup_draft_row(task_id: UUID) -> None:
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
    draft = _build_valid_draft()
    task_id = await _create_draft_row(draft)
    try:
        path_id = str(uuid.uuid4())
        client = AsyncMock(spec=httpx.AsyncClient)
        client.post = AsyncMock(side_effect=[
            _mock_response(201, {"data": {"id": path_id}}),
            _mock_response(200, {"data": {"courses": []}}),
        ])
        client.aclose = AsyncMock()
        with patch.object(get_settings(), "learning_path_service_base_url", "http://lp-mock"), \
             patch("httpx.AsyncClient", return_value=client):
            response = await draft_service.approve_learning_path(
                task_id, trusted_user_id=TRUSTED_USER
            )
        if not response.success:
            return CaseResult(name, False, f"approve returned success=False: {response.message}")
        task = await _load_task(task_id)
        if task.status != AiTaskStatus.PUBLISHED.value:
            return CaseResult(name, False, f"DB status should be PUBLISHED, got {task.status}")
        publish_meta = (task.result_payload or {}).get("publish") if isinstance(task.result_payload, dict) else None
        if not publish_meta or publish_meta.get("learningPathId") != path_id:
            return CaseResult(name, False, f"publish metadata not persisted correctly: {publish_meta}")
        return CaseResult(name, True, f"DB status={task.status}, publish.learningPathId={path_id}")
    finally:
        await _cleanup_draft_row(task_id)


async def case_approve_flow_publish_failed() -> CaseResult:
    name = "A2 - approve flow with HTTP 500 from LP service: DB status = PUBLISH_FAILED"
    draft = _build_valid_draft()
    task_id = await _create_draft_row(draft)
    try:
        client = AsyncMock(spec=httpx.AsyncClient)
        client.post = AsyncMock(side_effect=[_mock_response(500, "boom")])
        client.aclose = AsyncMock()
        with patch.object(get_settings(), "learning_path_service_base_url", "http://lp-mock"), \
             patch("httpx.AsyncClient", return_value=client):
            response = await draft_service.approve_learning_path(
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
        await _cleanup_draft_row(task_id)


async def case_approve_flow_no_base_url() -> CaseResult:
    name = "A3 - approve flow without LEARNING_PATH_SERVICE_BASE_URL: DB status = APPROVED, no error"
    draft = _build_valid_draft()
    task_id = await _create_draft_row(draft)
    try:
        with patch.object(get_settings(), "learning_path_service_base_url", None):
            response = await draft_service.approve_learning_path(
                task_id, trusted_user_id=TRUSTED_USER
            )
        if not response.success:
            return CaseResult(name, False, f"expected success when skipped: {response.message}")
        task = await _load_task(task_id)
        if task.status != AiTaskStatus.APPROVED.value:
            return CaseResult(name, False, f"expected APPROVED, got {task.status}")
        return CaseResult(name, True, f"DB status={task.status}, publish skipped without error")
    finally:
        await _cleanup_draft_row(task_id)


async def main() -> int:
    cases = [
        case_validator_happy_path,
        case_validator_unknown_course,
        case_validator_duplicate_course,
        case_validator_duplicate_order,
        case_validator_bad_is_optional,
        case_validator_dangling_edge,
        case_validator_self_loop,
        case_validator_cycle,
        case_publisher_happy_path,
        case_publisher_create_failed,
        case_publisher_add_courses_failed,
        case_publisher_missing_identity,
        case_publisher_no_base_url,
        case_publisher_validation_failure,
        case_approve_flow_publish_success,
        case_approve_flow_publish_failed,
        case_approve_flow_no_base_url,
    ]
    results: list[CaseResult] = []
    for func in cases:
        print(f"\n--- {func.__name__}")
        try:
            result = await func()
        except Exception as exc:  # noqa: BLE001
            tb = "".join(traceback.format_exception_only(type(exc), exc)).strip()
            result = CaseResult(func.__name__, False, f"EXC: {tb}")
        print(("PASS " if result.passed else "FAIL ") + result.name + " :: " + result.detail)
        results.append(result)

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
