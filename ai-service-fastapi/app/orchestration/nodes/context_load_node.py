from __future__ import annotations

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

    conversation_context = {
        "recentMessages": context.recentMessages,
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
    skill_profile: dict = {}
    user_course_history: list = []
    user_ratings: list = []
    try:
        user_course_history = await catalog_service.fetch_user_course_history(user_id)
        user_ratings = await catalog_service.fetch_user_ratings(user_id)
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
        "skill_profile": skill_profile,
        "user_course_history": user_course_history,
        "user_ratings": user_ratings,
    }
    trace_step(state, "context_load", "Loaded Redis conversation context.",
               fileCount=len(file_contexts), freshFileContext=has_fresh_file_context,
               hitlResume=bool(context.awaitingClarification))
    updates["execution_trace"] = list(state.get("execution_trace", []))
    return updates
