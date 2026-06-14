"""
Step 11 - Platform-wide chatbot coverage E2E test.

Exercises the analytics semantic layer for the catalog / discovery questions
added to broaden chatbot coverage across the whole platform:

  - courses_by_instructor       "giảng viên A có bao nhiêu khóa học"
  - learner_course_instructors  "tôi đang học khóa của giảng viên nào"
  - course_catalog              "có những khóa học nào / khóa học về Docker"
  - course_pricing              "khóa nào đắt nhất / rẻ nhất"
  - course_structure            "khóa X gồm những bài học nào"
  - learning_path_catalog       "có những lộ trình học nào"
  - learning_path_courses       "lộ trình X gồm những khóa nào"
  - blog_catalog                "có blog nào về Docker / bài viết mới"

For every case it runs the *real* pipeline against the live PostgreSQL from
`.env`:

  intent router -> entity extraction -> semantic planner -> SQL guards
  (AST/PII/metric validator/access policy) -> SQL execution -> enrichment

and only marks a case PASS when an *independent* ground-truth query agrees with
the metric output. Run from the service root:

    python -m tests.test_step11_platform_coverage

Exit code is 0 only if every case passes.
"""

from __future__ import annotations

import asyncio
import sys
import traceback
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Callable

from sqlalchemy import text

SERVICE_ROOT = Path(__file__).resolve().parent.parent
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

from app.db.session import get_db_session  # noqa: E402
from app.orchestration.nodes.entity_extraction_node import entity_extraction_node  # noqa: E402
from app.orchestration.router.intent_router import IntentRouter  # noqa: E402
from app.services.analytics.metric_registry import get_metric  # noqa: E402
from app.services.analytics.planner import analytics_semantic_planner  # noqa: E402
from app.services.analytics.sql_ast_guard import (  # noqa: E402
    assert_no_pii,
    assert_tables_allowed,
    parse_and_inspect,
)
from app.services.analytics.sql_templates import build_metric_sql, build_params  # noqa: E402
from app.services.analytics.validator import validate_metric_plan  # noqa: E402
from app.services.analytics_service import analytics_service  # noqa: E402
from app.services.data_contract import (  # noqa: E402
    ANALYTICS_ALLOWED_TABLES,
    ANALYTICS_SENSITIVE_COLUMNS,
)

LEARNER_WITH_ENROLL = "eb2159ec-5992-4302-af92-e6c9b8b9b1a1"

_intent_router = IntentRouter()


@dataclass
class Case:
    name: str
    question: str
    expected_metric: str
    role: str = "LEARNER"
    user_id: str | None = None
    expected_intents: tuple[str, ...] = ("data_query", "visualization")
    # async validator(session, rows, params) -> (ok: bool, message: str)
    check: Callable[..., Any] | None = None
    # Simulated previous analytics turn (FE sends this back as activeAnalysis).
    # Used to prove a new question does NOT inherit the prior metric.
    prior_analysis: dict[str, Any] | None = None


def _policy(role: str, user_id: str | None) -> dict[str, Any]:
    return {
        "userRole": role,
        "trustedUserId": user_id,
        "piiAccess": role in {"ADMIN", "SUPER_ADMIN"},
        "sqlMaxRows": 200,
        "allowDataQuery": True,
    }


async def _extract_entities(question: str) -> dict[str, Any]:
    state = {"user_input": question, "entities": {}, "execution_trace": []}
    result = await entity_extraction_node(state)
    return result["entities"]


def _infer_scope(question: str, entities: dict[str, Any]) -> str:
    return analytics_service._infer_scope(question, entities)


