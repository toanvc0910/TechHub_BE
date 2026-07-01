"""
Step 10 - End-to-End Observability & Release E2E smoke test.

Drives the release-readiness service + admin observability against live infra
(PostgreSQL + Qdrant) and asserts the new Step-10 wiring:

  O1 - record_publish_event increments per-kind/per-status counters AND
       advances last_success / last_failure timestamps in observability.
  O2 - LP publisher PUBLISH path -> publish:learning_path:PUBLISHED counter +1
  O3 - LP publisher PUBLISH_FAILED path -> publish:learning_path:PUBLISH_FAILED
       counter +1
  O4 - Exercise publisher PUBLISH path -> publish:exercise:PUBLISHED counter +1
  R1 - release_readiness_service.snapshot() returns required top-level keys
       (capabilities, featureReadiness, publishAudit, dataContract, runtime).
  R2 - Each capability entry has status in CAPABILITY_STATUS enum + reason.
  R3 - publishAudit reads from ai_generation_tasks in real time: insert a
       PUBLISHED row -> snapshot shows >=1 PUBLISHED for that kind.
  R4 - Admin endpoint `/admin/release-readiness` returns same payload via
       FastAPI TestClient + admin trusted headers.

Run from the service root:

    python -m tests.test_step10_release_observability
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

SERVICE_ROOT = Path(__file__).resolve().parent.parent
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

import httpx  # noqa: E402
from sqlalchemy import delete  # noqa: E402

from app.core.config import get_settings  # noqa: E402
from app.core.enums import AiTaskStatus, AiTaskType  # noqa: E402
from app.db.models import AiGenerationTaskModel  # noqa: E402
from app.db.session import get_db_session  # noqa: E402
from app.services.observability_service import runtime_observability_service  # noqa: E402
from app.services.publishers import (  # noqa: E402
    exercise_publisher,
    learning_path_publisher,
)
from app.services.release_readiness_service import (  # noqa: E402
    CAPABILITY_STATUS,
    release_readiness_service,
)


REAL_COURSE_IDS = [
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


def _good_lp_draft() -> dict[str, Any]:
    courses = []
    nodes = []
    edges = []
    for index, cid in enumerate(REAL_COURSE_IDS, start=1):
        courses.append(
            {
                "courseId": cid,
                "title": f"Course {index}",
                "description": "test",
                "order": index,
                "positionX": 120 + (index - 1) * 280,
                "positionY": 220,
                "isOptional": "N",
            }
        )
        nodes.append({"id": cid, "type": "courseNode", "data": {"label": "x", "courseId": cid}, "position": {"x": 0, "y": 0}})
        if index > 1:
            edges.append({"id": f"e{index}", "source": REAL_COURSE_IDS[index - 2], "target": cid})
    return {
        "title": "Step 10 release smoke path",
        "description": "release smoke",
        "skills": ["python"],
        "courses": courses,
        "nodes": nodes,
        "edges": edges,
    }


def _mock_response(status_code: int, body: dict | list | str | None = None) -> httpx.Response:
    if isinstance(body, (dict, list)):
        return httpx.Response(status_code, json=body, request=httpx.Request("POST", "http://mock"))
    return httpx.Response(status_code, text=body or "", request=httpx.Request("POST", "http://mock"))


async def _snapshot_counters() -> dict[str, int]:
    snap = await runtime_observability_service.snapshot()
    return dict(snap.get("counters") or {})


# ---------------------------------------------------------------------------
# Observability tracking cases
# ---------------------------------------------------------------------------


async def case_record_publish_event_increments() -> CaseResult:
    name = "O1 - record_publish_event increments counters + timestamps"
    before = await _snapshot_counters()
    before_pub_total = int(before.get("publish:learning_path:total", 0))
    before_pub_ok = int(before.get("publish:learning_path:PUBLISHED", 0))
    before_pub_fail = int(before.get("publish:learning_path:PUBLISH_FAILED", 0))

    await runtime_observability_service.record_publish_event(
        kind="learning_path", status="PUBLISHED", target_id=str(uuid.uuid4())
    )
    await runtime_observability_service.record_publish_event(
        kind="learning_path", status="PUBLISH_FAILED", error="test failure"
    )

    after = await runtime_observability_service.snapshot()
    after_counters = dict(after.get("counters") or {})
    after_ts = (after.get("vectorOps") or {}).get("timestamps") or {}

    pub_total = int(after_counters.get("publish:learning_path:total", 0))
    pub_ok = int(after_counters.get("publish:learning_path:PUBLISHED", 0))
    pub_fail = int(after_counters.get("publish:learning_path:PUBLISH_FAILED", 0))

    if pub_total - before_pub_total != 2:
        return CaseResult(name, False, f"total counter +1+1 expected, got {pub_total - before_pub_total}")
    if pub_ok - before_pub_ok != 1:
        return CaseResult(name, False, f"PUBLISHED counter +1 expected, got {pub_ok - before_pub_ok}")
    if pub_fail - before_pub_fail != 1:
        return CaseResult(name, False, f"PUBLISH_FAILED counter +1 expected, got {pub_fail - before_pub_fail}")
    if "publish:last_success:learning_path" not in after_ts:
        return CaseResult(name, False, "last_success timestamp not recorded")
    if "publish:last_failure:learning_path" not in after_ts:
        return CaseResult(name, False, "last_failure timestamp not recorded")
    return CaseResult(name, True, f"counters total={pub_total} ok={pub_ok} fail={pub_fail}")


async def case_lp_publisher_records_published() -> CaseResult:
    name = "O2 - LP publisher PUBLISHED path increments publish:learning_path:PUBLISHED"
    before = await _snapshot_counters()
    before_ok = int(before.get("publish:learning_path:PUBLISHED", 0))
    path_id = str(uuid.uuid4())
    client = AsyncMock(spec=httpx.AsyncClient)
    client.post = AsyncMock(side_effect=[
        _mock_response(201, {"data": {"id": path_id}}),
        _mock_response(200, {"data": {"courses": []}}),
    ])
    with patch.object(get_settings(), "learning_path_service_base_url", "http://lp-mock"):
        outcome = await learning_path_publisher.publish(
            _good_lp_draft(), trusted_user_id=TRUSTED_USER, http_client=client
        )
    if outcome.status.value != "PUBLISHED":
        return CaseResult(name, False, f"unexpected outcome: {outcome.status}: {outcome.error}")
    after = await _snapshot_counters()
    delta = int(after.get("publish:learning_path:PUBLISHED", 0)) - before_ok
    if delta != 1:
        return CaseResult(name, False, f"PUBLISHED counter delta=1 expected, got {delta}")
    return CaseResult(name, True, f"learning_path PUBLISHED counter advanced by 1")


async def case_lp_publisher_records_failure() -> CaseResult:
    name = "O3 - LP publisher PUBLISH_FAILED path increments publish:learning_path:PUBLISH_FAILED"
    before = await _snapshot_counters()
    before_fail = int(before.get("publish:learning_path:PUBLISH_FAILED", 0))
    client = AsyncMock(spec=httpx.AsyncClient)
    client.post = AsyncMock(return_value=_mock_response(500, "boom"))
    with patch.object(get_settings(), "learning_path_service_base_url", "http://lp-mock"):
        outcome = await learning_path_publisher.publish(
            _good_lp_draft(), trusted_user_id=TRUSTED_USER, http_client=client
        )
    if outcome.status.value != "PUBLISH_FAILED":
        return CaseResult(name, False, f"unexpected outcome: {outcome.status}: {outcome.error}")
    after = await _snapshot_counters()
    delta = int(after.get("publish:learning_path:PUBLISH_FAILED", 0)) - before_fail
    if delta != 1:
        return CaseResult(name, False, f"PUBLISH_FAILED counter delta=1 expected, got {delta}")
    return CaseResult(name, True, "learning_path PUBLISH_FAILED counter advanced by 1")


async def case_exercise_publisher_records_published() -> CaseResult:
    name = "O4 - Exercise publisher PUBLISHED path increments publish:exercise:PUBLISHED"
    # Use mocked validator to avoid needing a seeded lesson fixture here.
    before = await _snapshot_counters()
    before_ok = int(before.get("publish:exercise:PUBLISHED", 0))
    fake_batch = type(
        "FakeBatch",
        (),
        {
            "course_id": REAL_COURSE_IDS[0],
            "lesson_id": str(uuid.uuid4()),
            "lesson_title": "fake",
            "exercises": (),
        },
    )()
    # Build a minimal mocked validator that returns a batch with one exercise
    # so request_payload is non-empty. We patch the publisher's validator dep.
    from app.services.publishers.exercise_validator import ValidatedExercise

    fake_batch.exercises = (
        ValidatedExercise(
            type="MULTIPLE_CHOICE",
            ai_format="mcq",
            question="What is x?",
            options=[{"text": "a", "correct": True}, {"text": "b", "correct": False}],
            test_cases=[],
            difficulty="BEGINNER",
            explanation=None,
            order_index=1,
        ),
    )

    exercise_id = str(uuid.uuid4())
    client = AsyncMock(spec=httpx.AsyncClient)
    client.post = AsyncMock(return_value=_mock_response(200, {"data": [{"id": exercise_id}]}))

    with patch.object(get_settings(), "course_service_base_url", "http://course-mock"), \
         patch(
             "app.services.publishers.exercise_publisher.exercise_validator.validate",
             AsyncMock(return_value=fake_batch),
         ):
        outcome = await exercise_publisher.publish(
            course_id=REAL_COURSE_IDS[0],
            lesson_id=str(uuid.uuid4()),
            payload={"mcq": [{}]},  # validator mocked so this is ignored
            trusted_user_id=TRUSTED_USER,
            http_client=client,
        )
    if outcome.status.value != "PUBLISHED":
        return CaseResult(name, False, f"unexpected outcome: {outcome.status}: {outcome.error}")
    after = await _snapshot_counters()
    delta = int(after.get("publish:exercise:PUBLISHED", 0)) - before_ok
    if delta != 1:
        return CaseResult(name, False, f"PUBLISHED counter delta=1 expected, got {delta}")
    return CaseResult(name, True, "exercise PUBLISHED counter advanced by 1")


# ---------------------------------------------------------------------------
# Release readiness service cases
# ---------------------------------------------------------------------------


async def case_readiness_snapshot_shape() -> CaseResult:
    name = "R1 - readiness snapshot has all required top-level keys"
    snap = await release_readiness_service.snapshot()
    required = {"generatedAt", "capabilities", "featureReadiness", "publishAudit", "dataContract", "runtime"}
    missing = required.difference(snap.keys())
    if missing:
        return CaseResult(name, False, f"missing keys: {missing}")
    if not isinstance(snap.get("capabilities"), dict) or not snap["capabilities"]:
        return CaseResult(name, False, "capabilities empty or wrong type")
    return CaseResult(name, True, f"top-level keys: {sorted(snap.keys())}")


async def case_capabilities_status_valid() -> CaseResult:
    name = "R2 - every capability has status in CAPABILITY_STATUS"
    snap = await release_readiness_service.snapshot()
    capabilities = snap.get("capabilities") or {}
    expected_caps = {
        "recommendation",
        "analytics_personal",
        "analytics_platform",
        "learning_path_publish",
        "exercise_publish",
        "vector_index",
    }
    missing = expected_caps.difference(capabilities.keys())
    if missing:
        return CaseResult(name, False, f"missing capability keys: {missing}")
    invalid = []
    for cap, info in capabilities.items():
        status = info.get("status")
        if status not in CAPABILITY_STATUS:
            invalid.append((cap, status))
        if not info.get("reason"):
            invalid.append((cap, "no_reason"))
    if invalid:
        return CaseResult(name, False, f"bad status/reason entries: {invalid}")
    overview = {cap: capabilities[cap]["status"] for cap in expected_caps}
    return CaseResult(name, True, f"capability statuses: {overview}")


async def case_publish_audit_reflects_db() -> CaseResult:
    name = "R3 - publishAudit reflects ai_generation_tasks counts in real time"
    # Insert a PUBLISHED LP row and assert it appears in the audit; cleanup after.
    task_id = uuid.uuid4()
    async with get_db_session() as session:
        task = AiGenerationTaskModel(
            id=task_id,
            task_type=AiTaskType.LEARNING_PATH_GENERATION.value,
            status=AiTaskStatus.PUBLISHED.value,
            target_reference="step10-test",
            request_payload={"step": 10},
            result_payload={"publish": {"learningPathId": str(uuid.uuid4())}},
            prompt="test",
            model_used="step10-test",
        )
        session.add(task)
        await session.flush()
    try:
        snap = await release_readiness_service.snapshot()
        lp_audit = (snap.get("publishAudit") or {}).get("learning_path") or {}
        published = int(lp_audit.get("PUBLISHED", 0))
        last_at = lp_audit.get("lastPublishedAt")
        if published < 1:
            return CaseResult(name, False, f"expected PUBLISHED >= 1 after insert, got {published}")
        if not last_at:
            return CaseResult(name, False, "lastPublishedAt not set in audit")
        # And the capability should now be E2E_VERIFIED
        cap = (snap.get("capabilities") or {}).get("learning_path_publish") or {}
        if cap.get("status") != "E2E_VERIFIED":
            return CaseResult(
                name,
                False,
                f"capability learning_path_publish status should be E2E_VERIFIED, got {cap.get('status')} ({cap.get('reason')})",
            )
        return CaseResult(name, True, f"PUBLISHED={published} lastPublishedAt={last_at} capStatus={cap.get('status')}")
    finally:
        async with get_db_session() as session:
            await session.execute(delete(AiGenerationTaskModel).where(AiGenerationTaskModel.id == task_id))


async def case_admin_endpoint_returns_snapshot() -> CaseResult:
    name = "R4 - /admin/release-readiness returns the same snapshot via FastAPI"
    from fastapi.testclient import TestClient

    from app.main import app

    async with httpx.AsyncClient():
        pass

    client = TestClient(app)
    # Admin context dependency expects trusted headers with admin role.
    response = client.get(
        "/api/ai/admin/release-readiness",
        headers={
            "X-User-Id": TRUSTED_USER,
            "X-User-Email": "admin@techhub.local",
            "X-User-Roles": "ADMIN",
            "X-Request-Source": "proxy-client",
        },
    )
    if response.status_code != 200:
        return CaseResult(name, False, f"HTTP {response.status_code}: {response.text[:200]}")
    body = response.json()
    data = body.get("data") if isinstance(body, dict) else None
    if not isinstance(data, dict) or "capabilities" not in data:
        return CaseResult(name, False, f"response missing data.capabilities: {body}")
    return CaseResult(name, True, f"admin endpoint returned {len(data['capabilities'])} capabilities")


# ---------------------------------------------------------------------------
# Runner
# ---------------------------------------------------------------------------


async def main() -> int:
    cases = [
        case_record_publish_event_increments,
        case_lp_publisher_records_published,
        case_lp_publisher_records_failure,
        case_exercise_publisher_records_published,
        case_readiness_snapshot_shape,
        case_capabilities_status_valid,
        case_publish_audit_reflects_db,
        case_admin_endpoint_returns_snapshot,
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
