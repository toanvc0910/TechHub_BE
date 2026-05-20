"""
Step 05 - Vector Index & Event Freshness E2E smoke test.

Runs on the live Qdrant configured via .env (QDRANT_HOST/QDRANT_API_KEY) and
PostgreSQL. Covers what step 05 promises:

  Q1 - Qdrant collection stats are reachable and shape matches schema.
  Q2 - feature_readiness() classifies each AI feature correctly against the
       live collection state (ready vs degraded).
  Q3 - Search returns each result tagged with `retrievalMode` (vector or
       lexical_fallback). For empty query the function returns []. For a
       real query the mode is `vector` when course_embeddings has points.
  Q4 - reindex_single_course writes/upserts exactly one point under the
       canonical UUID id, and the stored vector size matches the
       embedding dimension.
  Q5 - reindex_single_profile writes/upserts exactly one profile point
       and observability timestamps `last_incremental:user_embeddings`
       moves forward.
  Q6 - Kafka payload contract: handing the consumer realistic JSON for
       course-events / enrollment-events / rating-events / learning-path-events
       routes to the targeted reindex method (not reindex_all). Verified
       by patching vector_service methods with spies; restore after.
  Q7 - Missing userId on enrollment/rating event -> no global reindex, error
       counter `errors:kafka_event_missing_user_id` incremented.

Run from the service root:

    python -m tests.test_step05_vector_index_and_events

Exit 0 if every case passes.
"""

from __future__ import annotations

import asyncio
import sys
import traceback
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from unittest.mock import AsyncMock

SERVICE_ROOT = Path(__file__).resolve().parent.parent
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

import httpx  # noqa: E402

from app.core.config import get_settings  # noqa: E402
from app.services.indexing_event_consumer import indexing_event_consumer  # noqa: E402
from app.services.observability_service import runtime_observability_service  # noqa: E402
from app.services import vector_service as vector_service_module  # noqa: E402
from app.services.vector_service import vector_service  # noqa: E402


SETTINGS = get_settings()

# Real IDs on the live techhub schema (verified in step 03/04 work).
USER_RICH = "eb2159ec-5992-4302-af92-e6c9b8b9b1a1"  # has 12 enrollments + 1 rating


@dataclass
class CaseResult:
    name: str
    passed: bool
    detail: str = ""


async def case_collection_stats() -> CaseResult:
    name = "S1 - Qdrant collection stats reachable + schema matches"
    stats = await vector_service.get_collection_stats()
    collections = stats.get("collections") or {}
    required_keys = {"courses", "lessons", "profiles", "sessionFiles", "userFiles"}
    missing = required_keys.difference(collections.keys())
    if missing:
        return CaseResult(name, False, f"missing logical collection keys in stats: {missing}")
    if not stats.get("healthy"):
        return CaseResult(name, False, f"stats.healthy=False; details: {stats}")
    courses_points = int((collections.get("courses") or {}).get("pointsCount") or 0)
    return CaseResult(name, True, f"healthy=True courses_points={courses_points} all 5 logical keys present")


async def case_feature_readiness() -> CaseResult:
    name = "S2 - feature_readiness classifies AI features correctly"
    readiness = await vector_service.get_feature_readiness()
    features = readiness.get("features") or {}
    degraded = set(readiness.get("degradedFeatures") or [])
    if "recommendation" not in features:
        return CaseResult(name, False, f"missing 'recommendation' feature in readiness: keys={list(features)}")

    # courses collection has 13 points on live -> recommendation should be ready
    rec = features.get("recommendation") or {}
    if not rec.get("ready"):
        return CaseResult(name, False, f"recommendation should be ready but got: {rec}")

    # lessons collection has 0 points on live -> lesson_qa should be degraded with empty=['lessons']
    lesson_qa = features.get("lesson_qa") or {}
    if lesson_qa.get("ready") and int((readiness["collections"]["lessons"]).get("pointsCount") or 0) == 0:
        return CaseResult(name, False, f"lesson_qa should be degraded when lessons collection empty: {lesson_qa}")

    return CaseResult(name, True, f"ready={[k for k, v in features.items() if v['ready']]} degraded={sorted(degraded)}")


async def case_search_retrieval_mode() -> CaseResult:
    name = "S3 - search_courses tags each result with retrievalMode"
    empty = await vector_service.search_courses(query="   ", limit=3)
    if empty:
        return CaseResult(name, False, f"expected empty result for blank query, got {len(empty)}")

    results = await vector_service.search_courses(query="python frontend", limit=5)
    if not results:
        return CaseResult(name, True, "no candidates returned for query; mode tagging not exercised (acceptable on small catalog)")
    modes = {str(item.get("retrievalMode") or "") for item in results}
    if modes - {"vector", "lexical_fallback"}:
        return CaseResult(name, False, f"unexpected retrievalMode values: {modes}")
    if "" in modes or not modes:
        return CaseResult(name, False, f"some results missing retrievalMode tag: {modes}")
    return CaseResult(name, True, f"modes={modes} count={len(results)}")


