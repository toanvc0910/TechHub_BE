from __future__ import annotations

import re
import unicodedata
from decimal import Decimal
from numbers import Number
from typing import Any

from sqlalchemy import text

from app.core.config import get_settings
from app.db.session import get_db_session
from app.schemas.analytics_contract import (
    DEFAULT_COLOR_PALETTE,
    build_available_chart_types,
    build_column_meta,
    detect_empty_state,
)
from app.services.analytics import analytics_semantic_planner, validate_metric_plan
from app.services.analytics.sql_ast_guard import (
    SqlAstGuardError,
    assert_no_pii,
    assert_tables_allowed,
    parse_and_inspect,
)
from app.services.data_contract import (
    ANALYTICS_ALLOWED_TABLES,
    ANALYTICS_SENSITIVE_COLUMNS,
    TABLES,
    data_contract_registry,
    render_analytics_schema_context,
)
from app.services.llm_gateway import switchable_ai_gateway
from app.services.request_instructions import append_request_instructions
from app.services.runtime_policy_service import runtime_policy_service


class AnalyticsService:
    def __init__(self) -> None:
        self._settings = get_settings()

    _allowed_tables = set(ANALYTICS_ALLOWED_TABLES)
    _sensitive_columns = set(ANALYTICS_SENSITIVE_COLUMNS)
    _runtime_tables = TABLES
    _schema_context = render_analytics_schema_context()

    async def execute(
        self,
        question: str,
        entities: dict[str, Any] | None = None,
        *,
        request_context: Any | None = None,
        user_id: str | None = None,
        prior_analysis: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        await self._refresh_contract_context()
        policy = await runtime_policy_service.resolve(request_context)
        # Enrich the incoming entities/scope with prior-analysis context so a
        # short follow-up like "đổi sang biểu đồ đường" inherits scope/metric
        # from the earlier analytics turn instead of being treated as a brand
        # new query.
        resolved_entities = self._merge_prior_entities(entities or {}, prior_analysis)
        scope = self._infer_scope_with_prior(question, resolved_entities, prior_analysis)
        trusted_user_id = str(policy.get("trustedUserId") or user_id or "").strip() or None
        if policy.get("trustedUserId") and user_id and str(policy["trustedUserId"]) != str(user_id):
            raise ValueError("Analytics user context does not match trusted identity.")

        # Shortcut: if this is clearly just a chart-type swap and we have the
        # previous SQL to rerun, skip the LLM planner entirely.
        chart_swap_type = self._detect_chart_swap(question, prior_analysis) if prior_analysis else None
        if chart_swap_type and prior_analysis and prior_analysis.get("sql"):
            plan = self._plan_from_prior(
                prior_analysis,
                chart_type=chart_swap_type,
                question=question,
                user_id=trusted_user_id,
                scope=scope,
            )
        else:
            plan = await self._plan_query(
                question,
                resolved_entities,
                policy=policy,
                user_id=trusted_user_id,
                scope=scope,
                prior_analysis=prior_analysis,
            )
        scope = str(plan.get("scope") or scope)
        self._enforce_analytics_access(
            policy,
            scope=scope,
            trusted_user_id=trusted_user_id,
            metric=str(plan.get("metric") or ""),
        )
        sql = self._validate_sql(
            str(plan.get("sql") or ""),
            policy=policy,
            metric=str(plan.get("metric") or ""),
            scope=scope,
        )
        params = dict(plan.get("params") or {})
        active_plan = plan
        try:
            rows, columns = await self._execute_sql(sql, params=params)
        except Exception:
            fallback_plan = self._fallback_plan(
                question,
                resolved_entities,
                user_id=trusted_user_id,
                scope=scope,
                prefer_semantic=False,
            )
            fallback_sql = self._validate_sql(
                str(fallback_plan.get("sql") or ""),
                policy=policy,
                metric=str(fallback_plan.get("metric") or ""),
                scope=str(fallback_plan.get("scope") or scope),
            )
            if fallback_sql.strip() == sql.strip():
                raise
            active_plan = fallback_plan
            scope = str(active_plan.get("scope") or scope)
            sql = fallback_sql
            params = dict(active_plan.get("params") or {})
            rows, columns = await self._execute_sql(sql, params=params)
        else:
            if self._should_retry_with_fallback(question, resolved_entities, scope=scope, rows=rows):
                fallback_plan = self._fallback_plan(
                    question,
                    resolved_entities,
                    user_id=trusted_user_id,
                    scope=scope,
                    prefer_semantic=False,
                )
                fallback_sql = self._validate_sql(
                    str(fallback_plan.get("sql") or ""),
                    policy=policy,
                    metric=str(fallback_plan.get("metric") or ""),
                    scope=str(fallback_plan.get("scope") or scope),
                )
                if fallback_sql.strip() != sql.strip():
                    fallback_rows, fallback_columns = await self._execute_sql(
                        fallback_sql,
                        params=dict(fallback_plan.get("params") or {}),
                    )
                    if fallback_rows:
                        active_plan = fallback_plan
                        scope = str(active_plan.get("scope") or scope)
                        sql = fallback_sql
                        params = dict(active_plan.get("params") or {})
                        rows, columns = fallback_rows, fallback_columns

        rows = self._normalize_rows(rows)
        if str(active_plan.get("metric") or "") in self._INSTRUCTOR_NAME_METRICS:
            rows = await self._enrich_instructor_names(rows)
            columns = list(rows[0].keys()) if rows else columns

        summary = await self._summarize(
            question,
            rows,
            active_plan,
            scope=scope,
            request_context=request_context,
        )
        chart_type = active_plan.get("chartType") or "bar"
        suggested_actions = self._build_suggested_actions(
            plan=active_plan,
            rows=rows,
            columns=columns,
            scope=scope,
            chart_type=chart_type,
            question=question,
            entities=resolved_entities,
        )
        chart_options = self._build_chart_options(
            rows=rows,
            columns=columns,
            chart_type=chart_type,
        )
        column_meta = [meta.model_dump() for meta in build_column_meta(rows, columns)]
        return {
            "metric": active_plan.get("metric") or resolved_entities.get("metric") or "analytics",
            "timeRange": active_plan.get("timeRange") or resolved_entities.get("time_range") or "all_time",
            "rows": rows,
            "rowCount": len(rows),
            "columns": columns,
            "columnMeta": column_meta,
            "sql": sql,
            "tables": sorted(self._collect_tables(sql)),
            "chartType": chart_type,
            "title": active_plan.get("title") or "TechHub analytics",
            "summary": summary,
            "executionMode": active_plan.get("executionMode") or "llm_planner",
            "explanation": active_plan.get("explanation") or self._build_plan_explanation(active_plan, scope=scope),
            "logicSummary": self._build_logic_summary(active_plan, rows=rows, scope=scope),
            "metricDefinition": active_plan.get("metricDefinition"),
            "scope": scope,
            "scopeLabel": self._scope_label(scope),
            "policy": {
                "userRole": policy.get("userRole"),
                "sqlMaxRows": policy.get("sqlMaxRows"),
                "piiAccess": policy.get("piiAccess"),
            },
            "suggestedActions": suggested_actions,
            "chartOptions": chart_options,
        }

    # Metrics whose rows carry an `instructor_id` that must be resolved to a
    # public instructor display name for output.
    _INSTRUCTOR_NAME_METRICS = frozenset({"learner_course_instructors", "courses_by_instructor"})

    async def _enrich_instructor_names(
        self,
        rows: list[dict[str, Any]],
    ) -> list[dict[str, Any]]:
        """Resolve `instructor_id` UUIDs to public instructor display names.

        The analytics SQL only projects `instructor_id` (a non-PII UUID) so the
        PII guard stays satisfied. Here we run a single controlled, parameterized
        lookup to surface the instructor's public name (the same name shown on
        the course page) and drop the raw id from the output.
        """
        instructor_ids = sorted(
            {str(row["instructor_id"]) for row in rows if row.get("instructor_id")}
        )
        name_map: dict[str, str] = {}
        if instructor_ids:
            lookup_sql = """
                SELECT
                    u.id::text AS id,
                    COALESCE(NULLIF(TRIM(pr.full_name), ''), u.username, 'Giang vien') AS name
                FROM users u
                LEFT JOIN profiles pr
                    ON pr.user_id = u.id
                   AND pr.is_active = 'Y'
                WHERE u.id = ANY(:ids)
            """
            async with get_db_session() as session:
                result = await session.execute(text(lookup_sql), {"ids": instructor_ids})
                name_map = {row["id"]: row["name"] for row in result.mappings().all()}
        for row in rows:
            raw_id = row.pop("instructor_id", None)
            row["instructor"] = name_map.get(str(raw_id), "Giang vien") if raw_id else "Giang vien"
        return rows

    async def _execute_sql(
        self,
        sql: str,
        *,
        params: dict[str, Any] | None = None,
    ) -> tuple[list[dict[str, Any]], list[str]]:
        async with get_db_session() as session:
            result = await session.execute(text(sql), params or {})
            rows = [dict(row) for row in result.mappings().all()]
        columns = list(rows[0].keys()) if rows else self._extract_selected_columns(sql)
        return rows, columns

    async def _plan_query(
        self,
        question: str,
        entities: dict[str, Any],
        *,
        policy: dict[str, Any],
        user_id: str | None,
        scope: str,
        prior_analysis: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        semantic_plan = analytics_semantic_planner.plan(
            question,
            entities,
            policy=policy,
            user_id=user_id,
            scope=scope,
            prior_analysis=prior_analysis,
        )
        if semantic_plan:
            return semantic_plan

        fallback = self._fallback_plan(question, entities, user_id=user_id, scope=scope, prefer_semantic=False)
        if self._should_force_deterministic_plan(question, entities, scope=scope, user_id=user_id):
            return fallback
        prior_block = self._render_prior_analysis_for_prompt(prior_analysis)
        prompt = (
            "You are the TechHub analytics SQL planner.\n"
            f"{self._schema_context}\n"
            f"User question: {question}\n"
            f"Extracted entities: {entities}\n"
            f"{prior_block}"
            f"Runtime policy: user_role={policy.get('userRole')} | sql_max_rows={policy.get('sqlMaxRows')} | "
            f"pii_access={policy.get('piiAccess')} | request_user_id={user_id or 'n/a'} | analytics_scope={scope}\n"
            "Return valid JSON only with keys: metric, timeRange, title, chartType, sql, explanation.\n"
            "SQL must be safe for PostgreSQL and only use allowed tables.\n"
            "Always add a LIMIT clause. Do not select raw PII columns when pii_access is false.\n"
            "If analytics_scope=personal and request_user_id exists, SQL must use the named placeholder :user_id.\n"
            "If a Previous analysis block is present, treat the user question as a refinement of it "
            "(filter, chart swap, scope pivot, time pivot) and reuse its metric/scope unless the "
            "user explicitly asks to change them."
        )
        plan = await switchable_ai_gateway.generate_structured_json(prompt=prompt, fallback_payload=fallback)
        if not isinstance(plan, dict) or not plan.get("sql"):
            return fallback
        planned_sql = str(plan.get("sql") or "")
        if scope == "personal" and user_id and ":user_id" not in planned_sql.lower():
            return fallback
        return {
            "metric": plan.get("metric") or fallback["metric"],
            "timeRange": plan.get("timeRange") or fallback["timeRange"],
            "title": plan.get("title") or fallback["title"],
            "chartType": plan.get("chartType") or fallback["chartType"],
            "sql": plan.get("sql") or fallback["sql"],
            "explanation": plan.get("explanation") or self._build_plan_explanation(fallback, scope=scope),
            "params": fallback.get("params", {}),
            "scope": scope,
            "executionMode": "llm_planner",
        }

    @staticmethod
    def _render_prior_analysis_for_prompt(prior_analysis: dict[str, Any] | None) -> str:
        if not prior_analysis or not isinstance(prior_analysis, dict):
            return ""
        title = str(prior_analysis.get("title") or "previous analysis")
        metric = str(prior_analysis.get("metric") or "n/a")
        scope_prev = str(prior_analysis.get("scope") or "n/a")
        chart_type = str(prior_analysis.get("chartType") or "n/a")
        time_range = str(prior_analysis.get("timeRange") or "n/a")
        sql = str(prior_analysis.get("sql") or "").strip()
        sql_block = sql if len(sql) <= 600 else f"{sql[:600]}..."
        return (
            "Previous analysis (treat the current question as a refinement of this):\n"
            f"- title: {title}\n"
            f"- metric: {metric}\n"
            f"- scope: {scope_prev}\n"
            f"- chartType: {chart_type}\n"
            f"- timeRange: {time_range}\n"
            f"- sql: {sql_block}\n\n"
        )

    @staticmethod
    def _merge_prior_entities(
        entities: dict[str, Any],
        prior_analysis: dict[str, Any] | None,
    ) -> dict[str, Any]:
        if not prior_analysis or not isinstance(prior_analysis, dict):
            return dict(entities)
        merged = dict(entities)
        # Inherit metric / time_range / scope only when the current question
        # didn't already specify them. The current extractor uses snake_case
        # keys, whereas the FE snapshot uses camelCase.
        if not merged.get("metric") and prior_analysis.get("metric"):
            merged["metric"] = prior_analysis["metric"]
        if not merged.get("time_range") and prior_analysis.get("timeRange"):
            merged["time_range"] = prior_analysis["timeRange"]
        if not merged.get("scope") and prior_analysis.get("scope"):
            merged["scope"] = prior_analysis["scope"]
        if not merged.get("enrollment_scope") and prior_analysis.get("logicSummary"):
            logic = prior_analysis.get("logicSummary") or {}
            if isinstance(logic, dict) and logic.get("enrollmentScope"):
                merged["enrollment_scope"] = logic["enrollmentScope"]
        return merged

    def _infer_scope_with_prior(
        self,
        question: str,
        entities: dict[str, Any],
        prior_analysis: dict[str, Any] | None,
    ) -> str:
        scope = self._infer_scope(question, entities)
        if scope == "platform" and prior_analysis and isinstance(prior_analysis, dict):
            prior_scope = str(prior_analysis.get("scope") or "").lower()
            if prior_scope in {"personal", "platform"}:
                normalized = self._normalize_query_text(question or "")
                # If the follow-up doesn't itself mention scope keywords, keep
                # the scope of the previous analysis so "đổi sang biểu đồ
                # đường" on personal data stays personal.
                scope_tokens = [
                    "cua toi",
                    "cho toi",
                    "toan he thong",
                    "toan bo",
                    "toan doanh nghiep",
                    "platform",
                    "all users",
                    "so voi toan",
                ]
                if not any(token in f"{normalized} " for token in scope_tokens):
                    return prior_scope
        return scope

    @staticmethod
    def _detect_chart_swap(
        question: str,
        prior_analysis: dict[str, Any] | None,
    ) -> str | None:
        """If the user's follow-up is only asking for a chart-type swap, return
        the requested chart type; else None."""
        if not prior_analysis or not isinstance(prior_analysis, dict):
            return None
        normalized = AnalyticsService._static_normalize(question or "")
        if not normalized:
            return None
        # Must be a short utterance to be treated as a pure chart swap.
        if len(normalized.split()) > 12:
            return None
        type_map = {
            "line": ("bieu do duong", "line chart", "sang line", "duong", "line"),
            "bar": ("bieu do cot", "bar chart", "sang bar", "cot", "bar"),
            "pie": ("bieu do tron", "pie chart", "sang pie", "tron", "pie"),
        }
        requested: str | None = None
        for chart_type, tokens in type_map.items():
            if any(token in normalized for token in tokens):
                requested = chart_type
                break
        if not requested:
            return None
        # Look for swap verbs so we don't accidentally match "phân tích bằng
        # biểu đồ cột" as a full new request.
        swap_verbs = (
            "doi sang",
            "chuyen sang",
            "ve lai",
            "ve bieu do",
            "tao bieu do",
            "chuyen qua",
            "doi thanh",
            "render",
            "switch to",
            "change to",
            "draw",
            "create",
        )
        if not any(verb in normalized for verb in swap_verbs):
            # Also accept very short messages like "line chart" / "pie"
            if len(normalized.split()) > 3:
                return None
        return requested

    @staticmethod
    def _plan_from_prior(
        prior_analysis: dict[str, Any],
        *,
        chart_type: str,
        question: str,
        user_id: str | None,
        scope: str,
    ) -> dict[str, Any]:
        prior_sql = str(prior_analysis.get("sql") or "")
        params = {"user_id": user_id} if scope == "personal" and user_id else {}
        return {
            "metric": prior_analysis.get("metric") or "analytics",
            "timeRange": prior_analysis.get("timeRange") or "all_time",
            "title": prior_analysis.get("title") or "TechHub analytics",
            "chartType": chart_type,
            "sql": prior_sql,
            "params": params,
            "scope": scope,
            "executionMode": "prior_refine",
            "explanation": (
                f"Refinement of the previous analysis: same SQL, rendered as '{chart_type}' chart "
                f"per the follow-up '{question}'."
            ),
        }

    @staticmethod
    def _static_normalize(text: str) -> str:
        lowered = (text or "").lower().replace("\u0111", "d").replace("Ä‘", "d")
        normalized = unicodedata.normalize("NFD", lowered)
        without_marks = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
        without_punctuation = re.sub(r"[^\w\s]", " ", without_marks)
        return re.sub(r"\s+", " ", without_punctuation).strip()

    def _fallback_plan(
        self,
        question: str,
        entities: dict[str, Any],
        *,
        user_id: str | None,
        scope: str,
        prefer_semantic: bool = True,
    ) -> dict[str, Any]:
        if prefer_semantic:
            semantic_plan = analytics_semantic_planner.plan(
                question,
                entities,
                policy={
                    "userRole": "SUPER_ADMIN",
                    "trustedUserId": user_id,
                    "sqlMaxRows": self._settings.sql_default_limit,
                    "piiAccess": False,
                },
                user_id=user_id,
                scope=scope,
            )
            if semantic_plan:
                return semantic_plan

        lowered = self._normalize_query_text(question)
        time_filter = self._time_filter_sql(entities.get("time_range"))
        params = {"user_id": user_id} if scope == "personal" and user_id else {}
        enrollment_scope_filter = "AND e.user_id = :user_id" if params else ""
        progress_scope_filter = "AND p.user_id = :user_id" if params else ""
        path_scope_filter = "AND pp.user_id = :user_id" if params else ""
        active_enrollment_filter = (
            "AND e.status IN ('ENROLLED', 'IN_PROGRESS')"
            if entities.get("enrollment_scope") == "active"
            else ""
        )

        if any(token in lowered for token in ["tien do", "completion", "hoan thanh", "progress"]):
            if params:
                return {
                    "metric": "progress",
                    "timeRange": entities.get("time_range") or "all_time",
                    "title": (
                        "Tien do cac khoa hoc ban dang theo hoc"
                        if entities.get("enrollment_scope") == "active"
                        else "Tien do hoc tap cua ban"
                    ),
                    "chartType": "bar",
                    "executionMode": "deterministic_fallback",
                    "explanation": (
                        "Dung deterministic fallback de tinh trung binh completion theo tung khoa hoc nguoi dung dang theo hoc, "
                        "uu tien enrollments active va progress cua chinh user hien tai."
                    ),
                    "sql": f"""
                        SELECT
                            c.title AS label,
                            ROUND(COALESCE(AVG(COALESCE(p.completion, 0.0)), 0.0)::numeric, 2) AS value
                        FROM enrollments e
                        JOIN courses c
                            ON c.id = e.course_id
                           AND c.is_active = 'Y'
                           AND c.status = 'PUBLISHED'
                        LEFT JOIN chapters ch
                            ON ch.course_id = c.id
                        LEFT JOIN lessons l
                            ON l.chapter_id = ch.id
                           AND l.is_active = 'Y'
                        LEFT JOIN progress p
                            ON p.lesson_id = l.id
                           AND p.user_id = e.user_id
                           AND p.is_active = 'Y'
                        WHERE e.is_active = 'Y'
                          {enrollment_scope_filter}
                          {active_enrollment_filter}
                          {time_filter('COALESCE(p.updated, e.updated, e.created)')}
                        GROUP BY e.id, e.course_id, c.id, c.title
                        ORDER BY MAX(e.updated) DESC NULLS LAST, value DESC, c.title ASC
                        LIMIT 10
                    """,
                    "params": params,
                    "scope": scope,
                }
            return {
                "metric": "progress",
                "timeRange": entities.get("time_range") or "all_time",
                "title": "Tien do hoc tap cua ban" if params else "Tien do hoc tap toan he thong",
                "chartType": "bar",
                "executionMode": "deterministic_fallback",
                "explanation": (
                    "Dung deterministic fallback de tong hop progress trung binh theo khoa hoc tu bang progress va lessons."
                ),
                "sql": f"""
                    SELECT
                        c.title AS label,
                        ROUND(COALESCE(AVG(p.completion), 0.0)::numeric, 2) AS value
                    FROM courses c
                    LEFT JOIN chapters ch ON ch.course_id = c.id
                    LEFT JOIN lessons l ON l.chapter_id = ch.id AND l.is_active = 'Y'
                    LEFT JOIN progress p ON p.lesson_id = l.id AND p.is_active = 'Y'
                    WHERE c.is_active = 'Y'
                      AND c.status = 'PUBLISHED'
                      {progress_scope_filter}
                      {time_filter('COALESCE(p.updated, c.updated)')}
                    GROUP BY c.id, c.title
                    ORDER BY value DESC, c.title ASC
                    LIMIT 10
                """,
                "params": params,
                "scope": scope,
            }

        if any(token in lowered for token in ["lesson", "bai hoc", "content type", "noi dung"]):
            return {
                "metric": "lesson_mix",
                "timeRange": entities.get("time_range") or "all_time",
                "title": "Co cau bai hoc da hoc cua ban" if params else "Co cau bai hoc theo loai noi dung",
                "chartType": "pie",
                "executionMode": "deterministic_fallback",
                "explanation": (
                    "Dung deterministic fallback de dem so bai hoc theo content_type, "
                    "co loc theo user hien tai neu cau hoi mang scope ca nhan."
                ),
                "sql": (
                    """
                    SELECT
                        COALESCE(l.content_type::text, 'UNKNOWN') AS label,
                        COUNT(*)::int AS value
                    FROM progress p
                    JOIN lessons l
                        ON l.id = p.lesson_id
                       AND l.is_active = 'Y'
                    WHERE p.is_active = 'Y'
                      AND p.user_id = :user_id
                    GROUP BY COALESCE(l.content_type::text, 'UNKNOWN')
                    ORDER BY value DESC, label ASC
                    """
                    if params
                    else
                    """
                    SELECT
                        COALESCE(l.content_type::text, 'UNKNOWN') AS label,
                        COUNT(*)::int AS value
                    FROM lessons l
                    WHERE l.is_active = 'Y'
                    GROUP BY COALESCE(l.content_type::text, 'UNKNOWN')
                    ORDER BY value DESC, label ASC
                    """
                ),
                "params": params,
                "scope": scope,
            }

        if any(token in lowered for token in ["learning path", "lo trinh", "path"]):
            return {
                "metric": "learning_path_completion",
                "timeRange": entities.get("time_range") or "all_time",
                "title": "Tien do learning path cua ban" if params else "Tien do learning path toan he thong",
                "chartType": "bar",
                "executionMode": "deterministic_fallback",
                "explanation": (
                    "Dung deterministic fallback de tong hop completion trung binh theo learning path."
                ),
                "sql": f"""
                    SELECT
                        lp.title AS label,
                        ROUND(COALESCE(AVG(pp.completion), 0.0)::numeric, 2) AS value
                    FROM learning_paths lp
                    LEFT JOIN path_progress pp
                        ON pp.path_id = lp.id
                       AND pp.is_active = 'Y'
                    WHERE lp.is_active = 'Y'
                      {path_scope_filter}
                      {time_filter('COALESCE(pp.updated, lp.updated)')}
                    GROUP BY lp.id, lp.title
                    ORDER BY value DESC, lp.title ASC
                    LIMIT 10
                """,
                "params": params,
                "scope": scope,
            }

        return {
            "metric": "enrollments_by_level",
            "timeRange": entities.get("time_range") or "all_time",
            "title": "So khoa hoc ban da ghi danh theo cap do" if params else "So luong ghi danh theo cap do khoa hoc",
            "chartType": "bar",
            "executionMode": "deterministic_fallback",
            "explanation": (
                "Dung deterministic fallback de dem so enrollments theo cap do khoa hoc, "
                "co the loc theo user hien tai va trang thai dang hoc."
            ),
            "sql": f"""
                SELECT
                    COALESCE(c.level::text, 'UNKNOWN') AS label,
                    COUNT(DISTINCT e.id)::int AS value
                FROM enrollments e
                JOIN courses c
                   ON c.id = e.course_id
                   AND c.is_active = 'Y'
                WHERE e.is_active = 'Y'
                  {enrollment_scope_filter}
                  {active_enrollment_filter}
                  {time_filter('COALESCE(e.updated, e.created)')}
                GROUP BY COALESCE(c.level::text, 'UNKNOWN')
                ORDER BY value DESC, label ASC
            """,
            "params": params,
            "scope": scope,
        }

    async def _summarize(
        self,
        question: str,
        rows: list[dict[str, Any]],
        plan: dict[str, Any],
        *,
        scope: str,
        request_context: Any | None = None,
    ) -> str:
        if not rows:
            return "Khong co du lieu phu hop voi bo loc hien tai."

        preview = rows[:5]
        fallback = self._fallback_summary(rows, plan, scope=scope)
        prompt = (
            "Tom tat nhanh bang tieng Viet ket qua analytics cua TechHub.\n"
            f"Cau hoi goc: {question}\n"
            f"Title: {plan.get('title')}\n"
            f"Scope: {self._scope_label(scope)}\n"
            f"Rows preview: {preview}\n"
            "Tra ve 1-2 cau ngan gon, chi noi insight quan trong nhat."
        )
        summary = await switchable_ai_gateway.generate_text(
            prompt=append_request_instructions(prompt, request_context)
        )
        cleaned = summary.strip()
        return cleaned or fallback

    def _fallback_summary(self, rows: list[dict[str, Any]], plan: dict[str, Any], *, scope: str) -> str:
        if not rows:
            return "Khong co du lieu de tong hop."
        top = rows[0]
        label = top.get("label") or top.get("title") or top.get("name") or "muc dau tien"
        value = top.get("value")
        scope_prefix = "Du lieu cua ban" if scope == "personal" else "Du lieu toan he thong"
        if value is not None:
            return f"{scope_prefix}: noi bat nhat la {label} voi gia tri {value}."
        return f"{scope_prefix}: da tong hop {len(rows)} dong du lieu."

    def _build_plan_explanation(self, plan: dict[str, Any], *, scope: str) -> str:
        metric = str(plan.get("metric") or "analytics")
        title = str(plan.get("title") or "TechHub analytics")
        scope_label = self._scope_label(scope)
        chart_type = str(plan.get("chartType") or "bar")
        execution_mode = str(plan.get("executionMode") or "llm_planner")
        if execution_mode == "deterministic_fallback":
            return f"Logic deterministic fallback duoc su dung cho metric '{metric}' tren scope '{scope_label}', sau do render bang chart type '{chart_type}' voi title '{title}'."
        return f"LLM planner da sinh SQL cho metric '{metric}' tren scope '{scope_label}', sau do render bang chart type '{chart_type}' voi title '{title}'."

    @staticmethod
    def _build_chart_options(
        *,
        rows: list[dict[str, Any]],
        columns: list[str],
        chart_type: str,
    ) -> dict[str, Any]:
        """Compute FE-facing chart options from the raw result.

        - `availableChartTypes` lets the FE offer the correct swap set
        - `emptyState` is a high-level hint so previews can render consistent
          empty/all-zero/single-category variants
        - `colorPalette` stays aligned with the FE recharts default palette
        """
        if not rows or not columns:
            return {
                "availableChartTypes": [],
                "colorPalette": list(DEFAULT_COLOR_PALETTE),
                "emptyState": "empty",
                "valueAxisLabel": None,
                "categoryAxisLabel": None,
                "stacked": False,
                "legend": True,
            }

        sample = rows[0]
        category_key: str | None = None
        for column in columns:
            if isinstance(sample.get(column), str):
                category_key = column
                break
        numeric_keys = [
            column
            for column in columns
            if isinstance(sample.get(column), Number) and not isinstance(sample.get(column), bool)
        ]
        if category_key is None and columns:
            category_key = columns[0]
        numeric_values: list[float] = []
        for row in rows:
            for key in numeric_keys:
                value = row.get(key)
                if isinstance(value, Number) and not isinstance(value, bool):
                    try:
                        numeric_values.append(float(value))
                    except (TypeError, ValueError):
                        continue
        category_count = len({row.get(category_key) for row in rows}) if category_key else 0
        empty_state = detect_empty_state(rows, numeric_values, category_count)
        available = build_available_chart_types(len(numeric_keys) or 1, category_count)
        # Surface the primary numeric key as the value axis label where useful.
        value_axis_label = numeric_keys[0] if len(numeric_keys) == 1 else None
        return {
            "availableChartTypes": available,
            "colorPalette": list(DEFAULT_COLOR_PALETTE),
            "emptyState": empty_state,
            "valueAxisLabel": value_axis_label,
            "categoryAxisLabel": category_key,
            "stacked": False,
            "legend": True,
        }

    def _build_logic_summary(self, plan: dict[str, Any], *, rows: list[dict[str, Any]], scope: str) -> dict[str, Any]:
        return {
            "metric": plan.get("metric") or "analytics",
            "title": plan.get("title") or "TechHub analytics",
            "chartType": plan.get("chartType") or "bar",
            "timeRange": plan.get("timeRange") or "all_time",
            "scope": scope,
            "scopeLabel": self._scope_label(scope),
            "executionMode": plan.get("executionMode") or "llm_planner",
            "rowCount": len(rows),
        }

    def _build_suggested_actions(
        self,
        *,
        plan: dict[str, Any],
        rows: list[dict[str, Any]],
        columns: list[str],
        scope: str,
        chart_type: str,
        question: str,
        entities: dict[str, Any],
    ) -> list[dict[str, Any]]:
        """Build deterministic analyst-style follow-up actions from the query plan.

        Returns a list of dict actions. Shape:
          {
            "id": str,
            "label": str,
            "description": str,
            "kind": "prompt" | "change_chart_type" | "export_csv" | "copy_sql" | "refine_filter",
            "prompt": str | None,   # for kind=prompt / refine_filter
            "payload": dict | None, # for kind=change_chart_type (has "chartType")
            "icon": str,
            "tone": "primary" | "secondary",
          }
        """
        actions: list[dict[str, Any]] = []
        normalized_chart = (chart_type or "bar").lower()
        has_rows = bool(rows)
        single_series = len(columns) <= 2
        metric = str(plan.get("metric") or entities.get("metric") or "analytics")
        title = str(plan.get("title") or "phan tich").lower()
        time_range = str(plan.get("timeRange") or entities.get("time_range") or "all_time").lower()

        # 1. Chart type alternatives (client-side swap for snappy UX)
        if has_rows:
            alternatives: list[str] = []
            if normalized_chart != "line":
                alternatives.append("line")
            if normalized_chart != "bar":
                alternatives.append("bar")
            if normalized_chart != "pie" and single_series:
                alternatives.append("pie")
            for alt in alternatives[:2]:
                label_map = {"line": "Chuyen sang bieu do duong", "bar": "Chuyen sang bieu do cot", "pie": "Chuyen sang bieu do tron"}
                actions.append(
                    {
                        "id": f"chart-{alt}",
                        "label": label_map.get(alt, f"Chuyen sang {alt}"),
                        "description": f"Render lai ket qua hien tai duoi dang {alt} chart.",
                        "kind": "change_chart_type",
                        "payload": {"chartType": alt},
                        "icon": f"chart-{alt}",
                        "tone": "secondary",
                    }
                )

        # 2. Scope pivot (personal <-> platform)
        if scope == "personal":
            actions.append(
                {
                    "id": "scope-platform",
                    "label": "So sanh voi toan he thong",
                    "description": "Chay lai truy van tren du lieu toan he thong de doi chieu.",
                    "kind": "prompt",
                    "prompt": f"So sanh {metric} cua toi voi toan he thong",
                    "icon": "scope",
                    "tone": "primary",
                }
            )
        else:
            actions.append(
                {
                    "id": "scope-personal",
                    "label": "Chi lay du lieu cua toi",
                    "description": "Loc lai truy van theo du lieu ca nhan cua ban.",
                    "kind": "prompt",
                    "prompt": f"Chi lay {metric} cua toi",
                    "icon": "scope",
                    "tone": "primary",
                }
            )

        # 3. SQL explanation (analyst workflow)
        if plan.get("sql"):
            actions.append(
                {
                    "id": "explain-sql",
                    "label": "Giai thich cau SQL nay",
                    "description": "AI dien giai tung buoc cua SQL vua chay de ban kiem chung.",
                    "kind": "prompt",
                    "prompt": "Giai thich chi tiet cau SQL vua chay: tung buoc lam gi, tai sao can cac JOIN va WHERE do.",
                    "icon": "explain",
                    "tone": "secondary",
                }
            )

        # 4. Context-aware refine filter
        refine = self._build_refine_action(question=question, entities=entities, metric=metric, scope=scope)
        if refine:
            actions.append(refine)

        # 5. Time-range pivot
        if time_range in {"all_time", ""} and has_rows:
            actions.append(
                {
                    "id": "time-this-month",
                    "label": "Chi lay du lieu thang nay",
                    "description": "Thu hep cua so thoi gian ve thang hien tai.",
                    "kind": "prompt",
                    "prompt": f"Chi lay {metric} trong thang nay",
                    "icon": "calendar",
                    "tone": "secondary",
                }
            )
        elif time_range == "this_month":
            actions.append(
                {
                    "id": "time-all",
                    "label": "Mo rong toan thoi gian",
                    "description": "Bo gioi han thang de xem toan bo du lieu.",
                    "kind": "prompt",
                    "prompt": f"Lay {metric} cho toan bo thoi gian",
                    "icon": "calendar",
                    "tone": "secondary",
                }
            )

        # 6. Export + copy SQL (client-side actions, no re-query needed)
        if has_rows:
            actions.append(
                {
                    "id": "export-csv",
                    "label": "Xuat CSV",
                    "description": "Tai xuong toan bo ket qua dang CSV.",
                    "kind": "export_csv",
                    "icon": "download",
                    "tone": "secondary",
                }
            )
        if plan.get("sql"):
            actions.append(
                {
                    "id": "copy-sql",
                    "label": "Sao chep SQL",
                    "description": "Copy cau SQL da chay vao clipboard.",
                    "kind": "copy_sql",
                    "icon": "copy",
                    "tone": "secondary",
                }
            )

        # Deduplicate by id, preserve order, cap at 7 actions to keep UI clean
        seen: set[str] = set()
        deduped: list[dict[str, Any]] = []
        for action in actions:
            action_id = str(action.get("id") or "")
            if not action_id or action_id in seen:
                continue
            seen.add(action_id)
            deduped.append(action)
            if len(deduped) >= 7:
                break
        return deduped

    def _build_refine_action(
        self,
        *,
        question: str,
        entities: dict[str, Any],
        metric: str,
        scope: str,
    ) -> dict[str, Any] | None:
        normalized = self._normalize_query_text(question or "")
        enrollment_scope = str(entities.get("enrollment_scope") or "").lower()

        if "progress" in metric.lower() or "tien do" in normalized or "completion" in normalized:
            if enrollment_scope != "active" and scope == "personal":
                return {
                    "id": "filter-active-enrollments",
                    "label": "Chi lay khoa dang hoc",
                    "description": "Loc ket qua ve cac khoa co trang thai dang hoc.",
                    "kind": "prompt",
                    "prompt": "Loc lai ket qua, chi lay cac khoa toi dang hoc",
                    "icon": "filter",
                    "tone": "secondary",
                }

        if metric.lower() in {"lesson_mix", "lessons"} or "bai hoc" in normalized:
            return {
                "id": "filter-published",
                "label": "Chi lay bai hoc da xuat ban",
                "description": "Loc bai hoc co trang thai published.",
                "kind": "prompt",
                "prompt": "Chi lay bai hoc da xuat ban trong ket qua nay",
                "icon": "filter",
                "tone": "secondary",
            }

        return None

    def _validate_sql(
        self,
        sql: str,
        *,
        policy: dict[str, Any],
        metric: str | None = None,
        scope: str = "platform",
    ) -> str:
        normalized = self._normalize_platform_flags(re.sub(r"\s+", " ", sql).strip())
        if not normalized:
            raise ValueError("Analytics planner did not produce SQL.")

        lowered = normalized.lower().rstrip(";")
        if not (lowered.startswith("select ") or lowered.startswith("with ")):
            raise ValueError("Only read-only SELECT analytics queries are allowed.")

        forbidden = [" update ", " delete ", " drop ", " alter ", " insert ", " truncate ", " create ", " grant "]
        if any(token in f" {lowered} " for token in forbidden):
            raise ValueError("Unsafe SQL generated for analytics query.")

        # AST-based safety guard: parses the SQL with sqlglot so we can detect
        # tables/columns regardless of expression nesting. Falls back to the
        # legacy regex checks only if parsing fails, so behaviour for the
        # deterministic templates remains identical.
        try:
            parsed = parse_and_inspect(normalized)
            assert_tables_allowed(parsed, self._allowed_tables)
            tables = set(parsed.tables)
            if not policy.get("piiAccess", False):
                try:
                    assert_no_pii(parsed, self._sensitive_columns)
                except SqlAstGuardError as exc:
                    raise ValueError(str(exc)) from exc
        except SqlAstGuardError as exc:
            raise ValueError(str(exc)) from exc

        validate_metric_plan(
            metric,
            sql=normalized,
            tables=tables,
            policy=policy,
            scope=scope,
            table_contracts=self._runtime_tables,
        )

        limited = self._apply_limit(normalized.rstrip(";"), max_rows=int(policy.get("sqlMaxRows") or self._settings.sql_default_limit))
        return limited

    async def _refresh_contract_context(self) -> None:
        contract = await data_contract_registry.get_contract()
        self._allowed_tables = set(contract.analytics_allowed_tables)
        self._sensitive_columns = set(contract.analytics_sensitive_columns)
        self._runtime_tables = contract.tables
        self._schema_context = contract.render_analytics_schema_context()

    # Platform-scope metrics that expose only public catalog information and are
    # therefore safe for any authenticated user (including learners) to query.
    PUBLIC_PLATFORM_METRICS = frozenset({
        "courses_by_instructor",
        "course_catalog",
        "course_pricing",
        "course_structure",
        "learning_path_catalog",
        "learning_path_courses",
        "blog_catalog",
    })

    @classmethod
    def _enforce_analytics_access(
        cls,
        policy: dict[str, Any],
        *,
        scope: str,
        trusted_user_id: str | None,
        metric: str = "",
    ) -> None:
        user_role = str(policy.get("userRole") or "USER").upper()
        if not policy.get("allowDataQuery", False):
            raise ValueError("Current runtime policy does not allow data-query access for this request.")

        if scope == "personal":
            if not trusted_user_id:
                raise ValueError("Personal analytics requires trusted user identity.")
            if user_role not in {"LEARNER", "INSTRUCTOR", "STAFF", "ADMIN", "SUPER_ADMIN"}:
                raise ValueError("Current role cannot access personal analytics.")
            return

        if scope == "platform":
            if metric in cls.PUBLIC_PLATFORM_METRICS:
                if user_role not in {"LEARNER", "INSTRUCTOR", "STAFF", "ADMIN", "SUPER_ADMIN"}:
                    raise ValueError("Current role cannot access analytics.")
                return
            if user_role not in {"INSTRUCTOR", "STAFF", "ADMIN", "SUPER_ADMIN"}:
                raise ValueError("Platform analytics requires instructor, staff, or admin role.")

    @staticmethod
    def _normalize_platform_flags(sql: str) -> str:
        normalized = re.sub(
            r"((?:[a-z_][a-z0-9_]*\.)?is_active)\s*=\s*true\b",
            r"\1 = 'Y'",
            sql,
            flags=re.I,
        )
        normalized = re.sub(
            r"((?:[a-z_][a-z0-9_]*\.)?is_active)\s*=\s*false\b",
            r"\1 = 'N'",
            normalized,
            flags=re.I,
        )
        return normalized

    @staticmethod
    def _extract_selected_columns(sql: str) -> list[str]:
        match = re.search(r"select\s+(.*?)\s+from\s", sql, flags=re.I | re.S)
        if not match:
            return []
        raw_columns = match.group(1).split(",")
        columns = []
        for raw in raw_columns:
            alias_match = re.search(r"\bas\s+([a-z_][a-z0-9_]*)\b", raw, flags=re.I)
            if alias_match:
                columns.append(alias_match.group(1))
            else:
                columns.append(raw.strip().split(".")[-1])
        return [column.strip('"') for column in columns]

    def _collect_sensitive_selected_columns(self, sql: str) -> set[str]:
        match = re.search(r"select\s+(.*?)\s+from\s", sql, flags=re.I | re.S)
        if not match:
            return set()
        select_clause = match.group(1)
        blocked = set()
        for column in self._sensitive_columns:
            pattern = rf'(?<![a-z0-9_])(?:[a-z_][a-z0-9_]*\.)?"?{re.escape(column)}"?(?![a-z0-9_])'
            if re.search(pattern, select_clause, flags=re.I):
                blocked.add(column)
        return blocked

    @staticmethod
    def _collect_tables(sql: str) -> set[str]:
        return set(re.findall(r"\b(?:from|join)\s+([a-z_][a-z0-9_]*)", sql.lower()))

    @staticmethod
    def _apply_limit(sql: str, *, max_rows: int) -> str:
        limit_match = re.search(r"\blimit\s+(\d+)\b", sql, flags=re.I)
        if limit_match:
            current = int(limit_match.group(1))
            if current > max_rows:
                return re.sub(r"\blimit\s+\d+\b", f"LIMIT {max_rows}", sql, flags=re.I)
            return sql
        return f"{sql} LIMIT {max_rows}"

    @staticmethod
    def _time_filter_sql(time_range: str | None):
        mapping = {
            "today": "AND {column} >= date_trunc('day', now())",
            "this_month": "AND {column} >= date_trunc('month', now())",
        }

        def render(column: str) -> str:
            template = mapping.get(str(time_range or "").lower())
            return template.format(column=column) if template else ""

        return render

    @staticmethod
    def _normalize_text(text: str) -> str:
        lowered = (text or "").lower().replace("đ", "d")
        normalized = unicodedata.normalize("NFD", lowered)
        without_marks = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
        without_punctuation = re.sub(r"[^\w\s]", " ", without_marks)
        return re.sub(r"\s+", " ", without_punctuation).strip()

    @staticmethod
    def _normalize_query_text(text: str) -> str:
        lowered = (text or "").lower().replace("đ", "d").replace("Ä‘", "d")
        normalized = unicodedata.normalize("NFD", lowered)
        without_marks = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
        without_punctuation = re.sub(r"[^\w\s]", " ", without_marks)
        return re.sub(r"\s+", " ", without_punctuation).strip()

    def _infer_scope(self, question: str, entities: dict[str, Any]) -> str:
        explicit_scope = str(entities.get("scope") or "").lower()
        if explicit_scope in {"personal", "platform"}:
            return explicit_scope
        normalized = self._normalize_query_text(question)
        personal_tokens = [
            "cua toi",
            "cho toi",
            "toi da",
            "toi dang",
            "lich su hoc cua toi",
            "lich su hoc hien tai",
            "dang theo hoc",
            "dang hoc",
            "toi dang hoc",
            "toi dang theo hoc",
            "my ",
        ]
        if any(token in f"{normalized} " for token in personal_tokens):
            return "personal"
        return "platform"

    @staticmethod
    def _scope_label(scope: str) -> str:
        return "Du lieu cua ban" if scope == "personal" else "Du lieu toan he thong"

    @staticmethod
    def _normalize_query_text(text: str) -> str:
        lowered = (text or "").lower().replace("\u0111", "d").replace("Ä‘", "d").replace("Ã„â€˜", "d")
        normalized = unicodedata.normalize("NFD", lowered)
        without_marks = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
        without_punctuation = re.sub(r"[^\w\s]", " ", without_marks)
        return re.sub(r"\s+", " ", without_punctuation).strip()

    def _should_force_deterministic_plan(
        self,
        question: str,
        entities: dict[str, Any],
        *,
        scope: str,
        user_id: str | None,
    ) -> bool:
        if scope != "personal" or not user_id:
            return False
        normalized = self._normalize_query_text(question)
        metric = str(entities.get("metric") or "")
        if metric == "progress":
            return True
        return entities.get("enrollment_scope") == "active" and any(
            token in normalized for token in ["tien do", "completion", "hoan thanh", "progress", "bieu do", "chart"]
        )

    def _should_retry_with_fallback(
        self,
        question: str,
        entities: dict[str, Any],
        *,
        scope: str,
        rows: list[dict[str, Any]],
    ) -> bool:
        if rows:
            return False
        if scope != "personal":
            return False
        normalized = self._normalize_query_text(question)
        metric = str(entities.get("metric") or "")
        if metric == "progress":
            return True
        return entities.get("enrollment_scope") == "active" and any(
            token in normalized for token in ["bieu do", "chart", "progress", "tien do", "hoan thanh"]
        )

    @staticmethod
    def _normalize_rows(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
        normalized_rows: list[dict[str, Any]] = []
        for row in rows:
            normalized_row: dict[str, Any] = {}
            for key, value in row.items():
                if isinstance(value, Decimal):
                    normalized_row[key] = float(value)
                else:
                    normalized_row[key] = value
            normalized_rows.append(normalized_row)
        return normalized_rows


analytics_service = AnalyticsService()
