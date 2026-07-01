from __future__ import annotations

import logging
import re
import unicodedata

from app.orchestration.state.orchestrator_state import OrchestratorState, trace_step

logger = logging.getLogger(__name__)


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

    instructor_name = _extract_instructor_name(text)
    if instructor_name:
        entities["instructor_name"] = instructor_name

    course_name = _extract_course_name(text)
    if course_name:
        entities["course_name"] = course_name

    path_name = _extract_path_name(text)
    if path_name:
        entities["path_name"] = path_name

    topic_match = re.search(
        r"\b(python|java|javascript|typescript|react|next\.?js|node|spring|"
        r"database|postgresql|postgres|sql|nosql|mongodb|redis|"
        r"data science|data engineering|machine learning|deep learning|ai|"
        r"frontend|backend|fullstack|full-stack|fastapi|docker|kubernetes|"
        r"devops|cloud|aws|azure|gcp|spark|etl|microservice|api|rest|graphql|"
        r"security|bao mat|testing|kiem thu|postman|git|github|html|css)\b",
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

    logger.debug(
        "entity extraction",
        extra={
            "event": "entity_extract",
            "userInput": text,
            "entities": entities,
        },
    )
    trace_step(state, "entity_extract", "Extracted entities from user input.", entities=entities)
    return {
        "entities": entities,
        "execution_trace": list(state.get("execution_trace", [])),
    }


_INSTRUCTOR_TRIGGER = (
    r"(?:gi[aả]ng\s*vi[eê]n|gi[aá]o\s*vi[eê]n|gv|th[aầ]y(?:\s*gi[aá]o)?|"
    r"c[oô](?:\s*gi[aá]o)?|instructor|teacher)"
)
_INSTRUCTOR_STOP = (
    r"(?:c[oó]|d[aạ]y|day|[dđ]ang|s[oở]\s*h[uữ]u|so\s*huu|bao\s*nhi[eê]u|bao\s*nhieu|"
    r"g[oồ]m|gom|hi[eệ]n|hien|l[aà]|teaches?|owns?|v[oớ]i|voi|\?|$)"
)
_INSTRUCTOR_NAME_PATTERN = re.compile(
    _INSTRUCTOR_TRIGGER + r"\s+(?:t[eê]n\s+)?(?P<name>[^\d,?.!\n]+?)\s*" + _INSTRUCTOR_STOP,
    re.IGNORECASE,
)
_INSTRUCTOR_QUESTION_WORDS = {"nao", "nay", "do", "ai", "gi", "kia"}


def _extract_instructor_name(text: str) -> str | None:
    """Pull an instructor's name out of a course-context question.

    Returns None unless the question is clearly about courses, so generic
    chit-chat that happens to contain "thầy"/"cô" is not misread as a query.
    Question words like "nào" (in "giảng viên nào") are intentionally rejected
    so they fall through to the personal "which instructor teaches my courses"
    metric instead.
    """
    if not text:
        return None
    if not re.search(r"kh[oó]a\s*h[oọ]c|course|m[oô]n\s*h[oọ]c", text, re.IGNORECASE):
        return None
    match = _INSTRUCTOR_NAME_PATTERN.search(text)
    if not match:
        return None
    name = re.sub(r"\s+", " ", match.group("name")).strip(" -:–")
    if not name or len(name) > 60:
        return None
    if _normalize_text(name) in _INSTRUCTOR_QUESTION_WORDS:
        return None
    return name


_NAME_STOP = (
    r"(?:g[oồ]m|bao\s*g[oồ]m|c[oó]|bao\s*nhi[eê]u|bao\s*nhieu|l[aà]|"
    r"d[aạ]y|day|n[aà]o|nay|kh[oô]ng|\?|$)"
)
_COURSE_NAME_PATTERN = re.compile(
    r"kh[oó]a\s*(?:h[oọ]c)?\s+(?P<name>[^\d,?.!\n]+?)\s*" + _NAME_STOP,
    re.IGNORECASE,
)
_PATH_NAME_PATTERN = re.compile(
    r"(?:l[oộ]\s*tr[iì]nh|learning\s*path)\s+(?:h[oọ]c\s+)?(?P<name>[^\d,?.!\n]+?)\s*" + _NAME_STOP,
    re.IGNORECASE,
)
_NAME_QUESTION_WORDS = {"nao", "nay", "do", "gi", "kia", "hoc", "nhung", "cac", "ve"}


def _clean_name(raw: str | None) -> str | None:
    if not raw:
        return None
    name = re.sub(r"\s+", " ", raw).strip(" -:–\"'")
    if not name or len(name) > 80:
        return None
    if _normalize_text(name) in _NAME_QUESTION_WORDS:
        return None
    return name


def _extract_course_name(text: str) -> str | None:
    """Capture a course name for "khóa học X gồm những bài học nào".

    Only fires when the question is about a course's lessons/chapters, so it
    does not hijack generic course questions. The captured fragment is matched
    with LIKE downstream, so a partial title is enough.
    """
    if not text:
        return None
    if not re.search(r"b[aà]i\s*h[oọ]c|lesson|ch[uươ]+ng|chuong|n[oộ]i\s*dung", text, re.IGNORECASE):
        return None
    match = _COURSE_NAME_PATTERN.search(text)
    return _clean_name(match.group("name")) if match else None


def _extract_path_name(text: str) -> str | None:
    """Capture a learning-path name for "lộ trình X gồm những khóa nào".

    Only fires when the question asks for the courses inside the path, so the
    learning-path catalog / completion intents are not shadowed.
    """
    if not text:
        return None
    if not re.search(r"kh[oó]a|course|g[oồ]m", text, re.IGNORECASE):
        return None
    match = _PATH_NAME_PATTERN.search(text)
    return _clean_name(match.group("name")) if match else None


def _normalize_text(text: str) -> str:
    lowered = (text or "").lower().replace("\u0111", "d").replace("Ä‘", "d").replace("Ã„â€˜", "d")
    normalized = unicodedata.normalize("NFD", lowered)
    without_marks = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    return re.sub(r"\s+", " ", without_marks).strip()
