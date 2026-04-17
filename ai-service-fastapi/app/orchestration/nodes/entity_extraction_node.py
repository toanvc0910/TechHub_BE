from __future__ import annotations

import re

from app.orchestration.state.orchestrator_state import OrchestratorState, trace_step


async def entity_extraction_node(state: OrchestratorState) -> dict:
    text = state["user_input"]
    lowered = text.lower()
    entities: dict[str, str] = dict(state.get("entities", {}))

    level_map = {
        "beginner": ["co ban", "moi bat dau", "beginner"],
        "intermediate": ["trung cap", "intermediate"],
        "advanced": ["nang cao", "advanced"],
    }
    for level, terms in level_map.items():
        if any(term in lowered for term in terms):
            entities["level"] = level
            break

    topic_match = re.search(
        r"\b(python|java|javascript|react|sql|data science|machine learning|ai|frontend|backend)\b",
        lowered,
    )
    if topic_match:
        entities["topic"] = topic_match.group(1)

    if any(token in lowered for token in ["thong ke", "analytics", "report", "bao nhieu", "tong hop"]):
        entities["metric"] = "summary"
    if any(token in lowered for token in ["enrollment", "ghi danh"]):
        entities["metric"] = "enrollments"
    if any(token in lowered for token in ["tien do", "completion", "hoan thanh", "progress"]):
        entities["metric"] = "progress"
    if any(token in lowered for token in ["lesson", "bai hoc", "content type"]):
        entities["metric"] = "lesson_mix"
    if any(token in lowered for token in ["hom nay", "today"]):
        entities["time_range"] = "today"
    elif any(token in lowered for token in ["thang nay", "this month"]):
        entities["time_range"] = "this_month"

    uuid_match = re.search(
        r"\b([0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12})\b",
        lowered,
    )
    if uuid_match:
        entities["reference_id"] = uuid_match.group(1)

    trace_step(state, "entity_extract", "Extracted entities from user input.", entities=entities)
    return {
        "entities": entities,
        "execution_trace": list(state.get("execution_trace", [])),
    }
