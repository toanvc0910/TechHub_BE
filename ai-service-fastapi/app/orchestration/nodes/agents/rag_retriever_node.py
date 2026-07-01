from __future__ import annotations

from typing import Any

from app.orchestration.state.orchestrator_state import OrchestratorState, trace_step
from app.services.vector_service import vector_service


class RagRetrieverNode:
    def supports(self, intent: str, sub_intent: str | None = None) -> bool:
        del sub_intent
        return intent in {"recommendation", "knowledge"}

    async def execute(self, state: OrchestratorState) -> dict[str, Any]:
        entities = state.get("entities", {})
        intent = state.get("intent", "recommendation")

        # Build query from entities
        query = state["user_input"]
        if entities.get("topic"):
            query = f"{entities['topic']} {entities.get('level', '')}".strip()

        # Personalization: exclude completed courses
        course_history = state.get("user_course_history", [])
        completed_ids = {
            item["course_id"]
            for item in course_history
            if item.get("status") == "COMPLETED" or item.get("historyBucket") == "completed" or item.get("progress", 0) >= 0.95
        }

        # Enrich query with skill profile for recommendation intent
        skill_profile = state.get("skill_profile", {})
        if intent == "recommendation" and skill_profile:
            top_skills = sorted(skill_profile.items(), key=lambda x: x[1], reverse=True)[:3]
            skill_hint = " ".join(s[0] for s in top_skills)
            if skill_hint and not entities.get("topic"):
                query = f"{query} {skill_hint}".strip()

        results = await vector_service.search_courses(
            query,
            limit=10,
            level=entities.get("level"),
            exclude_course_ids=completed_ids if completed_ids else None,
        )

        # Re-rank based on personalization signals
        if intent == "recommendation" and course_history:
            results = self._personalized_rerank(results, state)

        citations = [
            {
                "title": item.get("payload", {}).get("title", "Course"),
                "courseId": item.get("payload", {}).get("id") or item.get("payload", {}).get("course_id"),
                "score": item.get("score"),
            }
            for item in results
        ]
        trace_step(state, "rag_retrieve", "Retrieved and personalized documents from vector store.",
                   count=len(results), excludedCompleted=len(completed_ids),
                   skillsUsed=len(skill_profile))
        return {
            "query_result": {"documents": results},
            "citations": citations,
            "execution_trace": list(state.get("execution_trace", [])),
        }

    def _personalized_rerank(self, results: list[dict], state: OrchestratorState) -> list[dict]:
        """Re-rank search results using user's skill profile, ratings, and history."""
        skill_profile = state.get("skill_profile", {})
        user_skills = set(skill_profile.keys())
        ratings_map = {r["course_id"]: r["score"] for r in state.get("user_ratings", [])}
        in_progress_ids = {
            item["course_id"]
            for item in state.get("user_course_history", [])
            if item.get("status") == "IN_PROGRESS" or item.get("historyBucket") in {"in_progress", "enrolled"}
        }

        reranked = []
        for item in results:
            payload = item.get("payload", {})
            course_id = str(payload.get("id") or payload.get("course_id") or "")
            base_score = float(item.get("score", 0))

            # Boost in-progress courses (continue learning)
            if course_id in in_progress_ids:
                base_score += 0.15

            # Boost courses with adjacent skills (skill gap filling)
            course_skills = {str(s).lower() for s in payload.get("skills", [])}
            new_skills = course_skills - {s.lower() for s in user_skills}
            overlap = course_skills & {s.lower() for s in user_skills}
            if overlap and new_skills:
                # Best: course builds on existing skills AND teaches new ones
                base_score += 0.12
            elif new_skills:
                base_score += 0.06

            # Boost based on user ratings pattern (prefer topics user rated highly)
            rated_skills = set()
            for r in state.get("user_ratings", []):
                if r.get("score", 0) >= 4:
                    rated_skills.update(str(s).lower() for s in r.get("skills", []))
            if course_skills & rated_skills:
                base_score += 0.08

            reranked.append({**item, "score": min(base_score, 0.99)})

        reranked.sort(key=lambda x: x.get("score", 0), reverse=True)
        return reranked[:5]


rag_retriever_node = RagRetrieverNode()
