from __future__ import annotations

import asyncio
import re
import unicodedata
from collections.abc import AsyncIterator
from datetime import datetime, timezone
from time import perf_counter
from uuid import UUID, uuid4

from sqlalchemy import delete, select
from sqlalchemy.orm import selectinload

from app.core.config import get_settings
from app.core.enums import ChatSender
from app.db.models import ChatMessageModel, ChatSessionModel
from app.db.session import get_db_session
from app.orchestration.graph.chat_orchestrator_graph import chat_orchestrator_graph
from app.orchestration.model.model_selector_service import model_selector_service
from app.orchestration.nodes.context_load_node import context_load_node
from app.orchestration.nodes.context_save_node import context_save_node
from app.orchestration.nodes.agents.file_agent_node import file_agent_node
from app.orchestration.state.orchestrator_state import OrchestratorState, make_initial_state, trace_step
from app.orchestration.stream.stream_event_emitter import StreamEventEmitter, text_chunk_event
from app.schemas.chat import (
    ChatMessageDetailResponse,
    ChatMessageRequest,
    ChatMessageResponse,
    ChatSessionResponse,
)
from app.services.llm_gateway import switchable_ai_gateway
from app.services.observability_service import runtime_observability_service
from app.services.prompt_sanitization import prompt_sanitization_service
from app.services.rate_limit import rate_limiting_service
from app.services.runtime_request_context import runtime_request_context_service


