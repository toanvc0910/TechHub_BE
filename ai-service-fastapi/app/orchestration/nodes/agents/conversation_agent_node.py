from __future__ import annotations

from typing import Any

from app.core.config import get_settings
from app.orchestration.state.orchestrator_state import OrchestratorState, trace_step
from app.services.llm_gateway import switchable_ai_gateway


class ConversationAgentNode:
    def supports(self, intent: str, sub_intent: str | None = None) -> bool:
        del sub_intent
        return intent in {"conversation", "clarify"}

    async def execute(self, state: OrchestratorState) -> dict[str, Any]:
        settings = get_settings()
        intent = state.get("intent", "conversation")

        # ── Clarify mode: generate dynamic question + quick reply options via LLM ──
        if intent == "clarify":
            return await self._generate_clarification(state, settings)

        # ── Normal conversation ──
        response = await switchable_ai_gateway.stream_and_emit(
            prompt=state["user_input"],
            system_prompt=settings.system_prompt,
            model=state.get("selected_model"),
        )
        trace_step(state, "conversation_agent", "Generated conversational response.")
        return {
            "final_response": response,
            "response_streamed": True,
            "execution_trace": list(state.get("execution_trace", [])),
        }

    async def _generate_clarification(self, state: OrchestratorState, settings: Any) -> dict[str, Any]:
        """Generate a clarification question + 3 quick-reply options via LLM.

        This follows the plan: hitl_gate triggers → conversation_agent(clarify)
        generates dynamic question instead of hard-coded templates.
        """
        sub_intent = state.get("sub_intent") or "unknown"
        user_input = state["user_input"]

        prompt = (
            "Nguoi dung hoi mot cau chua du ro de he thong xu ly.\n"
            f"Cau hoi goc: \"{user_input}\"\n"
            f"Ly do can lam ro: {sub_intent}\n\n"
            "Hay tao:\n"
            "1. Mot cau hoi ngan (tieng Viet) de hoi lai nguoi dung cho ro hon.\n"
            "2. Dung 3 lua chon ngan gon de nguoi dung click chon nhanh.\n\n"
            "Tra ve JSON only: {\"question\": \"...\", \"options\": [\"...\", \"...\", \"...\"]}"
        )

        fallback = {
            "question": "Ban co the noi ro hon chu de hoac muc tieu hoc tap de toi goi y chinh xac hon khong?",
            "options": ["Toi muon hoc lap trinh", "Toi can tu van khoa hoc", "Toi muon xem thong ke"],
        }

        result = await switchable_ai_gateway.generate_structured_json(
            prompt=prompt,
            fallback_payload=fallback,
        )

        question = str(result.get("question") or fallback["question"])
        options = result.get("options")
        if not isinstance(options, list) or len(options) < 2:
            options = fallback["options"]
        options = [str(o) for o in options[:4]]

        trace_step(state, "conversation_agent", f"Generated clarification (reason={sub_intent}).",
                   question=question, options=options)
        return {
            "final_response": question,
            "hitl_question": question,
            "hitl_options": options,
            "execution_trace": list(state.get("execution_trace", [])),
        }


conversation_agent_node = ConversationAgentNode()
