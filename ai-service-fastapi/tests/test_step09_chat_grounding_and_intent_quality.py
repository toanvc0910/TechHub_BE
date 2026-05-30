"""
Step 09 - Chat Grounding And Intent Quality E2E smoke test.

Three layers verified end-to-end without LLM/network dependency:

  Intent (30+ Vietnamese / mixed prompts):
    each prompt is fed through `intent_router.classify()` and we assert the
    expected intent. Prompts are designed to exercise tier-0 (file/active-
    analysis) and tier-1 (deterministic regex) paths so the test stays fast
    and reproducible.

  Entity extraction:
    chart_type, file_scope, metric, scope, time_range, level, topic, uuid
    pulled from real-shaped Vietnamese sentences.

  Quality flags / grounding contract:
    `compute_quality_flags()` is exercised for every agent path with
    synthetic state fixtures so callers see `grounded`, `data_sources`,
    `fallback_used`, `missing_data` for each intent.

  HITL gate:
    AI_HITL_ENABLED=true with a low-confidence state -> hitl_clarify_active.
    With high confidence and a non-cold-start user -> proceeds normally.

Run from the service root:

    python -m tests.test_step09_chat_grounding_and_intent_quality
"""

from __future__ import annotations

import asyncio
import sys
import traceback
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from unittest.mock import patch

# Force UTF-8 stdout so the Windows cp1252 console can print Vietnamese.
try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")  # type: ignore[attr-defined]
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")  # type: ignore[attr-defined]
except Exception:
    pass

SERVICE_ROOT = Path(__file__).resolve().parent.parent
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

from app.orchestration.nodes.entity_extraction_node import entity_extraction_node  # noqa: E402
from app.orchestration.nodes.hitl_gate_node import hitl_gate_node  # noqa: E402
from app.orchestration.nodes.response_compose_node import compute_quality_flags  # noqa: E402
from app.orchestration.router.intent_router import intent_router  # noqa: E402
from app.orchestration.state.orchestrator_state import make_initial_state  # noqa: E402


@dataclass
class CaseResult:
    name: str
    passed: bool
    detail: str = ""


# ---------------------------------------------------------------------------
# Intent classification: 30+ deterministic prompts.
# ---------------------------------------------------------------------------


INTENT_PROMPTS: list[tuple[str, str]] = [
    # ----- conversation (greeting/chit-chat) -----
    ("Xin chào TechHub AI", "conversation"),
    ("Hello, bạn có thể giúp gì không?", "conversation"),
    ("Cảm ơn bạn nhiều", "conversation"),
    ("Hi, mình mới tham gia nền tảng", "conversation"),
    ("Chào bạn, hello!", "conversation"),
    ("Ten toi la gi?", "conversation"),
    ("What is my name?", "conversation"),
    # ----- recommendation -----
    ("Gợi ý khóa học phù hợp cho người mới bắt đầu", "recommendation"),
    ("Tôi nên học khóa nào tiếp theo?", "recommendation"),
    ("Khóa nào phù hợp với người đang học Python?", "recommendation"),
    ("Đề xuất khóa học cho frontend developer", "recommendation"),
    ("Recommend a backend course for me please", "recommendation"),
    # ----- knowledge -----
    ("Giải thích khái niệm closure trong JavaScript", "knowledge"),
    ("Tại sao Python phổ biến trong machine learning?", "knowledge"),
    ("Closure là gì và khi nào nên dùng?", "knowledge"),
    ("Who is the creator of Python?", "knowledge"),
    ("How does async work in JavaScript?", "knowledge"),
    # ----- data_query -----
    ("Tổng hợp số liệu enrollment theo level", "data_query"),
    ("Có bao nhiêu học viên đang học khóa React?", "data_query"),
    ("Truy vấn analytics theo tháng", "data_query"),
    ("Thống kê tổng số khóa học đã xuất bản", "data_query"),
    ("Show me the report of completion rate", "data_query"),
    # ----- visualization -----
    ("Vẽ biểu đồ enrollment theo level", "visualization"),
    ("Tạo chart cho doanh thu tháng này", "visualization"),
    ("Show me a dashboard of learner progress", "visualization"),
    ("Vẽ đồ thị tiến độ học", "visualization"),
    ("Plot enrollment count per course", "visualization"),
    # ----- file_analysis -----
    ("Phân tích file PDF này giúp tôi", "file_analysis"),
    ("Tóm tắt tài liệu vừa upload", "file_analysis"),
    ("Đọc nội dung file docx và trả lời câu hỏi", "file_analysis"),
    ("Document này nói về điều gì?", "file_analysis"),
    ("Tài liệu vừa upload có gì quan trọng?", "file_analysis"),
]


