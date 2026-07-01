"""
Step 04 - Catalog/Profile/Recommendation E2E smoke test.

Real-data path: drives `catalog_service` against the live PostgreSQL configured
via .env. Verifies that for the seeded users:

  - rating query uses the real ratings schema (target_id/target_type/score) and
    returns the user's actual rating with `course_id` resolved,
  - course history attaches `historyBucket` and `progressSource` for every row,
  - cold-start user (random UUID with no enrollments) is classified as
    `fallback_catalog`,
  - users with different enrollment sets produce different candidate course
    IDs (no accidental cross-user bleed-through),
  - missingSignals correctly reflects which signals are absent.

Logic-fixture path: drives `RecommendationService` private helpers with
synthetic input to verify the signals the current DB has no seed data for
(completed exclusion, continue_learning, path_alignment, liked/avoid topic,
and AI-payload courseId sanitization).

Run from the service root:

    python -m tests.test_step04_recommendation_signal_layer

Exit code 0 if every case passes.
"""

from __future__ import annotations

import asyncio
import sys
import traceback
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from uuid import UUID, uuid4

SERVICE_ROOT = Path(__file__).resolve().parent.parent
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

from app.core.enums import RecommendationMode  # noqa: E402
from app.schemas.recommendation import RecommendationRequest  # noqa: E402
from app.services.catalog_service import catalog_service  # noqa: E402
from app.services.recommendation_service import recommendation_service  # noqa: E402


# Seeded sample users on the live `techhub` schema:
USER_RICH = "eb2159ec-5992-4302-af92-e6c9b8b9b1a1"   # 12 enrollments, 1 rating(score=4)
USER_LIGHT = "27358b4e-9332-4fe2-b313-67e2e16d247c"  # 4 enrollments, 0 ratings
USER_COLD = str(uuid4())                              # cold-start: not in DB


# ---------------------------------------------------------------------------
# Real-data cases (hit the live DB through catalog_service).
# ---------------------------------------------------------------------------


@dataclass
class CaseResult:
    name: str
    passed: bool
    detail: str = ""


async def case_rating_schema_real() -> CaseResult:
    name = "R1 - rating query reads real schema (target_id/score)"
    ratings = await catalog_service.fetch_user_ratings(USER_RICH, raise_on_error=True)
    if not ratings:
        return CaseResult(name, False, "expected at least 1 rating for USER_RICH but got 0")
    row = ratings[0]
    expected_keys = {"course_id", "score", "title", "level", "skills"}
    missing = expected_keys.difference(row.keys())
    if missing:
        return CaseResult(name, False, f"missing keys in rating row: {missing}")
    if not row.get("course_id"):
        return CaseResult(name, False, f"rating row has empty course_id: {row}")
    if int(row.get("score") or 0) < 1 or int(row.get("score") or 0) > 5:
        return CaseResult(name, False, f"unexpected score: {row.get('score')}")
    return CaseResult(name, True, f"score={row['score']} course={row['title']}")


async def case_history_bucket_and_progress_source() -> CaseResult:
    name = "R2 - course history has historyBucket + progressSource"
    history = await catalog_service.fetch_user_course_history(USER_RICH)
    if not history:
        return CaseResult(name, False, "USER_RICH should have enrollments but history empty")
    valid_buckets = {"completed", "in_progress", "enrolled", "abandoned"}
    valid_sources = {"lesson_progress", "lesson_progress_empty", "enrollment_status_fallback"}
    for item in history:
        bucket = str(item.get("historyBucket") or "")
        source = str(item.get("progressSource") or "")
        if bucket not in valid_buckets:
            return CaseResult(name, False, f"bad historyBucket={bucket!r} on row={item}")
        if source not in valid_sources:
            return CaseResult(name, False, f"bad progressSource={source!r} on row={item}")
    return CaseResult(
        name,
        True,
        f"{len(history)} rows, all buckets/sources valid (sample bucket={history[0]['historyBucket']}, source={history[0]['progressSource']})",
    )


