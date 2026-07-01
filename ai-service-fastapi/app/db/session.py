from __future__ import annotations

from contextlib import asynccontextmanager
from urllib.parse import quote_plus

from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from app.core.config import get_settings
from app.db.models import Base

settings = get_settings()


def _build_url() -> str:
    """Build asyncpg URL with credentials injected.

    Input:  postgresql+asyncpg://31.220.87.43:5432/techhub  (no creds)
    Output: postgresql+asyncpg://user:pass@31.220.87.43:5432/techhub
    """
    url = settings.database_url
    user = settings.database_username
    password = settings.database_password
    if user and "://" in url:
        scheme, rest = url.split("://", 1)
        # If URL already has credentials (user:pass@), don't inject again
        if "@" not in rest:
            creds = f"{quote_plus(user)}:{quote_plus(password)}" if password else quote_plus(user)
            url = f"{scheme}://{creds}@{rest}"
    return url


_statement_timeout_ms = max(0, int(settings.database_statement_timeout_ms))
_connect_args: dict = {}
if _statement_timeout_ms > 0:
    # asyncpg `server_settings` sets PostgreSQL session parameters when the
    # connection is established. statement_timeout protects the pool from a
    # single slow query starving every other request.
    _connect_args["server_settings"] = {"statement_timeout": str(_statement_timeout_ms)}

engine = create_async_engine(
    _build_url(),
    echo=settings.database_echo,
    pool_pre_ping=True,
    pool_size=settings.database_pool_size,
    max_overflow=settings.database_max_overflow,
    pool_recycle=settings.database_pool_recycle_seconds,
    pool_timeout=settings.database_pool_timeout_seconds,
    connect_args=_connect_args,
)
SessionLocal = async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)


@asynccontextmanager
async def get_db_session() -> AsyncSession:
    session = SessionLocal()
    try:
        yield session
        await session.commit()
    except Exception:
        await session.rollback()
        raise
    finally:
        await session.close()


async def maybe_create_schema() -> None:
    if not settings.database_auto_create:
        return
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
