from __future__ import annotations

import time
from collections import defaultdict, deque
from uuid import UUID

from redis.asyncio import Redis


class RateLimitingService:
    def __init__(self) -> None:
        self._redis: Redis | None = None
        self._memory: dict[str, deque[float]] = defaultdict(deque)

    def set_client(self, redis_client: Redis | None) -> None:
        self._redis = redis_client

    async def is_allowed(self, user_id: UUID) -> bool:
        minute_remaining = await self.get_remaining_requests_per_minute(user_id)
        hour_remaining = await self.get_remaining_requests_per_hour(user_id)
        return minute_remaining > 0 and hour_remaining > 0

    async def get_remaining_requests_per_minute(self, user_id: UUID) -> int:
        return await self._remaining(user_id, 60, 20)

    async def get_remaining_requests_per_hour(self, user_id: UUID) -> int:
        return await self._remaining(user_id, 3600, 100)

    async def _remaining(self, user_id: UUID, window_seconds: int, limit: int) -> int:
        key = f"rate-limit:{user_id}:{window_seconds}"
        now = time.time()
        if self._redis is not None:
            tx = self._redis.pipeline()
            tx.zremrangebyscore(key, 0, now - window_seconds)
            tx.zcard(key)
            tx.zadd(key, {str(now): now})
            tx.expire(key, window_seconds)
            _, count, _, _ = await tx.execute()
            return max(limit - int(count) - 1, 0)

        bucket = self._memory[key]
        while bucket and bucket[0] <= now - window_seconds:
            bucket.popleft()
        bucket.append(now)
        return max(limit - len(bucket), 0)


rate_limiting_service = RateLimitingService()
