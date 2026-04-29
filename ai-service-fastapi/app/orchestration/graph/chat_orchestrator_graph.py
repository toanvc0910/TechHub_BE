from __future__ import annotations

from time import perf_counter
from typing import Any

from langgraph.graph import END, StateGraph

from app.orchestration.nodes.agents.registry import agent_registry
from app.services.langfuse_service import langfuse_service
from app.orchestration.nodes.context_load_node import context_load_node
from app.orchestration.nodes.context_save_node import context_save_node
from app.orchestration.nodes.entity_extraction_node import entity_extraction_node
from app.orchestration.nodes.hitl_gate_node import hitl_gate_node
from app.orchestration.nodes.intent_node import intent_node
from app.orchestration.nodes.response_compose_node import response_compose_node
from app.orchestration.state.orchestrator_state import OrchestratorState
from app.orchestration.stream.stream_event_emitter import StreamEventEmitter, current_emitter


# ---------------------------------------------------------------------------
# Wrapper helpers: each wraps a node function to track timing and emit SSE
# ---------------------------------------------------------------------------

def _make_node(name: str, fn, detail: str):
    """Return an async node function compatible with LangGraph that also
    records timing into ``node_timings`` and emits a planning_step event
    as soon as the node finishes (progressive)."""

    async def _wrapper(state: OrchestratorState) -> dict[str, Any]:
        emitter = current_emitter.get()

        started = perf_counter()
        updates = await fn(state)
        duration_ms = round((perf_counter() - started) * 1000, 2)
        timings = dict(state.get("node_timings", {}))
        timings[name] = duration_ms
        updates["node_timings"] = timings

        # Emit a single progressive step event when the node completes so the
        # UI can animate steps appearing one-by-one instead of jumping from 0
        # to double the node count (start+end events).
        if emitter is not None:
            try:
                await emitter.emit("planning_step", {
                    "step": name,
                    "detail": detail,
                    "durationMs": duration_ms,
                    "status": "end",
                })
            except Exception:
                pass

        return updates

    _wrapper.__name__ = name  # type: ignore[attr-defined]
    _wrapper._detail = detail  # type: ignore[attr-defined]
    _wrapper._step_name = name  # type: ignore[attr-defined]
    return _wrapper


async def _agent_dispatch_node(state: OrchestratorState) -> dict[str, Any]:
    """Dispatch to the correct agent(s) based on state['intent']."""
    intent = state.get("intent", "conversation")
    agents = agent_registry.resolve_primary(intent)
    merged: dict[str, Any] = {}
    for agent in agents:
        # Each agent.execute now returns a partial dict
        result = await agent.execute(state)
        # Apply updates to state so next agent in chain sees them
        state.update(result)  # type: ignore[arg-type]
        merged.update(result)
    return merged


# ---------------------------------------------------------------------------
# Conditional edge functions
# ---------------------------------------------------------------------------

# ---------------------------------------------------------------------------
# Build the LangGraph StateGraph
# ---------------------------------------------------------------------------

def build_graph() -> Any:
    graph = StateGraph(OrchestratorState)

    graph.add_node("context_load", _make_node(
        "context_load", context_load_node,
        "Loaded session context from Redis cache.",
    ))
    graph.add_node("intent_router", _make_node(
        "intent_router", intent_node,
        "Classified request intent and selected model.",
    ))
    graph.add_node("hitl_gate", _make_node(
        "hitl_gate", hitl_gate_node,
        "Evaluated HITL clarify gate.",
    ))
    graph.add_node("entity_extract", _make_node(
        "entity_extract", entity_extraction_node,
        "Extracted routing entities for downstream agent.",
    ))
    graph.add_node("agent_dispatch", _make_node(
        "agent_dispatch", _agent_dispatch_node,
        "Agent execution completed.",
    ))
    graph.add_node("response_compose", _make_node(
        "response_compose", response_compose_node,
        "Composed final response payload.",
    ))
    graph.add_node("context_save", _make_node(
        "context_save", context_save_node,
        "Stored conversation state back into Redis hot path.",
    ))

    # Edges
    graph.set_entry_point("context_load")
    graph.add_edge("context_load", "intent_router")
    graph.add_edge("intent_router", "hitl_gate")
    # hitl_gate may override intent to 'clarify' — agent_dispatch then routes
    # to conversation_agent which handles clarify mode (generates dynamic question).
    # No conditional edge needed: always proceed through entity_extract → agent_dispatch.
    graph.add_edge("hitl_gate", "entity_extract")
    graph.add_edge("entity_extract", "agent_dispatch")
    graph.add_edge("agent_dispatch", "response_compose")
    graph.add_edge("response_compose", "context_save")
    graph.add_edge("context_save", END)

    return graph.compile()


# Singleton compiled graph
_compiled_graph = None


def _get_graph():
    global _compiled_graph
    if _compiled_graph is None:
        _compiled_graph = build_graph()
    return _compiled_graph


# ---------------------------------------------------------------------------
# Public API — drop-in replacement for the old ChatOrchestratorGraph class
# ---------------------------------------------------------------------------

class ChatOrchestratorGraph:
    async def run(self, state: OrchestratorState, emitter: StreamEventEmitter | None = None) -> OrchestratorState:
        if emitter:
            await emitter.emit("planning_start", {"conversationId": state.get("session_id", "")})

        # Langfuse trace for the entire chat request (v4 API)
        lf_trace = langfuse_service.create_trace(
            name="chat_orchestration",
            user_id=state.get("user_id"),
            session_id=state.get("session_id"),
            metadata={"mode": state.get("mode"), "request_id": state.get("request_id")},
            input=state.get("user_input", "")[:500],
        )

        graph = _get_graph()
        token = current_emitter.set(emitter)
        try:
            final_state: OrchestratorState = await graph.ainvoke(state)
        finally:
            current_emitter.reset(token)

        # Update Langfuse trace with results
        try:
            lf_trace.update(
                output=final_state.get("final_response", "")[:500],
                metadata={
                    "intent": final_state.get("intent"),
                    "confidence": final_state.get("intent_confidence"),
                    "model": final_state.get("selected_model"),
                    "hitl_clarify": final_state.get("hitl_clarify_active", False),
                    "pipeline": final_state.get("pipeline"),
                    "node_timings": final_state.get("node_timings", {}),
                },
            )
            # Log each node as a child span
            for step_name, duration_ms in final_state.get("node_timings", {}).items():
                node_span = lf_trace.start_observation(
                    name=step_name,
                    as_type="span",
                    metadata={"duration_ms": duration_ms},
                )
                node_span.end()
            lf_trace.end()
            langfuse_service.flush()
        except Exception:
            pass

        # planning_step events are now emitted progressively inside _make_node
        # as each node completes — no post-hoc loop needed.
        if emitter and final_state.get("citations"):
            await emitter.emit("citation", {"sources": final_state["citations"]})

        return final_state


chat_orchestrator_graph = ChatOrchestratorGraph()