async def case_intent_router_all_prompts() -> list[CaseResult]:
    results: list[CaseResult] = []
    for prompt, expected in INTENT_PROMPTS:
        state = make_initial_state(
            user_id="user-1",
            session_id="sess-1",
            user_input=prompt,
            mode="AUTO",
        )
        outcome = await intent_router.classify(state)
        passed = outcome.intent == expected
        results.append(
            CaseResult(
                name=f"I[{expected:14s}] -> {prompt[:48]}",
                passed=passed,
                detail=(
                    f"got intent={outcome.intent} confidence={outcome.confidence:.2f} "
                    f"rule={outcome.matched_rule}"
                ),
            )
        )
    return results


async def case_intent_router_file_fresh_context() -> CaseResult:
    name = "I-tier0 - has_fresh_file_context with empty utterance -> file_analysis"
    state = make_initial_state(
        user_id="user-1",
        session_id="sess-1",
        user_input="",
        mode="AUTO",
    )
    state["has_fresh_file_context"] = True
    outcome = await intent_router.classify(state)
    if outcome.intent != "file_analysis":
        return CaseResult(name, False, f"got {outcome.intent}")
    if outcome.matched_rule != "tier0:file-context":
        return CaseResult(name, False, f"expected tier0:file-context, got {outcome.matched_rule}")
    return CaseResult(name, True, f"confidence={outcome.confidence:.2f}")


async def case_intent_router_active_analysis_refine_chart() -> CaseResult:
    name = "I-tier0 - active analysis + chart-swap utterance -> visualization"
    state = make_initial_state(
        user_id="user-1",
        session_id="sess-1",
        user_input="đổi sang biểu đồ đường",
        mode="AUTO",
        request_context={"activeAnalysis": {"metric": "learner_course_progress", "chartType": "bar"}},
    )
    outcome = await intent_router.classify(state)
    if outcome.intent != "visualization":
        return CaseResult(name, False, f"got {outcome.intent}")
    if "active-analysis-refine" not in (outcome.matched_rule or ""):
        return CaseResult(name, False, f"expected tier0 active-analysis-refine, got {outcome.matched_rule}")
    return CaseResult(name, True, f"rule={outcome.matched_rule}")


async def case_intent_router_active_analysis_refine_filter() -> CaseResult:
    name = "I-tier0 - active analysis + filter utterance -> data_query"
    state = make_initial_state(
        user_id="user-1",
        session_id="sess-1",
        user_input="chỉ lấy beginner",
        mode="AUTO",
        request_context={"activeAnalysis": {"metric": "active_enrollments_by_level"}},
    )
    outcome = await intent_router.classify(state)
    if outcome.intent != "data_query":
        return CaseResult(name, False, f"got {outcome.intent}")
    if "active-analysis-refine" not in (outcome.matched_rule or ""):
        return CaseResult(name, False, f"expected tier0 active-analysis-refine, got {outcome.matched_rule}")
    return CaseResult(name, True, f"rule={outcome.matched_rule}")


# ---------------------------------------------------------------------------
# Entity extraction
# ---------------------------------------------------------------------------


async def _extract_entities(text: str, *, base_state: dict[str, Any] | None = None) -> dict[str, Any]:
    state = make_initial_state(user_id="u", session_id="s", user_input=text, mode="AUTO")
    if base_state:
        state.update(base_state)
    updates = await entity_extraction_node(state)
    return updates["entities"]


async def case_entity_chart_type_line() -> CaseResult:
    name = "E1 - chart_type line"
    entities = await _extract_entities("đổi sang biểu đồ đường cho dễ nhìn")
    if entities.get("chart_type") != "line":
        return CaseResult(name, False, f"got {entities}")
    return CaseResult(name, True, str(entities))


async def case_entity_chart_type_pie_not_trong() -> CaseResult:
    name = "E2 - chart_type does NOT match 'trong tháng này' (regression guard)"
    entities = await _extract_entities("doanh thu theo khóa trong tháng này")
    if entities.get("chart_type") == "pie":
        return CaseResult(name, False, f"false positive pie from 'trong': {entities}")
    return CaseResult(name, True, f"chart_type={entities.get('chart_type')}")


async def case_entity_chart_type_pie() -> CaseResult:
    name = "E3 - chart_type pie when explicitly requested"
    entities = await _extract_entities("vẽ biểu đồ tròn cho ti le hoan thanh")
    if entities.get("chart_type") != "pie":
        return CaseResult(name, False, f"got {entities}")
    return CaseResult(name, True, str(entities))


