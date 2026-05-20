from __future__ import annotations

import asyncio
from uuid import UUID

from fastapi import APIRouter, Depends, Request
from fastapi.responses import StreamingResponse

from app.api.dependencies.trusted_context import (
    TrustedContext,
    build_trusted_request_context,
    copy_model_with_updates,
    get_trusted_context,
    require_trusted_user,
    require_user_match,
)
from app.core.config import get_settings
from app.core.responses import success_response
from app.schemas.chat import ChatMessageRequest
from app.services.chat_service import chat_service
from app.services.llm_gateway import switchable_ai_gateway

router = APIRouter()
SSE_HEADERS = {
    "Cache-Control": "no-cache",
    "Connection": "keep-alive",
    "X-Accel-Buffering": "no",
}


@router.post("/messages")
async def send_message(
    request_body: ChatMessageRequest,
    request: Request,
    trusted: TrustedContext = Depends(get_trusted_context),
) -> dict:
    trusted_user_id = require_user_match(request_body.userId, trusted)
    trusted_body = copy_model_with_updates(
        request_body,
        userId=trusted_user_id,
        context=build_trusted_request_context(request_body.context, trusted),
    )
    response = await chat_service.send_message(trusted_body)
    return success_response(
        message="Chat processed",
        data=response.model_dump(mode="json"),
        path=request.url.path,
        status="AI_CHAT",
    )


@router.post("/stream")
async def stream_message(
    request_body: ChatMessageRequest,
    trusted: TrustedContext = Depends(get_trusted_context),
) -> StreamingResponse:
    trusted_user_id = require_user_match(request_body.userId, trusted)
    trusted_body = copy_model_with_updates(
        request_body,
        userId=trusted_user_id,
        context=build_trusted_request_context(request_body.context, trusted),
    )
    return StreamingResponse(
        chat_service.stream_message(trusted_body),
        media_type="text/event-stream",
        headers=SSE_HEADERS,
    )


@router.get("/stream/simple")
async def stream_simple(
    message: str,
    userId: UUID,
    trusted: TrustedContext = Depends(get_trusted_context),
) -> StreamingResponse:
    require_user_match(userId, trusted)

    async def iterator():
        settings = get_settings()
        chunk_size = max(1, int(settings.stream_emit_chunk_size or 1))
        delay_seconds = max(0, int(settings.stream_emit_delay_ms or 0)) / 1000
        text = await switchable_ai_gateway.generate_text(prompt=message)
        for chunk in [text[i : i + chunk_size] for i in range(0, len(text), chunk_size)] or [text]:
            yield f"event: message\ndata: {chunk}\n\n"
            if delay_seconds > 0:
                await asyncio.sleep(delay_seconds)
        yield "event: done\ndata: [DONE]\n\n"

    return StreamingResponse(iterator(), media_type="text/event-stream", headers=SSE_HEADERS)


@router.get("/stream/health")
async def stream_health(
    trusted: TrustedContext = Depends(require_trusted_user),
) -> StreamingResponse:
    # SSE liveness probe — must come from an internal trusted source so we
    # don't accidentally expose a public DoS-friendly streaming endpoint.
    _ = trusted

    async def iterator():
        for idx in range(5):
            yield f"event: ping\ndata: pong-{idx}\n\n"
        yield "event: done\ndata: [DONE]\n\n"

    return StreamingResponse(iterator(), media_type="text/event-stream", headers=SSE_HEADERS)


@router.post("/sessions")
async def create_session(
    userId: UUID,
    request: Request,
    mode: str | None = None,
    trusted: TrustedContext = Depends(get_trusted_context),
) -> dict:
    trusted_user_id = require_user_match(userId, trusted)
    response = await chat_service.create_session(trusted_user_id, mode)
    return success_response(message="Session created", data=response.model_dump(mode="json"), path=request.url.path)


@router.get("/sessions")
async def get_user_sessions(
    userId: UUID,
    request: Request,
    trusted: TrustedContext = Depends(get_trusted_context),
) -> dict:
    trusted_user_id = require_user_match(userId, trusted)
    sessions = await chat_service.get_user_sessions(trusted_user_id)
    return success_response(
        message="User sessions retrieved",
        data=[item.model_dump(mode="json") for item in sessions],
        path=request.url.path,
    )


@router.get("/sessions/{session_id}/messages")
async def get_session_messages(
    session_id: UUID,
    request: Request,
    trusted: TrustedContext = Depends(require_trusted_user),
) -> dict:
    messages = await chat_service.get_session_messages(session_id, trusted.user_id)
    return success_response(
        message="Session messages retrieved",
        data=[item.model_dump(mode="json") for item in messages],
        path=request.url.path,
    )


@router.delete("/sessions/{session_id}")
async def delete_session(
    session_id: UUID,
    userId: UUID,
    request: Request,
    trusted: TrustedContext = Depends(get_trusted_context),
) -> dict:
    trusted_user_id = require_user_match(userId, trusted)
    await chat_service.delete_session(session_id, trusted_user_id)
    return success_response(message="Session deleted successfully", data=None, path=request.url.path)
