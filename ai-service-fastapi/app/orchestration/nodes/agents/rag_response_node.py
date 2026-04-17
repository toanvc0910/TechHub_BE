from __future__ import annotations

from typing import Any

from app.core.config import get_settings
from app.orchestration.state.orchestrator_state import OrchestratorState, trace_step
from app.services.llm_gateway import switchable_ai_gateway


class RagResponseNode:
    def supports(self, intent: str, sub_intent: str | None = None) -> bool:
        del sub_intent
        return intent in {"recommendation", "knowledge"}

    async def execute(self, state: OrchestratorState) -> dict[str, Any]:
        settings = get_settings()
        documents = (state.get("query_result") or {}).get("documents", [])
        if not documents:
            trace_step(state, "rag_response", "Returned no-documents fallback.")
            return {
                "final_response": (
                    "Hien tai toi chua tim thay du lieu lien quan trong kho tri thuc. "
                    "Ban co the thu mo ta cu the hon ve chu de hoc."
                ),
                "execution_trace": list(state.get("execution_trace", [])),
            }

        context_lines = []
        for idx, item in enumerate(documents, start=1):
            payload = item.get("payload", {})
            skills = ", ".join(str(s) for s in payload.get("skills", [])[:5])
            context_lines.append(
                f"{idx}. {payload.get('title', 'Course')} | level={payload.get('level', 'n/a')} | "
                f"skills={skills} | description={payload.get('description', '')}"
            )

        # Build personalization context
        user_context = self._build_user_context(state)

        prompt = (
            "Du lieu tham khao tu TechHub:\n"
            + "\n".join(context_lines)
            + "\n\n"
            + user_context
            + f"Cau hoi nguoi dung: {state['user_input']}\n\n"
            + "Tra loi bang tieng Viet. Uu tien nhac den khoa hoc co trong danh sach.\n"
            + "Neu dang goi y, hay giai thich tai sao khoa hoc phu hop voi trinh do va muc tieu cua nguoi dung."
        )
        response = await switchable_ai_gateway.generate_text(
            prompt=prompt,
            system_prompt=settings.system_prompt,
            model=state.get("selected_model"),
        )
        trace_step(state, "rag_response", "Generated personalized answer from retrieved documents.")
        return {
            "final_response": response,
            "execution_trace": list(state.get("execution_trace", [])),
        }

    @staticmethod
    def _build_user_context(state: OrchestratorState) -> str:
        """Build a user context block to inject into the LLM prompt for personalization."""
        lines: list[str] = []
        skill_profile = state.get("skill_profile", {})
        course_history = state.get("user_course_history", [])

        if skill_profile:
            top_skills = sorted(skill_profile.items(), key=lambda x: x[1], reverse=True)[:6]
            skills_str = ", ".join(f"{name} ({level:.0%})" for name, level in top_skills)
            lines.append(f"Ky nang hien tai cua nguoi dung: {skills_str}")

        completed = [item["title"] for item in course_history if item.get("status") == "COMPLETED"]
        in_progress = [item["title"] for item in course_history if item.get("status") == "IN_PROGRESS"]

        if completed:
            lines.append(f"Khoa hoc da hoan thanh: {', '.join(completed[:5])}")
        if in_progress:
            lines.append(f"Dang hoc: {', '.join(in_progress[:3])}")

        # Ratings-based preferences
        ratings = state.get("user_ratings", [])
        liked = [r["title"] for r in ratings if r.get("score", 0) >= 4]
        if liked:
            lines.append(f"Khoa hoc duoc danh gia cao (>= 4 sao): {', '.join(liked[:4])}")

        if not lines:
            return ""
        return "Thong tin nguoi dung:\n" + "\n".join(f"- {line}" for line in lines) + "\n\n"


rag_response_node = RagResponseNode()
