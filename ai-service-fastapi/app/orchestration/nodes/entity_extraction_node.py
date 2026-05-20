from __future__ import annotations

import re
import unicodedata

from app.orchestration.state.orchestrator_state import OrchestratorState, trace_step


async def entity_extraction_node(state: OrchestratorState) -> dict:
    text = state["user_input"]
    lowered = text.lower()
    normalized = _normalize_text(text)
    entities: dict[str, str] = dict(state.get("entities", {}))

    level_map = {
        "beginner": ["co ban", "moi bat dau", "beginner"],
        "intermediate": ["trung cap", "intermediate"],
        "advanced": ["nang cao", "advanced"],
    }
    for level, terms in level_map.items():
        if any(term in normalized for term in [_normalize_text(item) for item in terms]):
            entities["level"] = level
            break

    topic_match = re.search(
        r"\b(python|java|javascript|react|sql|data science|machine learning|ai|frontend|backend|fastapi)\b",
        normalized,
    )
    if topic_match:
        entities["topic"] = topic_match.group(1)

    if any(token in normalized for token in ["thong ke", "analytics", "report", "bao nhieu", "tong hop", "dashboard"]):
        entities["metric"] = "summary"
    if any(token in normalized for token in ["enrollment", "ghi danh", "dang ky"]):
        entities["metric"] = "enrollments"
    if any(token in normalized for token in ["tien do", "completion", "hoan thanh", "progress"]):
        entities["metric"] = "progress"
    if any(
        token in normalized
        for token in [
            "dang theo hoc",
            "dang hoc",
            "hoc hien tai",
            "khoa hoc hien tai",
            "khoa hoc toi dang hoc",
            "current courses",
            "current course",
        ]
    ):
        entities["enrollment_scope"] = "active"
    if any(token in normalized for token in ["lesson", "bai hoc", "content type"]):
        entities["metric"] = "lesson_mix"
    if any(token in normalized for token in ["hom nay", "today"]):
        entities["time_range"] = "today"
    elif any(token in normalized for token in ["thang nay", "this month"]):
        entities["time_range"] = "this_month"

    analytics_tokens = [
        "thong ke",
        "bao nhieu",
        "tong hop",
        "report",
        "analytics",
        "dashboard",
        "bieu do",
        "chart",
        # Step 09 widened: progress/completion/lesson questions should also
        # trigger scope inference so "tiến độ học của tôi" picks personal.
        "tien do",
        "hoan thanh",
        "completion",
        "progress",
        "lesson",
        "bai hoc",
    ]
    personal_tokens = [
        "cua toi",
        "cho toi",
        "toi dang",
        "toi da",
        "my",
        "ca nhan",
        "lich su hoc cua toi",
        "lich su hoc hien tai",
        "dang theo hoc",
        "dang hoc",
        "toi dang hoc",
        "toi dang theo hoc",
    ]
    has_metric = bool(entities.get("metric"))
    has_analytics_token = any(token in normalized for token in analytics_tokens)
    has_personal_token = any(token in normalized for token in personal_tokens)
    if (has_metric or has_analytics_token) and has_personal_token:
        entities["scope"] = "personal"
    elif "scope" not in entities and (has_metric or has_analytics_token):
        entities["scope"] = "platform"

    uuid_match = re.search(
        r"\b([0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12})\b",
        lowered,
    )
    if uuid_match:
        entities["reference_id"] = uuid_match.group(1)

    # Chart type extraction for visualization/data_query flow. Word-boundary
    # matching with padded normalized text so "tron"/"cot" don't accidentally
    # match "trong"/"cong".
    padded = f" {normalized} "
    if "bieu do duong" in normalized or "line chart" in normalized or " line " in padded:
        entities["chart_type"] = "line"
    elif "bieu do tron" in normalized or "pie chart" in normalized or " tron " in padded or " pie " in padded:
        entities["chart_type"] = "pie"
    elif "bieu do cot" in normalized or "bar chart" in normalized or " cot " in padded or " bar " in padded:
        entities["chart_type"] = "bar"

    # File reference hint: explicit "file nay" / "tai lieu nay" / "tep nay"
    # signals the user wants the currently-active file, not just any file.
    if re.search(r"\b(file nay|tai lieu nay|tep nay|noi dung nay|document nay)\b", normalized):
        entities["file_scope"] = "active"
    elif re.search(r"\b(file|tai lieu|pdf|docx|document|tep)\b", normalized):
        entities["file_scope"] = "any"

    trace_step(state, "entity_extract", "Extracted entities from user input.", entities=entities)
    return {
        "entities": entities,
        "execution_trace": list(state.get("execution_trace", [])),
    }


def _normalize_text(text: str) -> str:
    lowered = (text or "").lower().replace("\u0111", "d").replace("Ä‘", "d").replace("Ã„â€˜", "d")
    normalized = unicodedata.normalize("NFD", lowered)
    without_marks = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    return re.sub(r"\s+", " ", without_marks).strip()
