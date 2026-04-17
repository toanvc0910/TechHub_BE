from __future__ import annotations

import math
import re
from collections import Counter
from typing import Any

from app.core.config import get_settings
from app.orchestration.router.intent_result import IntentResult
from app.orchestration.state.orchestrator_state import OrchestratorState
from app.services.llm_gateway import switchable_ai_gateway


class IntentRouter:
    def __init__(self) -> None:
        self._settings = get_settings()
        self._rules: list[tuple[str, str, list[str]]] = [
            ("file_analysis", "uploaded-file", [r"\b(pdf|file|tai lieu|docx|document|phan tich file)\b"]),
            ("visualization", "chart", [r"\b(chart|bieu do|visual|plot|dashboard)\b"]),
            ("data_query", "analytics", [r"\b(so lieu|thong ke|bao nhieu|analytics|report|tong hop)\b"]),
            ("recommendation", "course-advice", [r"\b(goi y|de xuat|recommend|phu hop|nen hoc)\b"]),
            ("knowledge", "lesson-qa", [r"\b(giai thich|khai niem|la gi|how|tai sao)\b"]),
            ("conversation", "greeting", [r"\b(xin chao|hello|hi|cam on)\b"]),
        ]
        self._semantic_templates: dict[str, list[str]] = {
            "recommendation": [
                "goi y khoa hoc phu hop cho nguoi moi bat dau",
                "nen hoc khoa nao de dat muc tieu lap trinh",
                "recommend a suitable TechHub course for me",
            ],
            "knowledge": [
                "giai thich khai niem trong bai hoc",
                "lesson nay noi ve dieu gi va tai sao quan trong",
                "explain the topic from this course",
            ],
            "data_query": [
                "thong ke enrollment theo level",
                "bao cao tien do hoc tap theo khoa",
                "analytics summary for the learning platform",
            ],
            "visualization": [
                "ve bieu do tu so lieu hoc tap",
                "tao chart cho enrollment analytics",
                "visualize the learning data",
            ],
            "file_analysis": [
                "phan tich file tai lieu duoc tai len",
                "tom tat noi dung pdf hoac docx",
                "analyze the uploaded document",
            ],
            "conversation": [
                "xin chao ban",
                "ban co the giup toi khong",
                "hello there",
            ],
        }
        self._template_embeddings: dict[str, list[list[float]]] = {}

    async def classify(self, state: OrchestratorState) -> IntentResult:
        text = state.get("user_input", "").lower().strip()

        if state.get("has_fresh_file_context"):
            return IntentResult(
                intent="file_analysis",
                sub_intent="file-context",
                confidence=0.98,
                reason="Detected file context attached to request.",
                matched_rule="tier0:file-context",
            )

        if state.get("file_contexts") and re.search(r"\b(file|tai lieu|document|pdf|docx|upload|tep|noi dung nay|tai lieu nay)\b", text):
            return IntentResult(
                intent="file_analysis",
                sub_intent="session-file-context",
                confidence=0.82,
                reason="Detected previously attached file context referenced by the current question.",
                matched_rule="tier0:session-file-context",
            )

        for intent, sub_intent, patterns in self._rules:
            for pattern in patterns:
                if re.search(pattern, text):
                    return IntentResult(
                        intent=intent,
                        sub_intent=sub_intent,
                        confidence=0.86,
                        reason=f"Matched rule pattern {pattern}.",
                        matched_rule=f"tier1:{pattern}",
                    )

        semantic_result = await self._semantic_classify(text)
        if semantic_result is not None:
            return semantic_result

        llm_result = await self._llm_fallback(text, state)
        if llm_result is not None:
            return llm_result

        mode = state.get("mode", "AUTO")
        mode_value = mode.value if hasattr(mode, "value") else str(mode)
        if mode_value == "ADVISOR":
            return IntentResult(
                intent="recommendation",
                sub_intent="advisor-default",
                confidence=0.58,
                reason="Advisor mode biases routing toward recommendation.",
                matched_rule="tier4:mode-bias",
            )

        return IntentResult(
            intent="conversation",
            sub_intent="fallback",
            confidence=0.45,
            reason="No strong lexical, semantic or LLM match. Falling back to conversation.",
            matched_rule="tier4:fallback",
        )

    async def _semantic_classify(self, text: str) -> IntentResult | None:
        tokens = [token for token in re.split(r"[^a-z0-9]+", text) if token]
        if len(tokens) < 2:
            return None

        if not self._template_embeddings:
            await self._warm_template_embeddings()

        query_embeddings = await switchable_ai_gateway.generate_embeddings([text])
        query_vector = query_embeddings[0] if query_embeddings else []
        if not query_vector:
            return self._token_overlap_fallback(tokens)

        best_intent = None
        best_score = 0.0
        for intent, vectors in self._template_embeddings.items():
            if not vectors:
                continue
            score = max(self._cosine_similarity(query_vector, vector) for vector in vectors)
            if score > best_score:
                best_intent = intent
                best_score = score

        if best_intent and best_score >= self._settings.intent_semantic_threshold:
            return IntentResult(
                intent=best_intent,
                sub_intent="semantic-template",
                confidence=min(0.58 + best_score * 0.35, 0.89),
                reason="Matched semantic template embeddings.",
                matched_rule="tier2:semantic-embedding",
            )

        token_result = self._token_overlap_fallback(tokens)
        if token_result and token_result.confidence >= 0.55:
            return token_result
        return None

    async def _warm_template_embeddings(self) -> None:
        for intent, templates in self._semantic_templates.items():
            embeddings = await switchable_ai_gateway.generate_embeddings(templates)
            self._template_embeddings[intent] = embeddings

    def _token_overlap_fallback(self, tokens: list[str]) -> IntentResult | None:
        token_counts = Counter(tokens)
        best_intent = None
        best_score = 0.0
        for intent, templates in self._semantic_templates.items():
            template_tokens = {
                token
                for template in templates
                for token in re.split(r"[^a-z0-9]+", template.lower())
                if token
            }
            overlap = sum(token_counts[token] for token in template_tokens)
            score = overlap / max(len(tokens), 1)
            if score > best_score:
                best_intent = intent
                best_score = score

        if best_intent and best_score >= 0.25:
            return IntentResult(
                intent=best_intent,
                sub_intent="token-overlap",
                confidence=min(0.42 + best_score, 0.67),
                reason="Matched semantic template by token overlap fallback.",
                matched_rule="tier2:token-overlap",
            )
        return None

    async def _llm_fallback(self, text: str, state: OrchestratorState) -> IntentResult | None:
        mode = state.get("mode", "AUTO")
        mode_value = mode.value if hasattr(mode, "value") else str(mode)
        fallback = {
            "intent": "recommendation" if mode_value == "ADVISOR" else "conversation",
            "sub_intent": "llm-fallback",
            "confidence": 0.52 if mode_value == "ADVISOR" else 0.46,
            "reason": "LLM fallback defaulted from chat mode bias.",
        }
        prompt = (
            "Phan loai intent cho yeu cau cua TechHub.\n"
            "Tap intent hop le: recommendation, knowledge, data_query, file_analysis, visualization, conversation.\n"
            "Tra ve JSON only voi keys: intent, sub_intent, confidence, reason.\n"
            "confidence la so thuc trong [0,1].\n"
            "Neu user can lam ro them thi chon conversation va sub_intent=clarify.\n"
            f"Mode: {mode_value}\n"
            f"User input: {text}\n"
        )
        result = await switchable_ai_gateway.generate_structured_json(prompt=prompt, fallback_payload=fallback)
        if not isinstance(result, dict):
            return None

        intent = str(result.get("intent") or fallback["intent"])
        if intent not in self._semantic_templates:
            intent = fallback["intent"]

        try:
            confidence = float(result.get("confidence") or fallback["confidence"])
        except (TypeError, ValueError):
            confidence = float(fallback["confidence"])

        return IntentResult(
            intent=intent,
            sub_intent=str(result.get("sub_intent") or fallback["sub_intent"]),
            confidence=max(0.0, min(confidence, 0.8)),
            reason=str(result.get("reason") or "Resolved by structured LLM fallback."),
            matched_rule="tier3:llm-fallback",
        )

    @staticmethod
    def _cosine_similarity(left: list[float], right: list[float]) -> float:
        if not left or not right or len(left) != len(right):
            return 0.0
        dot = sum(a * b for a, b in zip(left, right))
        left_norm = math.sqrt(sum(a * a for a in left))
        right_norm = math.sqrt(sum(b * b for b in right))
        if not left_norm or not right_norm:
            return 0.0
        return dot / (left_norm * right_norm)


intent_router = IntentRouter()