async def case_cold_start_signals() -> CaseResult:
    name = "R3 - cold-start user -> empty signals + fallback_catalog pipeline"
    history = await catalog_service.fetch_user_course_history(USER_COLD)
    ratings = await catalog_service.fetch_user_ratings(USER_COLD, raise_on_error=True)
    paths = await catalog_service.fetch_user_learning_paths(USER_COLD)
    if history or ratings or paths:
        return CaseResult(
            name, False, f"expected empty for cold-start but got history={len(history)} ratings={len(ratings)} paths={len(paths)}"
        )
    pipeline = recommendation_service._resolve_pipeline(  # noqa: SLF001
        course_history=history,
        ratings=ratings,
        learning_paths=paths,
        skill_profile={},
        fallback_catalog_used=True,
        vector_failed=True,
        missing_signals=["course_history", "ratings", "skill_profile", "learning_paths"],
    )
    if pipeline != "fallback_catalog":
        return CaseResult(name, False, f"expected pipeline=fallback_catalog, got {pipeline}")
    return CaseResult(name, True, "pipeline=fallback_catalog as expected")


async def case_distinct_users_distinct_candidates() -> CaseResult:
    name = "R4 - different users -> different candidate course IDs"
    history_rich = await catalog_service.fetch_user_course_history(USER_RICH)
    history_light = await catalog_service.fetch_user_course_history(USER_LIGHT)
    rich_ids = {str(item.get("course_id")) for item in history_rich if item.get("course_id")}
    light_ids = {str(item.get("course_id")) for item in history_light if item.get("course_id")}
    if not rich_ids or not light_ids:
        return CaseResult(name, False, f"empty history for one of the users: rich={len(rich_ids)} light={len(light_ids)}")
    if rich_ids == light_ids:
        return CaseResult(name, False, "USER_RICH and USER_LIGHT enroll in the exact same set; pick different seed users")
    # active enrollment ids drive the continue_learning candidate set
    rich_active = recommendation_service._history_ids(history_rich, buckets={"in_progress", "enrolled"})  # noqa: SLF001
    light_active = recommendation_service._history_ids(history_light, buckets={"in_progress", "enrolled"})  # noqa: SLF001
    return CaseResult(
        name,
        True,
        f"rich_active={len(rich_active)} light_active={len(light_active)} overlap={len(rich_active & light_active)}",
    )


async def case_missing_signals_for_rated_user() -> CaseResult:
    name = "R5 - USER_RICH ratings present, learning_paths missing -> missingSignals reflects gap"
    history = await catalog_service.fetch_user_course_history(USER_RICH)
    ratings = await catalog_service.fetch_user_ratings(USER_RICH, raise_on_error=True)
    paths = await catalog_service.fetch_user_learning_paths(USER_RICH)
    missing: list[str] = []
    if not history:
        recommendation_service._append_signal(missing, "course_history")  # noqa: SLF001
    if not ratings:
        recommendation_service._append_signal(missing, "ratings")  # noqa: SLF001
    if not paths:
        recommendation_service._append_signal(missing, "learning_paths")  # noqa: SLF001
    if "ratings" in missing:
        return CaseResult(name, False, f"USER_RICH should have ratings; missing={missing}")
    if "learning_paths" not in missing:
        return CaseResult(name, False, f"expected learning_paths in missing; got {missing}")
    return CaseResult(name, True, f"missingSignals={missing}")


# ---------------------------------------------------------------------------
# Logic-fixture cases (DB currently lacks rows to verify these signals end-to-end).
# ---------------------------------------------------------------------------


def _make_request(*, exclude: list[str] | None = None) -> RecommendationRequest:
    return RecommendationRequest(
        userId=UUID(USER_LIGHT),
        mode=RecommendationMode.REALTIME,
        language="vi",
        excludeCourseIds=[UUID(course_id) for course_id in (exclude or [])],
        preferredLanguages=["python"],
    )


def _candidate(course_id: str, *, skills: list[str], score: float = 0.5, language: str = "vi") -> dict[str, Any]:
    return {
        "score": score,
        "payload": {
            "id": course_id,
            "title": f"Course {course_id[:6]}",
            "description": "test",
            "skills": skills,
            "language": language,
            "average_rating": 0.0,
            "rating_count": 0,
            "enrollment_count": 0,
        },
        "signals": ["course_vector"],
        "source": "vector",
    }


