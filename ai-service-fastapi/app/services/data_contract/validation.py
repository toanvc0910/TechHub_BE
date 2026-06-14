from __future__ import annotations

import re
from typing import Any

from sqlalchemy import text

from app.db.session import get_db_session
from app.services.data_contract.runtime import data_contract_registry


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
    "integrated_blog": ("blogs", "users", "courses", "chapters", "lessons"),
    "chat_history": ("chat_sessions", "chat_messages"),
    "draft_approval": ("ai_generation_tasks",),
}


def _quote_identifier(name: str) -> str:
    if not re.fullmatch(r"[a-z_][a-z0-9_]*", name):
        raise ValueError(f"Unsafe SQL identifier: {name}")
    return f'"{name}"'


async def summarize_data_contract() -> dict[str, Any]:
    contract = await data_contract_registry.get_contract()
    return contract.summary()


async def validate_data_contract() -> dict[str, Any]:
    contract = await data_contract_registry.get_contract()
    expected_tables = set(contract.tables)
    expected_columns = {name: set(table_contract.columns) for name, table_contract in contract.tables.items()}

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
                where = " WHERE is_active = 'Y'" if table in contract.tables_with_is_active else ""
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
        "versionKey": contract.version_key,
        "source": contract.source,
        "loadedFromDb": contract.loaded_from_db,
        "loadErrors": list(contract.load_errors),
        "missingTables": missing_tables,
        "missingColumns": missing_columns,
        "rowCounts": row_counts,
        "featureReadiness": feature_readiness,
        **contract.summary(),
    }
