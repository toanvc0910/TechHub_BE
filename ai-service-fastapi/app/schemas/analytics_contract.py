"""Pydantic models + helpers defining the analytics response contract.

This is the single source of truth for the shape of `queryResult`,
`chartSpec`, `chartOptions`, and the related payloads emitted by the
analytics service. Both the live `done` metadata and the streaming
`artifact` event must conform to the same shapes so the FE never has to
guess the structure.
"""

from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, Field

# ---------------------------------------------------------------------------
# Constants / enums (stringly-typed for JSON interop)
# ---------------------------------------------------------------------------

SUPPORTED_CHART_TYPES: tuple[str, ...] = ("bar", "line", "pie")
SUPPORTED_SCOPES: tuple[str, ...] = ("personal", "platform")
SUPPORTED_COLUMN_KINDS: tuple[str, ...] = (
    "category",
    "numeric",
    "percentage",
    "currency",
    "duration_seconds",
    "datetime",
    "identifier",
    "text",
    "boolean",
)
SUPPORTED_EMPTY_STATES: tuple[str, ...] = ("ok", "empty", "all_zero", "single_category")

# Default color palette kept in sync with the FE recharts rendering so custom
# palettes emitted by the planner stay consistent with the in-chat preview.
DEFAULT_COLOR_PALETTE: tuple[str, ...] = (
    "#3b82f6",
    "#8b5cf6",
    "#06b6d4",
    "#f59e0b",
    "#ef4444",
    "#10b981",
)

SUGGESTED_ACTION_KINDS: tuple[str, ...] = (
    "prompt",
    "change_chart_type",
    "export_csv",
    "copy_sql",
    "refine_filter",
)


# ---------------------------------------------------------------------------
# Pydantic models
# ---------------------------------------------------------------------------


class ColumnMeta(BaseModel):
    """Format hint for a single column in the result set."""

    name: str
    kind: str = Field(default="text")
    unit: str | None = None
    format: str | None = None  # e.g. "0,0.00%", "%Y-%m-%d", "USD"
    description: str | None = None


class RuntimePolicySnapshot(BaseModel):
    userRole: str | None = None
    sqlMaxRows: int | None = None
    piiAccess: bool | None = None


class LogicSummary(BaseModel):
    metric: str
    title: str
    chartType: str
    timeRange: str
    scope: str
    scopeLabel: str
    executionMode: str
    rowCount: int


class ChartOptions(BaseModel):
    """Hints the FE uses to render and interact with the chart.

    `availableChartTypes` lets the FE show a switcher without guessing which
    chart types the current data can support. `emptyState` is a high-level
    hint so the FE can render "all values are zero" or "only one category"
    states consistently across previews.
    """

    availableChartTypes: list[str] = Field(default_factory=list)
    colorPalette: list[str] = Field(default_factory=lambda: list(DEFAULT_COLOR_PALETTE))
    emptyState: Literal["ok", "empty", "all_zero", "single_category"] = "ok"
    valueAxisLabel: str | None = None
    categoryAxisLabel: str | None = None
    stacked: bool = False
    legend: bool = True


class ChartDataset(BaseModel):
    label: str | None = None
    values: list[float] = Field(default_factory=list)


class ChartData(BaseModel):
    labels: list[str] = Field(default_factory=list)
    datasets: list[ChartDataset] = Field(default_factory=list)


class ChartSpec(BaseModel):
    type: str = "bar"
    title: str = "TechHub analytics"
    subtitle: str | None = None
    scope: str | None = None
    scopeLabel: str | None = None
    data: ChartData = Field(default_factory=ChartData)
    options: ChartOptions = Field(default_factory=ChartOptions)
    note: str | None = None


class SuggestedAction(BaseModel):
    id: str
    label: str
    description: str | None = None
    kind: str
    prompt: str | None = None
    payload: dict[str, Any] | None = None
    icon: str | None = None
    tone: Literal["primary", "secondary"] = "secondary"


