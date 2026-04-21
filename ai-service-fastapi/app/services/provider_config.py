from __future__ import annotations

import asyncio
import json
import logging
from typing import Any

from app.core.config import get_settings

logger = logging.getLogger(__name__)

REDIS_CONFIG_KEY = "provider_config:active"


class ProviderConfigService:
    def __init__(self) -> None:
        self._settings = get_settings()
        self._lock = asyncio.Lock()
        self._provider = self._settings.ai_provider
        self._models: dict[str, str] = {
            "openai": self._settings.openai_chat_model,
            "gemini": self._settings.gemini_chat_model,
        }
        self._redis: Any | None = None

    def set_redis(self, client: Any | None) -> None:
        self._redis = client

    async def load_persisted(self) -> None:
        """Load previously persisted provider config from Redis on startup."""
        if self._redis is None:
            return
        try:
            raw = await self._redis.get(REDIS_CONFIG_KEY)
            if raw is None:
                return
            data = json.loads(raw.decode("utf-8") if isinstance(raw, bytes) else raw)
            if isinstance(data, dict):
                if data.get("provider"):
                    self._provider = self._normalize_provider(data["provider"])
                if isinstance(data.get("models"), dict):
                    for provider, model in data["models"].items():
                        if provider in self._models:
                            self._models[provider] = model
                logger.info("Loaded persisted provider config from Redis: provider=%s", self._provider)
        except Exception as exc:
            logger.warning("Failed to load persisted provider config: %s", exc)

    async def _persist(self) -> None:
        """Persist current provider config to Redis so it survives restarts."""
        if self._redis is None:
            return
        try:
            data = json.dumps({"provider": self._provider, "models": self._models})
            await self._redis.set(REDIS_CONFIG_KEY, data)
        except Exception as exc:
            logger.warning("Failed to persist provider config to Redis: %s", exc)

    def _normalize_provider(self, provider: str | None) -> str:
        candidate = (provider or self._provider or self._settings.ai_provider).lower()
        if candidate in self._settings.supported_chat_models:
            return candidate
        return self._settings.ai_provider

    def _provider_has_api_key(self, provider: str) -> bool:
        if provider == "openai":
            return bool((self._settings.openai_api_key or "").strip())
        if provider == "gemini":
            return bool((self._settings.gemini_api_key or "").strip())
        return False

    def _provider_for_model(self, model: str | None) -> str | None:
        if not model:
            return None
        for provider, models in self._settings.supported_chat_models.items():
            if model in models:
                return provider
        return None

    def _resolve_effective_provider_locked(self, preferred_provider: str | None = None) -> str | None:
        candidate = self._normalize_provider(preferred_provider)
        if self._provider_has_api_key(candidate):
            return candidate
        for provider in self._settings.supported_chat_models:
            if provider != candidate and self._provider_has_api_key(provider):
                return provider
        return None

    def _resolve_model_locked(self, provider: str, preferred_model: str | None = None) -> str:
        # Preferred model takes priority (from request context or policy)
        if preferred_model:
            return preferred_model
        # Then use whatever admin configured via update() — stored in _models dict
        configured_model = self._models.get(provider)
        if configured_model:
            return configured_model
        # Fallback to env default
        return self._settings.active_chat_model(provider)

    def _build_metadata_locked(self) -> dict[str, Any]:
        requested_provider = self._normalize_provider(self._provider)
        effective_provider = self._resolve_effective_provider_locked(requested_provider)
        effective_embedding_provider = effective_provider
        availability = {
            provider: {
                "available": self._provider_has_api_key(provider),
                "configuredModel": self._models.get(provider),
                "reason": None if self._provider_has_api_key(provider) else "missing_api_key",
            }
            for provider in self._settings.supported_chat_models
        }
        available_providers = [
            provider for provider, details in availability.items() if details["available"]
        ]
        using_mock = effective_provider is None
        if using_mock:
            status_message = "No live LLM provider is configured. Service will use mock fallback responses."
        elif effective_provider != requested_provider:
            status_message = (
                f"Requested provider '{requested_provider}' is unavailable. "
                f"Using '{effective_provider}' instead."
            )
        else:
            status_message = f"Using '{effective_provider}' as active LLM provider."
        return {
            "requestedProvider": requested_provider,
            "effectiveProvider": effective_provider or requested_provider,
            "effectiveEmbeddingProvider": effective_embedding_provider or requested_provider,
            "activeEmbeddingModel": self._settings.openai_embedding_model
            if (effective_embedding_provider or requested_provider) == "openai"
            else self._settings.gemini_embedding_model,
            "availableProviders": available_providers,
            "providerAvailability": availability,
            "usingMockFallback": using_mock,
            "embeddingUsingMockFallback": using_mock,
            "statusMessage": status_message,
        }

    async def get_requested_provider(self) -> str:
        return self._normalize_provider(self._provider)

    async def get_provider(self) -> str:
        return self._resolve_effective_provider_locked(self._provider) or self._normalize_provider(self._provider)

    async def get_active_chat_model(self, provider: str | None = None, preferred_model: str | None = None) -> str:
        actual_provider = provider or await self.get_provider()
        return self._resolve_model_locked(actual_provider, preferred_model)

    async def get_active_embedding_model(self, provider: str | None = None) -> str:
        actual_provider = provider or await self.get_provider()
        if actual_provider == "openai":
            return self._settings.openai_embedding_model
        return self._settings.gemini_embedding_model

    async def get_current_models(self) -> dict[str, str]:
        return dict(self._models)

    async def get_supported_chat_models(self) -> dict[str, list[str]]:
        return self._settings.supported_chat_models

    async def get_metadata(self) -> dict[str, Any]:
        return self._build_metadata_locked()

    async def resolve_chat_target(
        self,
        *,
        preferred_model: str | None = None,
        preferred_provider: str | None = None,
    ) -> tuple[str | None, str]:
        requested_provider = self._normalize_provider(
            preferred_provider or self._provider_for_model(preferred_model) or self._provider
        )
        effective_provider = self._resolve_effective_provider_locked(requested_provider)
        if effective_provider is None:
            return None, self._resolve_model_locked(requested_provider, preferred_model)
        compatible_model = preferred_model if self._provider_for_model(preferred_model) == effective_provider else None
        return effective_provider, self._resolve_model_locked(effective_provider, compatible_model)

    async def resolve_embedding_target(
        self,
        *,
        preferred_provider: str | None = None,
    ) -> tuple[str | None, str]:
        requested_provider = self._normalize_provider(preferred_provider or self._provider)
        effective_provider = self._resolve_effective_provider_locked(requested_provider)
        if effective_provider is None:
            return None, await self.get_active_embedding_model(requested_provider)
        return effective_provider, await self.get_active_embedding_model(effective_provider)

    async def is_supported(self, provider: str) -> bool:
        return provider in self._settings.supported_chat_models

    async def is_model_supported(self, provider: str, model: str) -> bool:
        return model in self._settings.supported_chat_models.get(provider, [])

    async def update(self, provider: str | None = None, chat_model: str | None = None, embedding_model: str | None = None) -> dict[str, str]:
        async with self._lock:
            if provider:
                self._provider = self._normalize_provider(provider)
            if chat_model:
                actual_provider = self._normalize_provider(provider or self._provider)
                self._models[actual_provider] = chat_model
            if embedding_model:
                # Store embedding model override in settings
                actual_provider = self._normalize_provider(provider or self._provider)
                if actual_provider == "openai":
                    self._settings.openai_embedding_model = embedding_model
                elif actual_provider == "gemini":
                    self._settings.gemini_embedding_model = embedding_model
            await self._persist()
            return await self.get_current_models()


provider_config_service = ProviderConfigService()
