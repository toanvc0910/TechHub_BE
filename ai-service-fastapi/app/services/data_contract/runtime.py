from __future__ import annotations

import json
import time
from dataclasses import dataclass
from typing import Any

from sqlalchemy import text

from app.core.config import get_settings
from app.db.session import get_db_session
from app.services.analytics.metric_registry import METRIC_REGISTRY
from app.services.data_contract.joins import ANALYTICS_JOIN_HINTS, JOINS, MISSING_RELATIONS, JoinContract
from app.services.data_contract.metrics import METRICS
from app.services.data_contract.owners import DATA_OWNERS
from app.services.data_contract.tables import (
    TABLES,
    TableContract,
    quote_column,
)


@dataclass(frozen=True, slots=True)
class RuntimeMetricContract:
    name: str
    title: str
    description: str
    grain: str
    tables: tuple[str, ...]
    allowed_scopes: tuple[str, ...] = ()
    required_roles: tuple[str, ...] = ()
    required_params: tuple[str, ...] = ()
    owner_filtered_for_roles: tuple[str, ...] = ()
    default_chart: str = "bar"


@dataclass(frozen=True, slots=True)
class RuntimeDataContract:
    version_key: str
    source: str
    loaded_from_db: bool
    tables: dict[str, TableContract]
    joins: tuple[JoinContract, ...]
    metrics: dict[str, RuntimeMetricContract]
    data_owners: dict[str, tuple[str, ...]]
    known_missing_relations: dict[str, tuple[str, ...] | str]
    load_errors: tuple[str, ...] = ()

    @property
    def analytics_allowed_tables(self) -> tuple[str, ...]:
        return tuple(
            name for name, contract in self.tables.items() if contract.analytics_safe and contract.ai_readable
        )

    @property
    def ai_readable_tables(self) -> tuple[str, ...]:
        return tuple(name for name, contract in self.tables.items() if contract.ai_readable)

    @property
    def ai_writable_tables(self) -> tuple[str, ...]:
        return tuple(name for name, contract in self.tables.items() if contract.ai_writable)

    @property
    def sensitive_columns(self) -> tuple[str, ...]:
        return tuple(sorted({column for table in self.tables.values() for column in table.pii_columns}))

    @property
    def analytics_sensitive_columns(self) -> tuple[str, ...]:
        return tuple(
            sorted(
                {
                    column
                    for table in self.tables.values()
                    if table.analytics_safe and table.ai_readable
                    for column in table.pii_columns
                }
            )
        )

    @property
    def tables_with_is_active(self) -> tuple[str, ...]:
        return tuple(name for name, table in self.tables.items() if "is_active" in table.columns)

    def render_analytics_schema_context(self) -> str:
        hints = [item.purpose for item in self.joins if _is_analytics_hint(item)]
        if not hints:
            hints = list(ANALYTICS_JOIN_HINTS)

        lines = ["Tables and safe columns from the TechHub data contract:"]
        for name in self.analytics_allowed_tables:
            contract = self.tables[name]
            safe_columns = ", ".join(quote_column(column) for column in contract.safe_columns)
            lines.append(f"- {name}({safe_columns})")
        lines.extend(
            [
                "",
                "Important join rules:",
                *[f"- {hint}" for hint in hints],
                "",
                "Only generate read-only PostgreSQL SQL. Use SELECT or WITH only.",
                "Prefer aggregations useful for analytics dashboards.",
                "When grouping, alias the category column as label and the numeric aggregation as value.",
                "Do not select raw personal columns unless runtime policy explicitly allows PII.",
            ]
        )
        return "\n".join(lines)

    def summary(self) -> dict[str, Any]:
        return {
            "versionKey": self.version_key,
            "source": self.source,
            "loadedFromDb": self.loaded_from_db,
            "loadErrors": list(self.load_errors),
            "tables": {
                name: {
                    "owner": contract.owner,
                    "columns": list(contract.columns),
                    "piiColumns": list(contract.pii_columns),
                    "analyticsSafe": contract.analytics_safe,
                    "aiReadable": contract.ai_readable,
                    "aiWritable": contract.ai_writable,
                }
                for name, contract in self.tables.items()
            },
            "dataOwners": {owner: list(tables) for owner, tables in self.data_owners.items()},
            "analyticsAllowedTables": list(self.analytics_allowed_tables),
            "analyticsSensitiveColumns": list(self.analytics_sensitive_columns),
            "sensitiveColumns": list(self.sensitive_columns),
            "aiDirectWriteTables": list(self.ai_writable_tables),
            "metrics": {
                name: {
                    "tables": list(metric.tables),
                    "grain": metric.grain,
                    "description": metric.description,
                    "allowedScopes": list(metric.allowed_scopes),
                    "requiredRoles": list(metric.required_roles),
                }
                for name, metric in self.metrics.items()
            },
            "joins": [
                {
                    "leftTable": join.left_table,
                    "leftColumn": join.left_column,
                    "rightTable": join.right_table,
                    "rightColumn": join.right_column,
                    "purpose": join.purpose,
                }
                for join in self.joins
            ],
            "knownMissingRelations": self.known_missing_relations,
        }


