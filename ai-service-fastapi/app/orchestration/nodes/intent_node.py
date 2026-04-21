from __future__ import annotations

from app.orchestration.model.model_selector_service import model_selector_service
from app.orchestration.router.intent_router import intent_router
from app.orchestration.state.orchestrator_state import OrchestratorState, trace_step
from app.services.runtime_policy_service import runtime_policy_service


async def intent_node(state: OrchestratorState) -> dict:
    result = await intent_router.classify(state)
    state["intent"] = result.intent
    state["sub_intent"] = result.sub_intent
    state["intent_confidence"] = result.confidence
    state["intent_reason"] = result.reason
    state["matched_rule"] = result.matched_rule
    state = await runtime_policy_service.enforce_state_access(state)
    selected_model, complexity = await model_selector_service.select_model(state)
    trace_step(
        state,
        "intent",
        result.reason or "",
        intent=result.intent,
        confidence=result.confidence,
        model=selected_model,
    )
    return {
        "intent": result.intent,
        "sub_intent": result.sub_intent,
        "intent_confidence": result.confidence,
        "intent_reason": result.reason,
        "matched_rule": result.matched_rule,
        "selected_model": selected_model,
        "complexity": complexity,
        "execution_trace": list(state.get("execution_trace", [])),
    }
