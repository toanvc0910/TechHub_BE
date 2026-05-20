"""
Strict validator for AI-generated learning path drafts before they get
published to Learning Path Service. Every check enforces a business rule
documented in `docs/implementation-plan/06-learning-path-publish-flow.md`:

  - every courseId must exist, be active, and PUBLISHED in PostgreSQL
  - no duplicate courseId
  - every node.id must map to a course in `courses`
  - every edge source/target must point to a real node
  - no self-loop, no duplicate edge, no cycle
  - layoutEdges payload shape matches `LearningPath.LayoutEdge`
  - `isOptional` is either "Y" or "N"
  - `order` is unique, positive, and matches the courses list

The validator never calls Learning Path Service — it only protects the
boundary so we don't ship a known-bad draft upstream.
"""

from __future__ import annotations

from collections.abc import Iterable
from dataclasses import dataclass
from typing import Any

from sqlalchemy import text

from app.db.session import get_db_session


class LearningPathValidationError(ValueError):
    """Raised when a draft fails any business rule below."""


@dataclass(frozen=True, slots=True)
class ValidatedPath:
    title: str
    description: str
    skills: tuple[str, ...]
    courses: tuple[dict[str, Any], ...]
    layout_edges: tuple[dict[str, str], ...]


class LearningPathValidator:
    async def validate(self, draft: dict[str, Any]) -> ValidatedPath:
        if not isinstance(draft, dict):
            raise LearningPathValidationError("Draft payload must be a JSON object.")

        title = str(draft.get("title") or "").strip()
        if not title:
            raise LearningPathValidationError("Learning path title is required.")
        if len(title) > 255:
            raise LearningPathValidationError("Learning path title must be <= 255 chars.")

        description = str(draft.get("description") or "").strip()

        raw_courses = draft.get("courses")
        if not isinstance(raw_courses, list) or not raw_courses:
            raise LearningPathValidationError("Draft must contain a non-empty 'courses' array.")
        if len(raw_courses) > 30:
            raise LearningPathValidationError("Draft has too many courses (max 30).")

        normalized_courses = self._normalize_courses(raw_courses)
        course_ids = [course["courseId"] for course in normalized_courses]
        await self._ensure_courses_exist(course_ids)

        nodes = draft.get("nodes")
        edges = draft.get("edges") or draft.get("layoutEdges") or []
        self._validate_nodes(nodes, course_ids)
        cleaned_edges = self._validate_edges(edges, course_ids)

        # Cycle detection on the directed edge graph.
        self._ensure_acyclic(course_ids, cleaned_edges)

        skills = self._normalize_skills(draft.get("skills"))

        return ValidatedPath(
            title=title,
            description=description,
            skills=tuple(skills),
            courses=tuple(normalized_courses),
            layout_edges=tuple(cleaned_edges),
        )

    @staticmethod
    def _normalize_courses(raw_courses: list[Any]) -> list[dict[str, Any]]:
        seen_ids: set[str] = set()
        seen_orders: set[int] = set()
        normalized: list[dict[str, Any]] = []
        for index, item in enumerate(raw_courses, start=1):
            if not isinstance(item, dict):
                raise LearningPathValidationError(f"Course entry #{index} must be an object.")
            course_id = str(item.get("courseId") or "").strip()
            if not course_id:
                raise LearningPathValidationError(f"Course entry #{index} missing courseId.")
            if course_id in seen_ids:
                raise LearningPathValidationError(f"Duplicate courseId: {course_id}.")
            seen_ids.add(course_id)

            try:
                order = int(item.get("order") if item.get("order") is not None else index)
            except (TypeError, ValueError) as exc:
                raise LearningPathValidationError(f"Course {course_id} has non-integer order.") from exc
            if order < 1:
                raise LearningPathValidationError(f"Course {course_id} order must be >= 1.")
            if order in seen_orders:
                raise LearningPathValidationError(f"Duplicate order={order} (course {course_id}).")
            seen_orders.add(order)

            is_optional = str(item.get("isOptional") or "N").upper()
            if is_optional not in {"Y", "N"}:
                raise LearningPathValidationError(
                    f"Course {course_id} isOptional must be 'Y' or 'N' (got {item.get('isOptional')!r})."
                )

            position_x = item.get("positionX")
            position_y = item.get("positionY")
            try:
                position_x = int(position_x) if position_x is not None else 120 + (order - 1) * 280
                position_y = int(position_y) if position_y is not None else 220
            except (TypeError, ValueError) as exc:
                raise LearningPathValidationError(
                    f"Course {course_id} position must be integer."
                ) from exc

            normalized.append(
                {
                    "courseId": course_id,
                    "title": str(item.get("title") or "").strip(),
                    "description": str(item.get("description") or "").strip()[:150],
                    "thumbnail": item.get("thumbnail"),
                    "order": order,
                    "positionX": position_x,
                    "positionY": position_y,
                    "isOptional": is_optional,
                }
            )
        normalized.sort(key=lambda c: c["order"])
        return normalized

    @staticmethod
    async def _ensure_courses_exist(course_ids: Iterable[str]) -> None:
        ids = [cid for cid in course_ids if cid]
        if not ids:
            return
        sql = """
            SELECT id::text AS id, status::text AS status, is_active
            FROM courses
            WHERE id = ANY(CAST(:ids AS uuid[]))
        """
        async with get_db_session() as session:
            result = await session.execute(text(sql), {"ids": ids})
            rows = {row["id"]: row for row in result.mappings().all()}

        missing = [cid for cid in ids if cid not in rows]
        if missing:
            raise LearningPathValidationError(f"Unknown courseIds: {sorted(missing)}")

        invalid: list[str] = []
        for cid in ids:
            row = rows[cid]
            if str(row.get("is_active")) != "Y":
                invalid.append(f"{cid}(inactive)")
                continue
            if str(row.get("status") or "").upper() != "PUBLISHED":
                invalid.append(f"{cid}(status={row.get('status')})")
        if invalid:
            raise LearningPathValidationError(
                f"Courses not eligible for publish (must be active+PUBLISHED): {invalid}"
            )

    @staticmethod
    def _validate_nodes(nodes: Any, course_ids: list[str]) -> None:
        if nodes is None:
            return  # nodes optional; if present, must reference the same course ids
        if not isinstance(nodes, list):
            raise LearningPathValidationError("Draft 'nodes' must be a list when provided.")
        allowed = set(course_ids)
        seen: set[str] = set()
        for index, node in enumerate(nodes, start=1):
            if not isinstance(node, dict):
                raise LearningPathValidationError(f"Node #{index} must be an object.")
            node_id = str(node.get("id") or "").strip()
            if not node_id:
                raise LearningPathValidationError(f"Node #{index} missing id.")
            if node_id in seen:
                raise LearningPathValidationError(f"Duplicate node id: {node_id}.")
            seen.add(node_id)
            if node_id not in allowed:
                raise LearningPathValidationError(
                    f"Node {node_id} does not correspond to a course in this draft."
                )

    @staticmethod
    def _validate_edges(edges: Any, course_ids: list[str]) -> list[dict[str, str]]:
        if edges in (None, []):
            return []
        if not isinstance(edges, list):
            raise LearningPathValidationError("Draft 'edges'/'layoutEdges' must be a list.")
        allowed = set(course_ids)
        cleaned: list[dict[str, str]] = []
        seen_pairs: set[tuple[str, str]] = set()
        for index, edge in enumerate(edges, start=1):
            if not isinstance(edge, dict):
                raise LearningPathValidationError(f"Edge #{index} must be an object.")
            source = str(edge.get("source") or "").strip()
            target = str(edge.get("target") or "").strip()
            if not source or not target:
                raise LearningPathValidationError(f"Edge #{index} missing source/target.")
            if source == target:
                raise LearningPathValidationError(f"Edge #{index} forms a self-loop ({source}).")
            if source not in allowed:
                raise LearningPathValidationError(f"Edge #{index} source {source} not in courses.")
            if target not in allowed:
                raise LearningPathValidationError(f"Edge #{index} target {target} not in courses.")
            pair = (source, target)
            if pair in seen_pairs:
                raise LearningPathValidationError(f"Duplicate edge: {source} -> {target}.")
            seen_pairs.add(pair)
            cleaned.append({"source": source, "target": target})
        return cleaned

    @staticmethod
    def _ensure_acyclic(course_ids: list[str], edges: list[dict[str, str]]) -> None:
        adjacency: dict[str, list[str]] = {cid: [] for cid in course_ids}
        for edge in edges:
            adjacency[edge["source"]].append(edge["target"])

        # Iterative DFS, mark colors: 0=unseen, 1=on-stack, 2=done.
        color: dict[str, int] = {cid: 0 for cid in course_ids}
        for start in course_ids:
            if color[start] != 0:
                continue
            stack: list[tuple[str, int]] = [(start, 0)]
            while stack:
                node, idx = stack[-1]
                if idx == 0:
                    color[node] = 1
                neighbors = adjacency[node]
                if idx >= len(neighbors):
                    color[node] = 2
                    stack.pop()
                    continue
                stack[-1] = (node, idx + 1)
                nxt = neighbors[idx]
                if color[nxt] == 1:
                    raise LearningPathValidationError(
                        f"Cycle detected in learning path graph (back-edge {node} -> {nxt})."
                    )
                if color[nxt] == 0:
                    stack.append((nxt, 0))

    @staticmethod
    def _normalize_skills(raw: Any) -> list[str]:
        if not raw:
            return []
        if not isinstance(raw, list):
            raise LearningPathValidationError("'skills' must be a list of strings.")
        cleaned = [str(item).strip() for item in raw if str(item).strip()]
        return cleaned[:16]


learning_path_validator = LearningPathValidator()
