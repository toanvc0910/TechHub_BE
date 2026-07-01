from __future__ import annotations

from dataclasses import dataclass


@dataclass(slots=True)
class IntentResult:
    intent: str
    sub_intent: str | None
    confidence: float
    reason: str
    matched_rule: str | None = None
