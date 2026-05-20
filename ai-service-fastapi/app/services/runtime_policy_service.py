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
        trusted = context.get("trusted") if isinstance(context.get("trusted"), dict) else {}
        trusted_roles = self._normalize_roles(trusted.get("roles") or [])
        permissions = {f"ROLE_{role}" for role in trusted_roles}
        permissions.update({
            str(item).strip().upper()
            for item in (trusted.get("permissions") or [])
            if str(item).strip()
        })
        user_role = self._resolve_role(trusted_roles)
        model_override = context.get("modelOverride") if isinstance(context.get("modelOverride"), dict) else {}
        requested_provider = str(model_override.get("provider") or "").lower() or None
        requested_model = str(model_override.get("chatModel") or "").strip() or None

        allow_override = (
            self._settings.allow_admin_model_override
            if user_role in {"ADMIN", "SUPER_ADMIN", "STAFF"}
            else False
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

        max_complexity = self._resolve_complexity(context.get("maxComplexity"), default="HIGH")
        if max_complexity not in {"LOW", "MEDIUM", "HIGH"}:
            max_complexity = "HIGH"

        sql_max_rows = self._resolve_sql_limit(context.get("sqlMaxRows"), user_role=user_role)
        pii_access = user_role in {"ADMIN", "SUPER_ADMIN"} or "AI_PII_ACCESS" in permissions
        allow_data_work = user_role in {"ADMIN", "SUPER_ADMIN", "STAFF", "INSTRUCTOR", "LEARNER"}

        return {
            "tenantId": None,
            "trustedUserId": trusted.get("userId"),
            "trustedRoles": sorted(trusted_roles),
            "userRole": user_role,
            "permissions": sorted(permissions),
            "preferredProvider": requested_provider,
            "preferredModel": requested_model,
            "maxComplexity": max_complexity,
            "allowDataQuery": self._resolve_restrictive_flag(context, "allowDataQuery", default=allow_data_work),
            "allowVisualization": self._resolve_restrictive_flag(context, "allowVisualization", default=allow_data_work),
            "allowFileAnalysis": self._resolve_restrictive_flag(context, "allowFileAnalysis", default=True),
            "allowRecommendation": self._resolve_restrictive_flag(context, "allowRecommendation", default=True),
            "allowKnowledge": self._resolve_restrictive_flag(context, "allowKnowledge", default=True),
            "piiAccess": pii_access,
            "sqlMaxRows": sql_max_rows,
            "requireApprovalForData": self._resolve_admin_flag(context, "requireApprovalForData", user_role=user_role),
            "requireApprovalForViz": self._resolve_admin_flag(context, "requireApprovalForViz", user_role=user_role),
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
    def _resolve_restrictive_flag(context: dict[str, Any], key: str, *, default: bool) -> bool:
        raw = context.get(key)
        if raw is None:
            return default
        if isinstance(raw, bool):
            return raw if raw is False else default
        return False if str(raw).strip().lower() in {"0", "false", "no", "off"} else default

    def _resolve_sql_limit(self, raw: Any, *, user_role: str) -> int:
        default_limit = self._settings.sql_default_limit
        if user_role not in {"ADMIN", "SUPER_ADMIN", "STAFF"}:
            return default_limit
        return self._coerce_int(raw, default=default_limit, minimum=10, maximum=self._settings.sql_max_limit)

    @staticmethod
    def _resolve_admin_flag(context: dict[str, Any], key: str, *, user_role: str) -> bool:
        if user_role not in {"ADMIN", "SUPER_ADMIN", "STAFF"}:
            return False
        raw = context.get(key)
        if isinstance(raw, bool):
            return raw
        if raw is None:
            return False
        return str(raw).strip().lower() in {"1", "true", "yes", "on"}

    @staticmethod
    def _resolve_complexity(raw: Any, *, default: str) -> str:
        value = str(raw or default).strip().upper()
        if value not in {"LOW", "MEDIUM", "HIGH"}:
            return default
        return value

    @staticmethod
    def _normalize_roles(raw_roles: Any) -> set[str]:
        if not isinstance(raw_roles, list | tuple | set):
            raw_roles = [raw_roles]
        roles: set[str] = set()
        for raw_role in raw_roles:
            role = str(raw_role or "").strip().upper()
            if role.startswith("ROLE_"):
                role = role[5:]
            if role:
                roles.add(role)
        return roles

    @staticmethod
    def _resolve_role(roles: set[str]) -> str:
        for role in ("SUPER_ADMIN", "ADMIN", "STAFF", "INSTRUCTOR", "LEARNER"):
            if role in roles:
                return role
        if "SYSTEM" in roles:
            return "SYSTEM"
        return "USER"

    @staticmethod
    def _coerce_int(raw: Any, *, default: int, minimum: int, maximum: int) -> int:
        try:
            value = int(raw)
        except (TypeError, ValueError):
            return default
        return max(minimum, min(maximum, value))


runtime_policy_service = RuntimePolicyService()