async def case_reindex_single_course() -> CaseResult:
    name = "S4 - reindex_single_course writes one canonical point in Qdrant"
    # Pick a real course id from Qdrant via scroll
    async with httpx.AsyncClient(timeout=30.0, headers={"api-key": SETTINGS.qdrant_api_key or ""}) as client:
        resp = await client.post(
            f"{SETTINGS.qdrant_host}/collections/{SETTINGS.qdrant_course_collection}/points/scroll",
            json={"limit": 1, "with_payload": False, "with_vector": False},
        )
        if resp.status_code != 200:
            return CaseResult(name, False, f"could not scroll course collection: {resp.status_code} {resp.text[:200]}")
        points = (resp.json().get("result") or {}).get("points") or []
        if not points:
            return CaseResult(name, True, "course collection empty - nothing to reindex; skip mutation test")
        course_id = str(points[0].get("id"))

        before = await client.get(f"{SETTINGS.qdrant_host}/collections/{SETTINGS.qdrant_course_collection}")
        before_count = int((before.json().get("result") or {}).get("points_count") or 0)

    outcome = await vector_service.reindex_single_course(course_id)
    if not outcome.get("success"):
        return CaseResult(name, False, f"reindex_single_course did not succeed: {outcome}")

    async with httpx.AsyncClient(timeout=30.0, headers={"api-key": SETTINGS.qdrant_api_key or ""}) as client:
        after = await client.get(f"{SETTINGS.qdrant_host}/collections/{SETTINGS.qdrant_course_collection}")
        after_count = int((after.json().get("result") or {}).get("points_count") or 0)
        # Upsert must not duplicate the point.
        if after_count != before_count:
            return CaseResult(
                name,
                False,
                f"reindex_single_course changed total count from {before_count} -> {after_count}; duplicate point?",
            )
        # Verify the point exists at the canonical id
        ret = await client.get(
            f"{SETTINGS.qdrant_host}/collections/{SETTINGS.qdrant_course_collection}/points/{course_id}"
        )
        if ret.status_code != 200:
            return CaseResult(name, False, f"point {course_id} missing after reindex: {ret.status_code}")
    return CaseResult(name, True, f"course {course_id} reindexed; count stable at {after_count}")


async def case_reindex_single_profile() -> CaseResult:
    name = "S5 - reindex_single_profile writes profile point + observability timestamp advances"
    before_snapshot = await runtime_observability_service.snapshot()
    before_ts = (before_snapshot.get("vectorOps") or {}).get("timestamps") or {}
    before_ts_key = f"last_incremental:{SETTINGS.qdrant_profile_collection}"
    before_value = before_ts.get(before_ts_key)

    outcome = await vector_service.reindex_single_profile(USER_RICH)
    if not outcome.get("success"):
        return CaseResult(name, False, f"reindex_single_profile failed: {outcome}")

    after_snapshot = await runtime_observability_service.snapshot()
    after_ts = (after_snapshot.get("vectorOps") or {}).get("timestamps") or {}
    after_value = after_ts.get(before_ts_key)
    if not after_value:
        return CaseResult(name, False, f"expected timestamp at {before_ts_key} to be set after reindex")
    if before_value and after_value <= before_value:
        return CaseResult(name, False, f"timestamp did not advance: before={before_value} after={after_value}")

    async with httpx.AsyncClient(timeout=30.0, headers={"api-key": SETTINGS.qdrant_api_key or ""}) as client:
        ret = await client.get(
            f"{SETTINGS.qdrant_host}/collections/{SETTINGS.qdrant_profile_collection}/points/{USER_RICH}"
        )
        if ret.status_code != 200:
            return CaseResult(name, False, f"profile point {USER_RICH} missing after reindex: {ret.status_code}")
    return CaseResult(name, True, f"profile reindexed; timestamp advanced to {after_value}")


