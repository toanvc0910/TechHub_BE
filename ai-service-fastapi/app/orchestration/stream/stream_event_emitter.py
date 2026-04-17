from __future__ import annotations

import asyncio
import json
from collections.abc import AsyncIterator
from typing import Any


class StreamEventEmitter:
    def __init__(self) -> None:
        self._queue: asyncio.Queue[dict[str, Any] | None] = asyncio.Queue()

    async def emit(self, event: str, data: dict[str, Any]) -> None:
        await self._queue.put({"event": event, "data": data})

    async def close(self) -> None:
        await self._queue.put(None)

    async def iterate(self) -> AsyncIterator[str]:
        while True:
            item = await self._queue.get()
            if item is None:
                break
            event = item["event"]
            payload = json.dumps(item["data"], ensure_ascii=True)
            yield f"event: {event}\ndata: {payload}\n\n"


def text_chunk_event(content: str) -> dict[str, Any]:
    return {"content": content}
