from __future__ import annotations

import re
from typing import Any

from sqlalchemy import text

from app.core.config import get_settings
from app.db.session import get_db_session
from app.services.llm_gateway import switchable_ai_gateway
from app.services.runtime_policy_service import runtime_policy_service


class AnalyticsService:
    def __init__(self) -> None:
        self._settings = get_settings()

    _allowed_tables = {
        "courses",
        "chapters",
        "lessons",
        "enrollments",
        "progress",
        "learning_paths",
        "learning_path_courses",
        "path_progress",
        "profiles",
        "users",
        "course_skills",
        "skills",
    }
    _sensitive_columns = {"email", "bio", "full_name", "username", "location"}

    _schema_context = """
Tables and safe columns:
- courses(id, title, description, level, language, status, created, is_active)
- chapters(id, course_id, "order")
- lessons(id, chapter_id, title, content_type, estimated_duration, created, is_active)
- enrollments(id, user_id, course_id, status, created, updated, is_active)
- progress(id, user_id, lesson_id, completion, created, updated, is_active)
- learning_paths(id, title, description, created, updated, is_active)
- learning_path_courses(path_id, course_id, "order", position_x, position_y, is_optional)
- path_progress(id, user_id, path_id, completion, updated, is_active)
- profiles(id, user_id, preferred_language, full_name, bio, created, updated, is_active)
- users(id, username, email, status, created, is_active)
- course_skills(id, course_id, skill_id)
- skills(id, name, category)
Only generate read-only PostgreSQL SQL. Use SELECT or WITH only.
Prefer aggregations useful for analytics dashboards.
When grouping, alias the category column as label and the numeric aggregation as value.
"""

    async def execute(
        self,
        question: str,
        entities: dict[str, Any] | None = None,
        *,
        request_context: Any | None = None,
        user_id: str | None = None,
    ) -> dict[str, Any]:
        policy = await runtime_policy_service.resolve(request_context)
        if not policy.get("allowDataQuery", True):
            raise ValueError("Current runtime policy does not allow data-query access for this request.")

        plan = await self._plan_query(question, entities or {}, policy=policy, user_id=user_id)
        sql = self._validate_sql(str(plan.get("sql") or ""), policy=policy)

        async with get_db_session() as session:
            result = await session.execute(text(sql))
            rows = [dict(row) for row in result.mappings().all()]

        columns = list(rows[0].keys()) if rows else self._extract_selected_columns(sql)
        summary = await self._summarize(question, rows, plan)
        return {
            "metric": plan.get("metric") or entities.get("metric") or "analytics",
            "timeRange": plan.get("timeRange") or entities.get("time_range") or "all_time",
            "rows": rows,
            "columns": columns,
            "sql": sql,
            "tables": sorted(self._collect_tables(sql)),
            "chartType": plan.get("chartType") or "bar",
            "title": plan.get("title") or "TechHub analytics",
            "summary": summary,
            "policy": {
                "userRole": policy.get("userRole"),
                "sqlMaxRows": policy.get("sqlMaxRows"),
                "piiAccess": policy.get("piiAccess"),
            },
        }

    async def _plan_query(self, question: str, entities: dict[str, Any], *, policy: dict[str, Any], user_id: str | None) -> dict[str, Any]:
        fallback = self._fallback_plan(question, entities)
        prompt = (
            "You are the TechHub analytics SQL planner.\n"
            f"{self._schema_context}\n"
            f"User question: {question}\n"
            f"Extracted entities: {entities}\n\n"
            f"Runtime policy: user_role={policy.get('userRole')} | sql_max_rows={policy.get('sqlMaxRows')} | "
            f"pii_access={policy.get('piiAccess')} | request_user_id={user_id or 'n/a'}\n"
            "Return valid JSON only with keys: metric, timeRange, title, chartType, sql.\n"
            "SQL must be safe for PostgreSQL and only use allowed tables.\n"
            "Always add a LIMIT clause. Do not select raw PII columns when pii_access is false."
        )
        plan = await switchable_ai_gateway.generate_structured_json(prompt=prompt, fallback_payload=fallback)
        if not isinstance(plan, dict) or not plan.get("sql"):
            return fallback
        return {
            "metric": plan.get("metric") or fallback["metric"],
            "timeRange": plan.get("timeRange") or fallback["timeRange"],
            "title": plan.get("title") or fallback["title"],
            "chartType": plan.get("chartType") or fallback["chartType"],
            "sql": plan.get("sql") or fallback["sql"],
        }

    def _fallback_plan(self, question: str, entities: dict[str, Any]) -> dict[str, Any]:
        lowered = question.lower()
        time_filter = self._time_filter_sql(entities.get("time_range"))

        if any(token in lowered for token in ["tien do", "completion", "hoan thanh", "progress"]):
            return {
                "metric": "progress",
                "timeRange": entities.get("time_range") or "all_time",
                "title": "Average course progress",
                "chartType": "bar",
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
                      {time_filter('COALESCE(p.updated, c.updated)')}
                    GROUP BY c.id, c.title
                    ORDER BY value DESC, c.title ASC
                    LIMIT 10
                """,
            }

        if any(token in lowered for token in ["lesson", "bai hoc", "content type", "noi dung"]):
            return {
                "metric": "lesson_mix",
                "timeRange": entities.get("time_range") or "all_time",
                "title": "Lesson mix by content type",
                "chartType": "pie",
                "sql": """
                    SELECT
                        COALESCE(l.content_type::text, 'UNKNOWN') AS label,
                        COUNT(*)::int AS value
                    FROM lessons l
                    WHERE l.is_active = 'Y'
                    GROUP BY COALESCE(l.content_type::text, 'UNKNOWN')
                    ORDER BY value DESC, label ASC
                """,
            }

        if any(token in lowered for token in ["learning path", "lo trinh", "path"]):
            return {
                "metric": "learning_path_completion",
                "timeRange": entities.get("time_range") or "all_time",
                "title": "Learning path completion",
                "chartType": "bar",
                "sql": f"""
                    SELECT
                        lp.title AS label,
                        ROUND(COALESCE(AVG(pp.completion), 0.0)::numeric, 2) AS value
                    FROM learning_paths lp
                    LEFT JOIN path_progress pp
                        ON pp.path_id = lp.id
                       AND pp.is_active = 'Y'
                    WHERE lp.is_active = 'Y'
                      {time_filter('COALESCE(pp.updated, lp.updated)')}
                    GROUP BY lp.id, lp.title
                    ORDER BY value DESC, lp.title ASC
                    LIMIT 10
                """,
            }

        return {
            "metric": "enrollments_by_level",
            "timeRange": entities.get("time_range") or "all_time",
            "title": "Enrollments by course level",
            "chartType": "bar",
            "sql": f"""
                SELECT
                    COALESCE(c.level::text, 'UNKNOWN') AS label,
                    COUNT(DISTINCT e.id)::int AS value
                FROM enrollments e
                JOIN courses c
                    ON c.id = e.course_id
                   AND c.is_active = 'Y'
                WHERE e.is_active = 'Y'
                  {time_filter('COALESCE(e.updated, e.created)')}
                GROUP BY COALESCE(c.level::text, 'UNKNOWN')
                ORDER BY value DESC, label ASC
            """,
        }

    async def _summarize(self, question: str, rows: list[dict[str, Any]], plan: dict[str, Any]) -> str:
        if not rows:
            return "Khong co du lieu phu hop voi bo loc hien tai."

        preview = rows[:5]
        fallback = self._fallback_summary(rows, plan)
        prompt = (
            "Tom tat nhanh bang tieng Viet ket qua analytics cua TechHub.\n"
            f"Cau hoi goc: {question}\n"
            f"Title: {plan.get('title')}\n"
            f"Rows preview: {preview}\n"
            "Tra ve 1-2 cau ngan gon, chi noi insight quan trong nhat."
        )
        summary = await switchable_ai_gateway.generate_text(prompt=prompt)
        cleaned = summary.strip()
        return cleaned or fallback

    def _fallback_summary(self, rows: list[dict[str, Any]], plan: dict[str, Any]) -> str:
        if not rows:
            return "Khong co du lieu de tong hop."
        top = rows[0]
        label = top.get("label") or top.get("title") or top.get("name") or "muc dau tien"
        value = top.get("value")
        if value is not None:
            return f"{plan.get('title')}: noi bat nhat la {label} voi gia tri {value}."
        return f"{plan.get('title')}: da tong hop {len(rows)} dong du lieu."

    def _validate_sql(self, sql: str, *, policy: dict[str, Any]) -> str:
        normalized = re.sub(r"\s+", " ", sql).strip()
        if not normalized:
            raise ValueError("Analytics planner did not produce SQL.")

        lowered = normalized.lower().rstrip(";")
        if not (lowered.startswith("select ") or lowered.startswith("with ")):
            raise ValueError("Only read-only SELECT analytics queries are allowed.")

        forbidden = [" update ", " delete ", " drop ", " alter ", " insert ", " truncate ", " create ", " grant "]
        if any(token in f" {lowered} " for token in forbidden):
            raise ValueError("Unsafe SQL generated for analytics query.")

        tables = self._collect_tables(lowered)
        unknown = tables.difference(self._allowed_tables)
        if unknown:
            raise ValueError(f"Query references unsupported tables: {sorted(unknown)}")

        if not policy.get("piiAccess", False):
            selected_columns = {column.lower() for column in self._extract_selected_columns(normalized)}
            blocked = sorted(selected_columns.intersection(self._sensitive_columns))
            if blocked:
                raise ValueError(f"Query selects restricted columns without PII access: {blocked}")

        limited = self._apply_limit(normalized.rstrip(";"), max_rows=int(policy.get("sqlMaxRows") or self._settings.sql_default_limit))
        return limited

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


analytics_service = AnalyticsService()
