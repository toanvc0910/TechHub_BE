from __future__ import annotations

import re
import unicodedata
from typing import Any
from uuid import UUID

from app.core.config import get_settings
from app.core.enums import RecommendationMode
from app.orchestration.state.orchestrator_state import OrchestratorState, trace_step
from app.schemas.recommendation import RecommendationRequest
from app.services.llm_gateway import switchable_ai_gateway
from app.services.recommendation_service import recommendation_service
from app.services.request_instructions import append_request_instructions


class RagResponseNode:
    def supports(self, intent: str, sub_intent: str | None = None) -> bool:
        del sub_intent
        return intent in {"recommendation", "knowledge"}

    async def execute(self, state: OrchestratorState) -> dict[str, Any]:
        settings = get_settings()
        intent = state.get("intent")
        if intent == "recommendation":
            recommendation_response = await self._recommendation_response(state)
            if recommendation_response is not None:
                trace_step(state, "rag_response", "Used dedicated recommendation service for chat recommendation.")
                recommendation_response["execution_trace"] = list(state.get("execution_trace", []))
                return recommendation_response

        documents = (state.get("query_result") or {}).get("documents", [])
        if intent == "knowledge":
            documents = self._filter_relevant_documents(state["user_input"], documents)

        if not documents:
            if intent == "knowledge":
                fallback = await self._knowledge_fallback(state)
                trace_step(state, "rag_response", "Used general-knowledge fallback without course citations.")
                fallback["execution_trace"] = list(state.get("execution_trace", []))
                return fallback
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

        prompt = append_request_instructions((
            "Du lieu tham khao tu TechHub:\n"
            + "\n".join(context_lines)
            + "\n\n"
            + user_context
            + f"Cau hoi nguoi dung: {state['user_input']}\n\n"
            + "Tra loi bang tieng Viet. Uu tien nhac den khoa hoc co trong danh sach.\n"
            + "Neu dang goi y, hay giai thich tai sao khoa hoc phu hop voi trinh do va muc tieu cua nguoi dung."
        ), state.get("request_context"))
        response = await switchable_ai_gateway.stream_and_emit(
            prompt=prompt,
            system_prompt=settings.system_prompt,
            model=state.get("selected_model"),
        )
        filtered_citations = [
            {
                "title": item.get("payload", {}).get("title", "Course"),
                "courseId": item.get("payload", {}).get("id") or item.get("payload", {}).get("course_id"),
                "score": item.get("score"),
            }
            for item in documents[:5]
        ]
        trace_step(state, "rag_response", "Generated personalized answer from retrieved documents.")
        return {
            "final_response": response,
            "response_streamed": True,
            "citations": filtered_citations,
            "execution_trace": list(state.get("execution_trace", [])),
        }

    @staticmethod
    async def _recommendation_response(state: OrchestratorState) -> dict[str, Any] | None:
        try:
            request = RecommendationRequest(
                userId=UUID(str(state["user_id"])),
                mode=RecommendationMode.REALTIME,
                language="vi",
            )
            response = await recommendation_service.generate(request)
        except Exception:
            return None

        items = response.recommendations[:3]
        if not items:
            return None

        lines = ["Toi de xuat 3 khoa hoc phu hop hien tai:"]
        for index, item in enumerate(items, start=1):
            lines.append(
                f"{index}. {item.title} ({item.estimatedDuration})\n"
                f"   - Ly do: {item.reason}"
            )

        metadata = response.metadata if isinstance(response.metadata, dict) else {}
        return {
            "final_response": "\n".join(lines),
            "citations": metadata.get("citations", []),
            "query_result": {
                "recommendations": [item.model_dump(mode="json") for item in response.recommendations],
                "summary": "Danh sach goi y da duoc ca nhan hoa theo lich su hoc tap va ho so ky nang cua ban.",
            },
        }

    @staticmethod
    async def _knowledge_fallback(state: OrchestratorState) -> dict[str, Any]:
        settings = get_settings()
        response = await switchable_ai_gateway.stream_and_emit(
            prompt=append_request_instructions((
                "Ban la tro ly hoc tap cua TechHub.\n"
                "Nguoi dung dang hoi mot cau ly thuyet hoac khai niem tong quat.\n"
                "Neu kho tri thuc noi bo chua co tai lieu lien quan, hay van tra loi dua tren kien thuc chung mot cach ngan gon,"
                " chinh xac va de hieu. Khong duoc bịa them khoa hoc hay du lieu noi bo.\n"
                f"Cau hoi: {state['user_input']}"
            ), state.get("request_context")),
            system_prompt=settings.system_prompt,
            model=state.get("selected_model"),
        )
        return {
            "final_response": response,
            "response_streamed": True,
            "citations": [],
            "query_result": None,
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

    @classmethod
    def _filter_relevant_documents(cls, user_input: str, documents: list[dict[str, Any]]) -> list[dict[str, Any]]:
        normalized_query = cls._normalize_text(user_input)
        query_tokens = cls._meaningful_tokens(normalized_query)
        if not query_tokens:
            return documents[:5]

        filtered: list[dict[str, Any]] = []
        for item in documents:
            payload = item.get("payload", {})
            haystack = cls._normalize_text(
                " ".join(
                    [
                        str(payload.get("title") or ""),
                        str(payload.get("description") or ""),
                        " ".join(str(skill) for skill in payload.get("skills", []) or []),
                        " ".join(str(tag) for tag in payload.get("tags", []) or []),
                    ]
                )
            )
            haystack_tokens = cls._meaningful_tokens(haystack)
            overlap = query_tokens.intersection(haystack_tokens)
            score = float(item.get("score") or 0.0)
            if overlap or score >= 0.68:
                filtered.append(item)
        return filtered[:5]

    @staticmethod
    def _normalize_text(text: str) -> str:
        lowered = (text or "").lower().replace("đ", "d").replace("Ä‘", "d")
        normalized = unicodedata.normalize("NFD", lowered)
        without_marks = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
        return re.sub(r"\s+", " ", without_marks).strip()

    @staticmethod
    def _meaningful_tokens(text: str) -> set[str]:
        stopwords = {
            "la",
            "gi",
            "the",
            "nao",
            "cho",
            "toi",
            "mot",
            "hay",
            "va",
            "cua",
            "ban",
        }
        return {
            token
            for token in re.split(r"[^a-z0-9#+.-]+", text)
            if token and len(token) > 1 and token not in stopwords
        }


rag_response_node = RagResponseNode()
