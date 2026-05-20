"""
Step 03 - Analytics Semantic Layer E2E smoke test.

Runs every required test question from
`docs/implementation-plan/03-analytics-semantic-layer.md` end-to-end:

  planner -> validator -> SQL execution on the live PostgreSQL configured
  via `.env` (SPRING_DATASOURCE_URL/USERNAME/PASSWORD).

For each case we assert:
  1. semantic planner picks the expected metric (deterministic, no LLM),
  2. metric validator passes (tables, scope, :user_id, owner filter, PII),
  3. SQL executes on the real DB and returns either rows or a documented
     empty state (the current DB only has seed data for some metric tables,
     so empty rows are tolerated but logged loudly).

Run from the service root:

    python -m tests.test_step03_analytics_semantic_layer

Exit code is 0 if every case planned + validated + executed without error.
A non-empty rows count is not required for cases whose backing table is
empty in the current DB (the script prints which metric is data-blocked
so the user can seed and re-run for full `REAL_DATA_READY`).
"""

from __future__ import annotations

import asyncio
import sys
import traceback
from dataclasses import dataclass
from pathlib import Path
from typing import Any

# Allow `python -m tests.test_step03_...` from the service root.
SERVICE_ROOT = Path(__file__).resolve().parent.parent
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

from sqlalchemy import text  # noqa: E402

from app.db.session import get_db_session  # noqa: E402
from app.services.analytics import analytics_semantic_planner, validate_metric_plan  # noqa: E402
from app.services.analytics_service import analytics_service  # noqa: E402


@dataclass(frozen=True)
class Case:
    name: str
    question: str
    user_role: str
    scope: str
    user_id: str | None
    pii_access: bool
    expected_metric: str
    expected_chart_type: str | None = None
    must_have_rows: bool = False  # True when the underlying table is known to be seeded


# Seeded sample users observed on the live DB (`techhub` schema):
#   - Learner with enrollments      : 27358b4e-9332-4fe2-b313-67e2e16d247c
#   - Instructor owning courses     : 9a54a992-5fe9-4a6b-af7e-4a9c92d68fe5
LEARNER_USER_ID = "27358b4e-9332-4fe2-b313-67e2e16d247c"
INSTRUCTOR_USER_ID = "9a54a992-5fe9-4a6b-af7e-4a9c92d68fe5"
ADMIN_USER_ID = "eb2159ec-5992-4302-af92-e6c9b8b9b1a1"

# Required test questions from docs/implementation-plan/03-analytics-semantic-layer.md
CASES: list[Case] = [
    Case(
        name="Q1 - tien do hoc cua toi theo tung khoa",
        question="Tien do hoc cua toi theo tung khoa la bao nhieu?",
        user_role="LEARNER",
        scope="personal",
        user_id=LEARNER_USER_ID,
        pii_access=False,
        expected_metric="learner_course_progress",
        expected_chart_type="bar",
        # progress table is empty in the live DB -> empty rows OK.
    ),
    Case(
        name="Q2 - khoa nao co nhieu hoc vien dang hoc nhat",
        question="Khoa nao co nhieu hoc vien dang hoc nhat?",
        user_role="ADMIN",
        scope="platform",
        user_id=ADMIN_USER_ID,
        pii_access=False,
        expected_metric="active_enrollments_by_course",
        expected_chart_type="bar",
        must_have_rows=True,  # enrollments seeded
    ),
    Case(
        name="Q3 - diem danh gia trung binh tung khoa",
        question="Diem danh gia trung binh tung khoa?",
        user_role="ADMIN",
        scope="platform",
        user_id=ADMIN_USER_ID,
        pii_access=False,
        expected_metric="average_course_rating",
        expected_chart_type="bar",
        must_have_rows=True,  # ratings seeded (>=1) and courses seeded
    ),
    Case(
        name="Q4 - toi lam bai tap duoc bao nhieu diem",
        question="Toi lam bai tap duoc bao nhieu diem?",
        user_role="LEARNER",
        scope="personal",
        user_id=LEARNER_USER_ID,
        pii_access=False,
        expected_metric="learner_submissions",
        expected_chart_type="bar",
        # submissions empty -> empty rows OK.
    ),
    Case(
        name="Q5 - ti le hoan thanh cac learning path",
        question="Ti le hoan thanh cac learning path?",
        user_role="ADMIN",
        scope="platform",
        user_id=ADMIN_USER_ID,
        pii_access=False,
        expected_metric="learning_path_completion",
        expected_chart_type="bar",
        # learning_paths/path_progress empty -> empty rows OK.
    ),
    Case(
        name="Q6 - doanh thu theo khoa trong thang nay (admin)",
        question="Doanh thu theo khoa trong thang nay?",
        user_role="ADMIN",
        scope="platform",
        user_id=ADMIN_USER_ID,
        pii_access=False,
        expected_metric="revenue_by_course",
        expected_chart_type="bar",
        # transactions/payments seeded, but the "this_month" filter may still
        # return empty rows depending on seed dates -> tolerate empty.
    ),
    Case(
        name="Q6b - doanh thu theo khoa toan thoi gian (admin)",
        question="Doanh thu theo khoa?",
        user_role="ADMIN",
        scope="platform",
        user_id=ADMIN_USER_ID,
        pii_access=False,
        expected_metric="revenue_by_course",
        expected_chart_type="bar",
        must_have_rows=True,
    ),
    Case(
        name="Q6c - doanh thu theo khoa (instructor, owner-filtered)",
        question="Doanh thu theo khoa cua toi",
        user_role="INSTRUCTOR",
        scope="platform",
        user_id=INSTRUCTOR_USER_ID,
        pii_access=False,
        expected_metric="revenue_by_course",
        expected_chart_type="bar",
        # owner filter forces c.instructor_id = :user_id; may be empty depending on seed.
    ),
    Case(
        name="Q7 - ve bieu do tien do cua toi (chart reuse)",
        question="Ve bieu do tien do cua toi",
        user_role="LEARNER",
        scope="personal",
        user_id=LEARNER_USER_ID,
        pii_access=False,
        expected_metric="learner_course_progress",
        expected_chart_type="bar",
        # progress empty -> empty rows OK; we only check planner reuses the metric.
    ),
]


