from __future__ import annotations

from typing import Any

from app.core.config import get_settings
from app.core.enums import AiTaskStatus, AiTaskType
from app.db.models import AiGenerationTaskModel
from app.db.session import get_db_session
from app.schemas.learning_path import LearningPathDraftResponse, LearningPathGenerateRequest
from app.services.catalog_service import catalog_service
from app.services.provider_config import provider_config_service
from app.services.runtime_request_context import runtime_request_context_service
from app.services.vector_service import vector_service


class LearningPathService:
    def __init__(self) -> None:
        self._settings = get_settings()

    async def generate(self, request: LearningPathGenerateRequest) -> LearningPathDraftResponse:
        with runtime_request_context_service.begin(scope="learning_path_generate") as runtime_state:
            instructor_id = str(request.userId)
            user_profile = await catalog_service.fetch_user_profile(str(request.userId))
            course_history = await catalog_service.fetch_user_course_history(str(request.userId))
            active_paths = await catalog_service.fetch_user_learning_paths(str(request.userId))
            completed_ids = {
                item["course_id"]
                for item in course_history
                if item.get("course_id")
                and (item.get("status") == "COMPLETED" or float(item.get("progress") or 0.0) >= 0.95)
            }
            pipeline = "live"
            if self._settings.business_safe_mode_enabled:
                relevant_courses = []
                pipeline = "fallback"
            else:
                try:
                    relevant_courses = await vector_service.search_courses(
                        query=self._build_query(request, user_profile, course_history, active_paths),
                        limit=12,
                        language=request.language,
                        exclude_course_ids=completed_ids,
                        instructor_id=instructor_id,
                    )
                    relevant_courses = await self._hydrate_vector_courses(
                        relevant_courses,
                        instructor_id=instructor_id,
                        completed_ids=completed_ids,
                    )
                except Exception:
                    relevant_courses = []
                    pipeline = "fallback"
            preferred_courses = [
                course
                for course in await catalog_service.fetch_courses_by_ids(
                    [str(item) for item in request.preferredCourseIds or []],
                    instructor_id=instructor_id,
                )
                if course.get("id") not in completed_ids
            ]
            candidates = self._merge_candidates(relevant_courses, preferred_courses)

            if not candidates:
                catalog_courses = await catalog_service.fetch_published_courses(limit=12, instructor_id=instructor_id)
                candidates = self._merge_candidates([], [course for course in catalog_courses if course.get("id") not in completed_ids])
                pipeline = "fallback"
            if not candidates:
                raise ValueError("No published courses owned by this instructor are available to build a learning path.")

            prompt = self._build_prompt(request, candidates, user_profile, course_history, active_paths)
            fallback_path = self._fallback_path(request, candidates, user_profile, course_history)
            path = fallback_path
            path.setdefault("metadata", {})
            path["metadata"].update(
                {
                    "courseSource": "instructor_db_only",
                    "llmCourseSelection": False,
                    "candidateCourseIds": [
                        str(candidate["payload"].get("id") or candidate["payload"].get("course_id"))
                        for candidate in candidates
                    ],
                }
            )
            path = await self._ensure_path_courses_active(path, instructor_id=instructor_id)

            async with get_db_session() as session:
                task = AiGenerationTaskModel(
                    task_type=AiTaskType.LEARNING_PATH_GENERATION.value,
                    status=AiTaskStatus.DRAFT.value,
                    target_reference=request.goal,
                    request_payload=request.model_dump(mode="json"),
                    result_payload=path,
                    prompt=prompt,
                    model_used=await provider_config_service.get_active_chat_model(),
                )
                session.add(task)
                await session.flush()
                path.setdefault("metadata", {})
                path["metadata"].update(
                    {
                        "pipeline": pipeline,
                        "requestId": runtime_state.request_id,
                        "tokenUsage": runtime_request_context_service.snapshot(),
                        "citations": [
                            {
                                "kind": "course",
                                "courseId": course["courseId"],
                                "title": course["title"],
                                "order": course["order"],
                            }
                            for course in path.get("courses", [])[:8]
                        ],
                    }
                )
                return LearningPathDraftResponse(
                    taskId=str(task.id),
                    status=AiTaskStatus.DRAFT,
                    title=path.get("title"),
                    path=path,
                    nodes=path.get("nodes"),
                    edges=path.get("edges"),
                )

    async def _hydrate_vector_courses(
        self,
        vector_candidates: list[dict[str, Any]],
        *,
        instructor_id: str,
        completed_ids: set[str],
    ) -> list[dict[str, Any]]:
        score_by_id: dict[str, float] = {}
        ordered_ids: list[str] = []
        for candidate in vector_candidates:
            payload = candidate.get("payload", {}) if isinstance(candidate, dict) else {}
            course_id = str(payload.get("id") or payload.get("course_id") or "")
            if not course_id or course_id in completed_ids or course_id in score_by_id:
                continue
            score_by_id[course_id] = float(candidate.get("score") or 0.0)
            ordered_ids.append(course_id)

        if not ordered_ids:
            return []

        rows = await catalog_service.fetch_courses_by_ids(ordered_ids, instructor_id=instructor_id)
        rank_by_id = {course_id: index for index, course_id in enumerate(ordered_ids)}
        hydrated = [
            {
                "id": course["id"],
                "score": score_by_id.get(course["id"], 0.0),
                "payload": course,
                "retrievalMode": "vector_hydrated_from_db",
            }
            for course in rows
            if course.get("id") and course.get("id") not in completed_ids
        ]
        hydrated.sort(key=lambda item: rank_by_id.get(item["payload"]["id"], 9999))
        return hydrated

    async def _ensure_path_courses_active(self, path: dict[str, Any], *, instructor_id: str) -> dict[str, Any]:
        courses = path.get("courses")
        if not isinstance(courses, list) or not courses:
            raise ValueError("Learning path must contain at least one active published course.")

        ordered_ids: list[str] = []
        for item in courses:
            if not isinstance(item, dict):
                continue
            course_id = str(item.get("courseId") or item.get("course_id") or "")
            if course_id and course_id not in ordered_ids:
                ordered_ids.append(course_id)

        if not ordered_ids:
            raise ValueError("Learning path must contain valid course IDs.")

        active_rows = await catalog_service.fetch_courses_by_ids(ordered_ids, instructor_id=instructor_id)
        active_by_id = {str(course["id"]): course for course in active_rows if course.get("id")}
        missing_ids = [course_id for course_id in ordered_ids if course_id not in active_by_id]
        if missing_ids:
            raise ValueError(
                "Learning path contains courses that are inactive, unpublished, deleted, or not owned by this instructor: "
                + ", ".join(missing_ids)
            )

        hydrated_courses: list[dict[str, Any]] = []
        for index, item in enumerate(courses, start=1):
            if not isinstance(item, dict):
                continue
            course_id = str(item.get("courseId") or item.get("course_id") or "")
            source = active_by_id.get(course_id)
            if source is None:
                continue
            hydrated_courses.append(
                {
                    **item,
                    "courseId": course_id,
                    "title": str(source.get("title") or item.get("title") or f"Course {index}"),
                    "description": str(source.get("description") or item.get("description") or "")[:150],
                    "thumbnail": source.get("thumbnail"),
                    "order": int(item.get("order") or index),
                    "isOptional": "Y" if str(item.get("isOptional", "N")).upper() == "Y" else "N",
                }
            )

        hydrated_courses.sort(key=lambda item: item["order"])
        path["courses"] = hydrated_courses

        existing_nodes = {
            str(node.get("id")): node
            for node in path.get("nodes", [])
            if isinstance(node, dict) and node.get("id")
        }
        nodes: list[dict[str, Any]] = []
        for index, course in enumerate(hydrated_courses, start=1):
            node = existing_nodes.get(course["courseId"], {})
            position = node.get("position") if isinstance(node.get("position"), dict) else {}
            nodes.append(
                {
                    "id": course["courseId"],
                    "type": "courseNode",
                    "data": {
                        **(node.get("data") if isinstance(node.get("data"), dict) else {}),
                        "label": course["title"],
                        "courseId": course["courseId"],
                    },
                    "position": {
                        "x": float(position.get("x", course.get("positionX", 120 + (index - 1) * 280))),
                        "y": float(position.get("y", course.get("positionY", 220))),
                    },
                }
            )
        path["nodes"] = nodes
        path["edges"] = self._normalize_edges(path.get("edges"), hydrated_courses)
        path["layoutEdges"] = [
            {"source": edge["source"], "target": edge["target"]}
            for edge in path["edges"]
        ]
        path.setdefault("metadata", {})
        path["metadata"]["totalCourses"] = len(hydrated_courses)
        return path

    def _build_query(
        self,
        request: LearningPathGenerateRequest,
        user_profile: dict[str, Any] | None,
        course_history: list[dict[str, Any]],
        active_paths: list[dict[str, Any]],
    ) -> str:
        segments = [
            f"goal={request.goal}",
            f"current_level={request.currentLevel}",
            f"target_level={request.targetLevel}",
            f"timeframe={request.timeframe}",
            f"duration={request.duration}",
            f"language={request.language}",
        ]
        if user_profile:
            interests = user_profile.get("learning_history", {}).get("interests") if isinstance(user_profile.get("learning_history"), dict) else None
            if interests:
                if isinstance(interests, list):
                    segments.append("profile_interests=" + ", ".join(str(item) for item in interests))
                else:
                    segments.append(f"profile_interests={interests}")
        known_skills = sorted({str(skill) for item in course_history for skill in item.get("skills", [])})
        if known_skills:
            segments.append("known_skills=" + ", ".join(known_skills[:8]))
        in_progress = [item.get("title") for item in course_history if item.get("status") == "IN_PROGRESS"]
        if in_progress:
            segments.append("in_progress=" + ", ".join(str(item) for item in in_progress[:4]))
        if active_paths:
            segments.append("active_paths=" + ", ".join(str(path.get("title")) for path in active_paths[:3]))
        return "\n".join(segments)

    def _merge_candidates(
        self,
        vector_candidates: list[dict[str, Any]],
        preferred_courses: list[dict[str, Any]],
    ) -> list[dict[str, Any]]:
        merged: dict[str, dict[str, Any]] = {}
        for index, candidate in enumerate(vector_candidates, start=1):
            payload = candidate.get("payload", {})
            course_id = str(payload.get("id") or payload.get("course_id") or "")
            if not course_id:
                continue
            merged[course_id] = {
                "score": float(candidate.get("score") or 0.0),
                "payload": payload,
                "source": "vector",
                "rank": index,
            }

        for course in preferred_courses:
            course_id = str(course.get("id") or "")
            if not course_id:
                continue
            existing = merged.get(course_id)
            payload = course
            if existing is None:
                merged[course_id] = {
                    "score": 0.88,
                    "payload": payload,
                    "source": "preferred",
                    "rank": 0,
                }
            else:
                existing["score"] = max(existing["score"], 0.92)
                existing["source"] = "preferred+vector"

        results = list(merged.values())
        results.sort(
            key=lambda item: (
                -item["score"],
                self._level_order(item["payload"].get("level")),
                item["rank"],
            )
        )
        return results[:12]

    def _build_prompt(
        self,
        request: LearningPathGenerateRequest,
        candidates: list[dict[str, Any]],
        user_profile: dict[str, Any] | None,
        course_history: list[dict[str, Any]],
        active_paths: list[dict[str, Any]],
    ) -> str:
        candidate_lines = []
        for candidate in candidates:
            payload = candidate["payload"]
            candidate_lines.append(
                (
                    f"- courseId={payload.get('id') or payload.get('course_id')} | title={payload.get('title')} | "
                    f"level={payload.get('level')} | skills={payload.get('skills', [])} | "
                    f"language={payload.get('language')} | score={candidate['score']} | source={candidate['source']} | "
                    f"description={payload.get('description', '')}"
                )
            )

        history_lines = [
            (
                f"- title={item.get('title')} | status={item.get('status')} | "
                f"progress={item.get('progress')} | level={item.get('level')} | "
                f"skills={item.get('skills', [])}"
            )
            for item in course_history[:8]
        ]
        path_lines = [
            f"- title={path.get('title')} | completion={path.get('completion')} | courses={len(path.get('courses', []))}"
            for path in active_paths[:5]
        ]

        # Build skill profile context if available
        skill_lines = ""
        if user_profile and user_profile.get("skill_profile"):
            sp = user_profile["skill_profile"]
            if isinstance(sp, dict) and sp:
                skill_lines = "User skill profile: " + ", ".join(
                    f"{k} ({v:.0%})" for k, v in sorted(sp.items(), key=lambda x: x[1], reverse=True)[:8]
                ) + "\n"

        return (
            "# ROLE\n"
            "Ban la he thong tao learning path cho nen tang hoc tap TechHub.\n\n"
            "# RULES\n"
            "- Chi duoc su dung courseId co trong danh sach candidate. TUYET DOI khong tao UUID moi.\n"
            "- Path di theo thu tu tu currentLevel den targetLevel.\n"
            "- Khoa beginner truoc, intermediate giua, advanced cuoi.\n"
            "- Khong trung lap voi khoa da hoan thanh.\n"
            "- Uu tien khoa co skills lien quan den goal.\n"
            "- Neu user da co skill profile, hay chon khoa lap day skill gap.\n\n"
            "# OUTPUT FORMAT\n"
            "Tra ve JSON only voi keys:\n"
            "- title: ten learning path ngan gon\n"
            "- description: mo ta 1-2 cau\n"
            "- skills: array ky nang dat duoc\n"
            "- courses: array {courseId, title, description, thumbnail, order, positionX, positionY, isOptional}\n"
            "- nodes, edges: dung cho React Flow frontend\n"
            "- metadata: {estimatedDuration, courseCount}\n\n"
            "# USER CONTEXT\n"
            f"Goal: {request.goal}\n"
            f"Current level: {request.currentLevel}\n"
            f"Target level: {request.targetLevel}\n"
            f"Timeframe: {request.timeframe} | Duration: {request.duration}\n"
            f"Preferred language: {request.language}\n"
            + skill_lines
            + "Completed / in-progress courses:\n"
            + ("\n".join(history_lines) if history_lines else "- no history")
            + "\nActive learning paths:\n"
            + ("\n".join(path_lines) if path_lines else "- no active paths")
            + "\n\n# CANDIDATE COURSES\n"
            + "\n".join(candidate_lines)
            + "\n\nReturn a practical path of 3-8 courses, ordered logically from current to target level."
        )

    def _fallback_path(
        self,
        request: LearningPathGenerateRequest,
        candidates: list[dict[str, Any]],
        user_profile: dict[str, Any] | None,
        course_history: list[dict[str, Any]],
    ) -> dict[str, Any]:
        completed_levels = {
            str(item.get("level") or "").upper()
            for item in course_history
            if item.get("status") == "COMPLETED"
        }
        ordered = sorted(
            candidates,
            key=lambda item: (
                abs(self._level_order(item["payload"].get("level")) - self._target_anchor(request, completed_levels)),
                -item["score"],
            ),
        )[: min(max(3, len(candidates)), 8)]

        course_entries = []
        nodes = []
        edges = []
        skills: list[str] = []
        for index, candidate in enumerate(ordered, start=1):
            payload = candidate["payload"]
            course_id = str(payload.get("id") or payload.get("course_id"))
            position_x = 120 + (index - 1) * 280
            position_y = 220 if index % 2 == 1 else 360
            course_entries.append(
                {
                    "courseId": course_id,
                    "title": str(payload.get("title") or f"Course {index}"),
                    "description": str(payload.get("description") or "")[:150],
                    "thumbnail": payload.get("thumbnail"),
                    "order": index,
                    "positionX": position_x,
                    "positionY": position_y,
                    "isOptional": "Y" if self._level_order(payload.get("level")) > self._level_order(request.currentLevel) + 1 else "N",
                }
            )
            nodes.append(
                {
                    "id": course_id,
                    "type": "courseNode",
                    "data": {
                        "label": str(payload.get("title") or f"Course {index}"),
                        "courseId": course_id,
                    },
                    "position": {"x": float(position_x), "y": float(position_y)},
                }
            )
            if index > 1:
                previous_id = course_entries[index - 2]["courseId"]
                edges.append({"id": f"{previous_id}->{course_id}", "source": previous_id, "target": course_id})
            skills.extend(str(skill) for skill in payload.get("skills", []))

        metadata = {
            "goal": request.goal,
            "timeframe": request.timeframe,
            "duration": request.duration,
            "totalCourses": len(course_entries),
            "estimatedWeeks": max(len(course_entries) * 2, 4),
            "generatedForUserId": str(request.userId),
            "profileLanguage": (user_profile or {}).get("preferred_language"),
        }
        return {
            "title": f"Learning path for {request.goal}",
            "description": (
                f"Lo trinh tu {request.currentLevel} den {request.targetLevel} trong {request.timeframe}, "
                f"uu tien cac khoa hoc sat voi muc tieu '{request.goal}'."
            ),
            "skills": sorted(set(skills))[:8],
            "courses": course_entries,
            "layoutEdges": [{"source": item["source"], "target": item["target"]} for item in edges],
            "nodes": nodes,
            "edges": edges,
            "metadata": metadata,
        }

    def _normalize_payload(
        self,
        payload: dict[str, Any],
        fallback: dict[str, Any],
        candidates: list[dict[str, Any]],
    ) -> dict[str, Any]:
        if not isinstance(payload, dict):
            return fallback

        allowed_ids = {
            str(candidate["payload"].get("id") or candidate["payload"].get("course_id"))
            for candidate in candidates
        }
        candidate_by_id = {
            str(candidate["payload"].get("id") or candidate["payload"].get("course_id")): candidate["payload"]
            for candidate in candidates
        }
        courses = payload.get("courses")
        nodes = payload.get("nodes")
        edges = payload.get("edges")

        if not isinstance(courses, list):
            return fallback

        cleaned_courses = []
        for index, item in enumerate(courses, start=1):
            if not isinstance(item, dict):
                continue
            course_id = str(item.get("courseId") or "")
            if course_id not in allowed_ids:
                continue
            source_course = candidate_by_id.get(course_id, {})
            cleaned_courses.append(
                {
                    "courseId": course_id,
                    "title": str(source_course.get("title") or item.get("title") or ""),
                    "description": str(source_course.get("description") or item.get("description") or "")[:150],
                    "thumbnail": source_course.get("thumbnail") or item.get("thumbnail"),
                    "order": int(item.get("order") or index),
                    "positionX": int(item.get("positionX") or 120 + (index - 1) * 280),
                    "positionY": int(item.get("positionY") or 220),
                    "isOptional": "Y" if str(item.get("isOptional", "N")).upper() == "Y" else "N",
                }
            )

        if not cleaned_courses:
            return fallback

        cleaned_courses.sort(key=lambda item: item["order"])
        normalized_nodes = self._normalize_nodes(nodes, cleaned_courses)
        normalized_edges = self._normalize_edges(edges, cleaned_courses)
        return {
            "title": str(payload.get("title") or fallback["title"]),
            "description": str(payload.get("description") or fallback["description"]),
            "skills": self._normalize_skills(payload.get("skills"), cleaned_courses, candidates),
            "courses": cleaned_courses,
            "layoutEdges": [{"source": edge["source"], "target": edge["target"]} for edge in normalized_edges],
            "nodes": normalized_nodes,
            "edges": normalized_edges,
            "metadata": payload.get("metadata") if isinstance(payload.get("metadata"), dict) else fallback["metadata"],
        }

    def _normalize_nodes(self, raw_nodes: Any, courses: list[dict[str, Any]]) -> list[dict[str, Any]]:
        if isinstance(raw_nodes, list):
            indexed = {
                str(item.get("id")): item
                for item in raw_nodes
                if isinstance(item, dict) and item.get("id")
            }
        else:
            indexed = {}

        nodes = []
        for course in courses:
            node = indexed.get(course["courseId"], {})
            position = node.get("position") if isinstance(node, dict) else {}
            nodes.append(
                {
                    "id": course["courseId"],
                    "type": "courseNode",
                    "data": {
                        "label": course["title"],
                        "courseId": course["courseId"],
                    },
                    "position": {
                        "x": float((position or {}).get("x") or course["positionX"]),
                        "y": float((position or {}).get("y") or course["positionY"]),
                    },
                }
            )
        return nodes

    @staticmethod
    def _normalize_edges(raw_edges: Any, courses: list[dict[str, Any]]) -> list[dict[str, Any]]:
        allowed_ids = {course["courseId"] for course in courses}
        if not isinstance(raw_edges, list):
            raw_edges = []

        cleaned = []
        for item in raw_edges:
            if not isinstance(item, dict):
                continue
            source = str(item.get("source") or "")
            target = str(item.get("target") or "")
            if source in allowed_ids and target in allowed_ids and source != target:
                cleaned.append({"id": str(item.get("id") or f"{source}->{target}"), "source": source, "target": target})

        if cleaned:
            return cleaned

        sequential = []
        for index in range(1, len(courses)):
            source = courses[index - 1]["courseId"]
            target = courses[index]["courseId"]
            sequential.append({"id": f"{source}->{target}", "source": source, "target": target})
        return sequential

    @staticmethod
    def _normalize_skills(raw_skills: Any, courses: list[dict[str, Any]], candidates: list[dict[str, Any]]) -> list[str]:
        if isinstance(raw_skills, list) and raw_skills:
            return [str(skill) for skill in raw_skills[:8]]

        skill_map = {
            str(candidate["payload"].get("id") or candidate["payload"].get("course_id")): candidate["payload"].get("skills", [])
            for candidate in candidates
        }
        skills = {
            str(skill)
            for course in courses
            for skill in skill_map.get(course["courseId"], [])
        }
        return sorted(skills)[:8]

    @staticmethod
    def _level_order(level: Any) -> int:
        normalized = str(level or "").upper()
        mapping = {
            "BEGINNER": 0,
            "ALL_LEVELS": 1,
            "INTERMEDIATE": 2,
            "ADVANCED": 3,
            "EXPERT": 4,
        }
        return mapping.get(normalized, 1)

    def _target_anchor(self, request: LearningPathGenerateRequest, completed_levels: set[str]) -> int:
        if completed_levels:
            return min(self._level_order(request.targetLevel), max(self._level_order(level) for level in completed_levels) + 1)
        return max(self._level_order(request.currentLevel), 0)


learning_path_service = LearningPathService()
