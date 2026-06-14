from __future__ import annotations

import math
import re
import unicodedata
from collections import Counter
from typing import Any

from app.core.config import get_settings
from app.orchestration.memory.personal_memory import (
    asks_assistant_favorite_color,
    asks_personal_context_recall,
    extract_user_memory_facts,
)
from app.orchestration.router.intent_result import IntentResult
from app.orchestration.state.orchestrator_state import OrchestratorState
from app.services.llm_gateway import switchable_ai_gateway


# Concrete technology topics. A question naming one of these together with a
# course/learning context ("khóa học java", "muốn học docker") is a catalog
# search, not a profile-anchored recommendation. Vague role words
# (backend/frontend/ai) are intentionally excluded so they keep going to the
# recommender. Kept in sync with the topic extractor in entity_extraction_node.
_COURSE_TOPIC_KEYWORDS = (
    "java", "python", "javascript", "typescript", "react", "nextjs", "next js",
    "node", "nodejs", "spring", "spring boot", "database", "postgresql", "postgres",
    "sql", "nosql", "mongodb", "redis", "docker", "kubernetes", "devops", "cloud",
    "aws", "azure", "gcp", "spark", "etl", "kafka", "microservice", "graphql",
    "security", "bao mat", "testing", "kiem thu", "machine learning",
    "deep learning", "html", "css", "git", "github", "postman", "linux",
    "golang", "rust", "php", "ruby", "kotlin", "swift",
)
_COURSE_TOPIC_ALT = "|".join(re.escape(keyword) for keyword in _COURSE_TOPIC_KEYWORDS)


