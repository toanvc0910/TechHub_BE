from __future__ import annotations

from contextlib import contextmanager
from contextvars import ContextVar
from dataclasses import dataclass, field
from typing import Any
from uuid import uuid4


@dataclass(slots=True)
class RuntimeRequestState:
    request_id: str
    scope: str
    prompt_tokens: int = 0
    completion_tokens: int = 0
    embedding_tokens: int = 0
    llm_calls: int = 0
    metadata: dict[str, Any] = field(default_factory=dict)

    @property
    def total_tokens(self) -> int:
        return self.prompt_tokens + self.completion_tokens + self.embedding_tokens


class RuntimeRequestContextService:
    def __init__(self) -> None:
        self._state: ContextVar[RuntimeRequestState | None] = ContextVar("runtime_request_state", default=None)

    @contextmanager
    def begin(self, *, scope: str, request_id: str | None = None):
        state = RuntimeRequestState(request_id=request_id or str(uuid4()), scope=scope)
        token = self._state.set(state)
        try:
            yield state
        finally:
            self._state.reset(token)

    def current(self) -> RuntimeRequestState | None:
        return self._state.get()

    def current_request_id(self) -> str | None:
        state = self.current()
        return state.request_id if state else None

    def add_chat_usage(self, *, prompt_tokens: int, completion_tokens: int) -> None:
        state = self.current()
        if state is None:
            return
        state.prompt_tokens += max(prompt_tokens, 0)
        state.completion_tokens += max(completion_tokens, 0)
        state.llm_calls += 1

    def add_embedding_usage(self, *, tokens: int) -> None:
        state = self.current()
        if state is None:
            return
        state.embedding_tokens += max(tokens, 0)

    def attach_metadata(self, **metadata: Any) -> None:
        state = self.current()
        if state is None:
            return
        state.metadata.update(metadata)

    def snapshot(self) -> dict[str, Any]:
        state = self.current()
        if state is None:
            return {
                "requestId": None,
                "scope": None,
                "promptTokens": 0,
                "completionTokens": 0,
                "embeddingTokens": 0,
                "totalTokens": 0,
                "llmCalls": 0,
            }
        return {
            "requestId": state.request_id,
            "scope": state.scope,
            "promptTokens": state.prompt_tokens,
            "completionTokens": state.completion_tokens,
            "embeddingTokens": state.embedding_tokens,
            "totalTokens": state.total_tokens,
            "llmCalls": state.llm_calls,
            **state.metadata,
        }


runtime_request_context_service = RuntimeRequestContextService()
