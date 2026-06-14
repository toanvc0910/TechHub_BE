from __future__ import annotations

import logging
import re
import unicodedata
from typing import Any

from app.services.analytics.metric_registry import MetricDefinition, get_metric
from app.services.analytics.sql_templates import build_metric_sql, build_params

logger = logging.getLogger(__name__)


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
        logger.debug(
            "semantic planner metric selection",
            extra={
                "event": "planner_select",
                "selectedMetric": metric_key,
                "matched": definition is not None,
                "entities": entities,
                "priorMetric": (prior_analysis or {}).get("metric") if isinstance(prior_analysis, dict) else None,
            },
        )
        if definition is None:
            return None

        resolved_scope = self._resolve_scope(definition, scope)
        user_role = str(policy.get("userRole") or "USER").upper()
        self._ensure_role_allowed(definition, user_role)
        if resolved_scope not in definition.allowed_scopes:
            raise ValueError(f"Metric '{definition.key}' does not support scope '{resolved_scope}'.")

        trusted_user_id = str(policy.get("trustedUserId") or user_id or "").strip() or None
        params = build_params(
            definition,
            scope=resolved_scope,
            user_role=user_role,
            user_id=trusted_user_id,
            entities=entities,
        )
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
        # Instructor-centric questions. These take priority over the generic
        # personal-history / enrollment metrics below.
        instructor_name = str(entities.get("instructor_name") or "").strip()
        if instructor_name:
            # "Giang vien A co bao nhieu khoa hoc, gom nhung khoa nao?"
            return "courses_by_instructor"
        mentions_instructor = any(
            token in normalized for token in ("giang vien", "giao vien", "instructor", "giảng vien")
        )
        if mentions_instructor and (
            _looks_personal(normalized)
            or "toi dang hoc" in normalized
            or "dang theo hoc" in normalized
            or "khoa hoc cua toi" in normalized
        ):
            # "Toi dang hoc khoa hoc cua giang vien nao?"
            return "learner_course_instructors"

        # Named-entity catalog breakdowns. These only fire when the entity
        # extractor captured a concrete name, so they are unambiguous.
        if str(entities.get("course_name") or "").strip():
            # "Khoa hoc X gom nhung bai hoc nao?"
            return "course_structure"
        if str(entities.get("path_name") or "").strip():
            # "Lo trinh X gom nhung khoa hoc nao?"
            return "learning_path_courses"

        # Blog catalog / search.
        if any(token in normalized for token in ("blog", "bai viet", "bai blog")):
            return "blog_catalog"

        # Personalized "what should I learn next": "gợi ý lộ trình/khóa học tiếp
        # theo", "học gì tiếp theo", "nên học tiếp khóa nào". Continuation phrasing
        # ("tiếp theo"/"học tiếp") means the learner wants suggestions anchored on
        # what they are already taking, not the global popularity catalog. Must
        # beat the learning_path_catalog / course_catalog mappings below.
        wants_next = any(
            token in normalized
            for token in ("tiep theo", "hoc tiep", "ke tiep", "next", "hoc gi tiep", "nen hoc gi")
        )
        suggest_ctx = any(
            token in normalized
            for token in ("goi y", "de xuat", "nen hoc", "recommend", "lo trinh", "khoa", "learning path")
        )
        if wants_next and suggest_ctx:
            return "recommended_next_courses"

        # Learning-path catalog: either a listing ("có những lộ trình nào",
        # "danh sách lộ trình") OR a suggestion ("gợi ý lộ trình tiếp theo",
        # "nên học lộ trình nào"). Both should surface the available paths, not
        # the admin completion-rate table. Excluded when the user is clearly
        # asking about progress/completion or their own personal data.
        if any(token in normalized for token in ("lo trinh", "learning path")) and any(
            token in normalized
            for token in (
                "co nhung", "nhung lo trinh", "danh sach", "liet ke", "co bao nhieu",
                "gom nhung", "co lo trinh nao", "nhung lo trinh nao",
                "goi y", "de xuat", "nen hoc", "phu hop", "tiep theo", "bat dau", "muon hoc",
            )
        ) and not any(token in normalized for token in ("tien do", "hoan thanh", "completion", "cua toi")):
            return "learning_path_catalog"

        # Course pricing (most expensive / cheapest / free).
        if any(
            token in normalized
            for token in ("gia khoa", "hoc phi", "dat nhat", "mac nhat", "re nhat", "mien phi", "free", "gia re", "gia cao", "bao nhieu tien", "gia bao nhieu")
        ):
            return "course_pricing"

        # General course catalog / topic search. A concrete topic combined with
        # any course mention ("có khóa nào về database", "khóa học Docker") is a
        # catalog lookup, regardless of exact word order or polite "gợi ý"
        # phrasing.
        topic = str(entities.get("topic") or "").strip()
        if topic and ("khoa" in normalized or "course" in normalized):
            return "course_catalog"
        if any(
            token in normalized
            for token in ("co nhung khoa hoc", "nhung khoa hoc nao", "danh sach khoa hoc", "khoa hoc ve", "khoa hoc nao ve", "liet ke khoa hoc", "co khoa hoc nao", "khoa hoc pho bien", "khoa hoc lien quan")
        ):
            return "course_catalog"
        # Personal learning history: "which courses have I studied / enrolled in",
        # "khóa học của tôi", "đã học khóa nào của giảng viên nào". Map to the
        # personal course-progress metric, which lists the user's enrolled
        # courses with completion. Exclude instructor/platform phrasing
        # ("hoc vien dang hoc" = how many students are studying).
        personal_history_tokens = (
            "da hoc",
            "da hoan thanh",
            "da dang ky",
            "khoa hoc cua toi",
            "khoa cua toi",
            "lich su hoc",
            "khoa hoc nao cua",
            "hoc khoa hoc nao",
            "khoa hoc nao toi",
        )
        if "hoc vien" not in normalized and (
            any(token in normalized for token in personal_history_tokens)
            or ("dang hoc" in normalized and _looks_personal(normalized))
        ):
            return "learner_course_progress"
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
