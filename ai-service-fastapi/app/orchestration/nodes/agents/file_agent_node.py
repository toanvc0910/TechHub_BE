from __future__ import annotations

import re
from typing import Any

from app.core.config import get_settings
from app.orchestration.state.orchestrator_state import OrchestratorState, trace_step
from app.services.file_context_service import file_context_service
from app.services.llm_gateway import switchable_ai_gateway
from app.services.request_instructions import append_request_instructions


class FileAgentNode:
    def supports(self, intent: str, sub_intent: str | None = None) -> bool:
        del sub_intent
        return intent == "file_analysis"

    async def execute(self, state: OrchestratorState) -> dict[str, Any]:
        settings = get_settings()
        files = state.get("file_contexts", [])
        excerpts = self._extract_excerpts(files)
        citations = self._build_file_citations(files)

        if not excerpts:
            hits = await file_context_service.search_relevant_chunks(
                user_id=state["user_id"],
                session_id=state["session_id"],
                query=state["user_input"],
                limit=4,
            )
            excerpts = self._extract_search_hits(hits)
            if hits:
                citations = [
                    {
                        "title": str(item.get("payload", {}).get("name") or f"file-hit-{index + 1}"),
                        "kind": "file",
                        "excerpt": str(item.get("payload", {}).get("excerpt") or ""),
                        "score": item.get("score"),
                    }
                    for index, item in enumerate(hits)
                ]

        if not excerpts:
            normalized_input = self._normalize_text(state.get("user_input", ""))
            if not self._is_file_reference(normalized_input):
                response = await switchable_ai_gateway.stream_and_emit(
                    prompt=append_request_instructions((
                        "Nguoi dung dang hoi mot cau hoi thong thuong, nhung request da bi route nham qua file-analysis.\n"
                        "Hay tra loi nhu mot tro ly kien thuc tong quat bang tieng Viet, ngan gon, ro rang.\n"
                        "Neu cau hoi nhac den mot nguoi/thuc the ma ban khong du ngu canh de xac dinh chinh xac, "
                        "hay noi ro rang la ban chua du thong tin thay vi bịa them.\n\n"
                        f"Cau hoi user: {state['user_input']}"
                    ), state.get("request_context")),
                    system_prompt=settings.system_prompt,
                    model=state.get("selected_model"),
                )
                trace_step(
                    state,
                    "file_agent",
                    "Fallback to general answer because no analyzable file content was present and the user did not reference a file.",
                )
                return {
                    "final_response": response,
                    "response_streamed": True,
                    "citations": citations,
                    "execution_trace": list(state.get("execution_trace", [])),
                }

            trace_step(state, "file_agent", "No analyzable file content found.")
            return {
                "final_response": (
                    "Yeu cau file da duoc nhan, nhung hien chua co noi dung van ban kha dung de phan tich. "
                    "Hay gui file text/markdown/code hoac kem them doan trich."
                ),
                "citations": citations,
                "execution_trace": list(state.get("execution_trace", [])),
            }

        prompt = append_request_instructions((
            "Ban dang phan tich tai lieu do nguoi dung cung cap cho TechHub.\n"
            "Chi duoc dua tren doan trich sau, khong suy dien ngoai tai lieu.\n"
            "Hay tom tat ngan gon va tra loi cau hoi cua user neu trong tai lieu co thong tin.\n\n"
            f"Cau hoi user: {state['user_input']}\n"
            f"Tai lieu:\n{excerpts}"
        ), state.get("request_context"))
        response = await switchable_ai_gateway.stream_and_emit(
            prompt=prompt,
            system_prompt=settings.system_prompt,
            model=state.get("selected_model"),
        )
        trace_step(state, "file_agent", "Analyzed file excerpts from request context.", fileCount=len(files))
        return {
            "final_response": response,
            "response_streamed": True,
            "citations": citations,
            "execution_trace": list(state.get("execution_trace", [])),
        }

    @staticmethod
    def _extract_excerpts(files: list[dict[str, Any]]) -> str:
        excerpts: list[str] = []
        for index, file_item in enumerate(files, start=1):
            name = str(file_item.get("name") or file_item.get("filename") or f"file-{index}")
            content = file_item.get("content") or file_item.get("text") or file_item.get("excerpt")
            if content:
                excerpts.append(f"[{name}]\n{str(content)[:2500]}")
        return "\n\n".join(excerpts)

    @staticmethod
    def _extract_search_hits(hits: list[dict[str, Any]]) -> str:
        excerpts: list[str] = []
        for index, hit in enumerate(hits, start=1):
            payload = hit.get("payload") or {}
            name = str(payload.get("name") or f"indexed-file-{index}")
            content = str(payload.get("excerpt") or "")
            if content:
                excerpts.append(f"[{name}]\n{content}")
        return "\n\n".join(excerpts)

    @staticmethod
    def _build_file_citations(files: list[dict[str, Any]]) -> list[dict[str, Any]]:
        citations = []
        for index, file_item in enumerate(files):
            citations.append(
                {
                    "title": str(file_item.get("name") or file_item.get("filename") or f"file-{index + 1}"),
                    "kind": "file",
                    "excerpt": str(file_item.get("excerpt") or "")[:240],
                    "status": str(file_item.get("ingestionStatus") or ""),
                }
            )
        return citations

    @staticmethod
    def _normalize_text(text: str) -> str:
        return re.sub(r"\s+", " ", (text or "").lower().strip())

    @staticmethod
    def _is_file_reference(text: str) -> bool:
        if not text:
            return False
        return bool(
            re.search(
                r"\b(file|tai lieu|document|pdf|docx|upload|tep|noi dung nay|tai lieu nay|file nay|tep nay)\b",
                text,
            )
        )


file_agent_node = FileAgentNode()
