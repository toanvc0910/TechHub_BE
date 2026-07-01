from __future__ import annotations

import re


class PromptSanitizationService:
    _blocked_patterns = [
        r"ignore\s+all\s+previous\s+instructions",
        r"system\s+prompt",
        r"developer\s+message",
        r"reveal\s+hidden\s+instructions",
    ]

    def sanitize(self, message: str) -> str:
        normalized = re.sub(r"[\x00-\x08\x0b\x0c\x0e-\x1f]", "", message).strip()
        lowered = normalized.lower()
        for pattern in self._blocked_patterns:
            if re.search(pattern, lowered):
                raise ValueError("Prompt contains blocked instruction patterns.")
        return re.sub(r"\s{3,}", "  ", normalized)


prompt_sanitization_service = PromptSanitizationService()