class QueryResult(BaseModel):
    """Authoritative shape of an analytics response on the BE.

    Every field here is emitted unconditionally so FE can trust the shape
    between streaming (`artifact` event) and non-streaming (`done` metadata)
    deliveries.
    """

    metric: str = "analytics"
    timeRange: str = "all_time"
    title: str = "TechHub analytics"
    summary: str = ""
    rows: list[dict[str, Any]] = Field(default_factory=list)
    rowCount: int = 0
    columns: list[str] = Field(default_factory=list)
    columnMeta: list[ColumnMeta] = Field(default_factory=list)
    tables: list[str] = Field(default_factory=list)
    sql: str = ""
    chartType: str = "bar"
    executionMode: str = "llm_planner"
    explanation: str = ""
    logicSummary: LogicSummary | None = None
    scope: str = "platform"
    scopeLabel: str = ""
    policy: RuntimePolicySnapshot = Field(default_factory=RuntimePolicySnapshot)
    suggestedActions: list[SuggestedAction] = Field(default_factory=list)
    chartOptions: ChartOptions = Field(default_factory=ChartOptions)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def detect_empty_state(
    rows: list[dict[str, Any]],
    numeric_values: list[float],
    category_count: int,
) -> str:
    if not rows or category_count == 0:
        return "empty"
    if numeric_values and all(value == 0 for value in numeric_values):
        return "all_zero"
    if category_count == 1:
        return "single_category"
    return "ok"


def build_available_chart_types(dataset_count: int, category_count: int) -> list[str]:
    # Multi-series data doesn't render cleanly as pie, so we drop it.
    if dataset_count <= 1 and category_count > 0:
        return list(SUPPORTED_CHART_TYPES)
    return ["bar", "line"] if category_count > 0 else []


def infer_column_kind(name: str, sample_value: Any) -> str:
    lowered = (name or "").lower()
    if any(token in lowered for token in ("percent", "percentage", "rate", "ratio", "completion", "progress")):
        return "percentage"
    if any(token in lowered for token in ("price", "amount", "revenue", "cost", "usd", "vnd")):
        return "currency"
    if any(token in lowered for token in ("duration", "elapsed", "minutes", "seconds")):
        return "duration_seconds"
    if lowered in {"id"} or lowered.endswith("_id"):
        return "identifier"
    if lowered in {"created", "updated", "started", "ended"} or lowered.endswith("_at"):
        return "datetime"
    if isinstance(sample_value, bool):
        return "boolean"
    if isinstance(sample_value, (int, float)):
        return "numeric"
    if isinstance(sample_value, str):
        return "category"
    return "text"


def build_column_meta(rows: list[dict[str, Any]], columns: list[str]) -> list[ColumnMeta]:
    if not columns:
        return []
    sample = rows[0] if rows else {}
    metas: list[ColumnMeta] = []
    for column in columns:
        sample_value = sample.get(column) if isinstance(sample, dict) else None
        kind = infer_column_kind(column, sample_value)
        unit: str | None = None
        format_hint: str | None = None
        if kind == "percentage":
            unit = "%"
            format_hint = "0.[00]%"
        elif kind == "currency":
            unit = "USD"
            format_hint = "0,0.00"
        elif kind == "duration_seconds":
            unit = "seconds"
        metas.append(
            ColumnMeta(
                name=column,
                kind=kind,
                unit=unit,
                format=format_hint,
            )
        )
    return metas


__all__ = [
    "SUPPORTED_CHART_TYPES",
    "SUPPORTED_SCOPES",
    "SUPPORTED_COLUMN_KINDS",
    "SUPPORTED_EMPTY_STATES",
    "DEFAULT_COLOR_PALETTE",
    "SUGGESTED_ACTION_KINDS",
    "ColumnMeta",
    "RuntimePolicySnapshot",
    "LogicSummary",
    "ChartOptions",
    "ChartDataset",
    "ChartData",
    "ChartSpec",
    "SuggestedAction",
    "QueryResult",
    "detect_empty_state",
    "build_available_chart_types",
    "build_column_meta",
    "infer_column_kind",
]
