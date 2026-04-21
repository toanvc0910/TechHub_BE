from __future__ import annotations

from numbers import Number
from typing import Any

from app.orchestration.state.orchestrator_state import OrchestratorState, trace_step
from app.schemas.analytics_contract import DEFAULT_COLOR_PALETTE
from app.services.analytics_service import analytics_service


class VizAgentNode:
    def supports(self, intent: str, sub_intent: str | None = None) -> bool:
        del sub_intent
        return intent == "visualization"

    async def execute(self, state: OrchestratorState) -> dict[str, Any]:
        query_result = state.get("query_result")
        if query_result is None:
            request_context = state.get("request_context") or {}
            prior_analysis = (
                request_context.get("activeAnalysis")
                if isinstance(request_context, dict)
                else None
            )
            query_result = await analytics_service.execute(
                state["user_input"],
                state.get("entities", {}),
                request_context=state.get("request_context"),
                user_id=state["user_id"],
                prior_analysis=prior_analysis if isinstance(prior_analysis, dict) else None,
            )

        rows = query_result.get("rows", []) or []
        chart_type = query_result.get("chartType") or "bar"
        chart_spec = self._build_chart_spec(
            rows=rows,
            chart_type=chart_type,
            title=str(query_result.get("title") or "TechHub analytics"),
            subtitle=str(query_result.get("summary") or "").strip() or None,
            scope=query_result.get("scope"),
            scope_label=query_result.get("scopeLabel"),
            chart_options=query_result.get("chartOptions") or {},
        )
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

    @staticmethod
    def _build_chart_spec(
        *,
        rows: list[dict[str, Any]],
        chart_type: str,
        title: str,
        subtitle: str | None,
        scope: Any,
        scope_label: Any,
        chart_options: dict[str, Any],
    ) -> dict[str, Any]:
        """Produce a chart spec that matches the FE `ChartSpec` contract."""
        base_options = {
            "availableChartTypes": list(chart_options.get("availableChartTypes", [])),
            "colorPalette": list(chart_options.get("colorPalette", DEFAULT_COLOR_PALETTE)),
            "emptyState": chart_options.get("emptyState", "ok"),
            "valueAxisLabel": chart_options.get("valueAxisLabel"),
            "categoryAxisLabel": chart_options.get("categoryAxisLabel"),
            "stacked": bool(chart_options.get("stacked", False)),
            "legend": bool(chart_options.get("legend", True)),
        }
        scope_str = str(scope) if scope else None
        scope_label_str = str(scope_label) if scope_label else None

        if not rows:
            return {
                "type": chart_type,
                "title": title,
                "subtitle": subtitle,
                "scope": scope_str,
                "scopeLabel": scope_label_str,
                "data": {"labels": [], "datasets": []},
                "options": base_options,
                "note": None,
            }

        sample = rows[0]
        label_key = next((key for key, value in sample.items() if isinstance(value, str)), None)
        numeric_keys = [
            key
            for key, value in sample.items()
            if isinstance(value, Number) and not isinstance(value, bool)
        ]

        if label_key is None:
            label_key = next(iter(sample.keys()))
        if not numeric_keys:
            return {
                "type": chart_type,
                "title": title,
                "subtitle": subtitle,
                "scope": scope_str,
                "scopeLabel": scope_label_str,
                "data": {"labels": [], "datasets": []},
                "options": base_options,
                "note": "No numeric columns available to plot.",
            }

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
            "subtitle": subtitle,
            "scope": scope_str,
            "scopeLabel": scope_label_str,
            "data": {
                "labels": labels,
                "datasets": datasets,
            },
            "options": base_options,
            "note": None,
        }


viz_agent_node = VizAgentNode()
