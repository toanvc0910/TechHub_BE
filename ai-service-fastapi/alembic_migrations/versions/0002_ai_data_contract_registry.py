"""add AI data-contract registry tables.

Revision ID: 0002_ai_data_contract_registry
Revises: 0001_baseline
Create Date: 2026-06-05

The registry stores the production contract that AI uses to reason about
schema, joins, metrics, and vector contract freshness. Domain tables remain
owned by their Java services; these AI-owned metadata tables are safe for the
FastAPI service to manage.
"""

from __future__ import annotations

from alembic import op


revision = "0002_ai_data_contract_registry"
down_revision = "0001_baseline"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.execute(
        """
        CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

        CREATE TABLE IF NOT EXISTS ai_data_contract_versions (
            version_key VARCHAR(64) PRIMARY KEY,
            status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
                CHECK (status IN ('DRAFT', 'ACTIVE', 'RETIRED')),
            description TEXT,
            source VARCHAR(64) NOT NULL DEFAULT 'AI_SERVICE',
            activated_at TIMESTAMP WITH TIME ZONE,
            created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            created_by UUID REFERENCES users(id),
            updated_by UUID REFERENCES users(id),
            is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
        );

        CREATE TABLE IF NOT EXISTS ai_data_contract_tables (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            version_key VARCHAR(64) NOT NULL REFERENCES ai_data_contract_versions(version_key) ON DELETE CASCADE,
            table_name VARCHAR(128) NOT NULL,
            owner_service VARCHAR(128) NOT NULL,
            columns TEXT[] NOT NULL DEFAULT '{}',
            pii_columns TEXT[] NOT NULL DEFAULT '{}',
            analytics_safe BOOLEAN NOT NULL DEFAULT TRUE,
            ai_readable BOOLEAN NOT NULL DEFAULT TRUE,
            ai_writable BOOLEAN NOT NULL DEFAULT FALSE,
            description TEXT,
            metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
            created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            created_by UUID REFERENCES users(id),
            updated_by UUID REFERENCES users(id),
            is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N')),
            CONSTRAINT uniq_ai_contract_table UNIQUE (version_key, table_name)
        );

        CREATE INDEX IF NOT EXISTS idx_ai_contract_tables_version
            ON ai_data_contract_tables(version_key);
        CREATE INDEX IF NOT EXISTS idx_ai_contract_tables_name
            ON ai_data_contract_tables(table_name);
        CREATE INDEX IF NOT EXISTS idx_ai_contract_tables_flags
            ON ai_data_contract_tables(analytics_safe, ai_readable, ai_writable);

        CREATE TABLE IF NOT EXISTS ai_data_contract_relations (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            version_key VARCHAR(64) NOT NULL REFERENCES ai_data_contract_versions(version_key) ON DELETE CASCADE,
            left_table VARCHAR(128) NOT NULL,
            left_column VARCHAR(128) NOT NULL,
            right_table VARCHAR(128) NOT NULL,
            right_column VARCHAR(128) NOT NULL,
            relation_type VARCHAR(64) NOT NULL DEFAULT 'foreign_key',
            predicate TEXT,
            purpose TEXT NOT NULL,
            analytics_hint BOOLEAN NOT NULL DEFAULT FALSE,
            metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
            created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            created_by UUID REFERENCES users(id),
            updated_by UUID REFERENCES users(id),
            is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N'))
        );

        CREATE INDEX IF NOT EXISTS idx_ai_contract_relations_version
            ON ai_data_contract_relations(version_key);
        CREATE INDEX IF NOT EXISTS idx_ai_contract_relations_left
            ON ai_data_contract_relations(left_table, left_column);
        CREATE INDEX IF NOT EXISTS idx_ai_contract_relations_right
            ON ai_data_contract_relations(right_table, right_column);
        CREATE UNIQUE INDEX IF NOT EXISTS uniq_ai_contract_relation
            ON ai_data_contract_relations(
                version_key,
                left_table,
                left_column,
                right_table,
                right_column
            );

        CREATE TABLE IF NOT EXISTS ai_data_contract_metrics (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            version_key VARCHAR(64) NOT NULL REFERENCES ai_data_contract_versions(version_key) ON DELETE CASCADE,
            metric_name VARCHAR(128) NOT NULL,
            title VARCHAR(255) NOT NULL,
            description TEXT NOT NULL,
            grain VARCHAR(128) NOT NULL,
            tables TEXT[] NOT NULL DEFAULT '{}',
            allowed_scopes TEXT[] NOT NULL DEFAULT '{}',
            required_roles TEXT[] NOT NULL DEFAULT '{}',
            required_params TEXT[] NOT NULL DEFAULT '{}',
            owner_filtered_for_roles TEXT[] NOT NULL DEFAULT '{}',
            default_chart VARCHAR(32) NOT NULL DEFAULT 'bar',
            metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
            created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            created_by UUID REFERENCES users(id),
            updated_by UUID REFERENCES users(id),
            is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N')),
            CONSTRAINT uniq_ai_contract_metric UNIQUE (version_key, metric_name)
        );

        CREATE INDEX IF NOT EXISTS idx_ai_contract_metrics_version
            ON ai_data_contract_metrics(version_key);
        CREATE INDEX IF NOT EXISTS idx_ai_contract_metrics_name
            ON ai_data_contract_metrics(metric_name);

        CREATE TABLE IF NOT EXISTS ai_data_contract_vector_items (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            version_key VARCHAR(64) NOT NULL REFERENCES ai_data_contract_versions(version_key) ON DELETE CASCADE,
            item_key VARCHAR(255) NOT NULL,
            item_type VARCHAR(64) NOT NULL,
            source_table VARCHAR(128),
            source_name VARCHAR(255),
            qdrant_collection VARCHAR(128) NOT NULL,
            qdrant_point_id VARCHAR(128) NOT NULL,
            content_hash VARCHAR(64) NOT NULL,
            indexed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
            created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            created_by UUID REFERENCES users(id),
            updated_by UUID REFERENCES users(id),
            is_active VARCHAR(1) NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N')),
            CONSTRAINT uniq_ai_contract_vector_item UNIQUE (version_key, item_key)
        );

        CREATE INDEX IF NOT EXISTS idx_ai_contract_vector_items_version
            ON ai_data_contract_vector_items(version_key);
        CREATE INDEX IF NOT EXISTS idx_ai_contract_vector_items_type
            ON ai_data_contract_vector_items(item_type);
        """
    )
    op.execute(
        """
        DO $$
        DECLARE
            t text;
        BEGIN
            IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'update_updated') THEN
                FOREACH t IN ARRAY ARRAY[
                    'ai_data_contract_versions',
                    'ai_data_contract_tables',
                    'ai_data_contract_relations',
                    'ai_data_contract_metrics',
                    'ai_data_contract_vector_items'
                ]
                LOOP
                    IF NOT EXISTS (
                        SELECT 1
                          FROM pg_trigger
                         WHERE tgname = 'trg_update_' || t
                    ) THEN
                        EXECUTE 'CREATE TRIGGER trg_update_' || t ||
                                ' BEFORE UPDATE ON ' || t ||
                                ' FOR EACH ROW EXECUTE PROCEDURE update_updated()';
                    END IF;
                END LOOP;
            END IF;
        END;
        $$;
        """
    )


def downgrade() -> None:
    # Intentionally non-destructive. Contract metadata is production audit data.
    pass
