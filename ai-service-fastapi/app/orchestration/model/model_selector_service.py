from __future__ import annotations

from app.core.config import get_settings
from app.orchestration.state.orchestrator_state import OrchestratorState
from app.services.provider_config import provider_config_service
from app.services.runtime_policy_service import runtime_policy_service


class ModelSelectorService:
    def __init__(self) -> None:
        self._settings = get_settings()

    async def select_model(self, state: OrchestratorState) -> tuple[str, str]:
        complexity = "LOW"
        user_input = state.get("user_input", "")
        intent = state.get("intent", "conversation")
        if len(user_input) > 400 or intent in {"data_query", "visualization"}:
            complexity = "MEDIUM"
        if intent in {"file_analysis", "knowledge"} and len(user_input) > 800:
            complexity = "HIGH"

        policy = await runtime_policy_service.resolve(state.get("request_context"))
        complexity = self._cap_complexity(complexity, policy.get("maxComplexity") or "HIGH")

        provider, model = await provider_config_service.resolve_chat_target(
            preferred_provider=policy.get("preferredProvider"),
            preferred_model=policy.get("preferredModel"),
        )
        effective_provider = provider or await provider_config_service.get_provider()
        supported = await provider_config_service.get_supported_chat_models()
        provider_models = supported.get(effective_provider, [])
        if effective_provider == "openai" and complexity == "HIGH" and "gpt-4.1" in provider_models:
            model = "gpt-4.1"
        if effective_provider == "gemini" and complexity == "HIGH":
            if "gemini-3.5-flash" in provider_models:
                model = "gemini-3.5-flash"
            elif "gemini-2.5-pro" in provider_models:
                model = "gemini-2.5-pro"
        if effective_provider == "gemini" and complexity == "MEDIUM" and model not in provider_models:
            model = self._settings.gemini_chat_model
        if effective_provider == "openai" and complexity == "MEDIUM" and model not in provider_models:
            model = self._settings.openai_chat_model
        return model, complexity

    @staticmethod
    def _cap_complexity(complexity: str, max_complexity: str) -> str:
        order = {"LOW": 0, "MEDIUM": 1, "HIGH": 2}
        limited = min(order.get(complexity, 0), order.get(max_complexity, 2))
        reverse = {value: key for key, value in order.items()}
        return reverse[limited]


model_selector_service = ModelSelectorService()
