from __future__ import annotations

import re
import unicodedata
from typing import Any

from app.services.analytics.metric_registry import MetricDefinition, get_metric
from app.services.analytics.sql_templates import build_metric_sql, build_params


class AnalyticsSemanticPlanner:
    def plan(
        self,
        question: str,
        entities: dict[str, Any],
        *,
        policy: dict[str, Any],
        user_id: str | None,
        scope: str,
        prior_analysis: dict[str, Any] | None = None,
    ) -> dict[str, Any] | None:
        metric_key = self._select_metric(question, entities, prior_analysis)
        definition = get_metric(metric_key)
        if definition is None:
            return None

        resolved_scope = self._resolve_scope(definition, scope)
        user_role = str(policy.get("userRole") or "USER").upper()
        self._ensure_role_allowed(definition, user_role)
        if resolved_scope not in definition.allowed_scopes:
            raise ValueError(f"Metric '{definition.key}' does not support scope '{resolved_scope}'.")

        trusted_user_id = str(policy.get("trustedUserId") or user_id or "").strip() or None
        params = build_params(definition, scope=resolved_scope, user_role=user_role, user_id=trusted_user_id)
        time_range = self._resolve_time_range(question, entities, prior_analysis)
        chart_type = self._resolve_chart_type(question, definition.default_chart)
        sql = build_metric_sql(definition, scope=resolved_scope, user_role=user_role, time_range=time_range)

        return {
            "metric": definition.key,
            "timeRange": time_range,
            "title": definition.title,
            "chartType": chart_type,
            "sql": sql,
            "params": params,
            "scope": resolved_scope,
            "executionMode": "semantic_template",
            "explanation": self._explain(definition, resolved_scope),
            "metricDefinition": {
                "key": definition.key,
                "grain": definition.grain,
                "tables": list(definition.tables),
                "requiredParams": list(definition.required_params),
            },
        }

    def _select_metric(
        self,
        question: str,
        entities: dict[str, Any],
        prior_analysis: dict[str, Any] | None,
    ) -> str | None:
        explicit = str(entities.get("metric") or "").strip()
        if get_metric(explicit):
            return explicit
        if prior_analysis and isinstance(prior_analysis, dict):
            prior_metric = str(prior_analysis.get("metric") or "")
            if get_metric(prior_metric) and self._looks_like_refinement(question):
                return prior_metric

        normalized = _normalize(question)
        if any(token in normalized for token in ("doanh thu", "revenue", "gross revenue", "thu theo khoa")):
            return "revenue_by_course"
        if any(token in normalized for token in ("danh gia", "rating", "score trung binh", "diem danh gia")):
            return "average_course_rating"
        if any(token in normalized for token in ("phan bo diem", "grade distribution", "diem bai tap phan bo")):
            return "exercise_grade_distribution"
        if any(token in normalized for token in ("lam bai", "bai tap cua toi", "ket qua bai tap", "submission cua toi")):
            return "learner_submissions"
        if any(token in normalized for token in ("thoi gian hoc", "study time", "hoc bao lau")):
            return "study_time_by_course"
        if any(token in normalized for token in ("learning path", "lo trinh", "path completion", "hoan thanh lo trinh")):
            return "learning_path_completion"
        if any(token in normalized for token in ("tien do", "progress", "hoan thanh cua toi", "bieu do tien do")):
            return "learner_course_progress" if _looks_personal(normalized) else "course_completion_rate"
        if any(token in normalized for token in ("hoc vien dang hoc", "dang hoc nhieu", "nhieu hoc vien", "enrollment")):
            if "level" in normalized or "cap do" in normalized:
                return "active_enrollments_by_level"
            return "active_enrollments_by_course"
        if any(token in normalized for token in ("khoa nao hoan thanh", "hoan thanh tot", "completion rate")):
            return "course_completion_rate"
        return None

    @staticmethod
    def _resolve_scope(definition: MetricDefinition, requested_scope: str) -> str:
        if requested_scope in definition.allowed_scopes:
            return requested_scope
        if "personal" in definition.allowed_scopes and "platform" not in definition.allowed_scopes:
            return "personal"
        if "platform" in definition.allowed_scopes and "personal" not in definition.allowed_scopes:
            return "platform"
        return definition.allowed_scopes[0]

    @staticmethod
    def _ensure_role_allowed(definition: MetricDefinition, user_role: str) -> None:
        if user_role not in definition.required_roles:
            raise ValueError(f"Role '{user_role}' cannot access metric '{definition.key}'.")

    @staticmethod
    def _resolve_time_range(
        question: str,
        entities: dict[str, Any],
        prior_analysis: dict[str, Any] | None,
    ) -> str:
        explicit = str(entities.get("time_range") or entities.get("timeRange") or "").strip().lower()
        if explicit:
            return explicit
        normalized = _normalize(question)
        if any(token in normalized for token in ("hom nay", "today", "trong ngay")):
            return "today"
        if any(token in normalized for token in ("thang nay", "this month", "trong thang")):
            return "this_month"
        if any(token in normalized for token in ("30 ngay", "last 30", "mot thang qua")):
            return "last_30_days"
        if prior_analysis and isinstance(prior_analysis, dict) and prior_analysis.get("timeRange"):
            return str(prior_analysis["timeRange"])
        return "all_time"

    @staticmethod
    def _resolve_chart_type(question: str, default_chart: str) -> str:
        normalized = _normalize(question)
        padded = f" {normalized} "
        # Phrase tokens may be substrings; standalone tokens like "tron"/"cot"
        # must be whole words so they don't match "trong"/"cong"/"cot moc".
        if "bieu do duong" in normalized or "line chart" in normalized or " line " in padded:
            return "line"
        if "bieu do tron" in normalized or "pie chart" in normalized or " tron " in padded or " pie " in padded:
            return "pie"
        if "bieu do cot" in normalized or "bar chart" in normalized or " cot " in padded or " bar " in padded:
            return "bar"
        return default_chart

    @staticmethod
    def _looks_like_refinement(question: str) -> bool:
        normalized = _normalize(question)
        return any(
            token in normalized
            for token in ("doi sang", "chuyen sang", "loc", "thang nay", "hom nay", "line", "bar", "pie")
        )

    @staticmethod
    def _explain(definition: MetricDefinition, scope: str) -> str:
        tables = " -> ".join(definition.tables)
        return (
            f"Semantic template '{definition.key}' duoc chon cho scope '{scope}'. "
            f"SQL duoc build tu metric registry, dung cac bang: {tables}."
        )


def _normalize(text: str) -> str:
    lowered = (text or "").lower().replace("đ", "d")
    normalized = unicodedata.normalize("NFD", lowered)
    without_marks = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    without_punctuation = re.sub(r"[^\w\s]", " ", without_marks)
    return re.sub(r"\s+", " ", without_punctuation).strip()


def _looks_personal(normalized: str) -> bool:
    return any(token in f"{normalized} " for token in ("cua toi", "cho toi", "toi ", "my "))


analytics_semantic_planner = AnalyticsSemanticPlanner()
