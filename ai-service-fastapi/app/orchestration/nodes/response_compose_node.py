from __future__ import annotations

from app.orchestration.state.orchestrator_state import OrchestratorState, trace_step


async def response_compose_node(state: OrchestratorState) -> dict:
    # HITL clarify mode: the question IS the response
    if state.get("hitl_clarify_active"):
        final_response = state.get("hitl_question", "")
        trace_step(state, "response_compose", "HITL clarify mode: returning clarification question as response.")
        return {
            "final_response": final_response,
            "execution_trace": list(state.get("execution_trace", [])),
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

    trace_step(state, "response_compose", "Composed final response and metadata.")
    return {
        "final_response": final_response,
        "execution_trace": list(state.get("execution_trace", [])),
    }
