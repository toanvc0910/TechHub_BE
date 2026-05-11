from __future__ import annotations

from datetime import datetime
from typing import Any
from uuid import UUID

from pydantic import BaseModel, Field

from app.core.enums import ChatMode, ChatSender


class ChatMessageRequest(BaseModel):
    sessionId: UUID | None = None
    userId: UUID
    mode: ChatMode = ChatMode.AUTO
    message: str = Field(min_length=1, max_length=2000)
    context: Any | None = None


class ChatMessageResponse(BaseModel):
    sessionId: UUID
    messageId: UUID | None = None
    timestamp: datetime | None = None
    mode: ChatMode | None = None
    message: str | None = None
    answer: str | None = None
    context: Any | None = None
    metadata: dict[str, Any] | None = None


class ChatSessionResponse(BaseModel):
    id: UUID
    userId: UUID
    startedAt: datetime
    endedAt: datetime | None = None
    title: str | None = None
    context: Any | None = None


class ChatMessageDetailResponse(BaseModel):
    id: UUID
    sessionId: UUID
    sender: ChatSender
    content: str
    timestamp: datetime
    metadata: dict[str, Any] | None = None
