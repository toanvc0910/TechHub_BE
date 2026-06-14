from __future__ import annotations

import hashlib
import logging
import re
import uuid as _uuid
from collections.abc import Iterable
from datetime import date, datetime, timezone
from decimal import Decimal
from time import perf_counter
from typing import Any

import httpx

logger = logging.getLogger(__name__)

# Stable namespace for deriving file-chunk point IDs. Don't change unless
# you also re-ingest every chunk: existing rows would become orphaned.
_FILE_CHUNK_NAMESPACE = _uuid.UUID("9e2b3c6f-1d6a-4b9a-9d4b-4e5a7c8f0a01")

from app.core.config import get_settings
from app.schemas.admin import QdrantCollectionStats
from app.services.catalog_service import catalog_service
from app.services.data_contract import data_contract_registry
from app.services.llm_gateway import switchable_ai_gateway
from app.services.observability_service import runtime_observability_service


class VectorService:
    def __init__(self) -> None:
        self._settings = get_settings()
        self._client = httpx.AsyncClient(timeout=30.0)

    def _headers(self) -> dict[str, str]:
        headers: dict[str, str] = {"Content-Type": "application/json"}
        if self._settings.qdrant_api_key:
            headers["api-key"] = self._settings.qdrant_api_key
        return headers

    async def search_courses(
        self,
        query: str,
        limit: int = 5,
        level: str | None = None,
        language: str | None = None,
        exclude_course_ids: Iterable[str] | None = None,
        instructor_id: str | None = None,
        score_threshold: float | None = None,
    ) -> list[dict[str, Any]]:
        started = perf_counter()
        sanitized_query = query.strip()
        if not sanitized_query:
            return []

        must_filters = []
        if level:
            must_filters.append({"key": "level", "match": {"value": level.upper()}})
        if language:
            must_filters.append({"key": "language", "match": {"value": language.upper()}})
        if instructor_id:
            must_filters.append({"key": "instructor_id", "match": {"value": str(instructor_id)}})

        must_not_filters = []
        for course_id in exclude_course_ids or []:
            must_not_filters.append({"key": "course_id", "match": {"value": str(course_id)}})

        if self._settings.qdrant_host:
            try:
                embeddings = await switchable_ai_gateway.generate_embeddings(
                    [sanitized_query],
                    task_type="RETRIEVAL_QUERY",
                )
                embedding = embeddings[0] if embeddings else []
                if embedding:
                    payload: dict[str, Any] = {
                        "vector": embedding,
                        "limit": limit,
                        "with_payload": True,
                    }
                    if score_threshold is not None:
                        payload["score_threshold"] = score_threshold
                    if must_filters or must_not_filters:
                        payload["filter"] = {}
                        if must_filters:
                            payload["filter"]["must"] = must_filters
                        if must_not_filters:
                            payload["filter"]["must_not"] = must_not_filters

                    response = await self._client.post(
                        f"{self._settings.qdrant_host}/collections/{self._settings.qdrant_course_collection}/points/search",
                        headers=self._headers(),
                        json=payload,
                    )
                    response.raise_for_status()
                    results = response.json().get("result", [])
                    normalized = [self._normalize_scored_point(item) for item in results]
                    raw_count = len(normalized)
                    if score_threshold is not None:
                        normalized = [
                            item
                            for item in normalized
                            if float(item.get("score") or 0.0) >= score_threshold
                        ]
                    logger.info(
                        "Vector course search qdrant results query=%r instructor_id=%s language=%s raw_count=%s "
                        "filtered_count=%s threshold=%s scores=%s",
                        sanitized_query,
                        instructor_id,
                        language,
                        raw_count,
                        len(normalized),
                        score_threshold,
                        [round(float(item.get("score") or 0.0), 4) for item in normalized[:12]],
                    )
                    if normalized:
                        for item in normalized:
                            item["retrievalMode"] = "vector"
                        await runtime_observability_service.record_vector_operation(
                            operation="search_courses",
                            duration_ms=(perf_counter() - started) * 1000,
                            success=True,
                            collection=self._settings.qdrant_course_collection,
                            count=len(normalized),
                            mode="vector",
                        )
                        return normalized
                    logger.info(
                        "Vector course search qdrant produced no usable results after filtering; "
                        "falling back to lexical query=%r instructor_id=%s threshold=%s",
                        sanitized_query,
                        instructor_id,
                        score_threshold,
                    )
            except Exception as exc:
                logger.warning("Vector course search failed; falling back to lexical search: %s", exc)

        results = await self._lexical_search_courses(
            query=sanitized_query,
            limit=limit,
            level=level,
            language=language,
            exclude_course_ids=exclude_course_ids,
            instructor_id=instructor_id,
        )
        for item in results:
            item["retrievalMode"] = "lexical_fallback"
        logger.info(
            "Vector course lexical fallback results query=%r instructor_id=%s language=%s count=%s scores=%s",
            sanitized_query,
            instructor_id,
            language,
            len(results),
            [round(float(item.get("score") or 0.0), 4) for item in results[:12]],
        )
        await runtime_observability_service.record_vector_operation(
            operation="search_courses",
            duration_ms=(perf_counter() - started) * 1000,
            success=True,
            collection=self._settings.qdrant_course_collection,
            count=len(results),
            mode="lexical_fallback",
        )
        return results

    async def search_similar_profiles(
        self,
        query: str,
        *,
        limit: int = 5,
        exclude_user_ids: Iterable[str] | None = None,
    ) -> list[dict[str, Any]]:
        started = perf_counter()
        sanitized_query = query.strip()
        if not sanitized_query:
            return []

        must_not_filters = []
        for user_id in exclude_user_ids or []:
            must_not_filters.append({"key": "user_id", "match": {"value": str(user_id)}})

        if self._settings.qdrant_host:
            try:
                embeddings = await switchable_ai_gateway.generate_embeddings(
                    [sanitized_query],
                    task_type="RETRIEVAL_QUERY",
                )
                embedding = embeddings[0] if embeddings else []
                if embedding:
                    payload: dict[str, Any] = {
                        "vector": embedding,
                        "limit": limit,
                        "with_payload": True,
                    }
                    if must_not_filters:
                        payload["filter"] = {"must_not": must_not_filters}
                    response = await self._client.post(
                        f"{self._settings.qdrant_host}/collections/{self._settings.qdrant_profile_collection}/points/search",
                        headers=self._headers(),
                        json=payload,
                    )
                    response.raise_for_status()
                    results = response.json().get("result", [])
                    normalized = [self._normalize_scored_point(item) for item in results]
                    if normalized:
                        for item in normalized:
                            item["retrievalMode"] = "vector"
                        await runtime_observability_service.record_vector_operation(
                            operation="search_profiles",
                            duration_ms=(perf_counter() - started) * 1000,
                            success=True,
                            collection=self._settings.qdrant_profile_collection,
                            count=len(normalized),
                            mode="vector",
                        )
                        return normalized
            except Exception:
                pass

        results = await self._lexical_search_profiles(
            query=sanitized_query,
            limit=limit,
            exclude_user_ids=exclude_user_ids,
        )
        for item in results:
            item["retrievalMode"] = "lexical_fallback"
        await runtime_observability_service.record_vector_operation(
            operation="search_profiles",
            duration_ms=(perf_counter() - started) * 1000,
            success=True,
            collection=self._settings.qdrant_profile_collection,
            count=len(results),
            mode="lexical_fallback",
        )
        return results

    async def get_lesson(self, lesson_id: str) -> dict[str, Any] | None:
        if self._settings.qdrant_host:
            try:
                response = await self._client.get(
                    f"{self._settings.qdrant_host}/collections/{self._settings.qdrant_lesson_collection}/points/{lesson_id}",
                    headers=self._headers(),
                )
                response.raise_for_status()
                payload = response.json().get("result", {}).get("payload")
                if isinstance(payload, dict):
                    return payload
            except Exception:
                pass
        return await catalog_service.fetch_lesson_by_id(lesson_id)

    # Feature -> required Qdrant collections. Keep this map tight: each
    # AI feature should declare exactly the collections it needs to be
    # considered usable. `get_feature_readiness` reads collection stats
    # and reports whether every required collection has at least one point.
    FEATURE_COLLECTION_REQUIREMENTS: dict[str, tuple[str, ...]] = {
        "recommendation": ("courses",),
        "similar_learner": ("profiles",),
        "lesson_qa": ("lessons",),
        "exercise_grounding": ("lessons",),
        "session_file_chat": ("sessionFiles",),
        "user_file_chat": ("userFiles",),
        "course_rag": ("courses",),
        "integrated_blog": ("blogs",),
        "data_contract_search": ("dataContract",),
    }

    async def get_feature_readiness(self) -> dict[str, Any]:
        """Report whether each AI feature has its required Qdrant collections ready.

        A feature is `ready` only when every required collection exists,
        has status != `not_initialized` / `unavailable`, and has at least one
        point indexed. Degraded features are listed so admin observability
        can show what is currently lexical-only or unusable.
        """
        stats = await self.get_collection_stats()
        collections = stats.get("collections") or {}
        features: dict[str, dict[str, Any]] = {}
        degraded: list[str] = []

        for feature, required in self.FEATURE_COLLECTION_REQUIREMENTS.items():
            missing_collections: list[str] = []
            empty_collections: list[str] = []
            unavailable_collections: list[str] = []
            for logical_name in required:
                col = collections.get(logical_name)
                if col is None:
                    missing_collections.append(logical_name)
                    continue
                status = str(col.get("status") or "")
                points = int(col.get("pointsCount") or col.get("vectorCount") or 0)
                if status in {"unavailable"}:
                    unavailable_collections.append(logical_name)
                elif status == "not_initialized":
                    missing_collections.append(logical_name)
                elif points <= 0:
                    empty_collections.append(logical_name)
            ready = not (missing_collections or empty_collections or unavailable_collections)
            features[feature] = {
                "ready": ready,
                "required": list(required),
                "missing": missing_collections,
                "empty": empty_collections,
                "unavailable": unavailable_collections,
            }
            if not ready:
                degraded.append(feature)
        return {
            "healthy": stats.get("healthy", False),
            "features": features,
            "degradedFeatures": sorted(degraded),
            "collections": collections,
        }

    async def get_collection_stats(self) -> dict[str, Any]:
        collections = {
            "courses": self._settings.qdrant_course_collection,
            "lessons": self._settings.qdrant_lesson_collection,
            "blogs": self._settings.qdrant_blog_collection,
            "dataContract": self._settings.qdrant_data_contract_collection,
            "profiles": self._settings.qdrant_profile_collection,
            "sessionFiles": self._settings.qdrant_session_file_collection,
            "userFiles": self._settings.qdrant_user_file_collection,
        }
        stats: dict[str, QdrantCollectionStats] = {}
        healthy = True
        version: str | None = None
        for logical_name, collection in collections.items():
            try:
                response = await self._client.get(
                    f"{self._settings.qdrant_host}/collections/{collection}",
                    headers=self._headers(),
                )
                if response.status_code == 404:
                    stats[logical_name] = QdrantCollectionStats(vectorCount=0, pointsCount=0, status="not_initialized")
                    continue
                response.raise_for_status()
                result = response.json().get("result", {})
                stats[logical_name] = QdrantCollectionStats(
                    vectorCount=int(result.get("vectors_count") or result.get("points_count") or 0),
                    pointsCount=int(result.get("points_count") or 0),
                    status=str(result.get("status", "green")),
                )
            except Exception:
                healthy = False
                stats[logical_name] = QdrantCollectionStats(vectorCount=0, pointsCount=0, status="unavailable")

        try:
            response = await self._client.get(f"{self._settings.qdrant_host}/", headers=self._headers())
            response.raise_for_status()
            version = response.json().get("version")
        except Exception:
            healthy = False

        return {
            "collections": {name: stat.model_dump() for name, stat in stats.items()},
            "healthy": healthy,
            "version": version,
        }

    async def reindex_courses(self) -> dict[str, Any]:
        started = perf_counter()
        courses = await catalog_service.fetch_published_courses(limit=None)
        indexed = 0
        failed = 0

        if courses:
            texts = [catalog_service.build_course_search_document(course) for course in courses]
            embeddings = await switchable_ai_gateway.generate_embeddings(texts, task_type="RETRIEVAL_DOCUMENT")
            indexed, failed = await self._recreate_and_upsert(
                collection=self._settings.qdrant_course_collection,
                records=courses,
                embeddings=embeddings,
                payload_builder=self._course_payload,
            )
        await runtime_observability_service.record_vector_operation(
            operation="reindex_courses",
            duration_ms=(perf_counter() - started) * 1000,
            success=failed == 0,
            collection=self._settings.qdrant_course_collection,
            count=indexed,
        )

        return {
            "success": failed == 0,
            "message": "Course reindex completed from PostgreSQL to Qdrant.",
            "stats": {
                "indexed": indexed,
                "failed": failed,
                "duration": f"{perf_counter() - started:.2f}s",
            },
        }

    async def reindex_lessons(self) -> dict[str, Any]:
        started = perf_counter()
        lessons = await catalog_service.fetch_lessons()
        indexed = 0
        failed = 0

        if lessons:
            texts = [catalog_service.build_lesson_search_document(lesson) for lesson in lessons]
            embeddings = await switchable_ai_gateway.generate_embeddings(texts, task_type="RETRIEVAL_DOCUMENT")
            indexed, failed = await self._recreate_and_upsert(
                collection=self._settings.qdrant_lesson_collection,
                records=lessons,
                embeddings=embeddings,
                payload_builder=self._lesson_payload,
            )
        await runtime_observability_service.record_vector_operation(
            operation="reindex_lessons",
            duration_ms=(perf_counter() - started) * 1000,
            success=failed == 0,
            collection=self._settings.qdrant_lesson_collection,
            count=indexed,
        )

        return {
            "success": failed == 0,
            "message": "Lesson reindex completed from PostgreSQL to Qdrant.",
            "stats": {
                "indexed": indexed,
                "failed": failed,
                "duration": f"{perf_counter() - started:.2f}s",
            },
        }

    async def reindex_blogs(self) -> dict[str, Any]:
        started = perf_counter()
        blogs = await catalog_service.fetch_published_blogs(limit=None)
        indexed = 0
        failed = 0

        if blogs:
            texts = [catalog_service.build_blog_search_document(blog) for blog in blogs]
            embeddings = await switchable_ai_gateway.generate_embeddings(texts, task_type="RETRIEVAL_DOCUMENT")
            indexed, failed = await self._recreate_and_upsert(
                collection=self._settings.qdrant_blog_collection,
                records=blogs,
                embeddings=embeddings,
                payload_builder=self._blog_payload,
            )
        await runtime_observability_service.record_vector_operation(
            operation="reindex_blogs",
            duration_ms=(perf_counter() - started) * 1000,
            success=failed == 0,
            collection=self._settings.qdrant_blog_collection,
            count=indexed,
        )

        return {
            "success": failed == 0,
            "message": "Blog reindex completed from PostgreSQL to Qdrant.",
            "stats": {
                "indexed": indexed,
                "failed": failed,
                "duration": f"{perf_counter() - started:.2f}s",
            },
        }

    async def reindex_data_contract(self) -> dict[str, Any]:
        started = perf_counter()
        contract = await data_contract_registry.get_contract(force=True)
        records = self._build_data_contract_records(contract)
        indexed = 0
        failed = 0

        if records:
            embeddings = await switchable_ai_gateway.generate_embeddings(
                [record["text"] for record in records],
                task_type="RETRIEVAL_DOCUMENT",
            )
            indexed, failed = await self._recreate_and_upsert(
                collection=self._settings.qdrant_data_contract_collection,
                records=records,
                embeddings=embeddings,
                payload_builder=self._data_contract_payload,
            )
            await data_contract_registry.record_vector_items(
                version_key=contract.version_key,
                collection=self._settings.qdrant_data_contract_collection,
                items=[
                    {
                        "item_key": record["item_key"],
                        "item_type": record["item_type"],
                        "source_table": record.get("source_table"),
                        "source_name": record.get("source_name"),
                        "point_id": record["id"],
                        "content_hash": record["content_hash"],
                        "metadata": {"loadedFromDb": contract.loaded_from_db},
                    }
                    for record in records
                ],
            )

        await runtime_observability_service.record_vector_operation(
            operation="reindex_data_contract",
            duration_ms=(perf_counter() - started) * 1000,
            success=failed == 0,
            collection=self._settings.qdrant_data_contract_collection,
            count=indexed,
        )
        return {
            "success": failed == 0,
            "message": "Data-contract reindex completed from PostgreSQL contract registry to Qdrant.",
            "stats": {
                "indexed": indexed,
                "failed": failed,
                "duration": f"{perf_counter() - started:.2f}s",
                "versionKey": contract.version_key,
                "loadedFromDb": contract.loaded_from_db,
            },
        }

    async def reindex_single_course(self, course_id: str) -> dict[str, Any]:
        """Incrementally reindex a single course by ID instead of full reindex.

        IMPORTANT: point ID must be str(course["id"]) — same as full reindex
        uses in _recreate_and_upsert (record.get("id")) — so upsert overwrites
        the existing vector instead of creating a duplicate.
        """
        started = perf_counter()
        courses = await catalog_service.fetch_courses_by_ids([course_id])
        if not courses:
            return {"success": True, "message": f"Course {course_id} not found or not published.", "stats": {"indexed": 0, "failed": 0}}

        texts = [catalog_service.build_course_search_document(course) for course in courses]
        embeddings = await switchable_ai_gateway.generate_embeddings(texts, task_type="RETRIEVAL_DOCUMENT")
        points = []
        failed = 0
        for course, embedding in zip(courses, embeddings, strict=False):
            if not embedding:
                failed += 1
                continue
            # Use the same ID as full reindex: str(course["id"]) — the PostgreSQL UUID
            point_id = str(course.get("id") or course_id)
            points.append({
                "id": point_id,
                "vector": embedding,
                "payload": self._json_ready(self._course_payload(course)),
            })

        if points:
            await self._ensure_collection(self._settings.qdrant_course_collection, len(points[0]["vector"]))
            await self._upsert_points(self._settings.qdrant_course_collection, points)

        await runtime_observability_service.record_vector_operation(
            operation="reindex_single_course",
            duration_ms=(perf_counter() - started) * 1000,
            success=failed == 0,
            collection=self._settings.qdrant_course_collection,
            count=len(points),
        )
        return {"success": failed == 0, "message": f"Course {course_id} reindexed.", "stats": {"indexed": len(points), "failed": failed}}

    async def reindex_single_lesson(self, lesson_id: str) -> dict[str, Any]:
        """Incrementally reindex a single lesson by ID instead of full reindex.

        IMPORTANT: point ID must be str(lesson["id"]) — same as full reindex.
        """
        started = perf_counter()
        lesson = await catalog_service.fetch_lesson_by_id(lesson_id)
        if not lesson:
            return {"success": True, "message": f"Lesson {lesson_id} not found.", "stats": {"indexed": 0, "failed": 0}}

        text = catalog_service.build_lesson_search_document(lesson)
        embeddings = await switchable_ai_gateway.generate_embeddings([text], task_type="RETRIEVAL_DOCUMENT")
        points = []
        failed = 0
        if embeddings and embeddings[0]:
            # Use the same ID as full reindex: str(lesson["id"]) — the PostgreSQL UUID
            point_id = str(lesson.get("id") or lesson_id)
            points.append({
                "id": point_id,
                "vector": embeddings[0],
                "payload": self._json_ready(self._lesson_payload(lesson)),
            })
        else:
            failed = 1

        if points:
            await self._ensure_collection(self._settings.qdrant_lesson_collection, len(points[0]["vector"]))
            await self._upsert_points(self._settings.qdrant_lesson_collection, points)

        await runtime_observability_service.record_vector_operation(
            operation="reindex_single_lesson",
            duration_ms=(perf_counter() - started) * 1000,
            success=failed == 0,
            collection=self._settings.qdrant_lesson_collection,
            count=len(points),
        )
        return {"success": failed == 0, "message": f"Lesson {lesson_id} reindexed.", "stats": {"indexed": len(points), "failed": failed}}

    async def reindex_all(self) -> dict[str, Any]:
        started = perf_counter()
        course_result = await self.reindex_courses()
        lesson_result = await self.reindex_lessons()
        blog_result = await self.reindex_blogs()
        contract_result = await self.reindex_data_contract()
        profile_result = await self._reindex_behavior_profiles()

        counts = {
            "courses": course_result["stats"]["indexed"],
            "lessons": lesson_result["stats"]["indexed"],
            "blogs": blog_result["stats"]["indexed"],
            "dataContract": contract_result["stats"]["indexed"],
            "enrollments": profile_result["stats"]["indexed"],
        }
        failed = (
            course_result["stats"]["failed"]
            + lesson_result["stats"]["failed"]
            + blog_result["stats"]["failed"]
            + contract_result["stats"]["failed"]
            + profile_result["stats"]["failed"]
        )
        return {
            "success": failed == 0,
            "message": "Full system reindex completed using PostgreSQL, Qdrant, blogs and profile behavior aggregation.",
            "stats": {
                "indexed": sum(counts.values()),
                "failed": failed,
                "duration": f"{perf_counter() - started:.2f}s",
                "counts": counts,
            },
        }

    async def reindex_single_profile(self, user_id: str) -> dict[str, Any]:
        """Incrementally reindex one user profile vector.

        Used by Kafka event handlers (enrollment/rating/path) so a single
        user's signal change does not trigger a global reindex_all().
        """
        started = perf_counter()
        if not user_id:
            return {"success": False, "message": "user_id required", "stats": {"indexed": 0, "failed": 1}}

        profile = await catalog_service.fetch_user_profile(user_id)
        if profile is None:
            await runtime_observability_service.record_vector_operation(
                operation="reindex_single_profile",
                duration_ms=(perf_counter() - started) * 1000,
                success=False,
                collection=self._settings.qdrant_profile_collection,
                count=0,
            )
            return {
                "success": False,
                "message": f"User {user_id} not found or inactive.",
                "stats": {"indexed": 0, "failed": 1},
            }

        course_history = await catalog_service.fetch_user_course_history(user_id)
        profile["course_history"] = course_history
        summary_text = catalog_service.summarize_user_profile(profile, course_history)
        embeddings = await switchable_ai_gateway.generate_embeddings(
            [summary_text],
            task_type="RETRIEVAL_DOCUMENT",
        )
        embedding = embeddings[0] if embeddings else []
        if not embedding:
            await runtime_observability_service.record_vector_operation(
                operation="reindex_single_profile",
                duration_ms=(perf_counter() - started) * 1000,
                success=False,
                collection=self._settings.qdrant_profile_collection,
                count=0,
            )
            return {
                "success": False,
                "message": f"Failed to generate embedding for user {user_id}.",
                "stats": {"indexed": 0, "failed": 1},
            }

        point = {
            "id": str(profile.get("user_id") or user_id),
            "vector": embedding,
            "payload": self._json_ready(self._profile_payload(profile)),
        }
        await self._ensure_collection(self._settings.qdrant_profile_collection, len(embedding))
        await self._upsert_points(self._settings.qdrant_profile_collection, [point])

        await runtime_observability_service.record_vector_operation(
            operation="reindex_single_profile",
            duration_ms=(perf_counter() - started) * 1000,
            success=True,
            collection=self._settings.qdrant_profile_collection,
            count=1,
        )
        return {
            "success": True,
            "message": f"Profile {user_id} reindexed.",
            "stats": {"indexed": 1, "failed": 0},
        }

    async def _reindex_behavior_profiles(self) -> dict[str, Any]:
        started = perf_counter()
        profiles = await catalog_service.fetch_user_profiles_for_indexing()
        texts = [
            catalog_service.summarize_user_profile(profile, profile.get("course_history", []))
            for profile in profiles
        ]
        embeddings = await switchable_ai_gateway.generate_embeddings(texts, task_type="RETRIEVAL_DOCUMENT")
        indexed, failed = await self._recreate_and_upsert(
            collection=self._settings.qdrant_profile_collection,
            records=profiles,
            embeddings=embeddings,
            payload_builder=self._profile_payload,
        )
        await runtime_observability_service.record_vector_operation(
            operation="reindex_profiles",
            duration_ms=(perf_counter() - started) * 1000,
            success=failed == 0,
            collection=self._settings.qdrant_profile_collection,
            count=indexed,
        )
        return {
            "success": failed == 0,
            "message": "Behavior profile reindex completed.",
            "stats": {
                "indexed": indexed,
                "failed": failed,
                "duration": f"{perf_counter() - started:.2f}s",
            },
        }

    async def index_session_file_contexts(
        self,
        *,
        user_id: str,
        session_id: str,
        files: list[dict[str, Any]],
    ) -> dict[str, Any]:
        return await self._index_file_contexts(
            collection=self._settings.qdrant_session_file_collection,
            user_id=user_id,
            session_id=session_id,
            files=files,
            operation="index_session_files",
        )

    async def index_user_file_contexts(
        self,
        *,
        user_id: str,
        files: list[dict[str, Any]],
    ) -> dict[str, Any]:
        return await self._index_file_contexts(
            collection=self._settings.qdrant_user_file_collection,
            user_id=user_id,
            session_id=None,
            files=files,
            operation="index_user_files",
        )

    async def _index_file_contexts(
        self,
        *,
        collection: str,
        user_id: str,
        session_id: str | None,
        files: list[dict[str, Any]],
        operation: str,
    ) -> dict[str, Any]:
        started = perf_counter()
        # Idempotent re-ingest: drop existing chunks for each (user_id, file_id,
        # session_id) tuple before upserting. Otherwise re-parsing the same
        # file with shorter content would leave orphan chunks at higher index.
        for file_item in files:
            file_id = str(file_item.get("id") or file_item.get("fileId") or file_item.get("referenceId") or "")
            if not file_id:
                continue
            try:
                await self.delete_file_chunks(
                    collection=collection,
                    user_id=user_id,
                    file_id=file_id,
                    session_id=session_id,
                )
            except Exception:  # pragma: no cover - best effort cleanup
                logger.exception("Failed to delete prior chunks for file %s", file_id)

        chunk_records: list[dict[str, Any]] = []
        for file_item in files:
            text = str(file_item.get("content") or file_item.get("text") or "").strip()
            if not text:
                continue
            file_id = str(file_item.get("id") or file_item.get("fileId") or file_item.get("referenceId") or "")
            name = str(file_item.get("name") or file_item.get("filename") or file_id or "uploaded-file")
            mime_type = str(file_item.get("mimeType") or file_item.get("mime_type") or "")
            source_url = str(
                file_item.get("secureUrl")
                or file_item.get("publicUrl")
                or file_item.get("url")
                or file_item.get("cloudinarySecureUrl")
                or ""
            )
            source_updated_at = str(
                file_item.get("sourceUpdatedAt")
                or file_item.get("updatedAt")
                or file_item.get("updated")
                or ""
            ) or None
            content_hash = hashlib.sha1(text.encode("utf-8")).hexdigest()
            ingested_at = datetime.now(timezone.utc).isoformat()
            for index, chunk in enumerate(self._chunk_text(text), start=1):
                chunk_records.append(
                    {
                        "id": self._stable_point_id(session_id, file_id or name, index),
                        "text": chunk,
                        "payload": {
                            "id": file_id or name,
                            "file_id": file_id or None,
                            "session_id": session_id,
                            "user_id": user_id,
                            "name": name,
                            "mime_type": mime_type,
                            "source_url": source_url or None,
                            "chunk_index": index,
                            "excerpt": chunk[:400],
                            "content_hash": content_hash,
                            "source_updated_at": source_updated_at,
                            "ingested_at": ingested_at,
                        },
                    }
                )

        if not chunk_records:
            await runtime_observability_service.record_vector_operation(
                operation=operation,
                duration_ms=(perf_counter() - started) * 1000,
                success=True,
                collection=collection,
                count=0,
            )
            return {"indexed": 0, "failed": 0}

        embeddings = await switchable_ai_gateway.generate_embeddings(
            [item["text"] for item in chunk_records],
            task_type="RETRIEVAL_DOCUMENT",
        )
        points = []
        failed = 0
        for item, embedding in zip(chunk_records, embeddings, strict=False):
            if not embedding:
                failed += 1
                continue
            points.append(
                {
                    "id": item["id"],
                    "vector": embedding,
                    "payload": self._json_ready(item["payload"]),
                }
            )

        if points:
            await self._ensure_collection(collection, len(points[0]["vector"]))
            await self._upsert_points(collection, points)

        await runtime_observability_service.record_vector_operation(
            operation=operation,
            duration_ms=(perf_counter() - started) * 1000,
            success=failed == 0,
            collection=collection,
            count=len(points),
        )
        return {"indexed": len(points), "failed": failed}

    async def search_session_file_chunks(
        self,
        *,
        user_id: str,
        session_id: str,
        query: str,
        limit: int = 4,
        ) -> list[dict[str, Any]]:
        return await self._search_file_chunks(
            collection=self._settings.qdrant_session_file_collection,
            user_id=user_id,
            session_id=session_id,
            query=query,
            limit=limit,
            operation="search_session_files",
        )

    async def search_user_file_chunks(
        self,
        *,
        user_id: str,
        query: str,
        limit: int = 4,
    ) -> list[dict[str, Any]]:
        return await self._search_file_chunks(
            collection=self._settings.qdrant_user_file_collection,
            user_id=user_id,
            session_id=None,
            query=query,
            limit=limit,
            operation="search_user_files",
        )

    async def search_data_contract(self, query: str, *, limit: int = 8) -> list[dict[str, Any]]:
        started = perf_counter()
        if not query.strip() or not self._settings.qdrant_host:
            return []
        try:
            embeddings = await switchable_ai_gateway.generate_embeddings(
                [query.strip()],
                task_type="RETRIEVAL_QUERY",
            )
            query_vector = embeddings[0] if embeddings else []
            if not query_vector:
                return []
            response = await self._client.post(
                f"{self._settings.qdrant_host}/collections/{self._settings.qdrant_data_contract_collection}/points/search",
                headers=self._headers(),
                json={
                    "vector": query_vector,
                    "limit": limit,
                    "with_payload": True,
                },
            )
            response.raise_for_status()
            results = response.json().get("result", [])
            await runtime_observability_service.record_vector_operation(
                operation="search_data_contract",
                duration_ms=(perf_counter() - started) * 1000,
                success=True,
                collection=self._settings.qdrant_data_contract_collection,
                count=len(results),
            )
            return [self._normalize_scored_point(item) for item in results]
        except Exception:
            await runtime_observability_service.record_vector_operation(
                operation="search_data_contract",
                duration_ms=(perf_counter() - started) * 1000,
                success=False,
                collection=self._settings.qdrant_data_contract_collection,
                count=0,
            )
            return []

    async def delete_file_chunks(
        self,
        *,
        collection: str,
        user_id: str,
        file_id: str,
        session_id: str | None = None,
    ) -> int:
        """Delete all chunks for one file in one collection. Returns deleted count
        when Qdrant reports it (0 when collection doesn't exist).

        Used by re-ingest to keep chunks idempotent: when content changes the
        old chunks for higher indexes would otherwise remain orphaned. Also
        usable for "forget my file" flows.
        """
        if not self._settings.qdrant_host or not user_id or not file_id:
            return 0
        must = [
            {"key": "user_id", "match": {"value": str(user_id)}},
            {"key": "file_id", "match": {"value": str(file_id)}},
        ]
        if session_id:
            must.append({"key": "session_id", "match": {"value": str(session_id)}})
        try:
            response = await self._client.post(
                f"{self._settings.qdrant_host}/collections/{collection}/points/delete",
                headers=self._headers(),
                params={"wait": "true"},
                json={"filter": {"must": must}},
            )
            if response.status_code == 404:
                return 0
            response.raise_for_status()
            result = response.json().get("result") or {}
            return int(result.get("operation_id") or 0)
        except Exception:
            logger.exception("delete_file_chunks failed for file %s", file_id)
            return 0

    async def get_file_index_status(
        self,
        *,
        user_id: str,
        file_id: str,
        scope: str = "user",
        session_id: str | None = None,
    ) -> dict[str, Any]:
        """Report whether a user file has indexed chunks in Qdrant.

        Returns shape: {status: 'INDEXED'|'NOT_INDEXED'|'COLLECTION_MISSING', chunkCount: int}.
        scope='session' queries the session-file collection (requires session_id);
        scope='user' (default) queries the user-file collection.
        """
        if scope == "session":
            collection = self._settings.qdrant_session_file_collection
            if not session_id:
                return {"status": "NOT_INDEXED", "chunkCount": 0, "reason": "session_id required for scope=session"}
        else:
            collection = self._settings.qdrant_user_file_collection

        if not self._settings.qdrant_host or not user_id or not file_id:
            return {"status": "NOT_INDEXED", "chunkCount": 0, "reason": "missing user_id/file_id/qdrant_host"}

        must = [
            {"key": "user_id", "match": {"value": str(user_id)}},
            {"key": "file_id", "match": {"value": str(file_id)}},
        ]
        if scope == "session" and session_id:
            must.append({"key": "session_id", "match": {"value": str(session_id)}})

        try:
            response = await self._client.post(
                f"{self._settings.qdrant_host}/collections/{collection}/points/count",
                headers=self._headers(),
                json={"filter": {"must": must}, "exact": True},
            )
            if response.status_code == 404:
                return {"status": "COLLECTION_MISSING", "chunkCount": 0, "collection": collection}
            response.raise_for_status()
            count = int((response.json().get("result") or {}).get("count") or 0)
        except Exception as exc:
            logger.warning("get_file_index_status failed for %s: %s", file_id, exc)
            return {"status": "NOT_INDEXED", "chunkCount": 0, "reason": str(exc)}
        return {
            "status": "INDEXED" if count > 0 else "NOT_INDEXED",
            "chunkCount": count,
            "collection": collection,
        }

    async def _search_file_chunks(
        self,
        *,
        collection: str,
        user_id: str,
        session_id: str | None,
        query: str,
        limit: int,
        operation: str,
    ) -> list[dict[str, Any]]:
        started = perf_counter()
        if not query.strip() or not self._settings.qdrant_host:
            return []
        try:
            embeddings = await switchable_ai_gateway.generate_embeddings(
                [query.strip()],
                task_type="RETRIEVAL_QUERY",
            )
            query_vector = embeddings[0] if embeddings else []
            if not query_vector:
                return []
            response = await self._client.post(
                f"{self._settings.qdrant_host}/collections/{collection}/points/search",
                headers=self._headers(),
                json={
                    "vector": query_vector,
                    "limit": limit,
                    "with_payload": True,
                    "filter": {
                        "must": self._file_search_must_filter(user_id=user_id, session_id=session_id),
                    },
                },
            )
            response.raise_for_status()
            results = response.json().get("result", [])
            await runtime_observability_service.record_vector_operation(
                operation=operation,
                duration_ms=(perf_counter() - started) * 1000,
                success=True,
                collection=collection,
                count=len(results),
            )
            return [self._normalize_scored_point(item) for item in results]
        except Exception:
            await runtime_observability_service.record_vector_operation(
                operation=operation,
                duration_ms=(perf_counter() - started) * 1000,
                success=False,
                collection=collection,
                count=0,
            )
            return []

    async def _recreate_and_upsert(
        self,
        *,
        collection: str,
        records: list[dict[str, Any]],
        embeddings: list[list[float]],
        payload_builder,
    ) -> tuple[int, int]:
        if not records or not embeddings:
            return 0, len(records)

        paired = []
        for record, embedding in zip(records, embeddings, strict=False):
            if embedding:
                paired.append((record, embedding))

        if not paired:
            return 0, len(records)

        await self._recreate_collection(collection, len(paired[0][1]))

        indexed = 0
        failed = 0
        batch_size = 32
        for start in range(0, len(paired), batch_size):
            batch = paired[start : start + batch_size]
            points = []
            for record, embedding in batch:
                point_id = record.get("id") or record.get("user_id")
                if not point_id:
                    failed += 1
                    continue
                points.append(
                    {
                        "id": str(point_id),
                        "vector": embedding,
                        "payload": self._json_ready(payload_builder(record)),
                    }
                )
            if not points:
                continue
            try:
                await self._upsert_points(collection, points)
                indexed += len(points)
            except Exception:
                failed += len(points)
        failed += max(0, len(records) - len(paired))
        return indexed, failed

    async def _lexical_search_courses(
        self,
        *,
        query: str,
        limit: int,
        level: str | None,
        language: str | None,
        exclude_course_ids: Iterable[str] | None,
        instructor_id: str | None,
    ) -> list[dict[str, Any]]:
        courses = await catalog_service.fetch_published_courses(limit=200, instructor_id=instructor_id)
        query_terms = self._tokenize(query)
        excluded = {str(course_id) for course_id in (exclude_course_ids or [])}

        scored = []
        for course in courses:
            if course["id"] in excluded:
                continue
            if level and str(course.get("level") or "").lower() != level.lower():
                continue
            if language and str(course.get("language") or "").lower() != language.lower():
                continue

            document_terms = self._tokenize(catalog_service.build_course_search_document(course))
            overlap = len(query_terms.intersection(document_terms))
            title_hit = 1 if query.lower() in str(course.get("title") or "").lower() else 0
            skill_hit = len(query_terms.intersection({term.lower() for term in course.get("skills", [])}))
            score = overlap + title_hit * 1.5 + skill_hit * 0.8
            if score <= 0:
                continue
            scored.append(
                {
                    "id": course["id"],
                    "score": float(score),
                    "payload": self._course_payload(course),
                }
            )

        scored.sort(key=lambda item: item["score"], reverse=True)
        return scored[:limit]

    async def _lexical_search_profiles(
        self,
        *,
        query: str,
        limit: int,
        exclude_user_ids: Iterable[str] | None,
    ) -> list[dict[str, Any]]:
        profiles = await catalog_service.fetch_user_profiles_for_indexing()
        query_terms = self._tokenize(query)
        excluded = {str(user_id) for user_id in (exclude_user_ids or [])}

        scored = []
        for profile in profiles:
            user_id = str(profile.get("user_id") or "")
            if not user_id or user_id in excluded:
                continue
            document_terms = self._tokenize(
                catalog_service.summarize_user_profile(profile, profile.get("course_history", []))
            )
            overlap = len(query_terms.intersection(document_terms))
            if overlap <= 0:
                continue
            scored.append(
                {
                    "id": user_id,
                    "score": float(overlap),
                    "payload": self._profile_payload(profile),
                }
            )

        scored.sort(key=lambda item: item["score"], reverse=True)
        return scored[:limit]

    async def _recreate_collection(self, collection: str, vector_size: int) -> None:
        if not self._settings.qdrant_host:
            return
        try:
            await self._client.delete(
                f"{self._settings.qdrant_host}/collections/{collection}",
                headers=self._headers(),
            )
        except Exception:
            pass

        response = await self._client.put(
            f"{self._settings.qdrant_host}/collections/{collection}",
            headers=self._headers(),
            json={
                "vectors": {
                    "size": vector_size,
                    "distance": "Cosine",
                }
            },
        )
        response.raise_for_status()

    async def _ensure_collection(self, collection: str, vector_size: int) -> None:
        if not self._settings.qdrant_host:
            return
        response = await self._client.get(
            f"{self._settings.qdrant_host}/collections/{collection}",
            headers=self._headers(),
        )
        if response.status_code == 200:
            current_size = self._collection_vector_size(response.json())
            if current_size and current_size != vector_size:
                await self._recreate_collection(collection, vector_size)
            return
        response = await self._client.put(
            f"{self._settings.qdrant_host}/collections/{collection}",
            headers=self._headers(),
            json={
                "vectors": {
                    "size": vector_size,
                    "distance": "Cosine",
                }
            },
        )
        response.raise_for_status()

    @staticmethod
    def _collection_vector_size(payload: dict[str, Any]) -> int | None:
        result = payload.get("result") if isinstance(payload, dict) else None
        config = result.get("config") if isinstance(result, dict) else None
        params = config.get("params") if isinstance(config, dict) else None
        vectors = params.get("vectors") if isinstance(params, dict) else None
        if isinstance(vectors, dict):
            if isinstance(vectors.get("size"), int):
                return int(vectors["size"])
            for value in vectors.values():
                if isinstance(value, dict) and isinstance(value.get("size"), int):
                    return int(value["size"])
        return None

    async def _upsert_points(self, collection: str, points: list[dict[str, Any]]) -> None:
        response = await self._client.put(
            f"{self._settings.qdrant_host}/collections/{collection}/points",
            headers=self._headers(),
            params={"wait": "true"},
            json={"points": points},
        )
        response.raise_for_status()

    @staticmethod
    def _course_payload(course: dict[str, Any]) -> dict[str, Any]:
        return {
            "id": course.get("id"),
            "course_id": course.get("id"),
            "title": course.get("title"),
            "description": course.get("description"),
            "objectives": course.get("objectives") or [],
            "requirements": course.get("requirements") or [],
            "level": course.get("level"),
            "language": course.get("language"),
            "thumbnail": course.get("thumbnail"),
            "tags": course.get("tags") or [],
            "skills": course.get("skills") or [],
            "status": course.get("status"),
            "instructor_id": course.get("instructor_id"),
            "enrollment_count": course.get("enrollment_count") or 0,
            "average_rating": course.get("average_rating") or 0.0,
            "rating_count": course.get("rating_count") or 0,
            "created": course.get("created"),
            "updated": course.get("updated"),
        }

    @staticmethod
    def _lesson_payload(lesson: dict[str, Any]) -> dict[str, Any]:
        return {
            "id": lesson.get("id"),
            "lesson_id": lesson.get("id"),
            "course_id": lesson.get("course_id"),
            "chapter_id": lesson.get("chapter_id"),
            "title": lesson.get("title"),
            "description": lesson.get("description"),
            "content": lesson.get("content"),
            "video_url": lesson.get("video_url"),
            "content_type": lesson.get("content_type"),
            "document_urls": lesson.get("document_urls") or [],
            "workspace_enabled": lesson.get("workspace_enabled"),
            "workspace_languages": lesson.get("workspace_languages") or [],
            "workspace_template": lesson.get("workspace_template") or {},
            "estimated_duration": lesson.get("estimated_duration"),
            "is_free": lesson.get("is_free"),
            "mandatory": lesson.get("mandatory"),
            "course_title": lesson.get("course_title"),
            "course_level": lesson.get("course_level"),
            "course_language": lesson.get("course_language"),
        }

    @staticmethod
    def _blog_payload(blog: dict[str, Any]) -> dict[str, Any]:
        return {
            "id": blog.get("id"),
            "blog_id": blog.get("id"),
            "title": blog.get("title"),
            "content": blog.get("content"),
            "thumbnail": blog.get("thumbnail"),
            "author_id": blog.get("author_id"),
            "author_username": blog.get("author_username"),
            "author_full_name": blog.get("author_full_name"),
            "status": blog.get("status"),
            "tags": blog.get("tags") or [],
            "attachments": blog.get("attachments") or [],
            "related_course_ids": blog.get("related_course_ids") or [],
            "related_lesson_ids": blog.get("related_lesson_ids") or [],
            "created": blog.get("created"),
            "updated": blog.get("updated"),
        }

    @staticmethod
    def _profile_payload(profile: dict[str, Any]) -> dict[str, Any]:
        history = profile.get("course_history", [])
        return {
            "user_id": profile.get("user_id"),
            "email": profile.get("email"),
            "username": profile.get("username"),
            "full_name": profile.get("full_name"),
            "bio": profile.get("bio"),
            "location": profile.get("location"),
            "preferred_language": profile.get("preferred_language"),
            "learning_history": profile.get("learning_history") or {},
            "course_history": history,
            "completed_courses": [
                item.get("course_id")
                for item in history
                if item.get("status") == "COMPLETED" or item.get("historyBucket") == "completed"
            ],
            "in_progress_courses": [
                item.get("course_id")
                for item in history
                if item.get("status") == "IN_PROGRESS" or item.get("historyBucket") == "in_progress"
            ],
            "skills": sorted({skill for item in history for skill in item.get("skills", [])}),
        }

    @staticmethod
    def _data_contract_payload(record: dict[str, Any]) -> dict[str, Any]:
        return {
            "id": record.get("id"),
            "item_key": record.get("item_key"),
            "item_type": record.get("item_type"),
            "version_key": record.get("version_key"),
            "source": record.get("source"),
            "source_table": record.get("source_table"),
            "source_name": record.get("source_name"),
            "content_hash": record.get("content_hash"),
            "summary": record.get("summary"),
            "metadata": record.get("metadata") or {},
        }

    @staticmethod
    def _build_data_contract_records(contract) -> list[dict[str, Any]]:
        records: list[dict[str, Any]] = []
        for table_name, table in contract.tables.items():
            safe_columns = [column for column in table.columns if column not in set(table.pii_columns)]
            text = (
                f"table {table_name}\n"
                f"owner {table.owner}\n"
                f"columns {', '.join(table.columns)}\n"
                f"safe_columns {', '.join(safe_columns)}\n"
                f"pii_columns {', '.join(table.pii_columns)}\n"
                f"analytics_safe {table.analytics_safe}\n"
                f"ai_readable {table.ai_readable}\n"
                f"ai_writable {table.ai_writable}"
            )
            item_key = f"table:{table_name}"
            records.append(VectorService._data_contract_record(contract, item_key, "table", text, table_name, table_name))

        for join in contract.joins:
            source_name = f"{join.left_table}.{join.left_column}->{join.right_table}.{join.right_column}"
            text = (
                f"relation {source_name}\n"
                f"purpose {join.purpose}\n"
                f"join {join.left_table}.{join.left_column} = {join.right_table}.{join.right_column}"
            )
            item_key = f"relation:{source_name}"
            records.append(VectorService._data_contract_record(contract, item_key, "relation", text, join.left_table, source_name))

        for metric_name, metric in contract.metrics.items():
            text = (
                f"metric {metric_name}\n"
                f"title {metric.title}\n"
                f"description {metric.description}\n"
                f"grain {metric.grain}\n"
                f"tables {', '.join(metric.tables)}\n"
                f"allowed_scopes {', '.join(metric.allowed_scopes)}\n"
                f"required_roles {', '.join(metric.required_roles)}"
            )
            item_key = f"metric:{metric_name}"
            records.append(VectorService._data_contract_record(contract, item_key, "metric", text, None, metric_name))
        return records

    @staticmethod
    def _data_contract_record(
        contract,
        item_key: str,
        item_type: str,
        text: str,
        source_table: str | None,
        source_name: str,
    ) -> dict[str, Any]:
        content_hash = hashlib.sha256(text.encode("utf-8")).hexdigest()
        point_id = str(_uuid.uuid5(_uuid.NAMESPACE_URL, f"techhub:{contract.version_key}:{item_key}"))
        return {
            "id": point_id,
            "item_key": item_key,
            "item_type": item_type,
            "version_key": contract.version_key,
            "source": contract.source,
            "source_table": source_table,
            "source_name": source_name,
            "content_hash": content_hash,
            "summary": text[:500],
            "text": text,
            "metadata": {
                "loadedFromDb": contract.loaded_from_db,
                "contractSource": contract.source,
            },
        }

    @staticmethod
    def _normalize_scored_point(item: dict[str, Any]) -> dict[str, Any]:
        payload = item.get("payload") or {}
        if isinstance(payload, dict):
            normalized_payload = payload
        else:
            normalized_payload = {}
        if "id" not in normalized_payload and "course_id" in normalized_payload:
            normalized_payload["id"] = normalized_payload["course_id"]
        if "id" not in normalized_payload and "user_id" in normalized_payload:
            normalized_payload["id"] = normalized_payload["user_id"]
        return {
            "id": str(item.get("id") or normalized_payload.get("id") or normalized_payload.get("course_id")),
            "score": float(item.get("score") or 0.0),
            "payload": normalized_payload,
        }

    @staticmethod
    def _tokenize(text: str) -> set[str]:
        return {token for token in re.split(r"[^a-z0-9]+", text.lower()) if token}

    def _json_ready(self, value: Any) -> Any:
        if isinstance(value, dict):
            return {str(key): self._json_ready(item) for key, item in value.items()}
        if isinstance(value, list):
            return [self._json_ready(item) for item in value]
        if isinstance(value, tuple):
            return [self._json_ready(item) for item in value]
        if isinstance(value, set):
            return [self._json_ready(item) for item in sorted(value)]
        if isinstance(value, Decimal):
            return float(value)
        if isinstance(value, (datetime, date)):
            return value.isoformat()
        return value

    def _chunk_text(self, text: str) -> list[str]:
        chunk_size = max(self._settings.file_chunk_size, 300)
        overlap = max(0, min(self._settings.file_chunk_overlap, chunk_size // 2))
        normalized = re.sub(r"\s+\n", "\n", text).strip()
        if len(normalized) <= chunk_size:
            return [normalized]
        chunks: list[str] = []
        start = 0
        while start < len(normalized):
            end = min(len(normalized), start + chunk_size)
            chunk = normalized[start:end].strip()
            if chunk:
                chunks.append(chunk)
            if end >= len(normalized):
                break
            start = max(end - overlap, start + 1)
        return chunks

    @staticmethod
    def _stable_point_id(session_id: str | None, file_id: str, chunk_index: int) -> str:
        # Qdrant accepts only UUID or unsigned int as point IDs. Earlier code
        # used a raw sha1 hex digest, which Qdrant 1.13 rejects with HTTP 400.
        # Use deterministic UUIDv5 so re-ingest still hits the same id (and
        # therefore upserts in place rather than duplicating).
        scope = session_id or "global"
        return str(_uuid.uuid5(_FILE_CHUNK_NAMESPACE, f"{scope}:{file_id}:{chunk_index}"))

    @staticmethod
    def _file_search_must_filter(*, user_id: str, session_id: str | None) -> list[dict[str, Any]]:
        must = [{"key": "user_id", "match": {"value": user_id}}]
        if session_id:
            must.append({"key": "session_id", "match": {"value": session_id}})
        return must


vector_service = VectorService()