async def _run_case(case: Case) -> tuple[bool, str]:
    entities = await _extract_entities(case.question)

    intent = await _intent_router.classify(
        {"user_input": case.question, "entities": entities}
    )
    if intent.intent not in case.expected_intents:
        return False, (
            f"intent={intent.intent} (sub={intent.sub_intent}) "
            f"not in {case.expected_intents}"
        )

    # Mirror exactly what AnalyticsService.execute does before planning so the
    # prior-analysis carry-over path is exercised here too.
    prior = case.prior_analysis
    resolved_entities = analytics_service._merge_prior_entities(entities, prior)
    scope = analytics_service._infer_scope_with_prior(case.question, resolved_entities, prior)
    policy = _policy(case.role, case.user_id)
    plan = analytics_semantic_planner.plan(
        case.question,
        resolved_entities,
        policy=policy,
        user_id=case.user_id,
        scope=scope,
        prior_analysis=prior,
    )
    if plan is None:
        return False, f"planner returned None (entities={entities})"
    if plan["metric"] != case.expected_metric:
        return False, (
            f"metric={plan['metric']} expected={case.expected_metric} "
            f"(entities={entities})"
        )

    sql = plan["sql"]
    params = plan["params"]

    # Re-run the exact guard stack analytics_service applies.
    parsed = parse_and_inspect(sql)
    assert_tables_allowed(parsed, set(ANALYTICS_ALLOWED_TABLES))
    if not policy["piiAccess"]:
        assert_no_pii(parsed, set(ANALYTICS_SENSITIVE_COLUMNS))
    validate_metric_plan(
        plan["metric"], sql=sql, tables=set(parsed.tables), policy=policy, scope=plan["scope"]
    )
    analytics_service._enforce_analytics_access(
        policy, scope=plan["scope"], trusted_user_id=case.user_id, metric=plan["metric"]
    )

    async with get_db_session() as session:
        result = await session.execute(text(sql), params)
        rows = [dict(r) for r in result.mappings().all()]
        rows = analytics_service._normalize_rows(rows)
        if plan["metric"] in analytics_service._INSTRUCTOR_NAME_METRICS:
            rows = await analytics_service._enrich_instructor_names(rows)
        if case.check is None:
            return (len(rows) > 0), f"rows={len(rows)} (no extra check)"
        return await case.check(session, rows, params)

    return True, "ok"


# ---------------------------------------------------------------------------
# Ground-truth checks (independent queries) per case
# ---------------------------------------------------------------------------

async def _check_courses_by_instructor(session, rows, params):
    name = params["instructor_name"]
    gt = (await session.execute(text(
        """
        SELECT count(*) FROM courses c
        JOIN users u ON u.id = c.instructor_id AND u.is_active='Y'
        LEFT JOIN profiles pr ON pr.user_id = u.id AND pr.is_active='Y'
        WHERE c.is_active='Y' AND c.status='PUBLISHED'
          AND (lower(u.username) LIKE '%'||lower(:n)||'%'
               OR lower(pr.full_name) LIKE '%'||lower(:n)||'%')
        """
    ), {"n": name})).scalar()
    if len(rows) != gt:
        return False, f"courses_by_instructor rows={len(rows)} ground_truth={gt}"
    if rows and not all(r.get("instructor") for r in rows):
        return False, "some rows missing resolved instructor name"
    if rows and any("instructor_id" in r for r in rows):
        return False, "instructor_id should be dropped from output"
    return True, f"{len(rows)} courses for instructor '{name}' (gt={gt})"


async def _check_learner_instructors(session, rows, params):
    gt = (await session.execute(text(
        """
        SELECT count(DISTINCT c.id) FROM enrollments e
        JOIN courses c ON c.id=e.course_id AND c.is_active='Y' AND c.status='PUBLISHED'
        WHERE e.is_active='Y' AND e.user_id=:u
        """
    ), {"u": params["user_id"]})).scalar()
    if len(rows) != gt:
        return False, f"learner_course_instructors rows={len(rows)} gt={gt}"
    if rows and not all(r.get("instructor") for r in rows):
        return False, "some rows missing resolved instructor name"
    return True, f"{len(rows)} enrolled courses with instructor (gt={gt})"


async def _check_course_catalog_all(session, rows, params):
    gt = (await session.execute(text(
        "SELECT count(*) FROM courses WHERE is_active='Y' AND status='PUBLISHED'"
    ))).scalar()
    # template caps at 50
    expected = min(gt, 50)
    if len(rows) != expected:
        return False, f"course_catalog rows={len(rows)} expected={expected} (gt={gt})"
    if not all("price" in r and "level" in r for r in rows):
        return False, "rows missing price/level columns"
    return True, f"{len(rows)} courses listed (gt={gt})"


async def _check_course_catalog_topic(session, rows, params):
    topic = params.get("topic", "")
    if not topic:
        return False, "expected a topic filter but none captured"
    gt_titles = {r[0] for r in (await session.execute(text(
        """
        SELECT DISTINCT c.title FROM courses c
        LEFT JOIN course_skills cs ON cs.course_id=c.id
        LEFT JOIN skills sk ON sk.id=cs.skill_id AND sk.is_active='Y'
        WHERE c.is_active='Y' AND c.status='PUBLISHED'
          AND (lower(c.title) LIKE '%'||lower(:t)||'%'
               OR lower(COALESCE(c.description,'')) LIKE '%'||lower(:t)||'%'
               OR lower(COALESCE(sk.name,'')) LIKE '%'||lower(:t)||'%')
        """
    ), {"t": topic})).fetchall()}
    got_titles = {r["label"] for r in rows}
    if got_titles != gt_titles:
        return False, f"topic '{topic}': got={sorted(got_titles)} gt={sorted(gt_titles)}"
    return True, f"topic '{topic}' -> {len(rows)} courses match ground truth"