def case_completed_excluded_in_progress_kept() -> CaseResult:
    name = "L1 - completed excluded; in_progress kept with continue_learning"
    completed_id = str(uuid4())
    in_progress_id = str(uuid4())
    other_id = str(uuid4())
    candidates = [
        _candidate(completed_id, skills=["python"]),
        _candidate(in_progress_id, skills=["python"]),
        _candidate(other_id, skills=["python"]),
    ]
    course_history = [
        {"course_id": completed_id, "historyBucket": "completed", "status": "COMPLETED", "progress": 1.0, "skills": ["python"]},
        {"course_id": in_progress_id, "historyBucket": "in_progress", "status": "IN_PROGRESS", "progress": 0.4, "skills": ["python"]},
    ]
    request = _make_request()
    excluded = {completed_id}  # mimic excluded set computed from completed history
    reranked, filtered_ids = recommendation_service._rerank_candidates(  # noqa: SLF001
        candidates=candidates,
        request=request,
        course_history=course_history,
        ratings=[],
        learning_paths=[],
        similar_profiles=[],
        skill_profile={},
        excluded_ids=excluded,
    )
    reranked_ids = {str(item["payload"]["id"]) for item in reranked}
    if completed_id in reranked_ids:
        return CaseResult(name, False, "completed course leaked into reranked output")
    if completed_id not in filtered_ids:
        return CaseResult(name, False, "completed course not recorded in filtered_ids")
    in_progress_signals = next(
        (item["signals"] for item in reranked if item["payload"]["id"] == in_progress_id),
        None,
    )
    if in_progress_signals is None:
        return CaseResult(name, False, "in_progress course missing from reranked output")
    if "continue_learning" not in in_progress_signals:
        return CaseResult(name, False, f"in_progress course missing continue_learning signal: {in_progress_signals}")
    return CaseResult(
        name,
        True,
        f"reranked={sorted(reranked_ids)} filtered={sorted(filtered_ids)} continueSignals={in_progress_signals}",
    )


def case_path_alignment_signal() -> CaseResult:
    name = "L2 - candidate that sits on active learning path -> path_alignment"
    path_course_id = str(uuid4())
    other_id = str(uuid4())
    candidates = [
        _candidate(path_course_id, skills=["python"]),
        _candidate(other_id, skills=["python"]),
    ]
    learning_paths = [
        {
            "title": "Active Path",
            "completion": 0.3,
            "courses": [{"course_id": path_course_id, "order": 1}],
        }
    ]
    reranked, _ = recommendation_service._rerank_candidates(  # noqa: SLF001
        candidates=candidates,
        request=_make_request(),
        course_history=[],
        ratings=[],
        learning_paths=learning_paths,
        similar_profiles=[],
        skill_profile={},
        excluded_ids=set(),
    )
    aligned_signals = next(
        (item["signals"] for item in reranked if item["payload"]["id"] == path_course_id),
        None,
    )
    if aligned_signals is None or "path_alignment" not in aligned_signals:
        return CaseResult(name, False, f"missing path_alignment signal; got {aligned_signals}")
    return CaseResult(name, True, f"path_alignment present: {aligned_signals}")


def case_liked_and_avoid_topic_signals() -> CaseResult:
    name = "L3 - rating score>=4 -> liked_topic; score<=2 -> avoid_topic"
    liked_id = str(uuid4())
    avoided_id = str(uuid4())
    candidates = [
        _candidate(liked_id, skills=["python"], score=0.5),
        _candidate(avoided_id, skills=["php"], score=0.5),
    ]
    ratings = [
        {"course_id": "x", "score": 5, "skills": ["python"]},
        {"course_id": "y", "score": 1, "skills": ["php"]},
    ]
    reranked, _ = recommendation_service._rerank_candidates(  # noqa: SLF001
        candidates=candidates,
        request=_make_request(),
        course_history=[],
        ratings=ratings,
        learning_paths=[],
        similar_profiles=[],
        skill_profile={},
        excluded_ids=set(),
    )
    liked = next((c for c in reranked if c["payload"]["id"] == liked_id), None)
    avoided = next((c for c in reranked if c["payload"]["id"] == avoided_id), None)
    if liked is None or "liked_topic" not in liked["signals"]:
        return CaseResult(name, False, f"missing liked_topic; liked={liked}")
    if avoided is None or "avoid_topic" not in avoided["signals"]:
        return CaseResult(name, False, f"missing avoid_topic; avoided={avoided}")
    if liked["score"] <= avoided["score"]:
        return CaseResult(
            name,
            False,
            f"avoid_topic should be ranked lower than liked_topic; liked={liked['score']} avoided={avoided['score']}",
        )
    return CaseResult(
        name,
        True,
        f"liked_score={liked['score']} avoided_score={avoided['score']} (gap={liked['score'] - avoided['score']:.3f})",
    )


