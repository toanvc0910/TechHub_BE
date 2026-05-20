from __future__ import annotations

from typing import Any, TypedDict


class OrchestratorState(TypedDict, total=False):
    user_id: str
    session_id: str
    user_input: str
    mode: str
    request_context: Any
    file_contexts: list[dict[str, Any]]
    has_fresh_file_context: bool
    conversation_context: dict[str, Any]
    user_memory: dict[str, Any]
    policy_context: dict[str, Any]
    intent: str
    sub_intent: str | None
    intent_confidence: float
    intent_reason: str | None
    matched_rule: str | None
    entities: dict[str, Any]
    selected_model: str | None
    complexity: str
    query_result: dict[str, Any] | None
    citations: list[dict[str, Any]]
    final_response: str
    chart_spec: dict[str, Any] | None
    errors: list[str]
    execution_trace: list[dict[str, Any]]
    pipeline: str
    node_timings: dict[str, float]
    token_usage: dict[str, Any]
    request_id: str | None
    # Personalization fields
    skill_profile: dict[str, float]
    user_course_history: list[dict[str, Any]]
    user_ratings: list[dict[str, Any]]
    # HITL clarify flow fields
    hitl_clarify_active: bool
    hitl_question: str
    hitl_options: list[str]
    hitl_round: int
    # Streaming: set by agents when they have already emitted response chunks
    # via the request-scoped emitter; chat_service uses this to skip the
    # post-hoc fake-chunk loop.
    response_streamed: bool
    # Step 09 quality flags - populated by response_compose_node from agent
    # outputs so callers can see whether the final answer is grounded in real
    # data, which sources contributed, and whether any fallback was used.
    grounded: bool
    data_sources: list[str]
    fallback_used: bool
    missing_data: list[str]
    grounding_confidence: float


def make_initial_state(
    *,
    user_id: str,
    session_id: str,
    user_input: str,
    mode: str,
    request_context: Any = None,
    request_id: str | None = None,
) -> OrchestratorState:
    return OrchestratorState(
        user_id=user_id,
        session_id=session_id,
        user_input=user_input,
        mode=mode,
        request_context=request_context,
        file_contexts=[],
        has_fresh_file_context=False,
        conversation_context={},
        user_memory={},
        policy_context={},
        intent="conversation",
        sub_intent=None,
        intent_confidence=0.0,
        intent_reason=None,
        matched_rule=None,
        entities={},
        selected_model=None,
        complexity="LOW",
        query_result=None,
        citations=[],
        final_response="",
        chart_spec=None,
        errors=[],
        execution_trace=[],
        pipeline="orchestration",
        node_timings={},
        token_usage={},
        request_id=request_id,
        skill_profile={},
        user_course_history=[],
        user_ratings=[],
        hitl_clarify_active=False,
        hitl_question="",
        hitl_options=[],
        hitl_round=0,
        grounded=False,
        data_sources=[],
        fallback_used=False,
        missing_data=[],
        grounding_confidence=0.0,
    )


def trace_step(state: OrchestratorState, step: str, detail: str, **extra: Any) -> None:
    item: dict[str, Any] = {"step": step, "detail": detail}
    if extra:
        item.update(extra)
    state.setdefault("execution_trace", []).append(item)