async def case_entity_metric_progress_personal() -> CaseResult:
    name = "E4 - metric=progress, scope=personal"
    entities = await _extract_entities("tiến độ học của tôi theo từng khóa")
    if entities.get("metric") != "progress":
        return CaseResult(name, False, f"metric={entities.get('metric')}")
    if entities.get("scope") != "personal":
        return CaseResult(name, False, f"scope={entities.get('scope')}")
    return CaseResult(name, True, str(entities))


async def case_entity_time_range_this_month() -> CaseResult:
    name = "E5 - time_range=this_month"
    entities = await _extract_entities("thống kê doanh thu tháng này")
    if entities.get("time_range") != "this_month":
        return CaseResult(name, False, f"got {entities.get('time_range')}")
    return CaseResult(name, True, str(entities))


async def case_entity_file_scope_active() -> CaseResult:
    name = "E6 - file_scope=active for 'tài liệu này'"
    entities = await _extract_entities("tóm tắt tài liệu này giúp tôi")
    if entities.get("file_scope") != "active":
        return CaseResult(name, False, f"got {entities.get('file_scope')}")
    return CaseResult(name, True, str(entities))


async def case_entity_uuid_reference() -> CaseResult:
    name = "E7 - uuid reference_id extracted"
    uid = "ad0ac9c6-a60d-4895-8310-49b7b38e5009"
    entities = await _extract_entities(f"phân tích lesson {uid}")
    if entities.get("reference_id") != uid:
        return CaseResult(name, False, f"got {entities.get('reference_id')}")
    return CaseResult(name, True, str(entities))


# ---------------------------------------------------------------------------
# Quality flags / grounding contract
# ---------------------------------------------------------------------------


def _state(intent: str, **extras: Any) -> dict[str, Any]:
    s = make_initial_state(user_id="u", session_id="s", user_input="x", mode="AUTO")
    s["intent"] = intent
    s["intent_confidence"] = 0.85
    s.update(extras)
    return s


def case_grounding_data_query_with_rows() -> CaseResult:
    name = "G1 - data_query with rows -> grounded=True, postgres in sources"
    state = _state(
        "data_query",
        query_result={"rows": [{"label": "A", "value": 1}], "sql": "SELECT ...", "executionMode": "semantic_template"},
    )
    flags = compute_quality_flags(state, hitl_active=False)
    if not flags["grounded"]:
        return CaseResult(name, False, f"flags={flags}")
    if "postgres" not in flags["data_sources"]:
        return CaseResult(name, False, f"postgres missing: {flags}")
    if flags["fallback_used"]:
        return CaseResult(name, False, f"should not be fallback: {flags}")
    return CaseResult(name, True, str(flags))


def case_grounding_data_query_empty_rows() -> CaseResult:
    name = "G2 - data_query with 0 rows -> grounded=False, missing query_rows"
    state = _state(
        "data_query",
        query_result={"rows": [], "sql": "SELECT ...", "executionMode": "semantic_template"},
    )
    flags = compute_quality_flags(state, hitl_active=False)
    if flags["grounded"]:
        return CaseResult(name, False, f"should not be grounded: {flags}")
    if "query_rows" not in flags["missing_data"]:
        return CaseResult(name, False, f"missing_data should include query_rows: {flags}")
    return CaseResult(name, True, str(flags))


def case_grounding_data_query_fallback_sql() -> CaseResult:
    name = "G3 - data_query executionMode=deterministic_fallback -> fallback_used=True"
    state = _state(
        "data_query",
        query_result={"rows": [{"label": "A", "value": 1}], "sql": "SELECT ...", "executionMode": "deterministic_fallback"},
    )
    flags = compute_quality_flags(state, hitl_active=False)
    if not flags["fallback_used"]:
        return CaseResult(name, False, f"fallback_used should be True: {flags}")
    if "fallback_sql" not in flags["data_sources"]:
        return CaseResult(name, False, f"fallback_sql missing: {flags}")
    return CaseResult(name, True, str(flags))


def case_grounding_visualization_with_chart() -> CaseResult:
    name = "G4 - visualization with chart_spec -> chart in data_sources"
    state = _state(
        "visualization",
        query_result={"rows": [{"label": "A", "value": 1}], "executionMode": "semantic_template"},
        chart_spec={"type": "bar"},
    )
    flags = compute_quality_flags(state, hitl_active=False)
    if "chart" not in flags["data_sources"]:
        return CaseResult(name, False, f"chart missing: {flags}")
    if not flags["grounded"]:
        return CaseResult(name, False, f"flags={flags}")
    return CaseResult(name, True, str(flags))


