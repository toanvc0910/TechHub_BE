from __future__ import annotations

import logging
import re
import unicodedata

from app.orchestration.model.model_selector_service import model_selector_service
from app.orchestration.router.intent_router import intent_router
from app.orchestration.state.orchestrator_state import OrchestratorState, trace_step
from app.services.runtime_policy_service import runtime_policy_service

logger = logging.getLogger(__name__)


async def intent_node(state: OrchestratorState) -> dict:
    result = await intent_router.classify(state)
    logger.info(
        "intent classified",
        extra={
            "event": "intent_classified",
            "userInput": state.get("user_input", ""),
            "intent": result.intent,
            "subIntent": result.sub_intent,
            "confidence": result.confidence,
            "matchedRule": result.matched_rule,
        },
    )
    normalized_input = _normalize_text(state.get("user_input", ""))
    if result.intent == "file_analysis" and normalized_input.strip() and not _is_file_reference(normalized_input):
        downgraded_intent = "knowledge" if _looks_like_knowledge_or_compare(normalized_input) else "conversation"
        result.intent = downgraded_intent
        result.sub_intent = "file-misroute-guard"
        result.confidence = min(result.confidence, 0.68)
        result.reason = (
            "Guard downgraded file_analysis because the user message does not reference any file or document."
        )
        result.matched_rule = "guard:file-analysis-misroute"
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


def _normalize_text(text: str) -> str:
    lowered = (text or "").lower().strip().replace("đ", "d")
    normalized = unicodedata.normalize("NFD", lowered)
    without_marks = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    return re.sub(r"\s+", " ", without_marks)


def _is_file_reference(text: str) -> bool:
    if not text:
        return False
    return bool(
        re.search(
            r"\b(file|tai lieu|document|pdf|docx|upload|tep|noi dung nay|tai lieu nay|file nay|tep nay)\b",
            text,
        )
    )


def _looks_like_knowledge_or_compare(text: str) -> bool:
    if not text:
        return False
    return bool(
        re.search(
            r"\b(la gi|ai la|giai thich|khai niem|tai sao|who is|what is|so sanh|compare|khac nhau|giong nhau)\b",
            text,
        )
    )
