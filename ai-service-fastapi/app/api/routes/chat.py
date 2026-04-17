from __future__ import annotations

from uuid import UUID

from fastapi import APIRouter, Request
from fastapi.responses import StreamingResponse

from app.core.responses import success_response
from app.schemas.chat import ChatMessageRequest
from app.services.chat_service import chat_service
from app.services.llm_gateway import switchable_ai_gateway

router = APIRouter()


@router.post("/messages")
async def send_message(request_body: ChatMessageRequest, request: Request) -> dict:
    response = await chat_service.send_message(request_body)
    return success_response(
        message="Chat processed",
        data=response.model_dump(mode="json"),
        path=request.url.path,
        status="AI_CHAT",
    )


@router.post("/stream")
async def stream_message(request_body: ChatMessageRequest) -> StreamingResponse:
    return StreamingResponse(chat_service.stream_message(request_body), media_type="text/event-stream")


@router.get("/stream/simple")
async def stream_simple(message: str, userId: UUID) -> StreamingResponse:
    async def iterator():
        text = await switchable_ai_gateway.generate_text(prompt=message)
        for chunk in [text[i : i + 48] for i in range(0, len(text), 48)] or [text]:
            yield f"event: message\ndata: {chunk}\n\n"
        yield "event: done\ndata: [DONE]\n\n"

    del userId
    return StreamingResponse(iterator(), media_type="text/event-stream")


@router.get("/stream/health")
async def stream_health() -> StreamingResponse:
    async def iterator():
        for idx in range(5):
            yield f"event: ping\ndata: pong-{idx}\n\n"
        yield "event: done\ndata: [DONE]\n\n"

    return StreamingResponse(iterator(), media_type="text/event-stream")


@router.post("/sessions")
async def create_session(userId: UUID, request: Request, mode: str | None = None) -> dict:
    response = await chat_service.create_session(userId, mode)
    return success_response(message="Session created", data=response.model_dump(mode="json"), path=request.url.path)


@router.get("/sessions")
async def get_user_sessions(userId: UUID, request: Request) -> dict:
    sessions = await chat_service.get_user_sessions(userId)
    return success_response(
        message="User sessions retrieved",
        data=[item.model_dump(mode="json") for item in sessions],
        path=request.url.path,
    )


@router.get("/sessions/{session_id}/messages")
async def get_session_messages(session_id: UUID, request: Request) -> dict:
    messages = await chat_service.get_session_messages(session_id)
    return success_response(
        message="Session messages retrieved",
        data=[item.model_dump(mode="json") for item in messages],
        path=request.url.path,
    )


@router.delete("/sessions/{session_id}")
async def delete_session(session_id: UUID, userId: UUID, request: Request) -> dict:
    await chat_service.delete_session(session_id, userId)
    return success_response(message="Session deleted successfully", data=None, path=request.url.path)