def case_grounding_knowledge_with_citations() -> CaseResult:
    name = "G5 - knowledge with course citations -> grounded=True, qdrant"
    state = _state(
        "knowledge",
        citations=[{"kind": "course", "courseId": "abc", "title": "React"}],
    )
    flags = compute_quality_flags(state, hitl_active=False)
    if not flags["grounded"]:
        return CaseResult(name, False, f"flags={flags}")
    if "qdrant" not in flags["data_sources"]:
        return CaseResult(name, False, f"qdrant missing: {flags}")
    return CaseResult(name, True, str(flags))


def case_grounding_knowledge_no_citations() -> CaseResult:
    name = "G6 - knowledge with no citations -> grounded=False, missing rag_documents"
    state = _state("knowledge", citations=[])
    flags = compute_quality_flags(state, hitl_active=False)
    if flags["grounded"]:
        return CaseResult(name, False, f"should not be grounded: {flags}")
    if "rag_documents" not in flags["missing_data"]:
        return CaseResult(name, False, f"missing rag_documents not flagged: {flags}")
    return CaseResult(name, True, str(flags))


def case_grounding_file_with_citation() -> CaseResult:
    name = "G7 - file_analysis with file citation -> grounded + file source"
    state = _state(
        "file_analysis",
        citations=[{"kind": "user_file", "fileId": "f1"}],
    )
    flags = compute_quality_flags(state, hitl_active=False)
    if not flags["grounded"]:
        return CaseResult(name, False, f"flags={flags}")
    if "file" not in flags["data_sources"]:
        return CaseResult(name, False, f"file missing: {flags}")
    return CaseResult(name, True, str(flags))


def case_grounding_file_without_indexed_file() -> CaseResult:
    name = "G8 - file_analysis with no citation + no fresh context -> not grounded, missing file_chunks"
    state = _state("file_analysis", citations=[], has_fresh_file_context=False)
    flags = compute_quality_flags(state, hitl_active=False)
    if flags["grounded"]:
        return CaseResult(name, False, f"should not be grounded: {flags}")
    if "file_chunks" not in flags["missing_data"]:
        return CaseResult(name, False, f"missing file_chunks not flagged: {flags}")
    return CaseResult(name, True, str(flags))


def case_grounding_recommendation_with_citations() -> CaseResult:
    name = "G9 - recommendation with course citations -> grounded, postgres+qdrant"
    state = _state(
        "recommendation",
        citations=[{"kind": "course", "courseId": "c1"}, {"kind": "course", "courseId": "c2"}],
    )
    flags = compute_quality_flags(state, hitl_active=False)
    if not flags["grounded"]:
        return CaseResult(name, False, f"flags={flags}")
    if "postgres" not in flags["data_sources"] or "qdrant" not in flags["data_sources"]:
        return CaseResult(name, False, f"sources missing: {flags}")
    return CaseResult(name, True, str(flags))


def case_grounding_recommendation_no_citations() -> CaseResult:
    name = "G10 - recommendation with no citations -> not grounded, missing course_candidates"
    state = _state("recommendation", citations=[])
    flags = compute_quality_flags(state, hitl_active=False)
    if flags["grounded"]:
        return CaseResult(name, False, f"should not be grounded: {flags}")
    if "course_candidates" not in flags["missing_data"]:
        return CaseResult(name, False, f"missing course_candidates not flagged: {flags}")
    return CaseResult(name, True, str(flags))


def case_grounding_conversation() -> CaseResult:
    name = "G11 - conversation -> grounded=True (no data needed)"
    state = _state("conversation")
    flags = compute_quality_flags(state, hitl_active=False)
    if not flags["grounded"]:
        return CaseResult(name, False, f"conversation should be grounded: {flags}")
    if "llm" not in flags["data_sources"]:
        return CaseResult(name, False, f"llm missing: {flags}")
    return CaseResult(name, True, str(flags))


def case_grounding_hitl_active() -> CaseResult:
    name = "G12 - hitl_active -> missing awaiting_user_clarification, grounded=True"
    state = _state("clarify")
    flags = compute_quality_flags(state, hitl_active=True)
    if not flags["grounded"]:
        return CaseResult(name, False, f"flags={flags}")
    if "awaiting_user_clarification" not in flags["missing_data"]:
        return CaseResult(name, False, f"missing awaiting_user_clarification: {flags}")
    return CaseResult(name, True, str(flags))


# ---------------------------------------------------------------------------
# HITL gate (uses real DB via catalog_service for cold-start path)
# ---------------------------------------------------------------------------


