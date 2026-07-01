from __future__ import annotations

import json
import logging
import re
from collections import defaultdict
from collections.abc import Iterable
from typing import Any

from sqlalchemy import text

from app.db.session import get_db_session
from app.services.observability_service import runtime_observability_service

logger = logging.getLogger(__name__)

ACTIVE_COURSE_SQL = "UPPER(COALESCE(c.is_active::text, 'N')) IN ('Y', 'TRUE', 'T', '1')"
ACTIVE_BLOG_SQL = "UPPER(COALESCE(b.is_active::text, 'N')) IN ('Y', 'TRUE', 'T', '1')"
HTML_TAG_RE = re.compile(r"<[^>]+>")


def _normalize_uuidish(value: Any) -> str | None:
    if value is None:
        return None
    return str(value)


def _normalize_jsonish(value: Any, default: Any) -> Any:
    if value is None:
        return default
    if isinstance(value, (list, dict)):
        return value
    if isinstance(value, str):
        try:
            parsed = json.loads(value)
            if isinstance(parsed, type(default)):
                return parsed
        except json.JSONDecodeError:
            pass
    return default


class CatalogService:
    async def fetch_published_courses(
        self,
        limit: int | None = None,
        *,
        instructor_id: str | None = None,
    ) -> list[dict[str, Any]]:
        sql = f"""
            SELECT
                c.id,
                c.title,
                c.description,
                c.objectives,
                c.requirements,
                c.level,
                c.language,
                c.thumbnail,
                c.instructor_id,
                c.status,
                c.created,
                c.updated,
                COALESCE(
                    json_agg(DISTINCT t.name) FILTER (WHERE t.name IS NOT NULL),
                    '[]'::json
                ) AS tags,
                COALESCE(
                    json_agg(DISTINCT s.name) FILTER (WHERE s.name IS NOT NULL),
                    '[]'::json
                ) AS skills,
                COUNT(DISTINCT e.id) FILTER (WHERE e.is_active = 'Y') AS enrollment_count,
                COALESCE(AVG(r.score) FILTER (
                    WHERE r.target_type = 'COURSE'
                      AND r.is_active = 'Y'
                ), 0.0) AS average_rating,
                COUNT(DISTINCT r.id) FILTER (
                    WHERE r.target_type = 'COURSE'
                      AND r.is_active = 'Y'
                ) AS rating_count
            FROM courses c
            LEFT JOIN course_tags ct ON c.id = ct.course_id
            LEFT JOIN tags t ON ct.tag_id = t.id AND t.is_active = 'Y'
            LEFT JOIN course_skills cs ON c.id = cs.course_id
            LEFT JOIN skills s ON cs.skill_id = s.id
            LEFT JOIN enrollments e ON e.course_id = c.id
            LEFT JOIN ratings r
                ON r.target_id = c.id
               AND r.target_type = 'COURSE'
            WHERE {ACTIVE_COURSE_SQL}
              AND c.status = 'PUBLISHED'
        """
        params: dict[str, Any] = {}
        if instructor_id:
            sql += "\n              AND c.instructor_id = CAST(:instructor_id AS uuid)"
            params["instructor_id"] = str(instructor_id)
        sql += """
            GROUP BY c.id
            ORDER BY c.created DESC
        """
        if limit:
            sql += "\nLIMIT :limit"
            params["limit"] = limit

        async with get_db_session() as session:
            result = await session.execute(text(sql), params)
            return [self._normalize_course_row(dict(row)) for row in result.mappings().all()]

    async def fetch_courses_by_ids(
        self,
        course_ids: Iterable[str],
        *,
        instructor_id: str | None = None,
    ) -> list[dict[str, Any]]:
        # HOT PATH: recommendation_service calls this twice per request and
        # the old implementation fetched the entire published catalog then
        # filtered in Python — scaled with table size, not request size. Use
        # `id = ANY(:ids)` so PostgreSQL only joins/aggregates the rows we
        # actually need.
        ids = [str(course_id) for course_id in course_ids if course_id]
        if not ids:
            return []

        sql = f"""
            SELECT
                c.id,
                c.title,
                c.description,
                c.objectives,
                c.requirements,
                c.level,
                c.language,
                c.thumbnail,
                c.instructor_id,
                c.status,
                c.created,
                c.updated,
                COALESCE(
                    json_agg(DISTINCT t.name) FILTER (WHERE t.name IS NOT NULL),
                    '[]'::json
                ) AS tags,
                COALESCE(
                    json_agg(DISTINCT s.name) FILTER (WHERE s.name IS NOT NULL),
                    '[]'::json
                ) AS skills,
                COUNT(DISTINCT e.id) FILTER (WHERE e.is_active = 'Y') AS enrollment_count,
                COALESCE(AVG(r.score) FILTER (
                    WHERE r.target_type = 'COURSE'
                      AND r.is_active = 'Y'
                ), 0.0) AS average_rating,
                COUNT(DISTINCT r.id) FILTER (
                    WHERE r.target_type = 'COURSE'
                      AND r.is_active = 'Y'
                ) AS rating_count
            FROM courses c
            LEFT JOIN course_tags ct ON c.id = ct.course_id
            LEFT JOIN tags t ON ct.tag_id = t.id AND t.is_active = 'Y'
            LEFT JOIN course_skills cs ON c.id = cs.course_id
            LEFT JOIN skills s ON cs.skill_id = s.id
            LEFT JOIN enrollments e ON e.course_id = c.id
            LEFT JOIN ratings r
                ON r.target_id = c.id
               AND r.target_type = 'COURSE'
            WHERE {ACTIVE_COURSE_SQL}
              AND c.status = 'PUBLISHED'
              AND c.id = ANY(CAST(:ids AS uuid[]))
        """
        params: dict[str, Any] = {"ids": ids}
        if instructor_id:
            sql += "\n              AND c.instructor_id = CAST(:instructor_id AS uuid)"
            params["instructor_id"] = str(instructor_id)
        sql += """
            GROUP BY c.id
        """
        async with get_db_session() as session:
            result = await session.execute(text(sql), params)
            rows = [self._normalize_course_row(dict(row)) for row in result.mappings().all()]
        # Preserve caller-supplied order so recommendation re-rank keeps its
        # priority list.
        by_id = {course["id"]: course for course in rows}
        return [by_id[cid] for cid in ids if cid in by_id]

    async def fetch_lessons(self) -> list[dict[str, Any]]:
        sql = f"""
            SELECT
                l.id,
                l.title,
                l.description,
                l.content,
                l.video_url,
                l.chapter_id,
                l.content_type,
                l.document_urls,
                l.workspace_enabled,
                l.workspace_languages,
                l.workspace_template,
                l.estimated_duration,
                l.is_free,
                l.mandatory,
                c.course_id,
                crs.title AS course_title,
                crs.level AS course_level,
                crs.language AS course_language
            FROM lessons l
            JOIN chapters c ON l.chapter_id = c.id
            JOIN courses crs ON crs.id = c.course_id
            WHERE l.is_active = 'Y'
              AND crs.is_active = 'Y'
            ORDER BY crs.created DESC, c."order" ASC NULLS LAST, l."order" ASC NULLS LAST
        """
        async with get_db_session() as session:
            result = await session.execute(text(sql))
            return [self._normalize_lesson_row(dict(row)) for row in result.mappings().all()]

    async def fetch_lesson_by_id(self, lesson_id: str) -> dict[str, Any] | None:
        sql = f"""
            SELECT
                l.id,
                l.title,
                l.description,
                l.content,
                l.video_url,
                l.chapter_id,
                l.content_type,
                l.document_urls,
                l.workspace_enabled,
                l.workspace_languages,
                l.workspace_template,
                l.estimated_duration,
                l.is_free,
                l.mandatory,
                c.course_id,
                crs.title AS course_title,
                crs.level AS course_level,
                crs.language AS course_language
            FROM lessons l
            JOIN chapters c ON l.chapter_id = c.id
            JOIN courses crs ON crs.id = c.course_id
            WHERE l.id = :lesson_id
              AND l.is_active = 'Y'
              AND crs.is_active = 'Y'
            LIMIT 1
        """
        async with get_db_session() as session:
            result = await session.execute(text(sql), {"lesson_id": lesson_id})
            row = result.mappings().first()
            return self._normalize_lesson_row(dict(row)) if row else None

    async def fetch_published_blogs(self, limit: int | None = None) -> list[dict[str, Any]]:
        sql = f"""
            SELECT
                b.id,
                b.title,
                b.content,
                b.thumbnail,
                b.author_id,
                b.status,
                b.tags,
                b.attachments,
                b.related_course_ids,
                b.related_lesson_ids,
                b.created,
                b.updated,
                u.username AS author_username,
                p.full_name AS author_full_name
            FROM blogs b
            LEFT JOIN users u
                ON u.id = b.author_id
               AND u.is_active = 'Y'
            LEFT JOIN profiles p
                ON p.user_id = b.author_id
               AND p.is_active = 'Y'
            WHERE {ACTIVE_BLOG_SQL}
              AND b.status = 'PUBLISHED'
            ORDER BY b.created DESC
        """
        params: dict[str, Any] = {}
        if limit:
            sql += "\nLIMIT :limit"
            params["limit"] = limit

        async with get_db_session() as session:
            result = await session.execute(text(sql), params)
            return [self._normalize_blog_row(dict(row)) for row in result.mappings().all()]

    async def fetch_user_profile(self, user_id: str) -> dict[str, Any] | None:
        sql = """
            SELECT
                u.id AS user_id,
                u.email,
                u.username,
                p.full_name,
                p.bio,
                p.location,
                p.preferred_language,
                p.learning_history
            FROM users u
            LEFT JOIN profiles p
                ON p.user_id = u.id
               AND p.is_active = 'Y'
            WHERE u.id = :user_id
              AND u.is_active = 'Y'
            LIMIT 1
        """
        async with get_db_session() as session:
            result = await session.execute(text(sql), {"user_id": user_id})
            row = result.mappings().first()
            if row is None:
                return None
            payload = dict(row)
            payload["user_id"] = _normalize_uuidish(payload.get("user_id"))
            payload["learning_history"] = _normalize_jsonish(payload.get("learning_history"), {})
            payload["skill_profile"] = await self.compute_skill_profile(user_id)
            return payload

    async def fetch_user_ratings(self, user_id: str, *, raise_on_error: bool = False) -> list[dict[str, Any]]:
        """Fetch user's course ratings. score >= 4 indicates preference."""
        sql = f"""
            SELECT
                r.target_id AS course_id,
                r.score AS score,
                c.title,
                c.level,
                COALESCE(
                    json_agg(DISTINCT s.name) FILTER (WHERE s.name IS NOT NULL),
                    '[]'::json
                ) AS skills
            FROM ratings r
            JOIN courses c
                ON c.id = r.target_id
               AND r.target_type = 'COURSE'
               AND {ACTIVE_COURSE_SQL}
            LEFT JOIN course_skills cs ON cs.course_id = c.id
            LEFT JOIN skills s
                ON s.id = cs.skill_id
               AND s.is_active = 'Y'
            WHERE r.user_id = :user_id
              AND r.is_active = 'Y'
            GROUP BY r.target_id, r.score, c.title, c.level
            ORDER BY r.score DESC
        """
        try:
            async with get_db_session() as session:
                result = await session.execute(text(sql), {"user_id": user_id})
                items = []
                for row in result.mappings().all():
                    payload = dict(row)
                    payload["course_id"] = _normalize_uuidish(payload.get("course_id"))
                    payload["score"] = int(payload.get("score") or 0)
                    payload["skills"] = _normalize_jsonish(payload.get("skills"), [])
                    items.append(payload)
                return items
        except Exception:
            logger.warning(
                json.dumps(
                    {
                        "event": "catalog_signal_fetch_failed",
                        "signal": "ratings",
                        "userId": user_id,
                    },
                    ensure_ascii=False,
                ),
                exc_info=True,
            )
            await runtime_observability_service.record_error(scope="catalog_user_ratings")
            if raise_on_error:
                raise
            return []

    async def fetch_course_prerequisites(self, course_id: str) -> list[str]:
        """Return prerequisites when the domain schema supports them.

        techhub.sql currently has no course_prerequisites table. Returning an
        explicit empty list is safer than hiding a missing-table error and
        making recommendation logic look data-backed when it is not.
        """
        del course_id
        return []

    async def compute_skill_profile(self, user_id: str) -> dict[str, float]:
        """Compute skill proficiency from completed courses and ratings.

        Logic:
        - completed course with rating >= 4 → skill = 0.9
        - completed course without rating or rating < 4 → skill = 0.7
        - in-progress course → skill = 0.3 * progress
        """
        course_history = await self.fetch_user_course_history(user_id)
        ratings_list = await self.fetch_user_ratings(user_id)
        ratings_map = {r["course_id"]: r["score"] for r in ratings_list}
        skills: dict[str, float] = {}
        for item in course_history:
            course_id = item.get("course_id", "")
            status = item.get("status", "")
            history_bucket = item.get("historyBucket", "")
            progress = float(item.get("progress", 0))
            course_skills = item.get("skills", [])
            rating = ratings_map.get(course_id, 0)
            for skill_name in course_skills:
                if not skill_name:
                    continue
                if status == "COMPLETED" or history_bucket == "completed":
                    level = 0.9 if rating >= 4 else 0.7
                elif status == "IN_PROGRESS" or history_bucket == "in_progress":
                    level = round(0.3 * progress, 2)
                else:
                    level = 0.1
                skills[skill_name] = max(skills.get(skill_name, 0), level)
        return skills

    async def fetch_user_course_history(self, user_id: str) -> list[dict[str, Any]]:
        sql = f"""
            SELECT
                e.id AS enrollment_id,
                e.course_id,
                e.status,
                c.title,
                c.description,
                c.level,
                c.language,
                c.thumbnail,
                COALESCE(
                    AVG(COALESCE(p.completion, 0.0)) FILTER (WHERE l.id IS NOT NULL),
                    0.0
                ) AS progress,
                COUNT(DISTINCT l.id) AS lesson_count,
                COUNT(DISTINCT p.id) AS progress_record_count,
                COALESCE(
                    json_agg(DISTINCT s.name) FILTER (WHERE s.name IS NOT NULL),
                    '[]'::json
                ) AS skills
            FROM enrollments e
            JOIN courses c
                ON c.id = e.course_id
               AND {ACTIVE_COURSE_SQL}
            LEFT JOIN chapters ch
                ON ch.course_id = c.id
            LEFT JOIN lessons l
                ON l.chapter_id = ch.id
               AND l.is_active = 'Y'
            LEFT JOIN progress p
                ON p.lesson_id = l.id
               AND p.user_id = e.user_id
               AND p.is_active = 'Y'
            LEFT JOIN course_skills cs
                ON cs.course_id = c.id
            LEFT JOIN skills s
                ON s.id = cs.skill_id
            WHERE e.user_id = :user_id
              AND e.is_active = 'Y'
            GROUP BY e.id, e.course_id, e.status, c.id
            ORDER BY MAX(e.updated) DESC NULLS LAST, MAX(e.created) DESC NULLS LAST
        """
        async with get_db_session() as session:
            result = await session.execute(text(sql), {"user_id": user_id})
            items = []
            for row in result.mappings().all():
                payload = dict(row)
                payload["enrollment_id"] = _normalize_uuidish(payload.get("enrollment_id"))
                payload["course_id"] = _normalize_uuidish(payload.get("course_id"))
                payload = self._normalize_course_history_row(payload)
                payload["skills"] = _normalize_jsonish(payload.get("skills"), [])
                items.append(payload)
            return items

    async def fetch_user_learning_paths(self, user_id: str) -> list[dict[str, Any]]:
        sql = f"""
            SELECT
                lp.id AS path_id,
                lp.title,
                lp.description,
                COALESCE(MAX(pp.completion), 0.0) AS completion,
                COALESCE(
                    json_agg(
                        DISTINCT jsonb_build_object(
                            'course_id', lpc.course_id,
                            'order', lpc."order",
                            'position_x', lpc.position_x,
                            'position_y', lpc.position_y,
                            'is_optional', lpc.is_optional
                        )
                    ) FILTER (WHERE lpc.course_id IS NOT NULL),
                    '[]'::json
                ) AS courses
            FROM learning_paths lp
            LEFT JOIN path_progress pp
                ON pp.path_id = lp.id
               AND pp.user_id = :user_id
               AND pp.is_active = 'Y'
            LEFT JOIN learning_path_courses lpc
                ON lpc.path_id = lp.id
            WHERE lp.is_active = 'Y'
              AND pp.user_id = :user_id
            GROUP BY lp.id, lp.title, lp.description
            ORDER BY COALESCE(MAX(pp.updated), lp.updated) DESC NULLS LAST, lp.updated DESC
        """
        async with get_db_session() as session:
            result = await session.execute(text(sql), {"user_id": user_id})
            items = []
            for row in result.mappings().all():
                payload = dict(row)
                payload["path_id"] = _normalize_uuidish(payload.get("path_id"))
                courses = _normalize_jsonish(payload.get("courses"), [])
                courses = sorted(
                    (
                        {
                            **course,
                            "course_id": _normalize_uuidish(course.get("course_id")),
                            "order": int(course.get("order") or 0),
                        }
                        for course in courses
                    ),
                    key=lambda item: item["order"],
                )
                payload["completion"] = float(payload.get("completion") or 0.0)
                payload["courses"] = courses
                items.append(payload)
            return items

    async def fetch_user_profiles_for_indexing(self) -> list[dict[str, Any]]:
        profiles = {
            item["user_id"]: item
            for item in await self._fetch_profiles_snapshot()
        }
        histories = await self._fetch_all_enrollment_histories()

        for user_id, courses in histories.items():
            profile = profiles.setdefault(
                user_id,
                {
                    "user_id": user_id,
                    "email": None,
                    "username": None,
                    "full_name": None,
                    "bio": None,
                    "location": None,
                    "preferred_language": None,
                    "learning_history": {},
                },
            )
            profile["course_history"] = courses

        for profile in profiles.values():
            profile.setdefault("course_history", [])
        return list(profiles.values())

    async def _fetch_profiles_snapshot(self) -> list[dict[str, Any]]:
        sql = """
            SELECT
                u.id AS user_id,
                u.email,
                u.username,
                p.full_name,
                p.bio,
                p.location,
                p.preferred_language,
                p.learning_history
            FROM users u
            LEFT JOIN profiles p
                ON p.user_id = u.id
               AND p.is_active = 'Y'
            WHERE u.is_active = 'Y'
        """
        async with get_db_session() as session:
            result = await session.execute(text(sql))
            items = []
            for row in result.mappings().all():
                payload = dict(row)
                payload["user_id"] = _normalize_uuidish(payload.get("user_id"))
                payload["learning_history"] = _normalize_jsonish(payload.get("learning_history"), {})
                items.append(payload)
            return items

    async def _fetch_all_enrollment_histories(self) -> dict[str, list[dict[str, Any]]]:
        sql = f"""
            SELECT
                e.user_id,
                e.course_id,
                e.status,
                c.title,
                c.level,
                c.language,
                COALESCE(
                    AVG(COALESCE(p.completion, 0.0)) FILTER (WHERE l.id IS NOT NULL),
                    0.0
                ) AS progress,
                COUNT(DISTINCT l.id) AS lesson_count,
                COUNT(DISTINCT p.id) AS progress_record_count,
                COALESCE(
                    json_agg(DISTINCT s.name) FILTER (WHERE s.name IS NOT NULL),
                    '[]'::json
                ) AS skills
            FROM enrollments e
            JOIN courses c
                ON c.id = e.course_id
               AND {ACTIVE_COURSE_SQL}
            LEFT JOIN chapters ch
                ON ch.course_id = c.id
            LEFT JOIN lessons l
                ON l.chapter_id = ch.id
               AND l.is_active = 'Y'
            LEFT JOIN progress p
                ON p.lesson_id = l.id
               AND p.user_id = e.user_id
               AND p.is_active = 'Y'
            LEFT JOIN course_skills cs
                ON cs.course_id = c.id
            LEFT JOIN skills s
                ON s.id = cs.skill_id
            WHERE e.is_active = 'Y'
            GROUP BY e.user_id, e.course_id, e.status, c.id
            ORDER BY MAX(e.updated) DESC NULLS LAST, MAX(e.created) DESC NULLS LAST
        """
        async with get_db_session() as session:
            result = await session.execute(text(sql))
            grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
            for row in result.mappings().all():
                payload = dict(row)
                user_id = _normalize_uuidish(payload.get("user_id"))
                if user_id is None:
                    continue
                payload = self._normalize_course_history_row(payload)
                grouped[user_id].append(
                    {
                        "course_id": _normalize_uuidish(payload.get("course_id")),
                        "status": payload.get("status"),
                        "historyBucket": payload.get("historyBucket"),
                        "title": payload.get("title"),
                        "level": payload.get("level"),
                        "language": payload.get("language"),
                        "progress": float(payload.get("progress") or 0.0),
                        "progressSource": payload.get("progressSource"),
                        "lessonCount": int(payload.get("lessonCount") or 0),
                        "progressRecordCount": int(payload.get("progressRecordCount") or 0),
                        "skills": _normalize_jsonish(payload.get("skills"), []),
                    }
                )
            return grouped

    @staticmethod
    def build_course_search_document(course: dict[str, Any]) -> str:
        parts: list[str] = [
            str(course.get("title") or ""),
            str(course.get("description") or ""),
            " ".join(course.get("objectives") or []),
            " ".join(course.get("requirements") or []),
            " ".join(course.get("tags") or []),
            " ".join(course.get("skills") or []),
            str(course.get("level") or ""),
            str(course.get("language") or ""),
        ]
        return "\n".join(part for part in parts if part).strip()

    @staticmethod
    def build_lesson_search_document(lesson: dict[str, Any]) -> str:
        parts: list[str] = [
            str(lesson.get("course_title") or ""),
            str(lesson.get("title") or ""),
            str(lesson.get("description") or ""),
            str(lesson.get("content") or ""),
            " ".join(lesson.get("workspace_languages") or []),
            " ".join(lesson.get("document_urls") or []),
        ]
        return "\n".join(part for part in parts if part).strip()

    @staticmethod
    def build_blog_search_document(blog: dict[str, Any]) -> str:
        content = str(blog.get("content") or "")
        plain_content = HTML_TAG_RE.sub(" ", content)
        plain_content = re.sub(r"\s+", " ", plain_content).strip()
        parts: list[str] = [
            str(blog.get("title") or ""),
            plain_content,
            " ".join(blog.get("tags") or []),
            str(blog.get("author_full_name") or blog.get("author_username") or ""),
            "related_courses=" + ", ".join(blog.get("related_course_ids") or []),
            "related_lessons=" + ", ".join(blog.get("related_lesson_ids") or []),
        ]
        return "\n".join(part for part in parts if part).strip()

    @staticmethod
    def summarize_user_profile(profile: dict[str, Any], course_history: list[dict[str, Any]]) -> str:
        completed = [
            item["title"]
            for item in course_history
            if item.get("status") == "COMPLETED" or item.get("historyBucket") == "completed"
        ]
        in_progress = [
            item["title"]
            for item in course_history
            if item.get("status") == "IN_PROGRESS" or item.get("historyBucket") == "in_progress"
        ]
        skills = sorted({skill for item in course_history for skill in item.get("skills", [])})
        history = profile.get("learning_history") or {}
        if isinstance(history, dict):
            history_text = json.dumps(history, ensure_ascii=False)
        else:
            history_text = str(history)

        parts = [
            f"user={profile.get('user_id')}",
            f"name={profile.get('full_name') or profile.get('username') or ''}",
            f"bio={profile.get('bio') or ''}",
            f"preferred_language={profile.get('preferred_language') or ''}",
            f"skills={', '.join(skills)}",
            f"completed_courses={', '.join(completed)}",
            f"in_progress_courses={', '.join(in_progress)}",
            f"learning_history={history_text}",
        ]
        return "\n".join(part for part in parts if part.strip()).strip()

    @staticmethod
    def _normalize_course_row(row: dict[str, Any]) -> dict[str, Any]:
        row["id"] = _normalize_uuidish(row.get("id"))
        row["instructor_id"] = _normalize_uuidish(row.get("instructor_id"))
        row["objectives"] = _normalize_jsonish(row.get("objectives"), [])
        row["requirements"] = _normalize_jsonish(row.get("requirements"), [])
        row["tags"] = _normalize_jsonish(row.get("tags"), [])
        row["skills"] = _normalize_jsonish(row.get("skills"), [])
        row["enrollment_count"] = int(row.get("enrollment_count") or 0)
        row["average_rating"] = round(float(row.get("average_rating") or 0.0), 2)
        row["rating_count"] = int(row.get("rating_count") or 0)
        return row

    @staticmethod
    def _normalize_lesson_row(row: dict[str, Any]) -> dict[str, Any]:
        row["id"] = _normalize_uuidish(row.get("id"))
        row["course_id"] = _normalize_uuidish(row.get("course_id"))
        row["chapter_id"] = _normalize_uuidish(row.get("chapter_id"))
        row["document_urls"] = _normalize_jsonish(row.get("document_urls"), [])
        row["workspace_languages"] = _normalize_jsonish(row.get("workspace_languages"), [])
        row["workspace_template"] = _normalize_jsonish(row.get("workspace_template"), {})
        return row

    @staticmethod
    def _normalize_blog_row(row: dict[str, Any]) -> dict[str, Any]:
        row["id"] = _normalize_uuidish(row.get("id"))
        row["author_id"] = _normalize_uuidish(row.get("author_id"))
        row["tags"] = _normalize_jsonish(row.get("tags"), [])
        row["attachments"] = _normalize_jsonish(row.get("attachments"), [])
        row["related_course_ids"] = [
            value for value in (_normalize_uuidish(item) for item in _normalize_jsonish(row.get("related_course_ids"), []))
            if value
        ]
        row["related_lesson_ids"] = [
            value for value in (_normalize_uuidish(item) for item in _normalize_jsonish(row.get("related_lesson_ids"), []))
            if value
        ]
        return row

    @staticmethod
    def _normalize_course_history_row(row: dict[str, Any]) -> dict[str, Any]:
        status = str(row.get("status") or "ENROLLED").upper()
        raw_progress = float(row.get("progress") or 0.0)
        lesson_count = int(row.get("lesson_count") or row.get("lessonCount") or 0)
        progress_record_count = int(row.get("progress_record_count") or row.get("progressRecordCount") or 0)

        if lesson_count > 0:
            progress_source = "lesson_progress" if progress_record_count else "lesson_progress_empty"
            effective_progress = raw_progress
            if status == "COMPLETED":
                effective_progress = max(effective_progress, 1.0)
        else:
            progress_source = "enrollment_status_fallback"
            effective_progress = 1.0 if status == "COMPLETED" else 0.0

        effective_progress = round(max(0.0, min(float(effective_progress), 1.0)), 4)
        if status == "DROPPED":
            history_bucket = "abandoned"
        elif status == "COMPLETED" or effective_progress >= 0.95:
            history_bucket = "completed"
        elif status == "IN_PROGRESS" or 0.0 < effective_progress < 0.95:
            history_bucket = "in_progress"
        else:
            history_bucket = "enrolled"

        row["status"] = status
        row["progress"] = effective_progress
        row["progressRaw"] = round(max(0.0, min(raw_progress, 1.0)), 4)
        row["progressSource"] = progress_source
        row["lessonCount"] = lesson_count
        row["progressRecordCount"] = progress_record_count
        row["historyBucket"] = history_bucket
        return row


catalog_service = CatalogService()
