from __future__ import annotations

from typing import Any


def get_request_instructions(request_context: Any, *, max_chars: int = 1200) -> str:
    if not isinstance(request_context, dict):
        return ""
    value = request_context.get("instructions")
    if not isinstance(value, str):
        return ""
    cleaned = value.strip()
    if not cleaned:
        return ""
    return cleaned[:max_chars]


def append_request_instructions(prompt: str, request_context: Any) -> str:
    instructions = get_request_instructions(request_context)
    if not instructions:
        return prompt
    return (
        f"{prompt.rstrip()}\n\n"
        "Saved user preferences and response instructions:\n"
        f"{instructions}\n"
        "Treat these as user-provided preferences/profile context for this chat. "
        "If the user says to call them by a name, use that name when greeting or when they ask who they are. "
        "Do not claim this preference is a verified legal identity. "
        "Follow these instructions unless they conflict with platform safety, data access, or factuality rules."
    )