class DataContractRegistryService:
    def __init__(self) -> None:
        self._settings = get_settings()
        self._cache: RuntimeDataContract | None = None
        self._cache_expires_at = 0.0

    async def get_contract(self, *, force: bool = False) -> RuntimeDataContract:
        now = time.monotonic()
        if not force and self._cache is not None and now < self._cache_expires_at:
            return self._cache
        try:
            contract = await self._load_from_db()
        except Exception as exc:  # noqa: BLE001
            contract = self._static_contract(load_errors=(f"{type(exc).__name__}: {exc}"[:240],))
        self._cache = contract
        self._cache_expires_at = now + max(1, int(self._settings.data_contract_cache_ttl_seconds))
        return contract

    async def sync_static_contract(self) -> dict[str, Any]:
        version_key = self._settings.data_contract_version
        static_contract = self._static_contract()
        async with get_db_session() as session:
            await session.execute(
                text(
                    """
                    INSERT INTO ai_data_contract_versions (
                        version_key, status, description, source, activated_at, is_active
                    )
                    VALUES (
                        :version_key, 'ACTIVE', :description, 'AI_SERVICE_SYNC', CURRENT_TIMESTAMP, 'Y'
                    )
                    ON CONFLICT (version_key) DO UPDATE SET
                        status = 'ACTIVE',
                        description = EXCLUDED.description,
                        source = EXCLUDED.source,
                        activated_at = CURRENT_TIMESTAMP,
                        updated = CURRENT_TIMESTAMP,
                        is_active = 'Y'
                    """
                ),
                {
                    "version_key": version_key,
                    "description": "TechHub AI runtime data contract synced from the deployed service contract.",
                },
            )
            await session.execute(
                text(
                    "UPDATE ai_data_contract_versions SET status = 'RETIRED', updated = CURRENT_TIMESTAMP "
                    "WHERE version_key <> :version_key AND status = 'ACTIVE'"
                ),
                {"version_key": version_key},
            )
            await session.execute(text("UPDATE ai_data_contract_tables SET is_active = 'N' WHERE version_key = :version_key"), {"version_key": version_key})
            await session.execute(text("UPDATE ai_data_contract_relations SET is_active = 'N' WHERE version_key = :version_key"), {"version_key": version_key})
            await session.execute(text("UPDATE ai_data_contract_metrics SET is_active = 'N' WHERE version_key = :version_key"), {"version_key": version_key})

            table_count = 0
            for name, contract in static_contract.tables.items():
                await session.execute(
                    text(
                        """
                        INSERT INTO ai_data_contract_tables (
                            version_key, table_name, owner_service, columns, pii_columns,
                            analytics_safe, ai_readable, ai_writable, description, is_active
                        )
                        VALUES (
                            :version_key, :table_name, :owner_service, :columns, :pii_columns,
                            :analytics_safe, :ai_readable, :ai_writable, :description, 'Y'
                        )
                        ON CONFLICT (version_key, table_name) DO UPDATE SET
                            owner_service = EXCLUDED.owner_service,
                            columns = EXCLUDED.columns,
                            pii_columns = EXCLUDED.pii_columns,
                            analytics_safe = EXCLUDED.analytics_safe,
                            ai_readable = EXCLUDED.ai_readable,
                            ai_writable = EXCLUDED.ai_writable,
                            description = EXCLUDED.description,
                            updated = CURRENT_TIMESTAMP,
                            is_active = 'Y'
                        """
                    ),
                    {
                        "version_key": version_key,
                        "table_name": name,
                        "owner_service": contract.owner,
                        "columns": list(contract.columns),
                        "pii_columns": list(contract.pii_columns),
                        "analytics_safe": contract.analytics_safe,
                        "ai_readable": contract.ai_readable,
                        "ai_writable": contract.ai_writable,
                        "description": f"{contract.owner} table {name}",
                    },
                )
                table_count += 1

            relation_count = 0
            for join in static_contract.joins:
                await session.execute(
                    text(
                        """
                        INSERT INTO ai_data_contract_relations (
                            version_key, left_table, left_column, right_table, right_column,
                            relation_type, predicate, purpose, analytics_hint, is_active
                        )
                        VALUES (
                            :version_key, :left_table, :left_column, :right_table, :right_column,
                            'foreign_key', NULL, :purpose, :analytics_hint, 'Y'
                        )
                        ON CONFLICT (
                            version_key, left_table, left_column, right_table, right_column
                        ) DO UPDATE SET
                            relation_type = EXCLUDED.relation_type,
                            purpose = EXCLUDED.purpose,
                            analytics_hint = EXCLUDED.analytics_hint,
                            updated = CURRENT_TIMESTAMP,
                            is_active = 'Y'
                        """
                    ),
                    {
                        "version_key": version_key,
                        "left_table": join.left_table,
                        "left_column": join.left_column,
                        "right_table": join.right_table,
                        "right_column": join.right_column,
                        "purpose": join.purpose,
                        "analytics_hint": _is_analytics_hint(join),
                    },
                )
                relation_count += 1

            metric_count = 0
            for name, metric in static_contract.metrics.items():
                await session.execute(
                    text(
                        """
                        INSERT INTO ai_data_contract_metrics (
                            version_key, metric_name, title, description, grain, tables,
                            allowed_scopes, required_roles, required_params,
                            owner_filtered_for_roles, default_chart, is_active
                        )
                        VALUES (
                            :version_key, :metric_name, :title, :description, :grain, :tables,
                            :allowed_scopes, :required_roles, :required_params,
                            :owner_filtered_for_roles, :default_chart, 'Y'
                        )
                        ON CONFLICT (version_key, metric_name) DO UPDATE SET
                            title = EXCLUDED.title,
                            description = EXCLUDED.description,
                            grain = EXCLUDED.grain,
                            tables = EXCLUDED.tables,
                            allowed_scopes = EXCLUDED.allowed_scopes,
                            required_roles = EXCLUDED.required_roles,
                            required_params = EXCLUDED.required_params,
                            owner_filtered_for_roles = EXCLUDED.owner_filtered_for_roles,
                            default_chart = EXCLUDED.default_chart,
                            updated = CURRENT_TIMESTAMP,
                            is_active = 'Y'
                        """
                    ),
                    {
                        "version_key": version_key,
                        "metric_name": name,
                        "title": metric.title,
                        "description": metric.description,
                        "grain": metric.grain,
                        "tables": list(metric.tables),
                        "allowed_scopes": list(metric.allowed_scopes),
                        "required_roles": list(metric.required_roles),
                        "required_params": list(metric.required_params),
                        "owner_filtered_for_roles": list(metric.owner_filtered_for_roles),
                        "default_chart": metric.default_chart,
                    },
                )
                metric_count += 1

        self._cache = None
        self._cache_expires_at = 0.0
        return {
            "success": True,
            "status": "DATA_CONTRACT_SYNCED",
            "versionKey": version_key,
            "tables": table_count,
            "relations": relation_count,
            "metrics": metric_count,
        }

    async def record_vector_items(
        self,
        *,
        version_key: str,
        collection: str,
        items: list[dict[str, Any]],
    ) -> None:
        if not items:
            return
        try:
            async with get_db_session() as session:
                for item in items:
                    await session.execute(
                        text(
                            """
                            INSERT INTO ai_data_contract_vector_items (
                                version_key, item_key, item_type, source_table, source_name,
                                qdrant_collection, qdrant_point_id, content_hash, metadata, indexed_at, is_active
                            )
                            VALUES (
                                :version_key, :item_key, :item_type, :source_table, :source_name,
                                :collection, :point_id, :content_hash, CAST(:metadata AS jsonb), CURRENT_TIMESTAMP, 'Y'
                            )
                            ON CONFLICT (version_key, item_key) DO UPDATE SET
                                item_type = EXCLUDED.item_type,
                                source_table = EXCLUDED.source_table,
                                source_name = EXCLUDED.source_name,
                                qdrant_collection = EXCLUDED.qdrant_collection,
                                qdrant_point_id = EXCLUDED.qdrant_point_id,
                                content_hash = EXCLUDED.content_hash,
                                metadata = EXCLUDED.metadata,
                                indexed_at = CURRENT_TIMESTAMP,
                                updated = CURRENT_TIMESTAMP,
                                is_active = 'Y'
                            """
                        ),
                        {
                            "version_key": version_key,
                            "item_key": item["item_key"],
                            "item_type": item["item_type"],
                            "source_table": item.get("source_table"),
                            "source_name": item.get("source_name"),
                            "collection": collection,
                            "point_id": item["point_id"],
                            "content_hash": item["content_hash"],
                            "metadata": json.dumps(item.get("metadata") or {}),
                        },
                    )
        except Exception:
            # Vector item audit is useful but must not make reindex fail after
            # Qdrant has already accepted the points.
            return

    async def _load_from_db(self) -> RuntimeDataContract:
        if not await self._registry_exists():
            return self._static_contract(load_errors=("ai_data_contract_* tables not found; using static fallback",))

        async with get_db_session() as session:
            version_result = await session.execute(
                text(
                    """
                    SELECT version_key, source
                      FROM ai_data_contract_versions
                     WHERE status = 'ACTIVE'
                       AND is_active = 'Y'
                     ORDER BY activated_at DESC NULLS LAST, updated DESC NULLS LAST, created DESC
                     LIMIT 1
                    """
                )
            )
            version = version_result.mappings().first()
            if not version:
                return self._static_contract(load_errors=("no active data-contract version in DB; using static fallback",))
            version_key = str(version["version_key"])

            table_result = await session.execute(
                text(
                    """
                    SELECT table_name, owner_service, columns, pii_columns,
                           analytics_safe, ai_readable, ai_writable
                      FROM ai_data_contract_tables
                     WHERE version_key = :version_key
                       AND is_active = 'Y'
                     ORDER BY table_name
                    """
                ),
                {"version_key": version_key},
            )
            table_rows = list(table_result.mappings().all())
            if not table_rows:
                return self._static_contract(load_errors=(f"active DB contract {version_key} has no tables",))

            join_result = await session.execute(
                text(
                    """
                    SELECT left_table, left_column, right_table, right_column, purpose
                      FROM ai_data_contract_relations
                     WHERE version_key = :version_key
                       AND is_active = 'Y'
                     ORDER BY left_table, left_column, right_table, right_column
                    """
                ),
                {"version_key": version_key},
            )
            metric_result = await session.execute(
                text(
                    """
                    SELECT metric_name, title, description, grain, tables, allowed_scopes,
                           required_roles, required_params, owner_filtered_for_roles, default_chart
                      FROM ai_data_contract_metrics
                     WHERE version_key = :version_key
                       AND is_active = 'Y'
                     ORDER BY metric_name
                    """
                ),
                {"version_key": version_key},
            )
            join_rows = list(join_result.mappings().all())
            metric_rows = list(metric_result.mappings().all())

        tables = {
            str(row["table_name"]): TableContract(
                name=str(row["table_name"]),
                owner=str(row["owner_service"]),
                columns=tuple(row["columns"] or ()),
                pii_columns=tuple(row["pii_columns"] or ()),
                analytics_safe=bool(row["analytics_safe"]),
                ai_readable=bool(row["ai_readable"]),
                ai_writable=bool(row["ai_writable"]),
            )
            for row in table_rows
        }
        joins = tuple(
            JoinContract(
                str(row["left_table"]),
                str(row["left_column"]),
                str(row["right_table"]),
                str(row["right_column"]),
                str(row["purpose"]),
            )
            for row in join_rows
        )
        metrics = {
            str(row["metric_name"]): RuntimeMetricContract(
                name=str(row["metric_name"]),
                title=str(row["title"]),
                description=str(row["description"]),
                grain=str(row["grain"]),
                tables=tuple(row["tables"] or ()),
                allowed_scopes=tuple(row["allowed_scopes"] or ()),
                required_roles=tuple(row["required_roles"] or ()),
                required_params=tuple(row["required_params"] or ()),
                owner_filtered_for_roles=tuple(row["owner_filtered_for_roles"] or ()),
                default_chart=str(row["default_chart"] or "bar"),
            )
            for row in metric_rows
        }
        data_owners: dict[str, list[str]] = {}
        for name, contract in tables.items():
            data_owners.setdefault(contract.owner, []).append(name)
        return RuntimeDataContract(
            version_key=version_key,
            source=str(version["source"] or "DB"),
            loaded_from_db=True,
            tables=tables,
            joins=joins,
            metrics=metrics,
            data_owners={owner: tuple(sorted(names)) for owner, names in data_owners.items()},
            known_missing_relations=MISSING_RELATIONS,
        )

    async def _registry_exists(self) -> bool:
        async with get_db_session() as session:
            result = await session.execute(
                text(
                    """
                    SELECT to_regclass('public.ai_data_contract_versions') IS NOT NULL
                       AND to_regclass('public.ai_data_contract_tables') IS NOT NULL
                       AND to_regclass('public.ai_data_contract_relations') IS NOT NULL
                       AND to_regclass('public.ai_data_contract_metrics') IS NOT NULL AS exists
                    """
                )
            )
            return bool(result.scalar())

    def _static_contract(self, *, load_errors: tuple[str, ...] = ()) -> RuntimeDataContract:
        metrics = {
            name: RuntimeMetricContract(
                name=name,
                title=metric.title,
                description=metric.description,
                grain=metric.grain,
                tables=metric.tables,
                allowed_scopes=metric.allowed_scopes,
                required_roles=metric.required_roles,
                required_params=metric.required_params,
                owner_filtered_for_roles=metric.owner_filtered_for_roles,
                default_chart=metric.default_chart,
            )
            for name, metric in METRIC_REGISTRY.items()
        }
        for name, metric in METRICS.items():
            metrics.setdefault(
                name,
                RuntimeMetricContract(
                    name=name,
                    title=name.replace("_", " ").title(),
                    description=metric.description,
                    grain=metric.grain,
                    tables=metric.tables,
                ),
            )
        return RuntimeDataContract(
            version_key=self._settings.data_contract_version,
            source="STATIC_FALLBACK",
            loaded_from_db=False,
            tables=TABLES,
            joins=JOINS,
            metrics=metrics,
            data_owners=DATA_OWNERS,
            known_missing_relations=MISSING_RELATIONS,
            load_errors=load_errors,
        )


def _is_analytics_hint(join: JoinContract) -> bool:
    join_text = f"{join.left_table}.{join.left_column}->{join.right_table}.{join.right_column} {join.purpose}".lower()
    return any(
        token in join_text
        for token in (
            "rating",
            "course contains chapters",
            "chapter contains lessons",
            "course skill",
            "learning path contains courses",
            "transaction contains purchased courses",
            "payment belongs to transaction",
        )
    )


data_contract_registry = DataContractRegistryService()
