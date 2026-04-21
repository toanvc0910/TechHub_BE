from __future__ import annotations

from typing import Any, Protocol

from app.orchestration.state.orchestrator_state import OrchestratorState


class AgentNode(Protocol):
    def supports(self, intent: str, sub_intent: str | None = None) -> bool:
        ...

    async def execute(self, state: OrchestratorState) -> dict[str, Any]:
        ...
