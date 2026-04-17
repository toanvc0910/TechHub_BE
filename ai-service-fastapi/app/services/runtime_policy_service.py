from __future__ import annotations

from typing import Any

from app.core.config import get_settings
from app.orchestration.state.orchestrator_state import OrchestratorState, trace_step
from app.services.provider_config import provider_config_service


class RuntimePolicyService:
    def __init__(self) -> None:
        self._settings = get_settings()

    async def resolve(self, request_context: Any) -> dict[str, Any]:
        context = request_context if isinstance(request_context, dict) else {}
        tenant_policy = context.get("tenantPolicy") if isinstance(context.get("tenantPolicy"), dict) else {}
        user_role = str(context.get("userRole") or tenant_policy.get("userRole") or "USER").upper()
        permissions = {str(item).upper() for item in (context.get("permissions") or tenant_policy.get("permissions") or [])}
        model_override = context.get("modelOverride") if isinstance(context.get("modelOverride"), dict) else {}
        requested_provider = str(model_override.get("provider") or "").lower() or None
        requested_model = str(model_override.get("chatModel") or "").strip() or None

        allow_override = (
            self._settings.allow_admin_model_override
            if user_role in {"ADMIN", "SUPER_ADMIN", "STAFF"}
            else self._settings.allow_user_model_override
        )
        if requested_provider and not allow_override:
            requested_provider = None
            requested_model = None

        if requested_provider and not await provider_config_service.is_supported(requested_provider):
            requested_provider = None
            requested_model = None

        if requested_provider and requested_model:
            if not await provider_config_service.is_model_supported(requested_provider, requested_model):
                requested_model = None

        max_complexity = str(tenant_policy.get("maxComplexity") or context.get("maxComplexity") or "HIGH").upper()
        if max_complexity not in {"LOW", "MEDIUM", "HIGH"}:
            max_complexity = "HIGH"

        sql_max_rows = self._coerce_int(
            tenant_policy.get("sqlMaxRows") or context.get("sqlMaxRows"),
            default=self._settings.sql_default_limit,
            minimum=10,
            maximum=self._settings.sql_max_limit,
        )
        pii_access = user_role in {"ADMIN", "SUPER_ADMIN"} or "AI_PII_ACCESS" in permissions

        return {
            "tenantId": context.get("tenantId") or tenant_policy.get("tenantId"),
            "userRole": user_role,
            "permissions": sorted(permissions),
            "preferredProvider": requested_provider,
            "preferredModel": requested_model,
            "maxComplexity": max_complexity,
            "allowDataQuery": self._resolve_flag(context, tenant_policy, "allowDataQuery", default=True),
            "allowVisualization": self._resolve_flag(context, tenant_policy, "allowVisualization", default=True),
            "allowFileAnalysis": self._resolve_flag(context, tenant_policy, "allowFileAnalysis", default=True),
            "allowRecommendation": self._resolve_flag(context, tenant_policy, "allowRecommendation", default=True),
            "allowKnowledge": self._resolve_flag(context, tenant_policy, "allowKnowledge", default=True),
            "piiAccess": pii_access,
            "sqlMaxRows": sql_max_rows,
            "requireApprovalForData": self._resolve_flag(context, tenant_policy, "requireApprovalForData", default=False),
            "requireApprovalForViz": self._resolve_flag(context, tenant_policy, "requireApprovalForViz", default=False),
        }

    async def enforce_state_access(self, state: OrchestratorState) -> OrchestratorState:
        policy = await self.resolve(state.get("request_context"))
        state["policy_context"] = policy
        intent = state.get("intent", "conversation")

        blocked = {
            "data_query": not policy["allowDataQuery"],
            "visualization": not policy["allowVisualization"],
            "file_analysis": not policy["allowFileAnalysis"],
            "recommendation": not policy["allowRecommendation"],
            "knowledge": not policy["allowKnowledge"],
        }.get(intent, False)

        if blocked:
            original_intent = intent
            state["intent"] = "conversation"
            state["intent_reason"] = "policy_blocked"
            trace_step(
                state,
                "policy_gate",
                "Downgraded restricted intent to conversation by runtime policy.",
                originalIntent=original_intent,
                userRole=policy["userRole"],
            )
        return state

    @staticmethod
    def _resolve_flag(
        context: dict[str, Any],
        tenant_policy: dict[str, Any],
        key: str,
        *,
        default: bool,
    ) -> bool:
        raw = context.get(key)
        if raw is None:
            raw = tenant_policy.get(key)
        if raw is None:
            return default
        if isinstance(raw, bool):
            return raw
        return str(raw).strip().lower() in {"1", "true", "yes", "on"}

    @staticmethod
    def _coerce_int(raw: Any, *, default: int, minimum: int, maximum: int) -> int:
        try:
            value = int(raw)
        except (TypeError, ValueError):
            return default
        return max(minimum, min(maximum, value))


runtime_policy_service = RuntimePolicyService()
