from __future__ import annotations

from app.core.config import get_settings
from app.orchestration.state.orchestrator_state import OrchestratorState, trace_step
from app.services.catalog_service import catalog_service


async def hitl_gate_node(state: OrchestratorState) -> dict:
    """Decide whether to ask the user for clarification before proceeding.

    When triggered, sets hitl_clarify_active=True and intent='clarify'.
    The graph then routes to entity_extract -> agent_dispatch, which resolves
    to conversation_agent (supports intent='clarify'). The agent generates
    a dynamic clarification question via LLM instead of hard-coded templates.

    Reads AI_HITL_ENABLED and AI_HITL_CONFIDENCE_THRESHOLD from config.
    Hard rule: never trigger if hitl_round >= 1 (max 1 round).
    """
    settings = get_settings()

    if not settings.hitl_enabled:
        trace_step(state, "hitl_gate", "HITL disabled by config (AI_HITL_ENABLED=false).")
        return {"hitl_clarify_active": False, "execution_trace": list(state.get("execution_trace", []))}

    hitl_round = state.get("hitl_round", 0)
    if hitl_round >= 1:
        trace_step(state, "hitl_gate", "Skipped — already used 1 clarification round.")
        return {"hitl_clarify_active": False, "execution_trace": list(state.get("execution_trace", []))}

    intent = state.get("intent", "conversation")
    confidence = state.get("intent_confidence", 0.0)
    confidence_threshold = settings.hitl_confidence_threshold
    reason: str | None = None

    # Condition 1: Confidence below config threshold
    if confidence < confidence_threshold:
        reason = "low_confidence"

    # Condition 2: Cold start — recommendation but no enrollment
    if reason is None and intent == "recommendation":
        try:
            course_history = await catalog_service.fetch_user_course_history(state["user_id"])
        except Exception:
            course_history = []
        if not course_history:
            reason = "cold_start"

    # Condition 3: Ambiguous entity reference with no context
    if reason is None:
        user_input_lower = state["user_input"].lower()
        ambiguous_refs = ["khoa do", "cai do", "bai do", "mon do"]
        context_entities = state.get("conversation_context", {}).get("entities", {})
        if any(ref in user_input_lower for ref in ambiguous_refs) and not context_entities.get("topic"):
            reason = "ambiguous_entity"

    if reason is None:
        trace_step(state, "hitl_gate", "No clarification needed — proceeding.",
                   confidence=confidence, threshold=confidence_threshold, intent=intent)
        return {"hitl_clarify_active": False, "execution_trace": list(state.get("execution_trace", []))}

    # Trigger clarify: override intent to 'clarify' so agent_dispatch routes
    # to conversation_agent which handles intent='clarify'.
    # The agent will generate a dynamic question/options via LLM.
    trace_step(state, "hitl_gate", f"HITL clarify triggered: {reason}. Routing to conversation_agent(clarify).",
               reason=reason, confidence=confidence, threshold=confidence_threshold)
    return {
        "hitl_clarify_active": True,
        "intent": "clarify",
        "sub_intent": reason,
        "execution_trace": list(state.get("execution_trace", [])),
    }