class ChatService:
    def __init__(self) -> None:
        self._settings = get_settings()

    async def send_message(self, request: ChatMessageRequest) -> ChatMessageResponse:
        started = perf_counter()
        with runtime_request_context_service.begin(scope="chat_sync") as runtime_state:
            if not await rate_limiting_service.is_allowed(request.userId):
                raise ValueError("Too many requests. Please try again later.")

            sanitized = prompt_sanitization_service.sanitize(request.message)
            try:
                async with get_db_session() as session:
                    chat_session = await self._load_or_create_session(session, request)
                    await self._save_message(
                        session,
                        session_id=chat_session.id,
                        sender=ChatSender.USER.value,
                        content=sanitized,
                        metadata=self._build_user_message_metadata(request.context),
                    )
                    state = await self._execute_chat_pipeline(
                        make_initial_state(
                            user_id=str(request.userId),
                            session_id=str(chat_session.id),
                            user_input=sanitized,
                            mode=request.mode.value,
                            request_context=request.context,
                            request_id=runtime_state.request_id,
                        ),
                    )
                    self._attach_runtime_usage(state)
                    resolved_mode = self._resolve_mode(request.mode, state.get("intent", "conversation"))
                    assistant_metadata = self._build_metadata(
                        state,
                        requested_mode=request.mode.value,
                        resolved_mode=resolved_mode.value,
                    )
                    bot_message = await self._save_message(
                        session,
                        session_id=chat_session.id,
                        sender=ChatSender.BOT.value,
                        content=state.get("final_response", ""),
                        metadata=assistant_metadata,
                    )
                    response = ChatMessageResponse(
                        sessionId=chat_session.id,
                        messageId=bot_message.id,
                        timestamp=bot_message.timestamp,
                        mode=resolved_mode,
                        message=state.get("final_response", ""),
                        answer=state.get("final_response", ""),
                        context=request.context,
                        metadata=assistant_metadata,
                    )
                    await runtime_observability_service.record_chat_run(
                        request_id=runtime_state.request_id,
                        pipeline=state.get("pipeline", "orchestration"),
                        intent=state.get("intent", "unknown"),
                        success=True,
                        duration_ms=(perf_counter() - started) * 1000,
                        awaiting_approval=False,
                        fallback_reason=self._fallback_reason(state),
                        tokens_used=int(state.get("token_usage", {}).get("totalTokens") or 0),
                    )
                    return response
            except Exception:
                await runtime_observability_service.record_chat_run(
                    request_id=runtime_state.request_id,
                    pipeline="orchestration",
                    intent="unknown",
                    success=False,
                    duration_ms=(perf_counter() - started) * 1000,
                    awaiting_approval=False,
                    fallback_reason="sync_exception",
                    tokens_used=0,
                )
                await runtime_observability_service.record_error(scope="chat_sync")
                raise

    async def stream_message(self, request: ChatMessageRequest) -> AsyncIterator[str]:
        emitter = StreamEventEmitter()
        worker = asyncio.create_task(self._run_stream_workflow(request, emitter))
        try:
            async for event in emitter.iterate():
                yield event
        finally:
            await worker

    async def _run_stream_workflow(self, request: ChatMessageRequest, emitter: StreamEventEmitter) -> None:
        started = perf_counter()
        with runtime_request_context_service.begin(scope="chat_stream") as runtime_state:
            try:
                if not await rate_limiting_service.is_allowed(request.userId):
                    raise ValueError("Too many requests. Please try again later.")

                sanitized = prompt_sanitization_service.sanitize(request.message)
                async with get_db_session() as session:
                    chat_session = await self._load_or_create_session(session, request)
                    await emitter.emit(
                        "session",
                        {
                            "sessionId": str(chat_session.id),
                            "requestedMode": request.mode.value,
                            "requestId": runtime_state.request_id,
                        },
                    )
                    await self._save_message(
                        session,
                        session_id=chat_session.id,
                        sender=ChatSender.USER.value,
                        content=sanitized,
                        metadata=self._build_user_message_metadata(request.context),
                    )
                    state = await self._execute_chat_pipeline(
                        make_initial_state(
                            user_id=str(request.userId),
                            session_id=str(chat_session.id),
                            user_input=sanitized,
                            mode=request.mode.value,
                            request_context=request.context,
                            request_id=runtime_state.request_id,
                        ),
                        emitter=emitter,
                    )
                    self._attach_runtime_usage(state)
                    resolved_mode = self._resolve_mode(request.mode, state.get("intent", "conversation"))

                    # HITL clarify flow: emit hitl_question event
                    if state.get("hitl_clarify_active"):
                        await emitter.emit(
                            "hitl_question",
                            {
                                "question": state.get("hitl_question", ""),
                                "options": state.get("hitl_options", []),
                                "sessionId": str(chat_session.id),
                                "requestId": runtime_state.request_id,
                            },
                        )
                        if not state.get("response_streamed"):
                            for chunk in self._chunk_text(state.get("final_response", "")):
                                await emitter.emit("message", text_chunk_event(chunk))
                                await asyncio.sleep(self._stream_delay_seconds)
                        assistant_metadata = self._build_metadata(
                            state,
                            requested_mode=request.mode.value,
                            resolved_mode=resolved_mode.value,
                        )
                        bot_message = await self._save_message(
                            session,
                            session_id=chat_session.id,
                            sender=ChatSender.BOT.value,
                            content=state.get("final_response", ""),
                            metadata=assistant_metadata,
                        )
                        await emitter.emit(
                            "done",
                            {
                                "content": "[DONE]",
                                "sessionId": str(chat_session.id),
                                "messageId": str(bot_message.id),
                                **assistant_metadata,
                            },
                        )
                        await runtime_observability_service.record_chat_run(
                            request_id=runtime_state.request_id,
                            pipeline=state.get("pipeline", "orchestration"),
                            intent=state.get("intent", "unknown"),
                            success=True,
                            duration_ms=(perf_counter() - started) * 1000,
                            awaiting_approval=False,
                            fallback_reason="hitl_clarify",
                            tokens_used=int(state.get("token_usage", {}).get("totalTokens") or 0),
                        )
                        return

                    if not state.get("response_streamed"):
                        for chunk in self._chunk_text(state.get("final_response", "")):
                            await emitter.emit("message", text_chunk_event(chunk))
                            await asyncio.sleep(self._stream_delay_seconds)
                    if state.get("query_result") or state.get("chart_spec"):
                        query_result_payload = state.get("query_result") or {}
                        artifact_suggested_actions = (
                            query_result_payload.get("suggestedActions")
                            if isinstance(query_result_payload, dict)
                            else None
                        ) or []
                        await emitter.emit(
                            "artifact",
                            {
                                "sessionId": str(chat_session.id),
                                "queryResult": state.get("query_result"),
                                "chartSpec": state.get("chart_spec"),
                                "suggestedActions": artifact_suggested_actions,
                                "requestId": runtime_state.request_id,
                            },
                        )
                    assistant_metadata = self._build_metadata(
                        state,
                        requested_mode=request.mode.value,
                        resolved_mode=resolved_mode.value,
                    )
                    bot_message = await self._save_message(
                        session,
                        session_id=chat_session.id,
                        sender=ChatSender.BOT.value,
                        content=state.get("final_response", ""),
                        metadata=assistant_metadata,
                    )
                    await emitter.emit(
                        "done",
                        {
                            "content": "[DONE]",
                            "sessionId": str(chat_session.id),
                            "messageId": str(bot_message.id),
                            **assistant_metadata,
                        },
                    )
                    await runtime_observability_service.record_chat_run(
                        request_id=runtime_state.request_id,
                        pipeline=state.get("pipeline", "orchestration"),
                        intent=state.get("intent", "unknown"),
                        success=True,
                        duration_ms=(perf_counter() - started) * 1000,
                        awaiting_approval=False,
                        fallback_reason=self._fallback_reason(state),
                        tokens_used=int(state.get("token_usage", {}).get("totalTokens") or 0),
                    )
            except Exception as exc:  # pragma: no cover - surfaced to SSE
                await runtime_observability_service.record_chat_run(
                    request_id=runtime_state.request_id,
                    pipeline="orchestration",
                    intent="unknown",
                    success=False,
                    duration_ms=(perf_counter() - started) * 1000,
                    awaiting_approval=False,
                    fallback_reason=exc.__class__.__name__,
                    tokens_used=0,
                )
                await runtime_observability_service.record_error(scope="chat_stream")
                await emitter.emit("error", {"message": str(exc), "requestId": runtime_state.request_id})
            finally:
                await emitter.close()

    async def create_session(self, user_id: UUID, mode: str | None) -> ChatSessionResponse:
        async with get_db_session() as session:
            chat_session = ChatSessionModel(
                user_id=user_id,
                started_at=datetime.now(timezone.utc),
                context={"mode": (mode or "AUTO").upper()},
                is_active="Y",
            )
            session.add(chat_session)
            await session.flush()
            return self._to_session_response(chat_session)

    async def get_user_sessions(self, user_id: UUID) -> list[ChatSessionResponse]:
        async with get_db_session() as session:
            result = await session.execute(
                select(ChatSessionModel)
                .where(ChatSessionModel.user_id == user_id)
                .where(ChatSessionModel.is_active == "Y")
                .order_by(ChatSessionModel.started_at.desc())
            )
            return [self._to_session_response(item) for item in result.scalars().all()]

    async def get_session_messages(self, session_id: UUID) -> list[ChatMessageDetailResponse]:
        async with get_db_session() as session:
            result = await session.execute(
                select(ChatMessageModel)
                .where(ChatMessageModel.session_id == session_id)
                .where(ChatMessageModel.is_active == "Y")
                .order_by(ChatMessageModel.timestamp.asc())
            )
            messages = result.scalars().all()
            return [
                ChatMessageDetailResponse(
                    id=item.id,
                    sessionId=item.session_id,
                    sender=item.sender,
                    content=item.content,
                    timestamp=item.timestamp,
                    metadata=item.message_metadata,
                )
                for item in messages
            ]

    async def delete_session(self, session_id: UUID, user_id: UUID) -> None:
        async with get_db_session() as session:
            result = await session.execute(
                select(ChatSessionModel)
                .options(selectinload(ChatSessionModel.messages))
                .where(ChatSessionModel.id == session_id)
                .where(ChatSessionModel.user_id == user_id)
            )
            chat_session = result.scalar_one_or_none()
            if chat_session is None:
                raise ValueError("Session not found for user.")
            await session.execute(delete(ChatMessageModel).where(ChatMessageModel.session_id == session_id))
            await session.delete(chat_session)

    async def _load_or_create_session(self, session, request: ChatMessageRequest) -> ChatSessionModel:
        if request.sessionId:
            result = await session.execute(
                select(ChatSessionModel)
                .where(ChatSessionModel.id == request.sessionId)
                .where(ChatSessionModel.user_id == request.userId)
            )
            existing = result.scalar_one_or_none()
            if existing is not None:
                return existing

        chat_session = ChatSessionModel(
            id=request.sessionId or uuid4(),
            user_id=request.userId,
            started_at=datetime.now(timezone.utc),
            context=(
                {**request.context, "requestedMode": request.mode.value}
                if isinstance(request.context, dict)
                else {"requestedMode": request.mode.value}
            ),
            is_active="Y",
        )
        session.add(chat_session)
        await session.flush()
        return chat_session

    async def _save_message(self, session, *, session_id, sender: str, content: str, metadata: dict | None = None) -> ChatMessageModel:
        message = ChatMessageModel(
            session_id=session_id,
            sender=sender,
            content=content,
            message_metadata=metadata,
            timestamp=datetime.now(timezone.utc),
            is_active="Y",
        )
        session.add(message)
        await session.flush()
        return message

    @staticmethod
    def _build_user_message_metadata(request_context: dict | None) -> dict | None:
        if not isinstance(request_context, dict):
            return None
        attachments = request_context.get("fileContexts")
        if not isinstance(attachments, list) or not attachments:
            return None
        normalized_attachments = []
        for item in attachments:
            if not isinstance(item, dict):
                continue
            normalized_attachments.append(
                {
                    "id": item.get("id") or item.get("fileId"),
                    "fileId": item.get("fileId") or item.get("id"),
                    "name": item.get("name"),
                    "mimeType": item.get("mimeType"),
                    "fileType": item.get("fileType"),
                    "size": item.get("size"),
                    "secureUrl": item.get("secureUrl"),
                    "publicUrl": item.get("publicUrl"),
                    "cloudinarySecureUrl": item.get("cloudinarySecureUrl"),
                    "thumbnailUrl": item.get("thumbnailUrl"),
                    "content": item.get("content"),
                    "excerpt": item.get("excerpt"),
                    "description": item.get("description"),
                    "processingStatus": item.get("processingStatus"),
                }
            )
        return {"attachments": normalized_attachments} if normalized_attachments else None

    async def _execute_chat_pipeline(
        self,
        state: OrchestratorState,
        *,
        emitter: StreamEventEmitter | None = None,
    ) -> OrchestratorState:
        if not self._settings.orchestration_enabled:
            return await self._run_legacy_pipeline(
                state,
                reason="orchestration_disabled",
                emitter=emitter,
            )

        try:
            return await chat_orchestrator_graph.run(state, emitter=emitter)
        except Exception as exc:
            if not self._settings.legacy_fallback_enabled:
                raise
            state.setdefault("errors", []).append(f"orchestration_runtime:{exc}")
            if emitter is not None:
                await emitter.emit(
                    "planning_step",
                    {
                        "step": "rollback",
                        "detail": "Falling back to legacy chat pipeline after orchestration error.",
                    },
                )
            return await self._run_legacy_pipeline(
                state,
                reason=f"orchestration_error:{exc.__class__.__name__}",
                emitter=emitter,
            )

    async def _run_legacy_pipeline(
        self,
        state: OrchestratorState,
        *,
        reason: str,
        emitter: StreamEventEmitter | None = None,
    ) -> OrchestratorState:
        state["pipeline"] = "legacy"
        if not state.get("conversation_context"):
            updates = await context_load_node(state)
            state.update(updates)  # type: ignore[arg-type]
        state["intent"] = self._legacy_intent(state)
        state["intent_confidence"] = 0.78 if state["intent"] in {"recommendation", "file_analysis"} else 0.72
        state["matched_rule"] = "legacy:rollback"
        selected_model, complexity = await model_selector_service.select_model(state)
        state["selected_model"] = selected_model
        state["complexity"] = complexity
        trace_step(
            state,
            "legacy_fallback",
            "Using rollback-safe legacy chat pipeline.",
            reason=reason,
            intent=state["intent"],
            model=state["selected_model"],
        )
        if emitter is not None:
            await emitter.emit(
                "planning_step",
                {
                    "step": "legacy_fallback",
                    "detail": f"Using legacy pipeline ({reason}).",
                },
            )

        if state["intent"] == "file_analysis":
            updates = await file_agent_node.execute(state)
            state.update(updates)  # type: ignore[arg-type]
        else:
            state["final_response"] = await switchable_ai_gateway.generate_text(
                prompt=self._build_legacy_prompt(state),
                system_prompt=self._settings.system_prompt,
                model=state.get("selected_model"),
            )

        save_updates = await context_save_node(state)
        state.update(save_updates)  # type: ignore[arg-type]
        return state

    @staticmethod
    def _legacy_intent(state: OrchestratorState) -> str:
        normalized_text = ChatService._normalize_legacy_text(state.get("user_input", ""))
        if state.get("has_fresh_file_context"):
            return "file_analysis"
        if state.get("file_contexts") and ChatService._is_file_reference(normalized_text):
            return "file_analysis"
        if state.get("mode") == "ADVISOR":
            return "recommendation"
        if ChatService._looks_like_knowledge_question(normalized_text):
            return "knowledge"
        return "conversation"

    @staticmethod
    def _normalize_legacy_text(text: str) -> str:
        lowered = (text or "").lower().strip().replace("đ", "d")
        normalized = unicodedata.normalize("NFD", lowered)
        without_marks = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
        return re.sub(r"\s+", " ", without_marks)

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

    @staticmethod
    def _looks_like_knowledge_question(text: str) -> bool:
        if not text:
            return False
        return bool(
            re.search(
                r"\b(la gi|giai thich|khai niem|tai sao|how|what is|who is|ai la)\b",
                text,
            )
        )

    @staticmethod
    def _build_legacy_prompt(state: OrchestratorState) -> str:
        recent_messages = state.get("conversation_context", {}).get("recentMessages", [])[-4:]
        recent_context = "\n".join(
            f"{item.get('role', 'unknown')}: {item.get('content', '')}" for item in recent_messages if isinstance(item, dict)
        )
        user_memory = ", ".join(f"{key}={value}" for key, value in state.get("user_memory", {}).items())
        intent_hint = (
            "Nguoi dung dang can tu van hoc tap va de xuat khoa hoc phu hop."
            if state.get("intent") == "recommendation"
            else "Tra loi mot cach tro chuyen, ngan gon, dung trong pham vi TechHub."
        )
        return (
            f"{intent_hint}\n"
            f"Recent context:\n{recent_context or '(empty)'}\n"
            f"User memory: {user_memory or '(empty)'}\n"
            f"Current user message: {state['user_input']}"
        )

    @staticmethod
    def _fallback_reason(state: OrchestratorState) -> str | None:
        for trace_item in reversed(state.get("execution_trace", [])):
            if trace_item.get("step") == "legacy_fallback":
                return str(trace_item.get("reason") or "legacy_fallback")
        return None

    @property
    def _stream_delay_seconds(self) -> float:
        return max(0, int(self._settings.stream_emit_delay_ms or 0)) / 1000

    def _chunk_text(self, text: str, chunk_size: int | None = None) -> list[str]:
        actual_chunk_size = max(1, int(chunk_size or self._settings.stream_emit_chunk_size or 1))
        return [text[i : i + actual_chunk_size] for i in range(0, len(text), actual_chunk_size)] or [text]

    @staticmethod
    def _to_session_response(session_model: ChatSessionModel) -> ChatSessionResponse:
        return ChatSessionResponse(
            id=session_model.id,
            userId=session_model.user_id,
            startedAt=session_model.started_at,
            endedAt=session_model.ended_at,
            context=session_model.context,
        )

    @staticmethod
    def _resolve_mode(requested_mode, intent: str):
        if requested_mode.value != "AUTO":
            return requested_mode
        if intent == "recommendation":
            return type(requested_mode).ADVISOR
        return type(requested_mode).GENERAL

    @staticmethod
    def _build_metadata(state: OrchestratorState, *, requested_mode: str, resolved_mode: str) -> dict:
        query_result = state.get("query_result") or {}
        suggested_actions = (
            query_result.get("suggestedActions")
            if isinstance(query_result, dict)
            else None
        )
        return {
            "requestId": state.get("request_id"),
            "requestedMode": requested_mode,
            "resolvedMode": resolved_mode,
            "intent": state.get("intent"),
            "confidence": state.get("intent_confidence"),
            "model": state.get("selected_model"),
            "tokensUsed": state.get("token_usage", {}).get("totalTokens"),
            "tokenUsage": state.get("token_usage", {}),
            "citations": state.get("citations", []),
            "queryResult": state.get("query_result"),
            "chartSpec": state.get("chart_spec"),
            "suggestedActions": suggested_actions or [],
            "artifact": {
                "queryResult": state.get("query_result"),
                "chartSpec": state.get("chart_spec"),
            }
            if state.get("query_result") or state.get("chart_spec")
            else None,
            "trace": state.get("execution_trace", []),
            "pipeline": state.get("pipeline"),
            "nodeTimings": state.get("node_timings", {}),
            "hitlClarifyActive": state.get("hitl_clarify_active", False),
            "hitlQuestion": state.get("hitl_question"),
            "hitlOptions": state.get("hitl_options", []),
        }

    @staticmethod
    def _attach_runtime_usage(state: OrchestratorState) -> None:
        usage = runtime_request_context_service.snapshot()
        state["request_id"] = usage.get("requestId") or state.get("request_id")
        state["token_usage"] = usage


chat_service = ChatService()
