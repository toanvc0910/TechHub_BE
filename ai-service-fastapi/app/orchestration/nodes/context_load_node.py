from __future__ import annotations

import re
import unicodedata

from sqlalchemy import text

from app.core.enums import ChatSender
from app.db.session import get_db_session
from app.orchestration.memory.redis_memory_service import redis_memory_service
from app.orchestration.state.orchestrator_state import OrchestratorState, trace_step
from app.services.catalog_service import catalog_service
from app.services.file_context_service import file_context_service


async def context_load_node(state: OrchestratorState) -> dict:
    user_id = state["user_id"]
    session_id = state["session_id"]
    context = await redis_memory_service.get_context(user_id=user_id, session_id=session_id)
    user_memory = await redis_memory_service.get_user_memory(user_id)
    request_context = state.get("request_context") if isinstance(state.get("request_context"), dict) else {}
    request_file_contexts = request_context.get("fileContexts") or request_context.get("files") or []
    if not isinstance(request_file_contexts, list):
        request_file_contexts = []
    file_contexts = request_file_contexts or list(context.activeFiles or [])
    recent_messages = context.recentMessages
    context_source = "redis"
    if not recent_messages:
        recent_messages = await _load_recent_messages_from_db(
            user_id=user_id,
            session_id=session_id,
            current_input=state["user_input"],
        )
        if recent_messages:
            context_source = "postgres"

    conversation_context = {
        "recentMessages": recent_messages,
        "entities": context.entities,
        "filters": context.filters,
        "lastIntent": context.lastIntent,
        "lastQuery": context.lastQuery,
        "activeFiles": context.activeFiles,
    }

    has_fresh_file_context = bool(request_file_contexts)
    file_contexts = [item for item in file_contexts if isinstance(item, dict)]
    errors: list[str] = list(state.get("errors", []))

    if has_fresh_file_context and file_contexts:
        try:
            file_contexts = await file_context_service.hydrate_and_index_contexts(
                user_id=user_id,
                session_id=session_id,
                files=file_contexts,
            )
        except Exception as exc:
            errors.append(f"file_context_hydration_failed:{exc}")

    # Load user personalization data for downstream agents
    user_profile: dict = {}
    skill_profile: dict = {}
    user_course_history: list = []
    user_ratings: list = []
    try:
        fetched_profile = await catalog_service.fetch_user_profile(user_id)
        if isinstance(fetched_profile, dict):
            user_profile = fetched_profile
            profile_skills = fetched_profile.get("skill_profile")
            if isinstance(profile_skills, dict):
                skill_profile = profile_skills
    except Exception:
        pass
    try:
        user_course_history = await catalog_service.fetch_user_course_history(user_id)
        user_ratings = await catalog_service.fetch_user_ratings(user_id)
        if not skill_profile:
            skill_profile = await catalog_service.compute_skill_profile(user_id)
    except Exception:
        pass  # graceful degradation — RAG still works without personalization

    # HITL resume: if previous turn was a clarification, increment round
    hitl_round = state.get("hitl_round", 0)
    updated_input = state["user_input"]
    if context.awaitingClarification:
        hitl_round = 1
        original_question = context.awaitingClarification.get("originalQuestion", "")
        if original_question:
            updated_input = f"{original_question} -- Lam ro: {state['user_input']}"

    updates: dict = {
        "conversation_context": conversation_context,
        "has_fresh_file_context": has_fresh_file_context,
        "file_contexts": file_contexts,
        "user_memory": user_memory,
        "errors": errors,
        "hitl_round": hitl_round,
        "user_input": updated_input,
        "user_profile": user_profile,
        "skill_profile": skill_profile,
        "user_course_history": user_course_history,
        "user_ratings": user_ratings,
    }
    trace_step(state, "context_load", "Loaded conversation context.",
               fileCount=len(file_contexts), freshFileContext=has_fresh_file_context,
               hitlResume=bool(context.awaitingClarification), contextSource=context_source,
               recentMessageCount=len(recent_messages))
    updates["execution_trace"] = list(state.get("execution_trace", []))
    return updates


async def _load_recent_messages_from_db(
    *,
    user_id: str,
    session_id: str,
    current_input: str,
    limit: int = 12,
) -> list[dict[str, str]]:
    try:
        async with get_db_session() as session:
            result = await session.execute(
                text(
                    """
                    SELECT cm.sender, cm.content
                      FROM chat_messages cm
                      JOIN chat_sessions cs ON cs.id = cm.session_id
                     WHERE cm.session_id = CAST(:session_id AS uuid)
                       AND cs.user_id = CAST(:user_id AS uuid)
                       AND cm.is_active = 'Y'
                       AND cs.is_active = 'Y'
                     ORDER BY cm.timestamp DESC
                     LIMIT :limit
                    """
                ),
                {"session_id": session_id, "user_id": user_id, "limit": limit},
            )
            rows = list(result.mappings().all())
    except Exception:
        return []

    messages: list[dict[str, str]] = []
    for row in reversed(rows):
        sender = str(row.get("sender") or "").upper()
        content = str(row.get("content") or "").strip()
        if not content:
            continue
        role = "assistant" if sender == ChatSender.BOT.value else "user"
        messages.append({"role": role, "content": content})

    if (
        messages
        and messages[-1].get("role") == "user"
        and _normalize_text(messages[-1].get("content", "")) == _normalize_text(current_input)
    ):
        messages.pop()
    return messages[-10:]


def _normalize_text(text_value: str) -> str:
    lowered = (text_value or "").lower().strip().replace("\u0111", "d")
    normalized = unicodedata.normalize("NFD", lowered)
    without_marks = "".join(ch for ch in normalized if unicodedata.category(ch) != "Mn")
    return re.sub(r"\s+", " ", without_marks)
