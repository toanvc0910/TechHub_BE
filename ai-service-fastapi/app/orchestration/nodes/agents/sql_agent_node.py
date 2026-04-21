from __future__ import annotations

from typing import Any

from app.orchestration.state.orchestrator_state import OrchestratorState, trace_step
from app.services.analytics_service import analytics_service
from app.services.runtime_policy_service import runtime_policy_service


class SqlAgentNode:
    def supports(self, intent: str, sub_intent: str | None = None) -> bool:
        del sub_intent
        return intent in {"data_query", "visualization"}

    async def execute(self, state: OrchestratorState) -> dict[str, Any]:
        policy = await runtime_policy_service.resolve(state.get("request_context"))
        request_context = state.get("request_context") or {}
        prior_analysis = (
            request_context.get("activeAnalysis")
            if isinstance(request_context, dict)
            else None
        )
        result = await analytics_service.execute(
            state["user_input"],
            state.get("entities", {}),
            request_context=state.get("request_context"),
            user_id=state["user_id"],
            prior_analysis=prior_analysis if isinstance(prior_analysis, dict) else None,
        )
        citations = [
            {
                "kind": "analytics_query",
                "title": result.get("title"),
                "metric": result.get("metric"),
                "tables": result.get("tables", []),
                "sql": result.get("sql"),
            }
        ]
        trace_step(
            state,
            "sql_agent",
            "Executed analytics query from user intent.",
            metric=result.get("metric"),
            rowCount=len(result.get("rows", [])),
            sqlMaxRows=policy.get("sqlMaxRows"),
        )
        return {
            "policy_context": policy,
            "query_result": result,
            "citations": citations,
            "final_response": result.get("summary") or "Toi da tong hop du lieu analytics cho yeu cau cua ban.",
            "execution_trace": list(state.get("execution_trace", [])),
        }


sql_agent_node = SqlAgentNode()
