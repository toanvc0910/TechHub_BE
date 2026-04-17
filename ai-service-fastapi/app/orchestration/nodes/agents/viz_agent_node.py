from __future__ import annotations

from typing import Any

from app.orchestration.state.orchestrator_state import OrchestratorState, trace_step
from app.services.analytics_service import analytics_service


class VizAgentNode:
    def supports(self, intent: str, sub_intent: str | None = None) -> bool:
        del sub_intent
        return intent == "visualization"

    async def execute(self, state: OrchestratorState) -> dict[str, Any]:
        query_result = state.get("query_result")
        if query_result is None:
            query_result = await analytics_service.execute(
                state["user_input"],
                state.get("entities", {}),
                request_context=state.get("request_context"),
                user_id=state["user_id"],
            )

        rows = query_result.get("rows", [])
        chart_type = query_result.get("chartType") or "bar"
        chart_spec = self._build_chart_spec(rows, chart_type, query_result.get("title") or "TechHub analytics")
        citations = [
            {
                "kind": "visualization",
                "title": query_result.get("title"),
                "chartType": chart_spec.get("type"),
                "metric": query_result.get("metric"),
            }
        ]
        if chart_spec.get("data", {}).get("labels"):
            final_response = (
                query_result.get("summary")
                or "Toi da chuan bi du lieu va chart spec de frontend render bieu do."
            )
        else:
            final_response = "Khong du du lieu so de tao bieu do phu hop."

        trace_step(state, "viz_agent", "Created chart spec from analytics result.", chartType=chart_spec.get("type"))
        return {
            "query_result": query_result,
            "chart_spec": chart_spec,
            "citations": citations,
            "final_response": final_response,
            "execution_trace": list(state.get("execution_trace", [])),
        }

    def _build_chart_spec(self, rows: list[dict[str, Any]], chart_type: str, title: str) -> dict[str, Any]:
        if not rows:
            return {"type": chart_type, "title": title, "data": {"labels": [], "datasets": []}}

        sample = rows[0]
        label_key = next((key for key, value in sample.items() if isinstance(value, str)), None)
        numeric_keys = [
            key
            for key, value in sample.items()
            if isinstance(value, (int, float)) and not isinstance(value, bool)
        ]

        if label_key is None:
            label_key = next(iter(sample.keys()))
        if not numeric_keys:
            return {"type": chart_type, "title": title, "data": {"labels": [], "datasets": []}}

        labels = [str(row.get(label_key) or f"row-{index + 1}") for index, row in enumerate(rows)]
        datasets = [
            {
                "label": key,
                "values": [float(row.get(key) or 0.0) for row in rows],
            }
            for key in numeric_keys
        ]
        return {
            "type": chart_type,
            "title": title,
            "data": {
                "labels": labels,
                "datasets": datasets,
            },
        }


viz_agent_node = VizAgentNode()
