"""
Alembic environment for the AI service.

The AI service shares its PostgreSQL with the Java domain services and the
canonical schema lives in `TechHub_BE/techhub.sql`. The Java tables (courses,
lessons, ratings, ...) MUST NOT be touched by these migrations. This env only
takes responsibility for tables the AI service owns:

  - ai_generation_tasks
  - chat_sessions
  - chat_messages

The baseline migration `0001_baseline.py` is a NO-OP: it documents that the
above tables already exist (created either by the Java app or by the legacy
`Base.metadata.create_all` path) and that subsequent revisions should ALTER
them in a backward-compatible way.

Run from the service root:

    alembic -c alembic.ini upgrade head
"""

from __future__ import annotations

import os
from logging.config import fileConfig

from alembic import context
from dotenv import load_dotenv
from sqlalchemy import engine_from_config, pool

# Mirror app/core/config behaviour so `alembic upgrade` works the same as
# the running service, without forcing the operator to export each env var.
load_dotenv()

# Avoid importing app.* by default — that would load every service module
# (including httpx clients) just to run migrations. Pull the DB URL straight
# from env, with the same JDBC→asyncpg conversion the app uses.
config = context.config


def _build_sync_url() -> str:
    raw = os.getenv("SPRING_DATASOURCE_URL", "")
    user = os.getenv("SPRING_DATASOURCE_USERNAME", "")
    password = os.getenv("SPRING_DATASOURCE_PASSWORD", "")
    from urllib.parse import quote_plus

    if raw.startswith("jdbc:postgresql://"):
        url = "postgresql://" + raw.removeprefix("jdbc:postgresql://")
    elif raw.startswith("postgresql+asyncpg://"):
        url = "postgresql://" + raw.removeprefix("postgresql+asyncpg://")
    else:
        url = raw
    if user and "@" not in url.split("://", 1)[-1]:
        scheme, rest = url.split("://", 1)
        creds = f"{quote_plus(user)}:{quote_plus(password)}" if password else quote_plus(user)
        url = f"{scheme}://{creds}@{rest}"
    return url


if config.config_file_name is not None:
    fileConfig(config.config_file_name)

config.set_main_option("sqlalchemy.url", _build_sync_url())

target_metadata = None  # No autogenerate — schema is Java-owned. Hand-write revs.


def run_migrations_offline() -> None:
    url = config.get_main_option("sqlalchemy.url")
    context.configure(
        url=url,
        target_metadata=target_metadata,
        literal_binds=True,
        dialect_opts={"paramstyle": "named"},
    )
    with context.begin_transaction():
        context.run_migrations()


def run_migrations_online() -> None:
    connectable = engine_from_config(
        config.get_section(config.config_ini_section, {}),
        prefix="sqlalchemy.",
        poolclass=pool.NullPool,
    )
    with connectable.connect() as connection:
        context.configure(connection=connection, target_metadata=target_metadata)
        with context.begin_transaction():
            context.run_migrations()


if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
