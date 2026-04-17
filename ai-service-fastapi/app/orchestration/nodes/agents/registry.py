from __future__ import annotations

from app.orchestration.nodes.agents.conversation_agent_node import conversation_agent_node
from app.orchestration.nodes.agents.file_agent_node import file_agent_node
from app.orchestration.nodes.agents.rag_response_node import rag_response_node
from app.orchestration.nodes.agents.rag_retriever_node import rag_retriever_node
from app.orchestration.nodes.agents.sql_agent_node import sql_agent_node
from app.orchestration.nodes.agents.viz_agent_node import viz_agent_node


class AgentRegistry:
    def __init__(self) -> None:
        self._agents = [
            conversation_agent_node,
            rag_retriever_node,
            rag_response_node,
            sql_agent_node,
            file_agent_node,
            viz_agent_node,
        ]

    def resolve_primary(self, intent: str):
        if intent in {"recommendation", "knowledge"}:
            return [rag_retriever_node, rag_response_node]
        if intent == "visualization":
            return [sql_agent_node, viz_agent_node]
        for agent in self._agents:
            if agent.supports(intent):
                return [agent]
        return [conversation_agent_node]


agent_registry = AgentRegistry()