def _build_policy(case: Case) -> dict[str, Any]:
    return {
        "userRole": case.user_role,
        "trustedUserId": case.user_id,
        "sqlMaxRows": 100,
        "piiAccess": case.pii_access,
        "allowDataQuery": True,
    }


async def _run_case(case: Case) -> dict[str, Any]:
    policy = _build_policy(case)
    plan = analytics_semantic_planner.plan(
        case.question,
        entities={},
        policy=policy,
        user_id=case.user_id,
        scope=case.scope,
    )
    if plan is None:
        raise AssertionError(f"semantic planner returned None for question: {case.question!r}")

    metric = str(plan.get("metric") or "")
    if metric != case.expected_metric:
        raise AssertionError(
            f"metric mismatch: question={case.question!r} expected={case.expected_metric} got={metric}"
        )
    chart_type = str(plan.get("chartType") or "")
    if case.expected_chart_type and chart_type != case.expected_chart_type:
        raise AssertionError(
            f"chart-type mismatch: question={case.question!r} expected={case.expected_chart_type} got={chart_type}"
        )

    sql = str(plan.get("sql") or "").strip()
    if not sql:
        raise AssertionError(f"empty SQL for metric={metric}")

    # Re-use the production validator + limit/PII enforcement.
    validated_sql = analytics_service._validate_sql(  # noqa: SLF001
        sql,
        policy=policy,
        metric=metric,
        scope=str(plan.get("scope") or case.scope),
    )

    # Run the planner-built SQL on the live database through the same async
    # session used by AnalyticsService at runtime.
    async with get_db_session() as session:
        result = await session.execute(text(validated_sql), dict(plan.get("params") or {}))
        rows = [dict(row) for row in result.mappings().all()]
    columns = list(rows[0].keys()) if rows else []

    chart_options = analytics_service._build_chart_options(  # noqa: SLF001
        rows=rows,
        columns=columns,
        chart_type=chart_type,
    )

    if case.must_have_rows and not rows:
        raise AssertionError(
            f"expected non-empty rows for {metric} but got 0; live DB seed regressed?"
        )

    return {
        "metric": metric,
        "scope": plan.get("scope"),
        "chartType": chart_type,
        "rowCount": len(rows),
        "columns": columns,
        "sample": rows[:2],
        "chartOptionsKeys": sorted(chart_options.keys()),
        "executionMode": plan.get("executionMode"),
        "tables": list((plan.get("metricDefinition") or {}).get("tables") or []),
        "validatedSqlPreview": " ".join(validated_sql.split())[:160],
    }


async def main() -> int:
    passed: list[str] = []
    failed: list[tuple[str, str]] = []
    empty_blocks: list[str] = []
    for case in CASES:
        print(f"\n--- {case.name}")
        try:
            outcome = await _run_case(case)
        except Exception as exc:  # noqa: BLE001
            tb = "".join(traceback.format_exception_only(type(exc), exc)).strip()
            print(f"FAIL: {tb}")
            failed.append((case.name, tb))
            continue
        if outcome["rowCount"] == 0 and not case.must_have_rows:
            empty_blocks.append(f"{case.name} (metric={outcome['metric']})")
        print(
            f"PASS metric={outcome['metric']} scope={outcome['scope']} "
            f"chart={outcome['chartType']} rows={outcome['rowCount']} "
            f"columns={outcome['columns']}"
        )
        if outcome["sample"]:
            print(f"  sample[0]={outcome['sample'][0]}")
        print(f"  sql={outcome['validatedSqlPreview']}")
        passed.append(case.name)

    print("\n=================================================")
    print(f"PASSED: {len(passed)} / {len(CASES)}")
    if failed:
        print("FAILED:")
        for name, err in failed:
            print(f"  - {name}: {err}")
    if empty_blocks:
        print("EMPTY-DATA cases (planner/validator/SQL ok, table empty in live DB):")
        for name in empty_blocks:
            print(f"  - {name}")
        print(
            "  -> seed chapters/lessons/progress/submissions/learning_paths/path_progress/analytics "
            "to flip these to non-empty in a future run."
        )
    return 0 if not failed else 1


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
