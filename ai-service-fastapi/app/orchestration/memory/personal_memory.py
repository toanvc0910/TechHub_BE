from __future__ import annotations

import re
import unicodedata
from typing import Any


def extract_user_memory_facts(text_value: str) -> dict[str, str]:
    facts: dict[str, str] = {}
    favorite_color = extract_favorite_color(text_value)
    if favorite_color:
        facts["favorite_color"] = favorite_color

    preferred_name = extract_preferred_name(text_value)
    if preferred_name:
        facts["preferred_name"] = preferred_name
    return facts


def extract_favorite_color(text_value: str) -> str | None:
    raw_patterns = (
        r"(?:tôi|toi|mình|minh)\s+thích\s+màu\s+([^,.;!?]+)",
        r"màu\s+yêu\s+thích\s+của\s+(?:tôi|toi|mình|minh)\s+là\s+([^,.;!?]+)",
        r"my\s+favou?rite\s+colou?r\s+is\s+([^,.;!?]+)",
    )
    for pattern in raw_patterns:
        match = re.search(pattern, text_value or "", flags=re.IGNORECASE)
        if match:
            return clean_memory_value(match.group(1))

    normalized = normalize_text(text_value)
    normalized_patterns = (
        r"(?:toi|minh)\s+thich\s+mau\s+([^,.;!?]+)",
        r"mau\s+yeu\s+thich\s+cua\s+(?:toi|minh)\s+la\s+([^,.;!?]+)",
    )
    for pattern in normalized_patterns:
        match = re.search(pattern, normalized)
        if match:
            return clean_memory_value(match.group(1))
    return None


def extract_preferred_name(text_value: str) -> str | None:
    raw_patterns = (
        r"(?:tên|ten)\s+(?:tôi|toi|mình|minh)\s+là\s+([^,.;!?]+)",
        r"(?:hãy|hay)\s+gọi\s+(?:tôi|toi|mình|minh)\s+là\s+([^,.;!?]+)",
        r"call\s+me\s+([^,.;!?]+)",
    )
    for pattern in raw_patterns:
        match = re.search(pattern, text_value or "", flags=re.IGNORECASE)
        if match:
            return clean_memory_value(match.group(1))

    normalized = normalize_text(text_value)
    normalized_patterns = (
        r"ten\s+(?:toi|minh)\s+la\s+([^,.;!?]+)",
        r"hay\s+goi\s+(?:toi|minh)\s+la\s+([^,.;!?]+)",
    )
    for pattern in normalized_patterns:
        match = re.search(pattern, normalized)
        if match:
            return clean_memory_value(match.group(1))
    return None


def find_latest_favorite_color(
    *,
    user_memory: dict[str, Any] | None,
    conversation_context: dict[str, Any] | None,
) -> str | None:
    memory_color = clean_memory_value((user_memory or {}).get("favorite_color"))
    if memory_color:
        return memory_color

    recent = (conversation_context or {}).get("recentMessages")
    if not isinstance(recent, list):
        return None
    for message in reversed(recent):
        if not isinstance(message, dict):
            continue
        role = str(message.get("role") or message.get("sender") or "").lower()
        if role not in {"user", "human"}:
            continue
        color = extract_favorite_color(str(message.get("content") or ""))
        if color:
            return color
    return None


def asks_user_favorite_color(text_value: str) -> bool:
    normalized = normalize_text(text_value)
    patterns = (
        r"\b(?:toi|minh)\s+thich\s+mau\s+gi\b",
        r"\bmau\s+(?:yeu\s+thich\s+)?cua\s+(?:toi|minh)\s+(?:la\s+)?gi\b",
        r"\bwhat\s+is\s+my\s+favou?rite\s+colou?r\b",
    )
    return any(re.search(pattern, normalized) for pattern in patterns)


def asks_assistant_favorite_color(text_value: str) -> bool:
    normalized = normalize_text(text_value)
    patterns = (
        r"\bban\s+thich\s+mau\s+gi\b",
        r"\bmau\s+(?:yeu\s+thich\s+)?cua\s+ban\s+(?:la\s+)?gi\b",
        r"\bwhat\s+is\s+your\s+favou?rite\s+colou?r\b",
    )
    return any(re.search(pattern, normalized) for pattern in patterns)


def asks_personal_context_recall(text_value: str) -> bool:
    normalized = normalize_text(text_value)
    if asks_user_favorite_color(normalized):
        return True
    self_tokens = ("toi", "minh", "cua toi", "cua minh", "my")
    context_tokens = ("context", "ngu canh", "da noi", "vua noi", "truoc do", "luc nay", "nho")
    return any(token in normalized for token in self_tokens) and any(token in normalized for token in context_tokens)


def clean_memory_value(value: object | None) -> str | None:
    cleaned = str(value or "").strip(" \t\r\n\"'`")
    if not cleaned:
        return None
    if "?" in cleaned:
        return None
    split_markers = (
        " bạn thích",
        " ban thich",
        " còn bạn",
        " con ban",
        " vậy bạn",
        " vay ban",
        " and you",
    )
    lowered = cleaned.lower()
    cut_at = len(cleaned)
    for marker in split_markers:
        index = lowered.find(marker)
        if index >= 0:
            cut_at = min(cut_at, index)
    cleaned = cleaned[:cut_at].strip(" \t\r\n\"'`")
    if not cleaned or len(cleaned) > 80:
        return None
    normalized = normalize_text(cleaned)
    if (
        normalized in {"gi", "la gi", "what", "who", "ai", "context", "ngu canh"}
        or normalized.startswith(("gi ", "what ", "who "))
        or " tu context" in normalized
        or " trong context" in normalized
        or " from context" in normalized
    ):
        return None
    return cleaned


def normalize_text(text_value: str) -> str:
    lowered = (text_value or "").lower().strip().replace("\u0111", "d")
    normalized = unicodedata.normalize("NFD", lowered)
    without_marks = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    return re.sub(r"\s+", " ", without_marks)