class IntentRouter:
    def __init__(self) -> None:
        self._settings = get_settings()
        self._rules: list[tuple[str, str, list[str]]] = [
            ("file_analysis", "uploaded-file", [r"\b(pdf|file|tai lieu|docx|document|phan tich file)\b"]),
            ("visualization", "chart", [r"\b(chart|bieu do|visual|plot|dashboard|do thi)\b"]),
            # Personal learning-history lookups ("which courses have I studied",
            # "khóa học của tôi", "đã đăng ký khóa nào của giảng viên nào") are a
            # data query over the user's own enrollments — NOT a recommendation.
            # Must precede the semantic tier, which otherwise mis-routes phrases
            # mentioning "khoa hoc" to the recommendation template.
            ("data_query", "learning-history", [
                r"\b(da hoc|da hoan thanh|da dang ky|dang theo hoc|toi dang hoc|khoa hoc cua toi|khoa cua toi|lich su hoc|khoa hoc nao cua|hoc khoa hoc nao)\b",
            ]),
            # Instructor-centric course questions ("giảng viên A có bao nhiêu khóa
            # học", "tôi đang học khóa của giảng viên nào") resolve to analytics
            # over courses/instructors.
            ("data_query", "instructor-courses", [
                r"\b(giang vien|giao vien|instructor)\b.*\b(khoa hoc|khoa nao|bao nhieu|day|gom|so huu)\b",
                r"\b(khoa hoc|khoa nao)\b.*\b(giang vien|giao vien|instructor)\b",
            ]),
            # Catalog / discovery questions over courses, lessons, learning
            # paths and blogs ("có những khóa học nào", "lộ trình X gồm khóa
            # nào", "khóa nào rẻ nhất", "blog về Docker"). These resolve to
            # deterministic analytics templates.
            ("data_query", "catalog", [
                r"\b(blog|bai viet)\b",
                r"\b(lo trinh|learning path)\b",
                r"\b(danh sach|liet ke|co nhung|nhung khoa hoc|co khoa hoc|khoa hoc ve|khoa hoc nao)\b",
                r"\b(bai hoc nao|gom nhung|gom bai|chuong nao)\b",
                r"\b(gia|hoc phi|mien phi|free|dat nhat|re nhat|gia re|gia cao)\b",
                # "khóa ... về <chủ đề>" / "có khóa nào về X" — a topic-scoped
                # course lookup. Matches even when phrased politely as "gợi ý",
                # so it resolves deterministically instead of going to the
                # profile-anchored recommender.
                r"\bkhoa\b.{0,40}\bve\b",
                r"\bcourses?\b.{0,40}\b(about|on)\b",
                # "khóa học java", "java course", "muốn học docker" — a concrete
                # tech topic in a course/learning context is a catalog search.
                rf"\b(khoa hoc|khoa|course|mon hoc)\b.{{0,30}}\b({_COURSE_TOPIC_ALT})\b",
                rf"\b({_COURSE_TOPIC_ALT})\b.{{0,20}}\b(khoa hoc|khoa|course|mon hoc)\b",
                rf"\bhoc\b.{{0,15}}\b({_COURSE_TOPIC_ALT})\b",
            ]),
            ("data_query", "analytics", [r"\b(so lieu|thong ke|bao nhieu|analytics|report|tong hop|bang du lieu|truy van|query)\b"]),
            # Personalized "what to learn next" ("gợi ý khóa học tiếp theo", "nên
            # học gì tiếp theo", "học tiếp khóa nào"). Continuation phrasing means
            # the learner wants suggestions anchored on what they're already
            # taking — route to data_query so the planner picks
            # recommended_next_courses, NOT the generic RAG recommender below.
            ("data_query", "next-course", [
                r"\b(tiep theo|hoc tiep|ke tiep)\b",
                r"\bnen hoc gi\b",
            ]),
            ("recommendation", "course-advice", [r"\b(goi y|de xuat|recommend|phu hop|nen hoc)\b"]),
            ("conversation", "profile-identity", [r"\b(ten toi|ten cua toi|toi ten|toi la ai|ten minh|ten cua minh|minh ten|what is my name|who am i)\b"]),
            ("knowledge", "lesson-qa", [r"\b(giai thich|khai niem|la gi|ai la|who is|how|tai sao)\b"]),
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
        raw_text = state.get("user_input", "")
        text = self._normalize_text(raw_text)

        if state.get("has_fresh_file_context") and (not text.strip() or self._is_file_reference(text)):
            return IntentResult(
                intent="file_analysis",
                sub_intent="file-context",
                confidence=0.98,
                reason="Detected file context attached to request.",
                matched_rule="tier0:file-context",
            )

        if state.get("file_contexts") and self._is_file_reference(text):
            return IntentResult(
                intent="file_analysis",
                sub_intent="session-file-context",
                confidence=0.82,
                reason="Detected previously attached file context referenced by the current question.",
                matched_rule="tier0:session-file-context",
            )

        if (
            asks_personal_context_recall(text)
            or asks_assistant_favorite_color(text)
            or bool(extract_user_memory_facts(raw_text))
        ):
            return IntentResult(
                intent="conversation",
                sub_intent="personal-context-recall",
                confidence=0.9,
                reason="Detected a user-memory/context conversation.",
                matched_rule="tier0:personal-context-recall",
            )

        # Tier-0 follow-up refinement: if there's an active analysis in the
        # request context AND the user's utterance looks like a refinement
        # (short + contains filter/chart/compare verbs), route it back to
        # analytics so the prior query can be refined server-side.
        refine_result = self._match_active_analysis_refine(text, state)
        if refine_result is not None:
            return refine_result

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

        llm_result = await self._llm_fallback(raw_text, state)
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
            normalized_templates = [self._normalize_text(template) for template in templates]
            embeddings = await switchable_ai_gateway.generate_embeddings(normalized_templates)
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
    def _match_active_analysis_refine(
        normalized_text: str,
        state: OrchestratorState,
    ) -> IntentResult | None:
        request_context = state.get("request_context")
        if not isinstance(request_context, dict):
            return None
        prior = request_context.get("activeAnalysis")
        if not isinstance(prior, dict):
            return None
        if not normalized_text.strip():
            return None
        tokens = normalized_text.split()
        if len(tokens) > 18:
            return None
        chart_tokens = (
            "doi sang",
            "chuyen sang",
            "ve lai",
            "chuyen qua",
            "doi thanh",
            "switch to",
            "change to",
            "bieu do duong",
            "bieu do cot",
            "bieu do tron",
            "line chart",
            "bar chart",
            "pie chart",
        )
        filter_tokens = (
            "chi lay",
            "chi hien",
            "chi giu",
            "loc",
            "filter",
            "bo cac",
            "bo nhung",
            "them so sanh",
            "so sanh voi",
            "trong thang",
            "thang nay",
            "hom nay",
            "tuan nay",
            "nam nay",
            "beginner",
            "intermediate",
            "advanced",
            "dang hoc",
            "da hoc",
            "hoan thanh",
        )
        phrase = f" {normalized_text} "
        if any(token in phrase for token in chart_tokens):
            return IntentResult(
                intent="visualization",
                sub_intent="active-analysis-refine",
                confidence=0.82,
                reason="Follow-up on active analysis that mentions a chart swap.",
                matched_rule="tier0:active-analysis-refine:chart",
            )
        if any(token in phrase for token in filter_tokens):
            return IntentResult(
                intent="data_query",
                sub_intent="active-analysis-refine",
                confidence=0.78,
                reason="Follow-up on active analysis that mentions a filter/compare refinement.",
                matched_rule="tier0:active-analysis-refine:filter",
            )
        return None

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

    @staticmethod
    def _normalize_text(text: str) -> str:
        lowered = (text or "").lower().strip().replace("đ", "d")
        normalized = unicodedata.normalize("NFD", lowered)
        without_marks = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
        return re.sub(r"\s+", " ", without_marks)

    @staticmethod
    def _normalize_text(text: str) -> str:
        lowered = (text or "").lower().strip().replace("\u0111", "d").replace("Ä‘", "d").replace("Ã„â€˜", "d")
        normalized = unicodedata.normalize("NFD", lowered)
        without_marks = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
        return re.sub(r"\s+", " ", without_marks)


    @staticmethod
    def _is_file_reference(text: str) -> bool:
        if not text:
            return False
        return bool(
            re.search(
                r"\b(file|tai lieu|document|pdf|docx|upload|tep|noi dung nay|tai lieu nay|file nay|tep nay)\b",
                text,
            )
        )


intent_router = IntentRouter()
