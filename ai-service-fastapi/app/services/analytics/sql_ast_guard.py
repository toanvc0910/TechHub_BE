"""
AST-based safety guard for the analytics SQL planner output.

Replaces the previous regex-only checks (which could be fooled by simple
expression nesting like `concat(email, '')` or comments). sqlglot parses the
SQL into a real syntax tree so we can:

  - enumerate every table referenced (including subqueries and CTEs),
  - enumerate every column referenced anywhere in the SELECT projection list
    (regardless of how it is wrapped: `email`, `LOWER(email)`, `concat(u.email,
    '...')` all surface `email`),
  - detect forbidden statement types (UPDATE/DELETE/CREATE/...).

The guard is intentionally strict: if sqlglot cannot parse the SQL it raises,
because the existing regex layer cannot verify safety either. This is fine —
the analytics planner produces deterministic SQL from templates plus a tightly
controlled LLM fallback.
"""

from __future__ import annotations

from dataclasses import dataclass

import sqlglot
from sqlglot import expressions as exp


class SqlAstGuardError(ValueError):
    """Raised when the parsed SQL violates safety rules."""


def _resolve_forbidden_types() -> tuple[type, ...]:
    # sqlglot does not expose every statement node on every version; we look
    # up by name and skip anything missing. The textual scan in
    # analytics_service still blocks raw keywords as a backstop.
    names = ("Update", "Delete", "Insert", "Drop", "Alter", "Create", "Truncate", "Command")
    out: list[type] = []
    for name in names:
        node = getattr(exp, name, None)
        if isinstance(node, type):
            out.append(node)
    return tuple(out)


_FORBIDDEN_STATEMENT_TYPES: tuple[type, ...] = _resolve_forbidden_types()


@dataclass(frozen=True, slots=True)
class ParsedSql:
    expression: exp.Expression
    tables: frozenset[str]
    projected_columns: frozenset[str]


def parse_and_inspect(sql: str) -> ParsedSql:
    """Parse `sql` (Postgres dialect) and return the tables + projected
    columns used. Raises `SqlAstGuardError` on parse failure or forbidden
    statement type.
    """
    if not isinstance(sql, str) or not sql.strip():
        raise SqlAstGuardError("SQL is empty.")
    try:
        parsed = sqlglot.parse_one(sql, dialect="postgres")
    except sqlglot.errors.ParseError as exc:
        raise SqlAstGuardError(f"SQL parse error: {exc}") from exc
    if parsed is None:
        raise SqlAstGuardError("SQL parse returned no tree.")

    for forbidden in _FORBIDDEN_STATEMENT_TYPES:
        if list(parsed.find_all(forbidden)):
            raise SqlAstGuardError(f"Forbidden statement type: {forbidden.__name__}")

    # Allow either a Select or a With wrapping a Select.
    root_select = parsed if isinstance(parsed, exp.Select) else parsed.find(exp.Select)
    if root_select is None:
        raise SqlAstGuardError("SQL does not contain a SELECT.")

    tables = {table.name.lower() for table in parsed.find_all(exp.Table) if table.name}

    # Projected columns from every SELECT in the tree — captures CTE bodies
    # and subqueries too, which is what we want for PII enforcement: an inner
    # subquery that projects `email` and the outer query aliases it through
    # would still be a leak.
    projected: set[str] = set()
    for select_node in parsed.find_all(exp.Select):
        for projection in select_node.expressions or []:
            for column in projection.find_all(exp.Column):
                name = column.this.name if column.this is not None else ""
                if name:
                    projected.add(name.lower())
            # Also catch bare aliases like `SELECT email AS x` where the
            # column appears as the lhs of an Alias.
            if isinstance(projection, exp.Alias):
                inner = projection.this
                if isinstance(inner, exp.Column) and inner.this is not None:
                    projected.add(inner.this.name.lower())

    return ParsedSql(
        expression=parsed,
        tables=frozenset(tables),
        projected_columns=frozenset(projected),
    )


def assert_no_pii(parsed: ParsedSql, sensitive_columns: set[str]) -> None:
    """Raise if any projected column intersects `sensitive_columns`. Columns
    are compared case-insensitively.
    """
    sensitive_lower = {c.lower() for c in sensitive_columns}
    leaked = sorted(parsed.projected_columns.intersection(sensitive_lower))
    if leaked:
        raise SqlAstGuardError(
            f"Query projects PII columns without authorization: {leaked}"
        )


def assert_tables_allowed(parsed: ParsedSql, allowed_tables: set[str]) -> None:
    """Raise if any referenced table is outside `allowed_tables`."""
    unknown = sorted(parsed.tables.difference({t.lower() for t in allowed_tables}))
    if unknown:
        raise SqlAstGuardError(f"Query references unsupported tables: {unknown}")
