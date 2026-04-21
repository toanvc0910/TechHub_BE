from __future__ import annotations

from fastapi import APIRouter

router = APIRouter()


@router.get("/health")
async def health() -> dict[str, str]:
    return {"status": "UP"}


@router.get("/actuator/health")
async def actuator_health() -> dict[str, str]:
    return {"status": "UP"}
