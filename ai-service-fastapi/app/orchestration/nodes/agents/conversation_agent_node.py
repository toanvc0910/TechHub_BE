from __future__ import annotations

from typing import Any

from app.core.config import get_settings
from app.orchestration.memory.personal_memory import (
    asks_assistant_favorite_color,
    asks_user_favorite_color,
    extract_favorite_color,
    find_latest_favorite_color,
)
from app.orchestration.state.orchestrator_state import OrchestratorState, trace_step
from app.services.llm_gateway import switchable_ai_gateway
from app.services.request_instructions import append_request_instructions


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
        deterministic_response = self._answer_personal_memory_question(state)
        if deterministic_response:
            trace_step(state, "conversation_agent", "Answered deterministic personal-memory conversation.")
            return {
                "final_response": deterministic_response,
                "response_streamed": False,
                "execution_trace": list(state.get("execution_trace", [])),
            }

        response = await switchable_ai_gateway.stream_and_emit(
            prompt=append_request_instructions(self._build_conversation_prompt(state), state.get("request_context")),
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

        prompt = append_request_instructions((
            "Nguoi dung hoi mot cau chua du ro de he thong xu ly.\n"
            f"Cau hoi goc: \"{user_input}\"\n"
            f"Ly do can lam ro: {sub_intent}\n\n"
            "Hay tao:\n"
            "1. Mot cau hoi ngan (tieng Viet) de hoi lai nguoi dung cho ro hon.\n"
            "2. Dung 3 lua chon ngan gon de nguoi dung click chon nhanh.\n\n"
            "Tra ve JSON only: {\"question\": \"...\", \"options\": [\"...\", \"...\", \"...\"]}"
        ), state.get("request_context"))

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

    @staticmethod
    def _build_conversation_prompt(state: OrchestratorState) -> str:
        profile_context = ConversationAgentNode._build_profile_context(state)
        history_block = ConversationAgentNode._build_history_block(state)
        sections = [part for part in (profile_context, history_block) if part]
        if not sections:
            return state["user_input"]
        sections.append(f"Current user message: {state['user_input']}")
        return "\n\n".join(sections)

    @staticmethod
    def _build_history_block(state: OrchestratorState, max_turns: int = 8) -> str:
        conversation_context = state.get("conversation_context")
        if not isinstance(conversation_context, dict):
            return ""
        recent = conversation_context.get("recentMessages")
        if not isinstance(recent, list) or not recent:
            return ""

        lines: list[str] = []
        for message in recent[-max_turns:]:
            if not isinstance(message, dict):
                continue
            role = str(message.get("role") or message.get("sender") or "user").lower()
            raw_content = message.get("content") or message.get("text")
            content = str(raw_content).strip()[:400] if raw_content is not None else ""
            if not content:
                continue
            speaker = "Assistant" if role in {"assistant", "ai", "bot", "model"} else "User"
            lines.append(f"{speaker}: {content}")
        if not lines:
            return ""

        return (
            "Earlier messages in this same conversation (oldest first). "
            "Use them to stay consistent and to recall facts the user stated earlier "
            "(their name, preferences, project names, numbers, etc.). "
            "Prefer what the user said here over generic assumptions:\n"
            + "\n".join(lines)
        )

    @staticmethod
    def _build_profile_context(state: OrchestratorState) -> str:
        profile = state.get("user_profile") if isinstance(state.get("user_profile"), dict) else {}
        full_name = ConversationAgentNode._clean_profile_value(profile.get("full_name"))
        username = ConversationAgentNode._clean_profile_value(profile.get("username"))
        if not full_name and not username:
            return ""

        lines = [
            "Verified TechHub profile context for the current authenticated user:",
        ]
        if full_name:
            lines.append(f"- Full/display name: {full_name}")
        if username:
            lines.append(f"- Username: {username}")
        lines.append(
            "If the user asks for their name, answer from this profile context "
            "unless they gave a preferred name or nickname earlier in this "
            "conversation, in which case prefer that. Do not call it a legal identity."
        )
        return "\n".join(lines)

    @staticmethod
    def _answer_personal_memory_question(state: OrchestratorState) -> str | None:
        user_input = state.get("user_input", "")
        current_color = extract_favorite_color(user_input)
        remembered_color = current_color or find_latest_favorite_color(
            user_memory=state.get("user_memory"),
            conversation_context=state.get("conversation_context"),
        )
        asks_user_color = asks_user_favorite_color(user_input)
        asks_assistant_color = asks_assistant_favorite_color(user_input)

        if asks_user_color:
            if remembered_color:
                return f"Bạn thích màu {remembered_color}."
            return "Mình chưa thấy bạn nói màu yêu thích trong cuộc trò chuyện này."

        if current_color and asks_assistant_color:
            return (
                f"Mình ghi nhận bạn thích màu {current_color}. "
                "Còn mình là TechHub AI nên không có sở thích cá nhân về màu sắc."
            )

        return None

    @staticmethod
    def _clean_profile_value(value: Any) -> str:
        if value is None:
            return ""
        cleaned = str(value).strip()
        return cleaned[:120]


conversation_agent_node = ConversationAgentNode()
