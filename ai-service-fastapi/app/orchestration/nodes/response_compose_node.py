from __future__ import annotations

from typing import Any

from app.orchestration.state.orchestrator_state import OrchestratorState, trace_step


async def response_compose_node(state: OrchestratorState) -> dict:
    # HITL clarify mode: the question IS the response
    if state.get("hitl_clarify_active"):
        final_response = state.get("hitl_question", "")
        trace_step(state, "response_compose", "HITL clarify mode: returning clarification question as response.")
        flags = compute_quality_flags(state, hitl_active=True)
        return {
            "final_response": final_response,
            "execution_trace": list(state.get("execution_trace", [])),
            **flags,
        }

    final_response = state.get("final_response", "")
    chart_spec = state.get("chart_spec")
    query_result = state.get("query_result")

    if chart_spec and query_result is not None:
        final_response = (
            final_response
            or "Toi da chuan bi ket qua du lieu va chart spec de frontend co the render bieu do."
        )
    if not final_response:
        final_response = "Toi da tiep nhan yeu cau nhung chua tong hop duoc phan hoi phu hop."

    flags = compute_quality_flags(state, hitl_active=False)
    trace_step(
        state,
        "response_compose",
        "Composed final response and metadata.",
        grounded=flags["grounded"],
        dataSources=flags["data_sources"],
        fallbackUsed=flags["fallback_used"],
        missingData=flags["missing_data"],
    )
    return {
        "final_response": final_response,
        "execution_trace": list(state.get("execution_trace", [])),
        **flags,
    }


def compute_quality_flags(state: OrchestratorState, *, hitl_active: bool) -> dict[str, Any]:
    """Compute Step 09 grounding/quality flags from agent-populated state.

    Rules per intent:

      - data_query / visualization: grounded if query_result.rows non-empty;
        dataSource includes 'postgres'; chart adds 'chart'; lexical fallback
        from analytics_service surfaces as fallback_used.
      - knowledge / RAG: grounded iff at least one citation exists.
      - file_analysis: grounded iff at least one file citation exists OR
        a fresh file context is attached.
      - recommendation: grounded iff citations or final_response is not empty
        AND not a generic fallback string.
      - conversation / clarify: grounded=True (no data needed); HITL clarify
        path is explicitly flagged.

    Returns dict with keys: grounded, data_sources, fallback_used, missing_data,
    grounding_confidence.
    """
    if hitl_active:
        return {
            "grounded": True,
            "data_sources": ["llm"],
            "fallback_used": False,
            "missing_data": ["awaiting_user_clarification"],
            "grounding_confidence": float(state.get("intent_confidence") or 0.0),
        }

    intent = str(state.get("intent") or "conversation")
    citations = state.get("citations") or []
    query_result = state.get("query_result") or {}
    chart_spec = state.get("chart_spec")
    fresh_file = bool(state.get("has_fresh_file_context"))
    file_contexts = state.get("file_contexts") or []
    fallback_used = False
    data_sources: list[str] = []
    missing: list[str] = []
    grounded = False
    confidence = float(state.get("intent_confidence") or 0.0)

    if intent in {"data_query", "visualization"}:
        rows = query_result.get("rows") if isinstance(query_result, dict) else None
        row_count = len(rows) if isinstance(rows, list) else int(query_result.get("rowCount") or 0)
        execution_mode = str((query_result or {}).get("executionMode") or "").lower()
        grounded = row_count > 0
        if isinstance(query_result, dict) and (query_result.get("sql") or rows is not None):
            data_sources.append("postgres")
        if chart_spec:
            data_sources.append("chart")
        if execution_mode in {"deterministic_fallback", "llm_planner"} and execution_mode != "semantic_template":
            # Anything other than the metric-registry semantic_template
            # counts as a softer grounding signal but still real data.
            pass
        if execution_mode == "deterministic_fallback":
            fallback_used = True
            data_sources.append("fallback_sql")
        if row_count == 0:
            missing.append("query_rows")
            data_sources.append("llm")
    elif intent == "file_analysis":
        file_citations = [c for c in citations if isinstance(c, dict) and str(c.get("kind") or "") in {"file", "user_file", "session_file"}]
        grounded = bool(file_citations) or (fresh_file and bool(state.get("final_response")))
        if file_citations:
            data_sources.append("file")
        if fresh_file:
            data_sources.append("session_file")
        if not file_citations and not fresh_file:
            missing.append("file_chunks")
            data_sources.append("llm")
    elif intent == "knowledge":
        course_citations = [
            c for c in citations
            if isinstance(c, dict) and str(c.get("kind") or "").lower() in {"course", "lesson"}
        ]
        grounded = bool(course_citations)
        if course_citations:
            data_sources.append("qdrant")
        else:
            missing.append("rag_documents")
            data_sources.append("llm")
    elif intent == "recommendation":
        course_citations = [
            c for c in citations
            if isinstance(c, dict) and str(c.get("kind") or "").lower() == "course"
        ]
        grounded = bool(course_citations)
        if course_citations:
            data_sources.extend(["postgres", "qdrant"])
        else:
            missing.append("course_candidates")
            data_sources.append("llm")
    elif intent in {"conversation", "clarify"}:
        # Conversation/clarify legitimately does not need external data.
        grounded = True
        data_sources.append("llm")
    else:
        # Unknown intent: be conservative — count as not grounded.
        missing.append(f"intent:{intent}")
        data_sources.append("llm")

    if file_contexts and "session_file" not in data_sources and intent != "file_analysis":
        # File was attached but the agent did not consume it; flag for audit.
        data_sources.append("session_file")

    # Deduplicate while preserving order.
    seen: set[str] = set()
    deduped_sources: list[str] = []
    for source in data_sources:
        if source and source not in seen:
            seen.add(source)
            deduped_sources.append(source)

    return {
        "grounded": grounded,
        "data_sources": deduped_sources,
        "fallback_used": fallback_used,
        "missing_data": missing,
        "grounding_confidence": confidence,
    }
