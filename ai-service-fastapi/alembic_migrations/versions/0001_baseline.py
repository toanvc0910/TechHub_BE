"""baseline — adopt existing AI-service tables without recreation.

Revision ID: 0001_baseline
Revises:
Create Date: 2026-05-16

This revision intentionally performs no DDL. It exists to stamp `alembic_version`
on an environment whose tables (`ai_generation_tasks`, `chat_sessions`,
`chat_messages`) were created by the legacy `Base.metadata.create_all` path or
by the Java `techhub.sql` bootstrap.

Subsequent revisions should ALTER these tables in backward-compatible steps.
Add new columns as nullable, deploy code, then later make NOT NULL in a
follow-up revision. NEVER let Alembic try to manage Java-owned tables such as
`courses`, `lessons`, `ratings`, `enrollments`, etc.

To stamp an existing prod DB at this baseline without running DDL:

    alembic -c alembic.ini stamp 0001_baseline
"""

from __future__ import annotations


revision = "0001_baseline"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Intentional no-op. The AI-service-owned tables already exist when this
    # baseline is applied for the first time on an established environment.
    pass


def downgrade() -> None:
    # No-op so we never DROP tables that may hold production data.
    pass