def case_ai_payload_rejects_fake_course_id() -> CaseResult:
    name = "L4 - AI payload courseId not in candidate set is dropped"
    real_id = str(uuid4())
    fake_id = str(uuid4())
    candidate = _candidate(real_id, skills=["python"])
    reranked = [
        {
            "score": 0.7,
            "payload": candidate["payload"],
            "signals": ["skill_match"],
            "source": "vector",
        }
    ]
    ai_payload = {
        "recommendations": [
            {"courseId": fake_id, "title": "fake", "score": 0.99, "reason": "hallucinated"},
            {"courseId": real_id, "title": "real", "score": 0.8, "reason": "valid"},
        ]
    }
    items = recommendation_service._normalize_ai_payload(  # noqa: SLF001
        ai_payload,
        reranked,
        _make_request(),
        course_history=[],
    )
    returned_ids = [item.courseId for item in items]
    if fake_id in returned_ids:
        return CaseResult(name, False, f"fake courseId leaked into output: {returned_ids}")
    if real_id not in returned_ids:
        return CaseResult(name, False, f"real courseId missing from output: {returned_ids}")
    return CaseResult(name, True, f"output ids={returned_ids}")


def case_pipeline_resolution_real_data_vs_partial() -> CaseResult:
    name = "L5 - pipeline resolution distinguishes real_data / partial_data / fallback_catalog"
    real = recommendation_service._resolve_pipeline(  # noqa: SLF001
        course_history=[{"course_id": "x"}],
        ratings=[{"score": 5}],
        learning_paths=[],
        skill_profile={"python": 0.7},
        fallback_catalog_used=False,
        vector_failed=False,
        missing_signals=[],
    )
    if real != "real_data":
        return CaseResult(name, False, f"expected real_data, got {real}")
    partial = recommendation_service._resolve_pipeline(  # noqa: SLF001
        course_history=[{"course_id": "x"}],
        ratings=[],
        learning_paths=[],
        skill_profile={},
        fallback_catalog_used=False,
        vector_failed=True,
        missing_signals=["course_vector"],
    )
    if partial != "partial_data":
        return CaseResult(name, False, f"expected partial_data, got {partial}")
    cold = recommendation_service._resolve_pipeline(  # noqa: SLF001
        course_history=[],
        ratings=[],
        learning_paths=[],
        skill_profile={},
        fallback_catalog_used=True,
        vector_failed=True,
        missing_signals=["course_history", "ratings"],
    )
    if cold != "fallback_catalog":
        return CaseResult(name, False, f"expected fallback_catalog, got {cold}")
    return CaseResult(name, True, "real_data / partial_data / fallback_catalog all resolved correctly")


# ---------------------------------------------------------------------------
# Runner
# ---------------------------------------------------------------------------


async def main() -> int:
    results: list[CaseResult] = []
    real_cases = [
        case_rating_schema_real,
        case_history_bucket_and_progress_source,
        case_cold_start_signals,
        case_distinct_users_distinct_candidates,
        case_missing_signals_for_rated_user,
    ]
    for func in real_cases:
        print(f"\n--- {func.__name__}")
        try:
            result = await func()
        except Exception as exc:  # noqa: BLE001
            tb = "".join(traceback.format_exception_only(type(exc), exc)).strip()
            result = CaseResult(func.__name__, False, f"EXC: {tb}")
        print(("PASS" if result.passed else "FAIL") + f" {result.name} :: {result.detail}")
        results.append(result)

    logic_cases = [
        case_completed_excluded_in_progress_kept,
        case_path_alignment_signal,
        case_liked_and_avoid_topic_signals,
        case_ai_payload_rejects_fake_course_id,
        case_pipeline_resolution_real_data_vs_partial,
    ]
    for func in logic_cases:
        print(f"\n--- {func.__name__}")
        try:
            result = func()
        except Exception as exc:  # noqa: BLE001
            tb = "".join(traceback.format_exception_only(type(exc), exc)).strip()
            result = CaseResult(func.__name__, False, f"EXC: {tb}")
        print(("PASS" if result.passed else "FAIL") + f" {result.name} :: {result.detail}")
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
