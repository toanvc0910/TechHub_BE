from __future__ import annotations

from dataclasses import dataclass
from typing import Any
from uuid import UUID

from fastapi import Depends, HTTPException, Request, status


ROLE_PRIORITY = ("SUPER_ADMIN", "ADMIN", "STAFF", "INSTRUCTOR", "LEARNER", "USER")
ADMIN_ROLES = {"SUPER_ADMIN", "ADMIN", "STAFF", "SYSTEM"}
AI_CONTENT_ROLES = {"SUPER_ADMIN", "ADMIN", "STAFF", "INSTRUCTOR", "SYSTEM"}
INTERNAL_REQUEST_SOURCES = {
    "proxy-client",
    "course-service",
    "file-service",
    "payment-service",
    "user-service",
    "learning-path-service",
    "notification-service",
    "analytics-service",
}
CLIENT_AUTHORITY_KEYS = {
    "allowDataQuery",
    "allowFileAnalysis",
    "allowKnowledge",
    "allowRecommendation",
    "allowVisualization",
    "maxComplexity",
    "modelOverride",
    "permissions",
    "piiAccess",
    "requireApprovalForData",
    "requireApprovalForViz",
    "sqlMaxRows",
    "tenantId",
    "tenantPolicy",
    "trusted",
    "userId",
    "userRole",
}


@dataclass(frozen=True)
class TrustedContext:
    user_id: UUID | None
    email: str | None
    roles: tuple[str, ...]
    request_source: str | None

    @property
    def authenticated(self) -> bool:
        return self.user_id is not None

    @property
    def normalized_roles(self) -> set[str]:
        return set(self.roles)

    @property
    def policy_role(self) -> str:
        roles = self.normalized_roles
        for role in ROLE_PRIORITY:
            if role in roles:
                return role
        if "SYSTEM" in roles:
            return "SYSTEM"
        return "USER"

    def has_any_role(self, allowed_roles: set[str]) -> bool:
        return bool(self.normalized_roles.intersection(allowed_roles))

    def to_payload(self) -> dict[str, Any]:
        return {
            "userId": str(self.user_id) if self.user_id else None,
            "email": self.email,
            "roles": list(self.roles),
            "requestSource": self.request_source,
            "authenticated": self.authenticated,
        }


_TRUSTED_HEADERS_SINGLE = (
    "X-User-Id",
    "X-User-Email",
    "X-User-Roles",
    "X-Request-Source",
)


def _single_trusted_header(request: Request, name: str) -> str | None:
    """Return the trusted header value but refuse if the client sent it more
    than once.

    Defense-in-depth against header smuggling: the proxy is supposed to strip
    any client-supplied copy of these headers before forwarding. If both the
    client and the proxy somehow set the same name, Starlette would join them
    with `,` (e.g. `SUPER_ADMIN,USER`) and the role parser would happily grant
    SUPER_ADMIN. Reject the request entirely instead.
    """
    values = request.headers.getlist(name)
    if not values:
        return None
    if len(values) > 1:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Header {name} sent more than once; refusing request.",
        )
    return _clean_header(values[0])


def get_trusted_context(request: Request) -> TrustedContext:
    # Reject any duplicate trusted header upfront — never merge them.
    user_id_header = _single_trusted_header(request, "X-User-Id")
    email_header = _single_trusted_header(request, "X-User-Email")
    roles_header = _single_trusted_header(request, "X-User-Roles")
    source_header = _single_trusted_header(request, "X-Request-Source")

    user_id: UUID | None = None
    if user_id_header:
        try:
            user_id = UUID(user_id_header)
        except ValueError as exc:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid X-User-Id header.",
            ) from exc

    return TrustedContext(
        user_id=user_id,
        email=email_header,
        roles=_parse_roles(roles_header),
        request_source=source_header,
    )


def require_trusted_user(trusted: TrustedContext = Depends(get_trusted_context)) -> TrustedContext:
    _require_internal_source(trusted)
    if not trusted.authenticated:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Trusted user identity is required.",
        )
    return trusted


def require_admin_context(trusted: TrustedContext = Depends(get_trusted_context)) -> TrustedContext:
    _require_internal_source(trusted)
    if not trusted.authenticated and "SYSTEM" not in trusted.normalized_roles:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Trusted admin identity is required.",
        )
    if not trusted.has_any_role(ADMIN_ROLES):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admin role is required.",
        )
    return trusted


def require_ai_content_operator(trusted: TrustedContext = Depends(get_trusted_context)) -> TrustedContext:
    _require_internal_source(trusted)
    if not trusted.authenticated and "SYSTEM" not in trusted.normalized_roles:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Trusted user identity is required.",
        )
    if not trusted.has_any_role(AI_CONTENT_ROLES):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="AI content operation role is required.",
        )
    return trusted


def require_user_match(supplied_user_id: UUID | str | None, trusted: TrustedContext) -> UUID:
    _require_internal_source(trusted)
    if not trusted.authenticated or trusted.user_id is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Trusted user identity is required.",
        )

    if supplied_user_id is None:
        return trusted.user_id

    try:
        requested_user_id = supplied_user_id if isinstance(supplied_user_id, UUID) else UUID(str(supplied_user_id))
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid userId.",
        ) from exc

    if requested_user_id != trusted.user_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Request userId does not match authenticated user.",
        )
    return trusted.user_id


def build_trusted_request_context(raw_context: Any, trusted: TrustedContext) -> dict[str, Any]:
    if isinstance(raw_context, dict):
        context = dict(raw_context)
    elif raw_context is None:
        context = {}
    else:
        context = {"clientContext": raw_context}

    model_override = context.get("modelOverride") if isinstance(context.get("modelOverride"), dict) else None
    for key in CLIENT_AUTHORITY_KEYS:
        context.pop(key, None)
    if model_override and trusted.has_any_role(ADMIN_ROLES):
        context["modelOverride"] = model_override

    context["trusted"] = trusted.to_payload()
    context["userRole"] = trusted.policy_role
    context["permissions"] = sorted(f"ROLE_{role}" for role in trusted.roles)
    return context


def copy_model_with_updates(model: Any, **updates: Any) -> Any:
    if hasattr(model, "model_copy"):
        return model.model_copy(update=updates)
    return model.copy(update=updates)


def _require_internal_source(trusted: TrustedContext) -> None:
    if trusted.request_source not in INTERNAL_REQUEST_SOURCES:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Request must come from a trusted internal source.",
        )


def _parse_roles(raw_roles: str | None) -> tuple[str, ...]:
    normalized: list[str] = []
    for raw_role in (raw_roles or "").split(","):
        role = raw_role.strip().upper()
        if role.startswith("ROLE_"):
            role = role[5:]
        if role and role not in normalized:
            normalized.append(role)
    return tuple(normalized)


def _clean_header(value: str | None) -> str | None:
    if value is None:
        return None
    stripped = value.strip()
    return stripped or None
