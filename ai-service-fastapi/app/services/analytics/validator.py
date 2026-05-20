from __future__ import annotations

import re
from typing import Any

from app.services.analytics.metric_registry import get_metric
from app.services.data_contract import TABLES


def validate_metric_plan(
    metric_key: str | None,
    *,
    sql: str,
    tables: set[str],
    policy: dict[str, Any],
    scope: str,
) -> None:
    definition = get_metric(metric_key)
    if definition is None:
        return

    unsupported_tables = tables.difference(definition.tables)
    if unsupported_tables:
        raise ValueError(
            f"Metric '{definition.key}' cannot query tables outside its registry: {sorted(unsupported_tables)}"
        )

    user_role = str(policy.get("userRole") or "USER").upper()
    if user_role not in definition.required_roles:
        raise ValueError(f"Role '{user_role}' cannot access metric '{definition.key}'.")

    if scope not in definition.allowed_scopes:
        raise ValueError(f"Metric '{definition.key}' does not support scope '{scope}'.")

    if scope == "personal" and ":user_id" not in sql.lower():
        raise ValueError(f"Personal metric '{definition.key}' must filter by trusted :user_id.")

    if "user_id" in definition.required_params and ":user_id" not in sql.lower():
        raise ValueError(f"Metric '{definition.key}' requires trusted :user_id.")

    if user_role in definition.owner_filtered_for_roles and "instructor_id = :user_id" not in sql.lower():
        raise ValueError(f"Metric '{definition.key}' must filter instructor-owned rows by trusted :user_id.")

    if re.search(r"select\s+\*", sql, flags=re.I):
        raise ValueError("Analytics metric SQL cannot use SELECT *.")

    if not policy.get("piiAccess", False):
        blocked = _collect_pii_selected_columns(sql)
        if blocked:
            raise ValueError(f"Metric SQL selects restricted columns without PII access: {sorted(blocked)}")


def _collect_pii_selected_columns(sql: str) -> set[str]:
    select_match = re.search(r"select\s+(.*?)\s+from\s", sql, flags=re.I | re.S)
    if not select_match:
        return set()
    select_clause = select_match.group(1)
    alias_to_table = _collect_table_aliases(sql)
    blocked: set[str] = set()
    for alias, column in re.findall(r"\b([a-z_][a-z0-9_]*)\.\"?([a-z_][a-z0-9_]*)\"?", select_clause, flags=re.I):
        table_name = alias_to_table.get(alias.lower(), alias.lower())
        contract = TABLES.get(table_name)
        if contract and column.lower() in {item.lower() for item in contract.pii_columns}:
            blocked.add(column.lower())
    return blocked


def _collect_table_aliases(sql: str) -> dict[str, str]:
    aliases: dict[str, str] = {}
    for table, alias in re.findall(
        r"\b(?:from|join)\s+([a-z_][a-z0-9_]*)(?:\s+(?:as\s+)?([a-z_][a-z0-9_]*))?",
        sql,
        flags=re.I,
    ):
        table_name = table.lower()
        aliases[table_name] = table_name
        if alias:
            alias_lower = alias.lower()
            if alias_lower not in {"on", "where", "left", "right", "inner", "join", "group", "order"}:
                aliases[alias_lower] = table_name
    return aliases