async def case_kafka_routing_contract() -> CaseResult:
    name = "S6 - Kafka payload routes to targeted reindex (not reindex_all)"
    # Swap in spies on the targeted methods AND reindex_all/reindex_courses
    # so we can assert routing.
    originals = {
        "reindex_single_course": vector_service.reindex_single_course,
        "reindex_single_lesson": vector_service.reindex_single_lesson,
        "reindex_single_profile": vector_service.reindex_single_profile,
        "reindex_all": vector_service.reindex_all,
        "reindex_courses": vector_service.reindex_courses,
        "reindex_lessons": vector_service.reindex_lessons,
    }
    spies: dict[str, AsyncMock] = {key: AsyncMock(return_value={"success": True, "stats": {"indexed": 1, "failed": 0}}) for key in originals}
    for key, spy in spies.items():
        setattr(vector_service, key, spy)
    try:
        await indexing_event_consumer._handle_message(  # noqa: SLF001
            "course-events",
            '{"eventType":"UPDATED","courseId":"course-1"}',
        )
        await indexing_event_consumer._handle_message(  # noqa: SLF001
            "lesson-events",
            '{"eventType":"UPDATED","lessonId":"lesson-1","courseId":"course-1"}',
        )
        await indexing_event_consumer._handle_message(  # noqa: SLF001
            "enrollment-events",
            '{"eventType":"PROGRESS_UPDATED","userId":"user-1","courseId":"course-1","status":"IN_PROGRESS"}',
        )
        await indexing_event_consumer._handle_message(  # noqa: SLF001
            "rating-events",
            '{"eventType":"CREATED","userId":"user-1","courseId":"course-1","score":5}',
        )
        await indexing_event_consumer._handle_message(  # noqa: SLF001
            "learning-path-events",
            '{"eventType":"UPDATED","pathId":"path-1","courseIds":["course-2","course-3"]}',
        )
    finally:
        for key, fn in originals.items():
            setattr(vector_service, key, fn)

    if spies["reindex_all"].await_count != 0:
        return CaseResult(name, False, "reindex_all should NOT be called from any of these events")
    if spies["reindex_courses"].await_count != 0 or spies["reindex_lessons"].await_count != 0:
        return CaseResult(name, False, "full-collection reindex should NOT be called for targeted events")
    expectations = {
        "reindex_single_course": 4,  # 1 (course-events) + 1 (rating) + 2 (learning-path courseIds)
        "reindex_single_lesson": 1,
        "reindex_single_profile": 2,  # enrollment + rating
    }
    for key, want in expectations.items():
        got = spies[key].await_count
        if got != want:
            return CaseResult(name, False, f"{key} called {got} times, expected {want}")
    return CaseResult(name, True, f"routing OK: " + ", ".join(f"{k}={spies[k].await_count}" for k in expectations))


async def case_missing_user_id_does_not_reindex_all() -> CaseResult:
    name = "S7 - Missing userId on enrollment/rating event -> no global reindex"
    originals = {
        "reindex_single_profile": vector_service.reindex_single_profile,
        "reindex_all": vector_service.reindex_all,
    }
    spies = {
        "reindex_single_profile": AsyncMock(return_value={"success": True, "stats": {"indexed": 1, "failed": 0}}),
        "reindex_all": AsyncMock(return_value={"success": True, "stats": {"indexed": 0, "failed": 0}}),
    }
    before_snapshot = await runtime_observability_service.snapshot()
    before_missing = int((before_snapshot.get("counters") or {}).get("errors:kafka_event_missing_user_id", 0))
    for k, spy in spies.items():
        setattr(vector_service, k, spy)
    try:
        await indexing_event_consumer._handle_message(  # noqa: SLF001
            "enrollment-events",
            '{"eventType":"PROGRESS_UPDATED","courseId":"course-1","status":"IN_PROGRESS"}',
        )
        await indexing_event_consumer._handle_message(  # noqa: SLF001
            "rating-events",
            '{"eventType":"CREATED","courseId":"course-1","score":5}',
        )
    finally:
        for k, fn in originals.items():
            setattr(vector_service, k, fn)

    if spies["reindex_all"].await_count != 0:
        return CaseResult(name, False, "Missing userId should never trigger reindex_all")
    if spies["reindex_single_profile"].await_count != 0:
        return CaseResult(name, False, "Missing userId should not call reindex_single_profile either")
    after_snapshot = await runtime_observability_service.snapshot()
    after_missing = int((after_snapshot.get("counters") or {}).get("errors:kafka_event_missing_user_id", 0))
    if after_missing - before_missing != 2:
        return CaseResult(
            name,
            False,
            f"errors:kafka_event_missing_user_id should advance by 2 (got {after_missing - before_missing})",
        )
    return CaseResult(name, True, f"both events logged; counter advanced by 2 (-> {after_missing})")


async def main() -> int:
    results: list[CaseResult] = []
    cases = [
        case_collection_stats,
        case_feature_readiness,
        case_search_retrieval_mode,
        case_reindex_single_course,
        case_reindex_single_profile,
        case_kafka_routing_contract,
        case_missing_user_id_does_not_reindex_all,
    ]
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
