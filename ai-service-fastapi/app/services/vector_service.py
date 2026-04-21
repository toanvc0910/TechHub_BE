from __future__ import annotations

import hashlib
import re
from collections.abc import Iterable
from datetime import date, datetime
from decimal import Decimal
from time import perf_counter
from typing import Any

import httpx

from app.core.config import get_settings
from app.schemas.admin import QdrantCollectionStats
from app.services.catalog_service import catalog_service
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
                    if normalized:
                        await runtime_observability_service.record_vector_operation(
                            operation="search_courses",
                            duration_ms=(perf_counter() - started) * 1000,
                            success=True,
                            collection=self._settings.qdrant_course_collection,
                            count=len(normalized),
                        )
                        return normalized
            except Exception:
                pass

        results = await self._lexical_search_courses(
            query=sanitized_query,
            limit=limit,
            level=level,
            language=language,
            exclude_course_ids=exclude_course_ids,
        )
        await runtime_observability_service.record_vector_operation(
            operation="search_courses_lexical",
            duration_ms=(perf_counter() - started) * 1000,
            success=True,
            collection=self._settings.qdrant_course_collection,
            count=len(results),
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
                        await runtime_observability_service.record_vector_operation(
                            operation="search_profiles",
                            duration_ms=(perf_counter() - started) * 1000,
                            success=True,
                            collection=self._settings.qdrant_profile_collection,
                            count=len(normalized),
                        )
                        return normalized
            except Exception:
                pass

        results = await self._lexical_search_profiles(
            query=sanitized_query,
            limit=limit,
            exclude_user_ids=exclude_user_ids,
        )
        await runtime_observability_service.record_vector_operation(
            operation="search_profiles_lexical",
            duration_ms=(perf_counter() - started) * 1000,
            success=True,
            collection=self._settings.qdrant_profile_collection,
            count=len(results),
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

    async def get_collection_stats(self) -> dict[str, Any]:
        collections = {
            "courses": self._settings.qdrant_course_collection,
            "lessons": self._settings.qdrant_lesson_collection,
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
        profile_result = await self._reindex_behavior_profiles()

        counts = {
            "courses": course_result["stats"]["indexed"],
            "lessons": lesson_result["stats"]["indexed"],
            "enrollments": profile_result["stats"]["indexed"],
        }
        failed = (
            course_result["stats"]["failed"]
            + lesson_result["stats"]["failed"]
            + profile_result["stats"]["failed"]
        )
        return {
            "success": failed == 0,
            "message": "Full system reindex completed using PostgreSQL, Qdrant and profile behavior aggregation.",
            "stats": {
                "indexed": sum(counts.values()),
                "failed": failed,
                "duration": f"{perf_counter() - started:.2f}s",
                "counts": counts,
            },
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
    ) -> list[dict[str, Any]]:
        courses = await catalog_service.fetch_published_courses(limit=200)
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
            "completed_courses": [item.get("course_id") for item in history if item.get("status") == "COMPLETED"],
            "in_progress_courses": [item.get("course_id") for item in history if item.get("status") == "IN_PROGRESS"],
            "skills": sorted({skill for item in history for skill in item.get("skills", [])}),
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
        scope = session_id or "global"
        digest = hashlib.sha1(f"{scope}:{file_id}:{chunk_index}".encode("utf-8")).hexdigest()
        return digest

    @staticmethod
    def _file_search_must_filter(*, user_id: str, session_id: str | None) -> list[dict[str, Any]]:
        must = [{"key": "user_id", "match": {"value": user_id}}]
        if session_id:
            must.append({"key": "session_id", "match": {"value": session_id}})
        return must


vector_service = VectorService()