async def _check_course_pricing(session, rows, params):
    gt_max = (await session.execute(text(
        "SELECT max(price) FROM courses WHERE is_active='Y' AND status='PUBLISHED'"
    ))).scalar()
    if not rows:
        return False, "no rows"
    if float(rows[0]["value"]) != float(gt_max):
        return False, f"top price={rows[0]['value']} expected max={gt_max}"
    values = [float(r["value"]) for r in rows]
    if values != sorted(values, reverse=True):
        return False, "rows not sorted by price desc"
    return True, f"{len(rows)} courses, top price {rows[0]['value']} == max {gt_max}"


async def _check_course_structure(session, rows, params):
    name = params["course_name"]
    gt = (await session.execute(text(
        """
        SELECT count(*) FROM courses c
        JOIN chapters ch ON ch.course_id=c.id AND ch.is_active='Y'
        JOIN lessons l ON l.chapter_id=ch.id AND l.is_active='Y'
        WHERE c.is_active='Y' AND c.status='PUBLISHED'
          AND lower(c.title) LIKE '%'||lower(:n)||'%'
        """
    ), {"n": name})).scalar()
    expected = min(gt, 100)
    if len(rows) != expected:
        return False, f"course_structure rows={len(rows)} expected={expected} (gt={gt})"
    if gt == 0:
        return False, f"no lessons matched course name '{name}' - bad fixture"
    if not all(r.get("chapter") and r.get("content_type") for r in rows):
        return False, "rows missing chapter/content_type"
    return True, f"{len(rows)} lessons for course '{name}' (gt={gt})"


async def _check_lp_catalog(session, rows, params):
    gt = (await session.execute(text(
        "SELECT count(*) FROM learning_paths WHERE is_active='Y'"
    ))).scalar()
    expected = min(gt, 50)
    if len(rows) != expected:
        return False, f"learning_path_catalog rows={len(rows)} expected={expected} (gt={gt})"
    return True, f"{len(rows)} learning paths (gt={gt})"


async def _check_recommended_next(session, rows, params):
    if not rows:
        return False, "recommended_next_courses returned no rows"
    uid = params["user_id"]
    enrolled = {
        row["title"]
        for row in (await session.execute(text(
            """
            SELECT c.title
            FROM enrollments e
            JOIN courses c ON c.id = e.course_id
            WHERE e.is_active = 'Y' AND e.user_id = :uid
            """
        ), {"uid": uid})).mappings().all()
    }
    overlap = [row["label"] for row in rows if row.get("label") in enrolled]
    if overlap:
        return False, f"suggested already-enrolled courses: {overlap}"
    if not all(row.get("instructor") for row in rows):
        return False, "some rows missing resolved instructor name"
    if any("instructor_id" in row for row in rows):
        return False, "instructor_id should be dropped from output"
    return True, f"{len(rows)} next-course suggestions (none already enrolled)"


async def _check_lp_courses(session, rows, params):
    name = params["path_name"]
    gt = (await session.execute(text(
        """
        SELECT count(*) FROM learning_paths lp
        JOIN learning_path_courses lpc ON lpc.path_id=lp.id
        JOIN courses c ON c.id=lpc.course_id AND c.is_active='Y'
        WHERE lp.is_active='Y' AND lower(lp.title) LIKE '%'||lower(:n)||'%'
        """
    ), {"n": name})).scalar()
    expected = min(gt, 100)
    if len(rows) != expected:
        return False, f"learning_path_courses rows={len(rows)} expected={expected} (gt={gt})"
    if gt == 0:
        return False, f"no courses matched path '{name}' - bad fixture"
    return True, f"{len(rows)} courses in path '{name}' (gt={gt})"


async def _check_blog_catalog(session, rows, params):
    topic = params.get("topic", "")
    if topic:
        gt = (await session.execute(text(
            """
            SELECT count(*) FROM blogs b
            WHERE b.is_active='Y' AND b.status='PUBLISHED'
              AND (lower(b.title) LIKE '%'||lower(:t)||'%'
                   OR lower(COALESCE(b.content,'')) LIKE '%'||lower(:t)||'%'
                   OR lower(COALESCE(array_to_string(b.tags,' '),'')) LIKE '%'||lower(:t)||'%')
            """
        ), {"t": topic})).scalar()
    else:
        gt = (await session.execute(text(
            "SELECT count(*) FROM blogs WHERE is_active='Y' AND status='PUBLISHED'"
        ))).scalar()
    expected = min(gt, 50)
    if len(rows) != expected:
        return False, f"blog_catalog rows={len(rows)} expected={expected} (gt={gt}, topic='{topic}')"
    return True, f"{len(rows)} blogs (gt={gt}, topic='{topic}')"