async def case_hitl_low_confidence_triggers_clarify() -> CaseResult:
    name = "H1 - intent_confidence < threshold -> hitl_clarify_active=True"
    state = make_initial_state(
        user_id="user-rich",
        session_id="s",
        user_input="abc",
        mode="AUTO",
    )
    state["intent"] = "knowledge"
    state["intent_confidence"] = 0.1  # below default threshold
    updates = await hitl_gate_node(state)
    if not updates.get("hitl_clarify_active"):
        return CaseResult(name, False, f"expected clarify active, got {updates}")
    if updates.get("intent") != "clarify":
        return CaseResult(name, False, f"intent should become 'clarify', got {updates.get('intent')}")
    return CaseResult(name, True, f"sub_intent={updates.get('sub_intent')}")


async def case_hitl_high_confidence_passes_through() -> CaseResult:
    name = "H2 - high confidence + non-recommendation -> no clarify"
    state = make_initial_state(
        user_id="eb2159ec-5992-4302-af92-e6c9b8b9b1a1",  # USER_RICH from earlier steps
        session_id="s",
        user_input="vẽ biểu đồ enrollment theo level",
        mode="AUTO",
    )
    state["intent"] = "visualization"
    state["intent_confidence"] = 0.9
    updates = await hitl_gate_node(state)
    if updates.get("hitl_clarify_active"):
        return CaseResult(name, False, f"should not clarify: {updates}")
    return CaseResult(name, True, "passed through cleanly")


# ---------------------------------------------------------------------------
# Runner
# ---------------------------------------------------------------------------


async def main() -> int:
    results: list[CaseResult] = []

    # Tier-0 cases
    for func in [
        case_intent_router_file_fresh_context,
        case_intent_router_active_analysis_refine_chart,
        case_intent_router_active_analysis_refine_filter,
    ]:
        print(f"\n--- {func.__name__}")
        try:
            r = await func()
        except Exception as exc:  # noqa: BLE001
            tb = "".join(traceback.format_exception_only(type(exc), exc)).strip()
            r = CaseResult(func.__name__, False, f"EXC: {tb}")
        print(("PASS " if r.passed else "FAIL ") + r.name + " :: " + r.detail)
        results.append(r)

    # 30 prompt intent classification
    print("\n--- Intent prompts (30):")
    intent_results = await case_intent_router_all_prompts()
    for r in intent_results:
        print(("PASS " if r.passed else "FAIL ") + r.name + " :: " + r.detail)
    results.extend(intent_results)

    # Entity extraction
    for func in [
        case_entity_chart_type_line,
        case_entity_chart_type_pie_not_trong,
        case_entity_chart_type_pie,
        case_entity_metric_progress_personal,
        case_entity_time_range_this_month,
        case_entity_file_scope_active,
        case_entity_uuid_reference,
    ]:
        print(f"\n--- {func.__name__}")
        try:
            r = await func()
        except Exception as exc:  # noqa: BLE001
            tb = "".join(traceback.format_exception_only(type(exc), exc)).strip()
            r = CaseResult(func.__name__, False, f"EXC: {tb}")
        print(("PASS " if r.passed else "FAIL ") + r.name + " :: " + r.detail)
        results.append(r)

    # Grounding flags (sync)
    for func in [
        case_grounding_data_query_with_rows,
        case_grounding_data_query_empty_rows,
        case_grounding_data_query_fallback_sql,
        case_grounding_visualization_with_chart,
        case_grounding_knowledge_with_citations,
        case_grounding_knowledge_no_citations,
        case_grounding_file_with_citation,
        case_grounding_file_without_indexed_file,
        case_grounding_recommendation_with_citations,
        case_grounding_recommendation_no_citations,
        case_grounding_conversation,
        case_grounding_hitl_active,
    ]:
        print(f"\n--- {func.__name__}")
        try:
            r = func()
        except Exception as exc:  # noqa: BLE001
            tb = "".join(traceback.format_exception_only(type(exc), exc)).strip()
            r = CaseResult(func.__name__, False, f"EXC: {tb}")
        print(("PASS " if r.passed else "FAIL ") + r.name + " :: " + r.detail)
        results.append(r)

    # HITL gate
    for func in [
        case_hitl_low_confidence_triggers_clarify,
        case_hitl_high_confidence_passes_through,
    ]:
        print(f"\n--- {func.__name__}")
        try:
            r = await func()
        except Exception as exc:  # noqa: BLE001
            tb = "".join(traceback.format_exception_only(type(exc), exc)).strip()
            r = CaseResult(func.__name__, False, f"EXC: {tb}")
        print(("PASS " if r.passed else "FAIL ") + r.name + " :: " + r.detail)
        results.append(r)

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
