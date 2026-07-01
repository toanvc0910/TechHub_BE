from __future__ import annotations

from datetime import datetime
from typing import Any


def success_response(
    *,
    message: str,
    data: Any,
    path: str,
    status: str = "SUCCESS",
    code: int = 200,
) -> dict[str, Any]:
    return {
        "success": True,
        "status": status,
        "message": message,
        "data": data,
        "timestamp": datetime.utcnow().isoformat(),
        "path": path,
        "code": code,
    }


def error_response(
    *,
    message: str,
    path: str,
    status: str = "ERROR",
    code: int = 400,
    data: Any = None,
) -> dict[str, Any]:
    return {
        "success": False,
        "status": status,
        "message": message,
        "data": data,
        "timestamp": datetime.utcnow().isoformat(),
        "path": path,
        "code": code,
    }
