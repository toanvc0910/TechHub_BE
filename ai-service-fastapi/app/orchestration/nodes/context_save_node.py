from __future__ import annotations

from app.orchestration.memory.personal_memory import extract_user_memory_facts
from app.orchestration.memory.redis_memory_service import ConversationContext, redis_memory_service
from app.orchestration.state.orchestrator_state import OrchestratorState, trace_step


async def context_save_node(state: OrchestratorState) -> dict:
    user_memory = dict(state.get("user_memory", {}))
    user_memory.update(extract_user_memory_facts(state["user_input"]))

    recent = list(state.get("conversation_context", {}).get("recentMessages", []))
    recent.extend(
        [
            {"role": "user", "content": state["user_input"]},
            {"role": "assistant", "content": state.get("final_response", "")},
        ]
    )
    recent = recent[-10:]

    active_files: list[dict] = []
    for file_item in state.get("file_contexts", []):
        if not isinstance(file_item, dict):
            continue
        active_files.append(
            {
                "id": str(file_item.get("id") or file_item.get("fileId") or ""),
                "name": str(file_item.get("name") or file_item.get("filename") or "uploaded-file"),
                "mimeType": str(file_item.get("mimeType") or file_item.get("mime_type") or ""),
                "excerpt": str(file_item.get("excerpt") or "")[:500],
                "secureUrl": str(file_item.get("secureUrl") or file_item.get("url") or ""),
                "publicUrl": str(file_item.get("publicUrl") or ""),
                "ingestionStatus": str(file_item.get("ingestionStatus") or ""),
            }
        )

    # HITL clarify: save clarification state so next turn can resume
    awaiting_clarification = None
    if state.get("hitl_clarify_active"):
        awaiting_clarification = {
            "originalQuestion": state["user_input"],
            "clarifyQuestion": state.get("hitl_question", ""),
            "options": state.get("hitl_options", []),
        }

    context = ConversationContext(
        recentMessages=recent,
        entities={k: str(v) for k, v in state.get("entities", {}).items()},
        filters={k: str(v) for k, v in user_memory.items() if isinstance(v, (str, int, float, bool))},
        lastIntent=state.get("intent", "conversation"),
        lastQuery=state["user_input"],
        activeFiles=active_files[-5:],
        awaitingClarification=awaiting_clarification,
    )
    await redis_memory_service.save_context(
        user_id=state["user_id"],
        session_id=state["session_id"],
        context=context,
    )
    if user_memory != state.get("user_memory", {}):
        await redis_memory_service.update_user_memory(state["user_id"], user_memory)
    trace_step(state, "context_save", "Saved conversation context into Redis hot path.")
    return {"user_memory": user_memory, "execution_trace": list(state.get("execution_trace", []))}
