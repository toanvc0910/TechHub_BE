from __future__ import annotations

import logging
from collections.abc import Iterable
from datetime import datetime, timezone
from typing import Any
from uuid import UUID

from sqlalchemy import select

from app.core.config import get_settings
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
from app.services.observability_service import runtime_observability_service
from app.services.provider_config import provider_config_service
from app.services.runtime_request_context import runtime_request_context_service
from app.services.vector_service import vector_service

logger = logging.getLogger(__name__)


class RecommendationService:
    def __init__(self) -> None:
        self._settings = get_settings()

    async def generate(self, request: RecommendationRequest) -> RecommendationResponse:
        with runtime_request_context_service.begin(scope="recommendation_generate") as runtime_state:
            user_id = str(request.userId)
            user_profile = await catalog_service.fetch_user_profile(user_id)
            course_history = await catalog_service.fetch_user_course_history(user_id)
            learning_paths = await catalog_service.fetch_user_learning_paths(user_id)
            skill_profile = (user_profile or {}).get("skill_profile") or {}

            missing_signals: list[str] = []
            ratings: list[dict[str, Any]] = []
            ratings_failed = False
            try:
                ratings = await catalog_service.fetch_user_ratings(user_id, raise_on_error=True)
            except Exception:
                ratings_failed = True
                self._append_signal(missing_signals, "ratings")
                await runtime_observability_service.record_error(scope="recommendation_ratings")

            if not course_history:
                self._append_signal(missing_signals, "course_history")
            if not skill_profile:
                self._append_signal(missing_signals, "skill_profile")
            if not ratings and not ratings_failed:
                self._append_signal(missing_signals, "ratings")
            if not learning_paths:
                self._append_signal(missing_signals, "learning_paths")

            request_excluded_ids = {str(course_id) for course_id in request.excludeCourseIds or []}
            completed_ids = self._history_ids(course_history, buckets={"completed"})
            active_ids = self._history_ids(course_history, buckets={"in_progress", "enrolled"})
            excluded_ids = request_excluded_ids.union(completed_ids)

            query = self._build_query(request, user_profile, course_history, learning_paths, ratings)
            similar_profiles: list[dict[str, Any]] = []
            vector_candidates: list[dict[str, Any]] = []
            vector_failed = False

            if not self._settings.business_safe_mode_enabled:
                try:
                    similar_profiles = await vector_service.search_similar_profiles(
                        query=query,
                        limit=6,
                        exclude_user_ids=[user_id],
                    )
                    if not similar_profiles:
                        self._append_signal(missing_signals, "similar_learners")
                    elif any(
                        str(item.get("retrievalMode") or "") == "lexical_fallback"
                        for item in similar_profiles
                    ):
                        # Vector path down or empty -> service silently fell back to
                        # lexical search. Surface this so callers know the signal is
                        # weaker than usual.
                        vector_failed = True
                        self._append_signal(missing_signals, "similar_learners_lexical_fallback")
                except Exception:
                    logger.warning("Failed to search similar learner profiles", exc_info=True)
                    vector_failed = True
                    self._append_signal(missing_signals, "similar_learners")
                    await runtime_observability_service.record_error(scope="recommendation_similar_profiles")

                try:
                    vector_candidates = await vector_service.search_courses(
                        query=query,
                        limit=15,
                        language=request.language,
                        exclude_course_ids=excluded_ids,
                    )
                    if not vector_candidates:
                        self._append_signal(missing_signals, "course_vector")
                    elif any(
                        str(item.get("retrievalMode") or "") == "lexical_fallback"
                        for item in vector_candidates
                    ):
                        vector_failed = True
                        self._append_signal(missing_signals, "course_vector_lexical_fallback")
                except Exception:
                    logger.warning("Failed to search course vectors", exc_info=True)
                    vector_failed = True
                    self._append_signal(missing_signals, "course_vector")
                    await runtime_observability_service.record_error(scope="recommendation_course_vector")
            else:
                vector_failed = True
                self._append_signal(missing_signals, "course_vector")
                self._append_signal(missing_signals, "similar_learners")

            continue_candidates = await self._course_candidates_from_ids(
                active_ids.difference(request_excluded_ids),
                score=0.82,
                signals=["continue_learning"],
            )
            path_candidate_ids = self._active_path_course_ids(learning_paths).difference(excluded_ids)
            path_candidates = await self._course_candidates_from_ids(
                path_candidate_ids,
                score=0.72,
                signals=["path_alignment"],
            )

            candidates = self._merge_candidates([*continue_candidates, *path_candidates, *vector_candidates])
            candidate_course_ids = self._candidate_ids(candidates)
            fallback_catalog_used = False

            reranked, filtered_course_ids = self._rerank_candidates(
                candidates=candidates,
                request=request,
                course_history=course_history,
                ratings=ratings,
                learning_paths=learning_paths,
                similar_profiles=similar_profiles,
                skill_profile=skill_profile,
                excluded_ids=excluded_ids,
            )

            if not reranked:
                latest_courses = await catalog_service.fetch_published_courses(limit=12)
                fallback_candidates = [
                    {
                        "score": 0.35,
                        "payload": course,
                        "signals": ["catalog_quality"],
                        "source": "catalog",
                    }
                    for course in latest_courses
                    if course["id"] not in excluded_ids
                ]
                fallback_catalog_used = True
                candidates = self._merge_candidates(fallback_candidates)
                candidate_course_ids = self._candidate_ids(candidates)
                reranked, filtered_course_ids = self._rerank_candidates(
                    candidates=candidates,
                    request=request,
                    course_history=course_history,
                    ratings=ratings,
                    learning_paths=learning_paths,
                    similar_profiles=similar_profiles,
                    skill_profile=skill_profile,
                    excluded_ids=excluded_ids,
                )

            fallback_items = [self._build_item(candidate, request, course_history) for candidate in reranked[:5]]
            prompt = self._build_prompt(request, user_profile, course_history, learning_paths, ratings, reranked[:10])
            ai_payload = await switchable_ai_gateway.generate_structured_json(
                prompt=prompt,
                fallback_payload={"recommendations": [item.model_dump(mode="json") for item in fallback_items]},
            )
            recommendations = self._normalize_ai_payload(ai_payload, reranked, request, course_history)
            if not recommendations:
                recommendations = fallback_items
                self._append_signal(missing_signals, "llm_structured_output")

            used_signals = self._used_candidate_signals(reranked[:5])
            pipeline = self._resolve_pipeline(
                course_history=course_history,
                ratings=ratings,
                learning_paths=learning_paths,
                skill_profile=skill_profile,
                fallback_catalog_used=fallback_catalog_used,
                vector_failed=vector_failed,
                missing_signals=missing_signals,
            )
            metadata = {
                "userId": user_id,
                "generatedAt": datetime.now(timezone.utc).isoformat(),
                "totalRecommendations": len(recommendations),
                "status": AiTaskStatus.COMPLETED.value,
                "mode": request.mode.value,
                "query": query,
                "pipeline": pipeline,
                "usedSignals": used_signals,
                "missingSignals": sorted(missing_signals),
                "candidateCourseIds": candidate_course_ids,
                "filteredCourseIds": sorted(filtered_course_ids),
                "excludedCourseIds": sorted(excluded_ids),
                "requestId": runtime_state.request_id,
                "tokenUsage": runtime_request_context_service.snapshot(),
                "citations": self._build_citations(reranked[:5]),
                "similarProfiles": [
                    {
                        "userId": item.get("payload", {}).get("user_id"),
                        "score": item.get("score"),
                    }
                    for item in similar_profiles[:3]
                ],
            }

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
                result_payload={
                    "recommendations": [item.model_dump(mode="json") for item in recommendations],
                    "metadata": metadata,
                },
            )

            return RecommendationResponse(
                taskId=task_id,
                recommendations=recommendations,
                metadata=metadata,
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
            stored_metadata = payload.get("metadata") if isinstance(payload.get("metadata"), dict) else {}
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
                        **stored_metadata,
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
        ratings: list[dict[str, Any]],
    ) -> str:
        active_skills = sorted({skill for item in course_history for skill in item.get("skills", [])})
        in_progress_titles = [
            item["title"]
            for item in course_history
            if item.get("historyBucket") in {"in_progress", "enrolled"} or item.get("status") == "IN_PROGRESS"
        ]
        active_path_titles = [path["title"] for path in learning_paths[:2]]
        liked_topics = [item["title"] for item in ratings if int(item.get("score") or 0) >= 4]
        profile_history = user_profile.get("learning_history") if user_profile else {}

        segments = []
        if request.preferredLanguages:
            segments.append("preferred_topics=" + ", ".join(request.preferredLanguages))
        segments.append(f"response_language={request.language}")
        if active_skills:
            segments.append("known_skills=" + ", ".join(active_skills[:8]))
        if in_progress_titles:
            segments.append("continue_courses=" + ", ".join(in_progress_titles[:3]))
        if liked_topics:
            segments.append("liked_courses=" + ", ".join(liked_topics[:4]))
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

    async def _course_candidates_from_ids(
        self,
        course_ids: Iterable[str],
        *,
        score: float,
        signals: list[str],
    ) -> list[dict[str, Any]]:
        ordered_ids = [str(course_id) for course_id in course_ids if course_id]
        courses = await catalog_service.fetch_courses_by_ids(ordered_ids)
        return [
            {
                "score": score,
                "payload": course,
                "signals": list(signals),
                "source": "postgres",
            }
            for course in courses
        ]

    def _rerank_candidates(
        self,
        *,
        candidates: list[dict[str, Any]],
        request: RecommendationRequest,
        course_history: list[dict[str, Any]],
        ratings: list[dict[str, Any]],
        learning_paths: list[dict[str, Any]],
        similar_profiles: list[dict[str, Any]],
        skill_profile: dict[str, float],
        excluded_ids: set[str],
    ) -> tuple[list[dict[str, Any]], set[str]]:
        history_by_course = {str(item["course_id"]): item for item in course_history if item.get("course_id")}
        user_skills = {
            str(skill).lower()
            for item in course_history
            for skill in item.get("skills", [])
            if skill
        }.union(str(skill).lower() for skill in skill_profile)
        active_path_courses = self._active_path_course_ids(learning_paths)
        liked_skills = {
            str(skill).lower()
            for rating in ratings
            if int(rating.get("score") or 0) >= 4
            for skill in rating.get("skills", [])
            if skill
        }
        avoid_skills = {
            str(skill).lower()
            for rating in ratings
            if int(rating.get("score") or 0) <= 2
            for skill in rating.get("skills", [])
            if skill
        }
        similar_counts = self._similar_profile_course_counts(similar_profiles)
        max_score = max((float(item.get("score") or 0.0) for item in candidates), default=1.0) or 1.0

        reranked: list[dict[str, Any]] = []
        filtered_ids: set[str] = set()
        for candidate in candidates:
            payload = candidate.get("payload", {})
            course_id = str(payload.get("id") or payload.get("course_id") or "")
            if not course_id:
                continue
            history_item = history_by_course.get(course_id)
            if course_id in excluded_ids or (history_item and history_item.get("historyBucket") == "completed"):
                filtered_ids.add(course_id)
                continue

            score = (float(candidate.get("score") or 0.0) / max_score) if max_score else 0.0
            signals = list(candidate.get("signals") or [])

            if history_item and history_item.get("historyBucket") in {"in_progress", "enrolled"}:
                score += 0.18
                self._append_signal(signals, "continue_learning")

            if str(payload.get("language") or "").lower() == request.language.lower():
                score += 0.06
                self._append_signal(signals, "language_match")

            course_skills = {str(skill).lower() for skill in payload.get("skills", []) if skill}
            overlap = sorted(user_skills.intersection(course_skills))
            new_skills = sorted(course_skills.difference(user_skills))
            if overlap:
                score += min(0.12, 0.03 * len(overlap))
                self._append_signal(signals, "skill_match")
            if new_skills and user_skills:
                score += min(0.08, 0.02 * len(new_skills))
                self._append_signal(signals, "skill_gap")

            if course_skills.intersection(liked_skills):
                score += 0.1
                self._append_signal(signals, "liked_topic")
            if course_skills.intersection(avoid_skills):
                score -= 0.16
                self._append_signal(signals, "avoid_topic")

            if course_id in active_path_courses:
                score += 0.07
                self._append_signal(signals, "path_alignment")

            similar_count = similar_counts.get(course_id, 0)
            if similar_count:
                score += min(0.14, 0.035 * similar_count)
                self._append_signal(signals, "similar_learners")

            average_rating = float(payload.get("average_rating") or 0.0)
            rating_count = int(payload.get("rating_count") or 0)
            enrollment_count = int(payload.get("enrollment_count") or 0)
            if average_rating >= 4.0 or rating_count >= 5 or enrollment_count >= 20:
                score += min(0.08, 0.01 * rating_count + 0.0005 * enrollment_count)
                self._append_signal(signals, "catalog_quality")

            reranked.append(
                {
                    "score": round(max(0.0, min(score, 0.99)), 4),
                    "payload": payload,
                    "signals": sorted(signals),
                    "source": candidate.get("source") or "vector",
                }
            )

        reranked.sort(key=lambda item: item["score"], reverse=True)
        return reranked, filtered_ids

    def _build_prompt(
        self,
        request: RecommendationRequest,
        user_profile: dict[str, Any] | None,
        course_history: list[dict[str, Any]],
        learning_paths: list[dict[str, Any]],
        ratings: list[dict[str, Any]],
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
                    f"average_rating={payload.get('average_rating')} | rating_count={payload.get('rating_count')} | "
                    f"enrollment_count={payload.get('enrollment_count')} | score={candidate['score']} | "
                    f"signals={candidate.get('signals', [])} | description={payload.get('description', '')}"
                )
            )

        history_lines = [
            (
                f"- {item.get('title')} | status={item.get('status')} | bucket={item.get('historyBucket')} | "
                f"progress={item.get('progress')} | progress_source={item.get('progressSource')} | "
                f"skills={item.get('skills', [])}"
            )
            for item in course_history[:8]
        ]
        rating_lines = [
            f"- {item.get('title')} | score={item.get('score')} | skills={item.get('skills', [])}"
            for item in ratings[:6]
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
            "Score phai nam trong [0,1]. Ly do phai ngan gon va dua tren signals thuc te.\n\n"
            f"Recommendation mode: {request.mode.value}\n"
            f"User language: {request.language}\n"
            f"Preferred topics: {request.preferredLanguages or []}\n"
            f"User profile: {user_profile or {}}\n"
            "Course history:\n"
            + ("\n".join(history_lines) if history_lines else "- no course history")
            + "\nRatings:\n"
            + ("\n".join(rating_lines) if rating_lines else "- no ratings")
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
                    reason=str(
                        item.get("reason")
                        or self._reason_from_signals(candidate["signals"], payload_row, course_history)
                    ),
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
        del course_history
        if "continue_learning" in signals:
            return "Khóa học này đang nằm trong tiến trình học của bạn, phù hợp để học tiếp ngay."
        if "liked_topic" in signals:
            return "Chủ đề của khóa học gần với những nội dung bạn từng đánh giá cao."
        if "skill_match" in signals and "skill_gap" in signals:
            return "Khóa học vừa nối tiếp kỹ năng bạn đã có, vừa mở thêm kỹ năng còn thiếu."
        if "skill_match" in signals:
            return "Khóa học khớp với nhóm kỹ năng bạn đã thể hiện trong lịch sử học."
        if "skill_gap" in signals:
            return "Khóa học giúp mở rộng kỹ năng tiếp theo từ nền tảng hiện tại."
        if "path_alignment" in signals:
            return "Khóa học nằm trong lộ trình học bạn đang theo dõi."
        if "similar_learners" in signals:
            return "Những người học có hồ sơ gần bạn cũng quan tâm hoặc hoàn thành khóa này."
        if "catalog_quality" in signals:
            return "Khóa học có tín hiệu chất lượng tốt từ catalog như lượt học hoặc đánh giá."
        if "language_match" in signals:
            return "Ngôn ngữ của khóa học phù hợp với lựa chọn hiện tại của bạn."
        return f"Khóa học {payload.get('title', '')} là ứng viên phù hợp nhất từ dữ liệu hiện có."

    @staticmethod
    def _estimate_duration(payload: dict[str, Any], mode: RecommendationMode) -> str:
        del mode
        enrollment_count = int(payload.get("enrollment_count") or 0)
        if enrollment_count >= 200:
            return "6-8 weeks"
        if enrollment_count >= 50:
            return "4-6 weeks"
        return "2-4 weeks"

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
    ) -> str:
        async with get_db_session() as session:
            task = AiGenerationTaskModel(
                task_type=task_type.value,
                status=AiTaskStatus.COMPLETED.value,
                target_reference=target_reference,
                request_payload=request_payload,
                result_payload=result_payload,
                prompt=prompt,
                model_used=await provider_config_service.get_active_chat_model(),
            )
            session.add(task)
            await session.flush()
            return str(task.id)

    @staticmethod
    def _history_ids(course_history: list[dict[str, Any]], *, buckets: set[str]) -> set[str]:
        course_ids: set[str] = set()
        for item in course_history:
            course_id = item.get("course_id")
            if not course_id:
                continue
            bucket = str(item.get("historyBucket") or "").lower()
            status = str(item.get("status") or "").upper()
            if not bucket:
                if status == "COMPLETED" or float(item.get("progress") or 0.0) >= 0.95:
                    bucket = "completed"
                elif status == "DROPPED":
                    bucket = "abandoned"
                elif status == "IN_PROGRESS":
                    bucket = "in_progress"
                elif status == "ENROLLED":
                    bucket = "enrolled"
            if bucket in buckets:
                course_ids.add(str(course_id))
        return course_ids

    @staticmethod
    def _active_path_course_ids(learning_paths: list[dict[str, Any]]) -> set[str]:
        return {
            str(course["course_id"])
            for path in learning_paths
            if float(path.get("completion") or 0.0) < 1.0
            for course in path.get("courses", [])
            if course.get("course_id")
        }

    @classmethod
    def _merge_candidates(cls, candidates: list[dict[str, Any]]) -> list[dict[str, Any]]:
        merged: dict[str, dict[str, Any]] = {}
        for candidate in candidates:
            payload = candidate.get("payload", {})
            course_id = str(payload.get("id") or payload.get("course_id") or "")
            if not course_id:
                continue
            existing = merged.get(course_id)
            if existing is None:
                merged[course_id] = {
                    **candidate,
                    "payload": payload,
                    "signals": list(candidate.get("signals") or []),
                }
                continue
            existing["score"] = max(float(existing.get("score") or 0.0), float(candidate.get("score") or 0.0))
            for signal in candidate.get("signals") or []:
                cls._append_signal(existing["signals"], str(signal))
            if len(str(payload.get("description") or "")) > len(str(existing["payload"].get("description") or "")):
                existing["payload"] = payload
        return list(merged.values())

    @staticmethod
    def _candidate_ids(candidates: list[dict[str, Any]]) -> list[str]:
        return sorted(
            {
                str(candidate.get("payload", {}).get("id") or candidate.get("payload", {}).get("course_id"))
                for candidate in candidates
                if candidate.get("payload", {}).get("id") or candidate.get("payload", {}).get("course_id")
            }
        )

    @staticmethod
    def _similar_profile_course_counts(similar_profiles: list[dict[str, Any]]) -> dict[str, int]:
        counts: dict[str, int] = {}
        for profile in similar_profiles:
            payload = profile.get("payload", {})
            for history_item in payload.get("course_history", []) or []:
                course_id = str(history_item.get("course_id") or "")
                if not course_id:
                    continue
                status = str(history_item.get("historyBucket") or history_item.get("status") or "").lower()
                if status in {"completed", "in_progress", "enrolled"}:
                    counts[course_id] = counts.get(course_id, 0) + 1
        return counts

    @staticmethod
    def _append_signal(signals: list[str], signal: str) -> None:
        if signal and signal not in signals:
            signals.append(signal)

    @staticmethod
    def _used_candidate_signals(candidates: list[dict[str, Any]]) -> list[str]:
        return sorted({str(signal) for candidate in candidates for signal in candidate.get("signals", []) if signal})

    @staticmethod
    def _build_citations(candidates: list[dict[str, Any]]) -> list[dict[str, Any]]:
        return [
            {
                "kind": "course",
                "courseId": candidate["payload"].get("id") or candidate["payload"].get("course_id"),
                "title": candidate["payload"].get("title"),
                "score": candidate.get("score"),
                "signals": candidate.get("signals", []),
            }
            for candidate in candidates
        ]

    @staticmethod
    def _resolve_pipeline(
        *,
        course_history: list[dict[str, Any]],
        ratings: list[dict[str, Any]],
        learning_paths: list[dict[str, Any]],
        skill_profile: dict[str, float],
        fallback_catalog_used: bool,
        vector_failed: bool,
        missing_signals: list[str],
    ) -> str:
        has_personal_context = bool(course_history or ratings or learning_paths or skill_profile)
        if fallback_catalog_used and not has_personal_context:
            return "fallback_catalog"
        if not has_personal_context:
            return "fallback_catalog"
        if fallback_catalog_used or vector_failed or "llm_structured_output" in missing_signals:
            return "partial_data"
        return "real_data"


recommendation_service = RecommendationService()
