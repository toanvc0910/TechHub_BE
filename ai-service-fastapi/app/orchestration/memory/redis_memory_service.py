from __future__ import annotations

import json
from dataclasses import asdict, dataclass, field
from typing import Any

from redis.asyncio import Redis

from app.core.config import get_settings


@dataclass(slots=True)
class ConversationContext:
    recentMessages: list[dict[str, str]] = field(default_factory=list)
    entities: dict[str, str] = field(default_factory=dict)
    filters: dict[str, str] = field(default_factory=dict)
    lastIntent: str | None = None
    lastQuery: str | None = None
    activeFiles: list[dict[str, str]] = field(default_factory=list)
    awaitingClarification: dict[str, Any] | None = None


class RedisMemoryService:
    def __init__(self) -> None:
        self._settings = get_settings()
        self._redis: Redis | None = None
        self._fallback: dict[str, str] = {}

    def set_client(self, client: Redis | None) -> None:
        self._redis = client

    async def get_context(self, *, user_id: str, session_id: str) -> ConversationContext:
        key = self._context_key(user_id, session_id)
        payload = await self._get(key)
        if payload is None:
            return ConversationContext()
        data = json.loads(payload)
        return ConversationContext(
            recentMessages=data.get("recentMessages", []),
            entities=data.get("entities", {}),
            filters=data.get("filters", {}),
            lastIntent=data.get("lastIntent"),
            lastQuery=data.get("lastQuery"),
            activeFiles=data.get("activeFiles", []),
            awaitingClarification=data.get("awaitingClarification"),
        )

    async def save_context(self, *, user_id: str, session_id: str, context: ConversationContext) -> None:
        key = self._context_key(user_id, session_id)
        await self._set(key, json.dumps(asdict(context)))

    async def get_user_memory(self, user_id: str) -> dict:
        key = f"user-memory:{user_id}"
        payload = await self._get(key)
        return json.loads(payload) if payload else {}

    async def update_user_memory(self, user_id: str, memory: dict) -> None:
        key = f"user-memory:{user_id}"
        await self._set(key, json.dumps(memory))

    async def _get(self, key: str) -> str | None:
        if self._redis is not None:
            raw = await self._redis.get(key)
            return raw.decode("utf-8") if isinstance(raw, bytes) else raw
        return self._fallback.get(key)

    async def _set(self, key: str, value: str) -> None:
        if self._redis is not None:
            await self._redis.set(key, value, ex=self._settings.redis_ttl_seconds)
            return
        self._fallback[key] = value

    @staticmethod
    def _context_key(user_id: str, session_id: str) -> str:
        return f"ctx:{user_id}:{session_id}"


redis_memory_service = RedisMemoryService()
