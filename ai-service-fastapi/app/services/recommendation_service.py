from __future__ import annotations

from datetime import datetime, timezone
from typing import Any
from uuid import UUID

from sqlalchemy import select

from app.core.enums import AiTaskStatus, AiTaskType, RecommendationMode
from app.db.models import AiGenerationTaskModel
from app.db.session import get_db_session
from app.schemas.recommendation import (
    RecommendationHistoryItem,
    RecommendationItem,
    RecommendationRequest,
    RecommendationResponse,
)
from app.services.catalog_service import catalog_service
from app.services.llm_gateway import switchable_ai_gateway
from app.services.provider_config import provider_config_service
from app.core.config import get_settings
from app.services.runtime_request_context import runtime_request_context_service
from app.services.vector_service import vector_service


class RecommendationService:
    def __init__(self) -> None:
        self._settings = get_settings()

    async def generate(self, request: RecommendationRequest) -> RecommendationResponse:
        with runtime_request_context_service.begin(scope="recommendation_generate") as runtime_state:
            user_id = str(request.userId)
            user_profile = await catalog_service.fetch_user_profile(user_id)
            course_history = await catalog_service.fetch_user_course_history(user_id)
            learning_paths = await catalog_service.fetch_user_learning_paths(user_id)

            history_course_ids = {
                str(item["course_id"])
                for item in course_history
                if item.get("course_id")
            }
            completed_ids = {
                str(item["course_id"])
                for item in course_history
                if item.get("course_id")
                and (item.get("status") == "COMPLETED" or item.get("progress", 0.0) >= 0.95)
            }
            excluded_ids = {str(course_id) for course_id in request.excludeCourseIds or []}.union(history_course_ids)

            query = self._build_query(request, user_profile, course_history, learning_paths)
            reranked: list[dict[str, Any]] = []
            similar_profiles: list[dict[str, Any]] = []
            prompt: str
            pipeline = "live"
            if not self._settings.business_safe_mode_enabled:
                try:
                    similar_profiles = await vector_service.search_similar_profiles(
                        query=query,
                        limit=6,
                        exclude_user_ids=[user_id],
                    )
                    vector_candidates = await vector_service.search_courses(
                        query=query,
                        limit=15,
                        language=request.language,
                        exclude_course_ids=excluded_ids,
                    )
                    reranked = self._rerank_candidates(
                        vector_candidates=vector_candidates,
                        request=request,
                        course_history=course_history,
                        learning_paths=learning_paths,
                        similar_profiles=similar_profiles,
                    )
                except Exception:
                    pipeline = "fallback"
            else:
                pipeline = "fallback"

            if not reranked:
                latest_courses = await catalog_service.fetch_published_courses(limit=8)
                reranked = [
                    {
                        "score": 0.35,
                        "payload": course,
                        "signals": ["new_catalog_course"],
                    }
                    for course in latest_courses
                    if course["id"] not in excluded_ids
                ]
                pipeline = "fallback"

            fallback_items = [self._build_item(candidate, request, course_history) for candidate in reranked[:5]]
            prompt = self._build_prompt(request, user_profile, course_history, learning_paths, reranked[:10])
            ai_payload = await switchable_ai_gateway.generate_structured_json(
                prompt=prompt,
                fallback_payload={"recommendations": [item.model_dump(mode="json") for item in fallback_items]},
            )
            recommendations = self._normalize_ai_payload(ai_payload, reranked, request, course_history)
            if not recommendations:
                recommendations = fallback_items
                pipeline = "fallback"

            task_type = (
                AiTaskType.RECOMMENDATION_SCHEDULED
                if request.mode == RecommendationMode.SCHEDULED
                else AiTaskType.RECOMMENDATION_REALTIME
            )
            task_id = await self._persist_generation(
                task_type=task_type,
                target_reference=user_id,
                request_payload=request.model_dump(mode="json"),
                prompt=prompt,
                result_payload={"recommendations": [item.model_dump(mode="json") for item in recommendations]},
            )

            citations = [
                {
                    "kind": "course",
                    "courseId": candidate["payload"].get("id") or candidate["payload"].get("course_id"),
                    "title": candidate["payload"].get("title"),
                    "score": candidate.get("score"),
                    "signals": candidate.get("signals", []),
                }
                for candidate in reranked[:5]
            ]
            return RecommendationResponse(
                taskId=task_id,
                recommendations=recommendations,
                metadata={
                    "userId": user_id,
                    "generatedAt": datetime.now(timezone.utc).isoformat(),
                    "totalRecommendations": len(recommendations),
                    "status": AiTaskStatus.COMPLETED.value,
                    "mode": request.mode.value,
                    "query": query,
                    "pipeline": pipeline,
                    "requestId": runtime_state.request_id,
                    "tokenUsage": runtime_request_context_service.snapshot(),
                    "citations": citations,
                    "excludedCourseIds": sorted(excluded_ids),
                    "similarProfiles": [
                        {
                            "userId": item.get("payload", {}).get("user_id"),
                            "score": item.get("score"),
                        }
                        for item in similar_profiles[:3]
                    ],
                },
            )

    async def get_history(
        self,
        user_id: UUID,
        *,
        mode: RecommendationMode | None = None,
        limit: int = 20,
    ) -> list[RecommendationHistoryItem]:
        allowed_types = [AiTaskType.RECOMMENDATION_REALTIME.value, AiTaskType.RECOMMENDATION_SCHEDULED.value]
        async with get_db_session() as session:
            stmt = (
                select(AiGenerationTaskModel)
                .where(AiGenerationTaskModel.target_reference == str(user_id))
                .where(AiGenerationTaskModel.task_type.in_(allowed_types))
                .where(AiGenerationTaskModel.is_active == "Y")
                .order_by(AiGenerationTaskModel.created.desc())
                .limit(max(1, min(limit, 100)))
            )
            result = await session.execute(stmt)
            tasks = result.scalars().all()

        history: list[RecommendationHistoryItem] = []
        for task in tasks:
            task_mode = (
                RecommendationMode.SCHEDULED
                if task.task_type == AiTaskType.RECOMMENDATION_SCHEDULED.value
                else RecommendationMode.REALTIME
            )
            if mode is not None and task_mode != mode:
                continue

            payload = task.result_payload if isinstance(task.result_payload, dict) else {}
            items = payload.get("recommendations") if isinstance(payload, dict) else []
            recommendations = self._coerce_history_items(items)
            history.append(
                RecommendationHistoryItem(
                    taskId=str(task.id),
                    mode=task_mode,
                    status=task.status,
                    createdAt=task.created.astimezone(timezone.utc).isoformat(),
                    recommendations=recommendations,
                    metadata={
                        "model": task.model_used,
                        "request": task.request_payload,
                    },
                )
            )
        return history

    def _build_query(
        self,
        request: RecommendationRequest,
        user_profile: dict[str, Any] | None,
        course_history: list[dict[str, Any]],
        learning_paths: list[dict[str, Any]],
    ) -> str:
        active_skills = sorted({skill for item in course_history for skill in item.get("skills", [])})
        in_progress_titles = [item["title"] for item in course_history if item.get("status") == "IN_PROGRESS"]
        active_path_titles = [path["title"] for path in learning_paths[:2]]
        profile_history = user_profile.get("learning_history") if user_profile else {}

        segments = []
        if request.preferredLanguages:
            segments.append("preferred_topics=" + ", ".join(request.preferredLanguages))
        segments.append(f"response_language={request.language}")
        if active_skills:
            segments.append("known_skills=" + ", ".join(active_skills[:8]))
        if in_progress_titles:
            segments.append("in_progress=" + ", ".join(in_progress_titles[:3]))
        if active_path_titles:
            segments.append("active_paths=" + ", ".join(active_path_titles))
        if isinstance(profile_history, dict) and profile_history:
            interests = profile_history.get("interests") or profile_history.get("goals")
            if interests:
                if isinstance(interests, list):
                    segments.append("profile_interests=" + ", ".join(str(item) for item in interests))
                else:
                    segments.append(f"profile_interests={interests}")
        return "\n".join(segments)

    def _rerank_candidates(
        self,
        *,
        vector_candidates: list[dict[str, Any]],
        request: RecommendationRequest,
        course_history: list[dict[str, Any]],
        learning_paths: list[dict[str, Any]],
        similar_profiles: list[dict[str, Any]],
    ) -> list[dict[str, Any]]:
        history_by_course = {item["course_id"]: item for item in course_history if item.get("course_id")}
        user_skills = {skill.lower() for item in course_history for skill in item.get("skills", [])}
        active_path_courses = {
            course["course_id"]
            for path in learning_paths
            if path.get("completion", 0.0) < 1.0
            for course in path.get("courses", [])
        }
        similar_profile_completed_counts: dict[str, int] = {}
        similar_profile_in_progress_counts: dict[str, int] = {}
        for profile in similar_profiles:
            payload = profile.get("payload", {})
            for history_item in payload.get("course_history", []) or []:
                course_id = str(history_item.get("course_id") or "")
                if not course_id:
                    continue
                status = str(history_item.get("status") or "").upper()
                if status == "COMPLETED":
                    similar_profile_completed_counts[course_id] = (
                        similar_profile_completed_counts.get(course_id, 0) + 1
                    )
                elif status in {"IN_PROGRESS", "ENROLLED"}:
                    similar_profile_in_progress_counts[course_id] = (
                        similar_profile_in_progress_counts.get(course_id, 0) + 1
                    )

        reranked: list[dict[str, Any]] = []
        max_score = max((float(item.get("score") or 0.0) for item in vector_candidates), default=1.0)

        for candidate in vector_candidates:
            payload = candidate.get("payload", {})
            course_id = str(payload.get("id") or payload.get("course_id") or "")
            if not course_id:
                continue
            if course_id in history_by_course and history_by_course[course_id].get("status") == "COMPLETED":
                continue

            score = (float(candidate.get("score") or 0.0) / max_score) if max_score else 0.0
            signals: list[str] = []

            if str(payload.get("language") or "").lower() == request.language.lower():
                score += 0.08
                signals.append("language_match")

            course_level = str(payload.get("level") or "").lower()
            if course_level == "beginner" and not course_history:
                score += 0.1
                signals.append("beginner_friendly")
            elif course_level == "intermediate" and any(
                item.get("status") == "COMPLETED" and str(item.get("level") or "").lower() == "beginner"
                for item in course_history
            ):
                score += 0.06
                signals.append("next_level_progression")

            course_skills = {str(skill).lower() for skill in payload.get("skills", [])}
            overlap = sorted(user_skills.intersection(course_skills))
            if overlap:
                score += min(0.12, 0.03 * len(overlap))
                signals.append("skill_overlap:" + ", ".join(overlap[:3]))
            elif course_skills:
                score += 0.04
                signals.append("new_skill_surface")

            if course_id in active_path_courses:
                score += 0.05
                signals.append("active_learning_path")

            similar_completed = similar_profile_completed_counts.get(course_id, 0)
            if similar_completed:
                score += min(0.15, 0.03 * similar_completed)
                signals.append(f"similar_learners_completed:{similar_completed}")

            similar_active = similar_profile_in_progress_counts.get(course_id, 0)
            if similar_active:
                score += min(0.09, 0.02 * similar_active)
                signals.append(f"similar_learners_active:{similar_active}")

            reranked.append(
                {
                    "score": round(min(score, 0.99), 4),
                    "payload": payload,
                    "signals": signals,
                }
            )

        reranked.sort(key=lambda item: item["score"], reverse=True)
        return reranked

    def _build_prompt(
        self,
        request: RecommendationRequest,
        user_profile: dict[str, Any] | None,
        course_history: list[dict[str, Any]],
        learning_paths: list[dict[str, Any]],
        candidates: list[dict[str, Any]],
    ) -> str:
        candidate_lines = []
        for index, candidate in enumerate(candidates, start=1):
            payload = candidate["payload"]
            candidate_lines.append(
                (
                    f"{index}. id={payload.get('id') or payload.get('course_id')} | "
                    f"title={payload.get('title')} | level={payload.get('level')} | "
                    f"language={payload.get('language')} | skills={payload.get('skills', [])} | "
                    f"tags={payload.get('tags', [])} | base_score={candidate['score']} | "
                    f"signals={candidate.get('signals', [])} | description={payload.get('description', '')}"
                )
            )

        history_lines = [
            (
                f"- {item.get('title')} | status={item.get('status')} | "
                f"progress={item.get('progress')} | skills={item.get('skills', [])}"
            )
            for item in course_history[:8]
        ]
        path_lines = [
            f"- {path.get('title')} | completion={path.get('completion')} | courses={len(path.get('courses', []))}"
            for path in learning_paths[:5]
        ]

        return (
            "Ban la he thong goi y khoa hoc TechHub.\n"
            "Chi duoc su dung cac khoa hoc co trong danh sach ung vien. Khong duoc tao courseId moi.\n"
            "Tra ve JSON only voi key 'recommendations' la array. Moi phan tu gom:\n"
            "courseId, title, description, score, reason, tags, estimatedDuration.\n"
            "Score phai nam trong [0,1]. Ly do phai ngan gon va dua tren match thuc te.\n\n"
            f"Recommendation mode: {request.mode.value}\n"
            f"User language: {request.language}\n"
            f"Preferred topics: {request.preferredLanguages or []}\n"
            f"User profile: {user_profile or {}}\n"
            "Course history:\n"
            + ("\n".join(history_lines) if history_lines else "- no course history")
            + "\nLearning path progress:\n"
            + ("\n".join(path_lines) if path_lines else "- no active learning path")
            + "\nCandidate courses:\n"
            + ("\n".join(candidate_lines) if candidate_lines else "- no candidates")
            + "\nReturn top 5 recommendations."
        )

    def _normalize_ai_payload(
        self,
        payload: dict[str, Any],
        reranked: list[dict[str, Any]],
        request: RecommendationRequest,
        course_history: list[dict[str, Any]],
    ) -> list[RecommendationItem]:
        items = payload.get("recommendations") if isinstance(payload, dict) else None
        if not isinstance(items, list):
            return []

        candidate_map = {
            str(candidate["payload"].get("id") or candidate["payload"].get("course_id")): candidate
            for candidate in reranked
        }
        normalized: list[RecommendationItem] = []
        for item in items:
            if not isinstance(item, dict):
                continue
            course_id = str(item.get("courseId") or item.get("id") or "")
            candidate = candidate_map.get(course_id)
            if candidate is None:
                continue

            payload_row = candidate["payload"]
            normalized.append(
                RecommendationItem(
                    courseId=course_id,
                    title=str(item.get("title") or payload_row.get("title") or ""),
                    description=str(item.get("description") or payload_row.get("description") or ""),
                    score=float(item.get("score") or candidate["score"]),
                    reason=str(item.get("reason") or self._reason_from_signals(candidate["signals"], payload_row, course_history)),
                    tags=self._coerce_tags(item.get("tags"), payload_row),
                    estimatedDuration=str(
                        item.get("estimatedDuration")
                        or self._estimate_duration(payload_row, request.mode)
                    ),
                )
            )
        return normalized[:5]

    def _build_item(
        self,
        candidate: dict[str, Any],
        request: RecommendationRequest,
        course_history: list[dict[str, Any]],
    ) -> RecommendationItem:
        payload = candidate["payload"]
        return RecommendationItem(
            courseId=str(payload.get("id") or payload.get("course_id")),
            title=str(payload.get("title") or ""),
            description=str(payload.get("description") or ""),
            score=float(candidate["score"]),
            reason=self._reason_from_signals(candidate.get("signals", []), payload, course_history),
            tags=self._coerce_tags(None, payload),
            estimatedDuration=self._estimate_duration(payload, request.mode),
        )

    @staticmethod
    def _reason_from_signals(signals: list[str], payload: dict[str, Any], course_history: list[dict[str, Any]]) -> str:
        if signals:
            primary = signals[0]
            if primary.startswith("skill_overlap:"):
                return f"Khoa hoc nay noi tiep tot voi cac ky nang ban da co: {primary.split(':', 1)[1]}."
            if primary == "beginner_friendly":
                return "Muc do nhap mon phu hop khi ban dang o giai doan bat dau."
            if primary == "next_level_progression":
                return "No la buoc tiep theo hop ly sau cac khoa co ban ban da hoan thanh."
            if primary == "language_match":
                return "Ngon ngu va cach trinh bay cua khoa hoc phu hop voi preference hien tai."
            if primary == "active_learning_path":
                return "Khoa hoc nay nam trong lo trinh hoc ban dang theo doi."
            if primary == "new_skill_surface":
                return "Khoa hoc nay mo rong them mot nhom ky nang moi tu catalog hien co."
            if primary.startswith("similar_learners_completed:"):
                return "Nhung nguoi co ho so hoc tap gan voi ban da hoan thanh khoa hoc nay voi ket qua tot."
            if primary.startswith("similar_learners_active:"):
                return "Khoa hoc nay dang duoc nhieu nguoi co muc tieu hoc tap tuong tu ban theo hoc."

        if course_history:
            return "Khoa hoc duoc xep hang dua tren do lien quan semantic va lich su hoc tap hien co."
        return f"Khoa hoc {payload.get('title', '')} la ung vien phu hop nhat tu catalog hien tai."

    @staticmethod
    def _estimate_duration(payload: dict[str, Any], mode: RecommendationMode) -> str:
        enrollment_count = int(payload.get("enrollment_count") or 0)
        if enrollment_count >= 200:
            base = "6-8 weeks"
        elif enrollment_count >= 50:
            base = "4-6 weeks"
        else:
            base = "2-4 weeks"
        if mode == RecommendationMode.SCHEDULED:
            return base
        return base

    @staticmethod
    def _coerce_tags(ai_tags: Any, payload: dict[str, Any]) -> list[str]:
        if isinstance(ai_tags, list) and ai_tags:
            return [str(tag) for tag in ai_tags[:5]]
        payload_tags = [str(tag) for tag in (payload.get("tags") or [])[:3]]
        payload_skills = [str(skill) for skill in (payload.get("skills") or [])[:2]]
        return payload_tags or payload_skills or [str(payload.get("level") or "general")]

    @staticmethod
    def _coerce_history_items(items: Any) -> list[RecommendationItem]:
        if not isinstance(items, list):
            return []
        recommendations: list[RecommendationItem] = []
        for item in items:
            if not isinstance(item, dict):
                continue
            recommendations.append(
                RecommendationItem(
                    courseId=str(item.get("courseId") or item.get("id") or ""),
                    title=str(item.get("title") or ""),
                    description=str(item.get("description") or ""),
                    score=float(item.get("score") or 0.0),
                    reason=str(item.get("reason") or ""),
                    tags=[str(tag) for tag in item.get("tags") or []],
                    estimatedDuration=str(item.get("estimatedDuration") or ""),
                )
            )
        return recommendations

    async def _persist_generation(
        self,
        *,
        task_type: AiTaskType,
        target_reference: str,
        request_payload: dict[str, Any],
        prompt: str,
        result_payload: dict[str, Any],
        model_used: str | None = None,
    ) -> str:
        async with get_db_session() as session:
            task = AiGenerationTaskModel(
                task_type=task_type.value,
                status=AiTaskStatus.COMPLETED.value,
                target_reference=target_reference,
                request_payload=request_payload,
                result_payload=result_payload,
                prompt=prompt,
                model_used=model_used or await provider_config_service.get_active_chat_model(),
            )
            session.add(task)
            await session.flush()
            return str(task.id)

    # ------------------------------------------------------------------
    # Simple collaborative filtering pipeline (no LLM, no vector search)
    # ------------------------------------------------------------------

    @staticmethod
    def _jaccard(set_a: set[str], set_b: set[str]) -> float:
        if not set_a and not set_b:
            return 0.0
        return len(set_a & set_b) / len(set_a | set_b)

    def _score_tag_skill_collab(
        self, candidates: list[dict[str, Any]]
    ) -> list[dict[str, Any]]:
        if not candidates:
            return []

        max_co = max(c["co_count"] for c in candidates) or 1
        # All rows carry the same user_tags / user_skills — take from first row.
        user_tags: set[str] = set(candidates[0].get("user_tags") or [])
        user_skills: set[str] = set(candidates[0].get("user_skills") or [])

        for c in candidates:
            collab = c["co_count"] / max_co
            skill = self._jaccard(user_skills, set(c.get("skills") or []))
            tag = self._jaccard(user_tags, set(c.get("tags") or []))
            c["score"] = round(0.4 * collab + 0.35 * skill + 0.25 * tag, 4)

            signals: list[str] = []
            if collab > 0.5:
                signals.append("co_learners")
            if skill > 0.3:
                signals.append("skill_match")
            if tag > 0.3:
                signals.append("tag_match")
            c["signals"] = signals

        return sorted(candidates, key=lambda x: x["score"], reverse=True)

    @staticmethod
    def _reason_from_collab_signals(signals: list[str], skills: list[str]) -> str:
        parts: list[str] = []
        if "co_learners" in signals:
            parts.append("Học viên có hành trình học tương tự bạn đang học khóa này")
        if "skill_match" in signals and skills:
            skill_str = ", ".join(skills[:3])
            parts.append(f"Trùng kỹ năng: {skill_str}")
        if "tag_match" in signals:
            parts.append("Phù hợp với chủ đề bạn quan tâm")
        return ". ".join(parts) if parts else "Có thể phù hợp với bạn"

    async def generate_simple(
        self, request: RecommendationRequest, top_n: int = 5
    ) -> RecommendationResponse:
        """Lightweight recommendation — no LLM, no Qdrant.

        Uses co-enrollment collaborative filtering combined with
        tag/skill Jaccard similarity.
        """
        user_id = str(request.userId)
        candidates = await catalog_service.fetch_collaborative_candidates(
            user_id, limit=20
        )

        if not candidates:
            return RecommendationResponse(
                taskId=None,
                recommendations=[],
                metadata={"pipeline": "tag-skill-collab", "total_candidates": 0},
            )

        scored = self._score_tag_skill_collab(candidates)
        top = scored[:top_n]

        recommendations: list[RecommendationItem] = []
        for c in top:
            signals: list[str] = c.get("signals") or []
            course_skills: list[str] = c.get("skills") or []
            recommendations.append(
                RecommendationItem(
                    courseId=str(c.get("id") or ""),
                    title=str(c.get("title") or ""),
                    description=str(c.get("description") or ""),
                    score=float(c.get("score") or 0.0),
                    reason=self._reason_from_collab_signals(signals, course_skills),
                    tags=c.get("tags") or [],
                )
            )

        task_id = await self._persist_generation(
            task_type=AiTaskType.RECOMMENDATION_REALTIME,
            target_reference=user_id,
            request_payload=request.model_dump(mode="json"),
            prompt="rule-based:tag-skill-collab",
            result_payload={
                "recommendations": [r.model_dump(mode="json") for r in recommendations]
            },
            model_used="rule-based",
        )

        return RecommendationResponse(
            taskId=task_id,
            recommendations=recommendations,
            metadata={
                "pipeline": "tag-skill-collab",
                "total_candidates": len(candidates),
            },
        )


recommendation_service = RecommendationService()