CASES: list[Case] = [
    Case(
        name="Q2 instructor courses",
        question="Giảng viên instructor có bao nhiêu khóa học, gồm những khóa nào?",
        expected_metric="courses_by_instructor",
        check=_check_courses_by_instructor,
    ),
    Case(
        name="Q1 my courses' instructors",
        question="Tôi đang học khóa học của giảng viên nào?",
        expected_metric="learner_course_instructors",
        user_id=LEARNER_WITH_ENROLL,
        check=_check_learner_instructors,
    ),
    Case(
        name="course catalog (all)",
        question="Trên hệ thống có những khóa học nào?",
        expected_metric="course_catalog",
        check=_check_course_catalog_all,
    ),
    Case(
        name="course catalog (topic Docker)",
        question="Có những khóa học nào về Docker?",
        expected_metric="course_catalog",
        check=_check_course_catalog_topic,
    ),
    Case(
        # Topic-scoped lookup phrased politely as a recommendation must still
        # resolve deterministically to the catalog (not the profile recommender).
        name="topic search via 'gợi ý' phrasing",
        question="có khóa nào học về database không hãy gợi ý cho tôi",
        expected_metric="course_catalog",
        expected_intents=("data_query", "visualization"),
        check=_check_course_catalog_topic,
    ),
    Case(
        name="course pricing",
        question="Khóa học nào đắt nhất trên hệ thống?",
        expected_metric="course_pricing",
        check=_check_course_pricing,
    ),
    Case(
        name="course structure (lessons of a course)",
        question="Khóa học API testing với Postman gồm những bài học nào?",
        expected_metric="course_structure",
        check=_check_course_structure,
    ),
    Case(
        name="learning path catalog",
        question="Hệ thống có những lộ trình học nào?",
        expected_metric="learning_path_catalog",
        check=_check_lp_catalog,
    ),
    Case(
        name="learning path courses",
        question="Lộ trình TypeScript gồm những khóa học nào?",
        expected_metric="learning_path_courses",
        check=_check_lp_courses,
    ),
    Case(
        # "Suggest what I should learn next" is a PERSONAL recommendation anchored
        # on the learner's own enrollments — not the global learning-path catalog
        # and not the all-zero admin completion-rate table.
        name="recommended next courses",
        question="gợi ý cho tôi lộ trình học tiếp theo",
        expected_metric="recommended_next_courses",
        user_id=LEARNER_WITH_ENROLL,
        check=_check_recommended_next,
    ),
    Case(
        name="blog catalog (all)",
        question="Có những bài blog nào trên hệ thống?",
        expected_metric="blog_catalog",
        check=_check_blog_catalog,
    ),
    Case(
        name="blog catalog (topic)",
        question="Có blog nào về Docker không?",
        expected_metric="blog_catalog",
        check=_check_blog_catalog,
    ),
    # --- Prior-analysis carry-over regression cases -----------------------
    # A previous analytics turn must NOT bleed its metric into a brand-new,
    # unrelated question. (Reproduced the "blog question answered with course
    # progress" bug.)
    Case(
        name="carryover: blog after personal course turn",
        question="các blog hiện tại có trên web site này , và nội dung của các blog",
        expected_metric="blog_catalog",
        check=_check_blog_catalog,
        prior_analysis={
            "metric": "learner_course_instructors",
            "scope": "personal",
            "chartType": "bar",
            "timeRange": "all_time",
            "sql": "SELECT 1",
        },
    ),
    Case(
        name="carryover: course catalog after personal turn",
        question="Có những khóa học nào về Docker?",
        expected_metric="course_catalog",
        check=_check_course_catalog_topic,
        prior_analysis={
            "metric": "learner_course_progress",
            "scope": "personal",
            "chartType": "bar",
            "timeRange": "all_time",
            "sql": "SELECT 1",
        },
    ),
    Case(
        name="carryover: learning paths after personal turn",
        question="Hệ thống có những lộ trình học nào?",
        expected_metric="learning_path_catalog",
        check=_check_lp_catalog,
        prior_analysis={
            "metric": "learner_course_instructors",
            "scope": "personal",
            "chartType": "bar",
            "timeRange": "all_time",
            "sql": "SELECT 1",
        },
    ),
]


async def main() -> int:
    passed = 0
    failed = 0
    for case in CASES:
        try:
            ok, msg = await _run_case(case)
        except Exception as exc:  # noqa: BLE001
            ok, msg = False, f"EXC {exc!r}"
            traceback.print_exc()
        status = "PASS" if ok else "FAIL"
        if ok:
            passed += 1
        else:
            failed += 1
        print(f"[{status}] {case.name}: {msg}")
    print(f"\n{passed} passed, {failed} failed, {len(CASES)} total")
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    raise SystemExit(asyncio.run(main()))
