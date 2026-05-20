from __future__ import annotations

import re
from typing import Any

from sqlalchemy import text

from app.db.session import get_db_session
from app.services.data_contract.joins import MISSING_RELATIONS
from app.services.data_contract.metrics import METRICS
from app.services.data_contract.owners import AI_DIRECT_WRITE_TABLES, DATA_OWNERS
from app.services.data_contract.tables import (
    ANALYTICS_ALLOWED_TABLES,
    ANALYTICS_SENSITIVE_COLUMNS,
    SENSITIVE_COLUMNS,
    TABLES,
    TABLES_WITH_IS_ACTIVE,
)


FEATURE_TABLES = {
    "recommendation": (
        "users",
        "profiles",
        "courses",
        "chapters",
        "lessons",
        "enrollments",
        "progress",
        "ratings",
        "course_skills",
        "skills",
        "learning_paths",
        "learning_path_courses",
        "path_progress",
        "course_tags",
        "tags",
    ),
    "learning_path_generation": (
        "users",
        "profiles",
        "courses",
        "learning_paths",
        "learning_path_courses",
        "path_progress",
        "course_skills",
        "skills",
    ),
    "exercise_generation": ("courses", "chapters", "lessons", "exercises", "exercise_test_cases"),
    "file_analysis": ("files", "file_usage"),
    "analytics": (
        "courses",
        "chapters",
        "lessons",
        "enrollments",
        "progress",
        "ratings",
        "analytics",
        "transactions",
        "transaction_items",
        "payments",
    ),
    "chat_history": ("chat_sessions", "chat_messages"),
    "draft_approval": ("ai_generation_tasks",),
}


def _quote_identifier(name: str) -> str:
    if not re.fullmatch(r"[a-z_][a-z0-9_]*", name):
        raise ValueError(f"Unsafe SQL identifier: {name}")
    return f'"{name}"'


def summarize_data_contract() -> dict[str, Any]:
    return {
        "tables": {
            name: {
                "owner": contract.owner,
                "columns": list(contract.columns),
                "piiColumns": list(contract.pii_columns),
                "analyticsSafe": contract.analytics_safe,
                "aiReadable": contract.ai_readable,
                "aiWritable": contract.ai_writable,
            }
            for name, contract in TABLES.items()
        },
        "dataOwners": {owner: list(tables) for owner, tables in DATA_OWNERS.items()},
        "analyticsAllowedTables": list(ANALYTICS_ALLOWED_TABLES),
        "analyticsSensitiveColumns": list(ANALYTICS_SENSITIVE_COLUMNS),
        "sensitiveColumns": list(SENSITIVE_COLUMNS),
        "aiDirectWriteTables": list(AI_DIRECT_WRITE_TABLES),
        "metrics": {
            name: {
                "tables": list(metric.tables),
                "grain": metric.grain,
                "description": metric.description,
            }
            for name, metric in METRICS.items()
        },
        "knownMissingRelations": MISSING_RELATIONS,
    }


async def validate_data_contract() -> dict[str, Any]:
    expected_tables = set(TABLES)
    expected_columns = {name: set(contract.columns) for name, contract in TABLES.items()}

    async with get_db_session() as session:
        result = await session.execute(
            text(
                """
                SELECT table_name, column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                """
            )
        )
        actual_columns: dict[str, set[str]] = {}
        for row in result.mappings().all():
            actual_columns.setdefault(str(row["table_name"]), set()).add(str(row["column_name"]))

        missing_tables = sorted(expected_tables.difference(actual_columns))
        missing_columns = {
            table: sorted(columns.difference(actual_columns.get(table, set())))
            for table, columns in expected_columns.items()
            if table in actual_columns and columns.difference(actual_columns.get(table, set()))
        }

        row_counts: dict[str, int | None] = {}
        for table in sorted(expected_tables.intersection(actual_columns)):
            try:
                where = " WHERE is_active = 'Y'" if table in TABLES_WITH_IS_ACTIVE else ""
                count_result = await session.execute(text(f"SELECT COUNT(*) AS total FROM {_quote_identifier(table)}{where}"))
                row_counts[table] = int(count_result.scalar_one() or 0)
            except Exception:
                row_counts[table] = None

    feature_readiness = {
        feature: {
            "ready": all(table not in missing_tables and not missing_columns.get(table) for table in tables),
            "tables": list(tables),
            "missingTables": [table for table in tables if table in missing_tables],
            "tablesWithMissingColumns": {
                table: missing_columns[table] for table in tables if table in missing_columns
            },
            "rowCounts": {table: row_counts.get(table) for table in tables},
        }
        for feature, tables in FEATURE_TABLES.items()
    }

    success = not missing_tables and not missing_columns
    return {
        "success": success,
        "status": "DATA_CONTRACT_OK" if success else "DATA_CONTRACT_MISMATCH",
        "missingTables": missing_tables,
        "missingColumns": missing_columns,
        "rowCounts": row_counts,
        "featureReadiness": feature_readiness,
        **summarize_data_contract(),
    }
