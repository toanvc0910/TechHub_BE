from __future__ import annotations

import asyncio
import hashlib
import json
import re
from collections.abc import AsyncIterator
from typing import Any

import httpx

from app.core.config import get_settings
from app.orchestration.stream.stream_event_emitter import current_emitter, text_chunk_event
from app.services.langfuse_service import langfuse_service
from app.services.observability_service import runtime_observability_service
from app.services.provider_config import provider_config_service
from app.services.runtime_request_context import runtime_request_context_service


class SwitchableAiGateway:
    def __init__(self) -> None:
        self._settings = get_settings()
        self._client = httpx.AsyncClient(timeout=60.0)

    async def generate_text(
        self,
        *,
        prompt: str,
        system_prompt: str | None = None,
        model: str | None = None,
    ) -> str:
        provider, actual_model = await provider_config_service.resolve_chat_target(preferred_model=model)
        requested_provider = await provider_config_service.get_requested_provider()
        attempts: list[tuple[str, str]] = []
        if provider:
            attempts.append((provider, actual_model))
        if provider != "openai" and self._settings.openai_api_key:
            attempts.append(("openai", self._settings.openai_chat_model))
        if provider != "gemini" and self._settings.gemini_api_key:
            attempts.append(("gemini", self._settings.gemini_chat_model))

        response_text: str | None = None
        usage: dict[str, int] | None = None
        effective_provider = provider or requested_provider
        actual_used_model = actual_model
        mock_used = False
        for candidate_provider, candidate_model in attempts:
            try:
                if candidate_provider == "openai" and self._settings.openai_api_key:
                    response_text, usage = await self._generate_openai_text(
                        prompt=prompt,
                        system_prompt=system_prompt,
                        model=candidate_model,
                    )
                    effective_provider = candidate_provider
                    actual_used_model = candidate_model
                    break
                if candidate_provider == "gemini" and self._settings.gemini_api_key:
                    response_text, usage = await self._generate_gemini_text(
                        prompt=prompt,
                        system_prompt=system_prompt,
                        model=candidate_model,
                    )
                    effective_provider = candidate_provider
                    actual_used_model = candidate_model
                    break
            except Exception:
                continue
        if response_text is None:
            response_text = self._mock_text(prompt, actual_model)
            usage = None
            effective_provider = requested_provider
            actual_used_model = actual_model
            mock_used = True
        await runtime_observability_service.record_provider_event(
            kind="chat",
            requested_provider=requested_provider,
            effective_provider=effective_provider,
            model=actual_used_model,
            used_mock=mock_used,
        )
        await self._record_chat_usage(
            prompt=prompt,
            response=response_text,
            provider=effective_provider,
            model=actual_used_model,
            usage=usage,
        )
        # Langfuse generation trace (v4 API)
        if langfuse_service.enabled:
            try:
                prompt_tokens = (usage or {}).get("promptTokens") or (usage or {}).get("prompt_tokens") or self._estimate_token_count(prompt)
                completion_tokens = (usage or {}).get("completionTokens") or (usage or {}).get("completion_tokens") or self._estimate_token_count(response_text)
                trace = langfuse_service.create_trace(
                    name="llm_generate_text",
                    metadata={"provider": effective_provider, "model": actual_used_model, "mock": mock_used},
                    input=prompt[:500],
                )
                gen = trace.start_observation(
                    name=f"{effective_provider}/{actual_used_model}",
                    as_type="generation",
                    model=actual_used_model,
                    input=prompt,
                    metadata={"system_prompt": (system_prompt or "")[:200]},
                )
                gen.update(output=response_text, usage_details={"input": prompt_tokens, "output": completion_tokens})
                gen.end()
                trace.update(output=response_text[:300])
                trace.end()
                langfuse_service.flush()
            except Exception:
                pass
        return response_text

    async def generate_embeddings(self, texts: list[str], *, task_type: str | None = None) -> list[list[float]]:
        cleaned_texts = [text.strip() for text in texts if text and text.strip()]
        if not cleaned_texts:
            return []
        provider, embedding_model = await provider_config_service.resolve_embedding_target()
        requested_provider = await provider_config_service.get_requested_provider()
        effective_provider = provider or requested_provider
        await runtime_observability_service.record_provider_event(
            kind="embedding",
            requested_provider=requested_provider,
            effective_provider=effective_provider,
            model=embedding_model,
            used_mock=provider is None,
        )
        if provider == "openai" and self._settings.openai_api_key:
            try:
                embeddings, usage = await self._generate_openai_embeddings(cleaned_texts)
                await self._record_embedding_usage(
                    texts=cleaned_texts,
                    provider=provider,
                    model=embedding_model,
                    usage=usage,
                )
                return embeddings
            except Exception:
                pass
        if provider == "gemini" and self._settings.gemini_api_key:
            try:
                embeddings, usage = await self._generate_gemini_embeddings(
                    cleaned_texts,
                    model=embedding_model,
                    task_type=task_type,
                )
                await self._record_embedding_usage(
                    texts=cleaned_texts,
                    provider=provider,
                    model=embedding_model,
                    usage=usage,
                )
                return embeddings
            except Exception:
                pass
        if provider != "openai" and self._settings.openai_api_key:
            try:
                embeddings, usage = await self._generate_openai_embeddings(cleaned_texts)
                await self._record_embedding_usage(
                    texts=cleaned_texts,
                    provider="openai",
                    model=self._settings.openai_embedding_model,
                    usage=usage,
                )
                return embeddings
            except Exception:
                pass
        if provider != "gemini" and self._settings.gemini_api_key:
            try:
                embeddings, usage = await self._generate_gemini_embeddings(
                    cleaned_texts,
                    model=self._settings.gemini_embedding_model,
                    task_type=task_type,
                )
                await self._record_embedding_usage(
                    texts=cleaned_texts,
                    provider="gemini",
                    model=self._settings.gemini_embedding_model,
                    usage=usage,
                )
                return embeddings
            except Exception:
                pass
        embeddings = [self._deterministic_embedding(text, size=self._settings.embedding_dimension) for text in cleaned_texts]
        await self._record_embedding_usage(
            texts=cleaned_texts,
            provider=effective_provider,
            model=embedding_model,
            usage=None,
        )
        return embeddings

    async def stream_text(
        self,
        *,
        prompt: str,
        system_prompt: str | None = None,
        model: str | None = None,
    ) -> AsyncIterator[str]:
        """Yield incremental text chunks from the LLM provider.

        Tries OpenAI-compatible streaming first (SSE), then Gemini
        streamGenerateContent. Falls back to non-streaming generate_text
        split into chunks if no provider supports streaming.
        """
        provider, actual_model = await provider_config_service.resolve_chat_target(preferred_model=model)
        attempts: list[tuple[str, str]] = []
        if provider:
            attempts.append((provider, actual_model))
        if provider != "openai" and self._settings.openai_api_key:
            attempts.append(("openai", self._settings.openai_chat_model))
        if provider != "gemini" and self._settings.gemini_api_key:
            attempts.append(("gemini", self._settings.gemini_chat_model))

        for candidate_provider, candidate_model in attempts:
            try:
                if candidate_provider == "openai" and self._settings.openai_api_key:
                    async for delta in self._stream_openai_text(
                        prompt=prompt, system_prompt=system_prompt, model=candidate_model
                    ):
                        yield delta
                    return
                if candidate_provider == "gemini" and self._settings.gemini_api_key:
                    async for delta in self._stream_gemini_text(
                        prompt=prompt, system_prompt=system_prompt, model=candidate_model
                    ):
                        yield delta
                    return
            except Exception:
                continue

        # Fallback: no streaming-capable provider — emit fake chunks from full text
        text = await self.generate_text(prompt=prompt, system_prompt=system_prompt, model=model)
        for chunk in self._chunk_text(text):
            await asyncio.sleep(0)
            yield chunk

    async def stream_events(
        self,
        *,
        prompt: str,
        system_prompt: str | None = None,
        model: str | None = None,
    ) -> AsyncIterator[dict[str, str]]:
        """Yield structured stream events so hidden reasoning can be rendered
        separately from visible answer chunks."""
        provider, actual_model = await provider_config_service.resolve_chat_target(preferred_model=model)
        attempts: list[tuple[str, str]] = []
        if provider:
            attempts.append((provider, actual_model))
        if provider != "openai" and self._settings.openai_api_key:
            attempts.append(("openai", self._settings.openai_chat_model))
        if provider != "gemini" and self._settings.gemini_api_key:
            attempts.append(("gemini", self._settings.gemini_chat_model))

        for candidate_provider, candidate_model in attempts:
            try:
                if candidate_provider == "openai" and self._settings.openai_api_key:
                    async for event in self._stream_openai_events(
                        prompt=prompt, system_prompt=system_prompt, model=candidate_model
                    ):
                        yield event
                    return
                if candidate_provider == "gemini" and self._settings.gemini_api_key:
                    async for event in self._stream_gemini_events(
                        prompt=prompt, system_prompt=system_prompt, model=candidate_model
                    ):
                        yield event
                    return
            except Exception:
                continue

        text = await self.generate_text(prompt=prompt, system_prompt=system_prompt, model=model)
        for chunk in self._chunk_text(text):
            await asyncio.sleep(0)
            yield {"type": "message", "content": chunk}

    async def stream_and_emit(
        self,
        *,
        prompt: str,
        system_prompt: str | None = None,
        model: str | None = None,
    ) -> str:
        """Stream LLM output, emit each chunk as a ``message`` SSE event via
        the request-scoped ``current_emitter`` (if set), and return the full
        accumulated text. When no emitter is bound (e.g., blocking chat
        endpoint), falls back to non-streaming ``generate_text``.
        """
        emitter = current_emitter.get()
        if emitter is None:
            return await self.generate_text(prompt=prompt, system_prompt=system_prompt, model=model)

        full = []
        try:
            async for event in self.stream_events(prompt=prompt, system_prompt=system_prompt, model=model):
                event_type = str(event.get("type") or "message")
                delta = str(event.get("content") or "")
                if not delta:
                    continue
                if event_type == "thinking":
                    try:
                        await emitter.emit("thinking", {"content": delta})
                    except Exception:
                        pass
                    continue
                full.append(delta)
                try:
                    await self._emit_text_chunks(emitter, delta)
                except Exception:
                    pass
        except Exception:
            # If streaming blows up mid-way, fall back to non-streaming and
            # emit the remainder so the UI still gets a response.
            remainder = await self.generate_text(prompt=prompt, system_prompt=system_prompt, model=model)
            if remainder:
                full.append(remainder)
                try:
                    await self._emit_text_chunks(emitter, remainder)
                except Exception:
                    pass
        return "".join(full)

    async def _stream_openai_text(
        self, *, prompt: str, system_prompt: str | None, model: str
    ) -> AsyncIterator[str]:
        payload = {
            "model": model,
            "stream": True,
            "messages": [
                {"role": "system", "content": system_prompt or self._settings.system_prompt},
                {"role": "user", "content": prompt},
            ],
        }
        async with self._client.stream(
            "POST",
            f"{self._settings.openai_base_url}/chat/completions",
            headers={
                "Authorization": f"Bearer {self._settings.openai_api_key}",
                "Content-Type": "application/json",
                "Accept": "text/event-stream",
            },
            json=payload,
        ) as response:
            response.raise_for_status()
            async for raw_line in response.aiter_lines():
                if not raw_line:
                    continue
                line = raw_line.strip()
                if not line.startswith("data:"):
                    continue
                data_str = line[len("data:"):].strip()
                if data_str == "[DONE]":
                    break
                try:
                    event_obj = json.loads(data_str)
                except json.JSONDecodeError:
                    continue
                choices = event_obj.get("choices") or []
                if not choices:
                    continue
                delta_obj = choices[0].get("delta") or {}
                # Standard OpenAI-compatible streaming: delta.content
                # qwen/DBIZ thinking models may stream delta.reasoning_content first
                chunk = delta_obj.get("content") or delta_obj.get("reasoning_content") or ""
                if chunk:
                    yield chunk

    async def _stream_openai_events(
        self, *, prompt: str, system_prompt: str | None, model: str
    ) -> AsyncIterator[dict[str, str]]:
        payload = {
            "model": model,
            "stream": True,
            "messages": [
                {"role": "system", "content": system_prompt or self._settings.system_prompt},
                {"role": "user", "content": prompt},
            ],
        }
        async with self._client.stream(
            "POST",
            f"{self._settings.openai_base_url}/chat/completions",
            headers={
                "Authorization": f"Bearer {self._settings.openai_api_key}",
                "Content-Type": "application/json",
                "Accept": "text/event-stream",
            },
            json=payload,
        ) as response:
            response.raise_for_status()
            async for raw_line in response.aiter_lines():
                if not raw_line:
                    continue
                line = raw_line.strip()
                if not line.startswith("data:"):
                    continue
                data_str = line[len("data:"):].strip()
                if data_str == "[DONE]":
                    break
                try:
                    event_obj = json.loads(data_str)
                except json.JSONDecodeError:
                    continue
                choices = event_obj.get("choices") or []
                if not choices:
                    continue
                delta_obj = choices[0].get("delta") or {}
                reasoning = delta_obj.get("reasoning_content") or ""
                if reasoning:
                    yield {"type": "thinking", "content": str(reasoning)}
                chunk = delta_obj.get("content") or ""
                if chunk:
                    yield {"type": "message", "content": str(chunk)}

    async def _stream_gemini_text(
        self, *, prompt: str, system_prompt: str | None, model: str
    ) -> AsyncIterator[str]:
        payload = {
            "system_instruction": {"parts": [{"text": system_prompt or self._settings.system_prompt}]},
            "contents": [{"role": "user", "parts": [{"text": prompt}]}],
        }
        async with self._client.stream(
            "POST",
            f"{self._settings.gemini_base_url}/models/{model}:streamGenerateContent",
            params={"key": self._settings.gemini_api_key, "alt": "sse"},
            headers={"Content-Type": "application/json", "Accept": "text/event-stream"},
            json=payload,
        ) as response:
            response.raise_for_status()
            async for raw_line in response.aiter_lines():
                if not raw_line:
                    continue
                line = raw_line.strip()
                if not line.startswith("data:"):
                    continue
                data_str = line[len("data:"):].strip()
                if not data_str or data_str == "[DONE]":
                    continue
                try:
                    event_obj = json.loads(data_str)
                except json.JSONDecodeError:
                    continue
                for candidate in event_obj.get("candidates") or []:
                    parts = ((candidate.get("content") or {}).get("parts")) or []
                    for part in parts:
                        chunk = part.get("text") or ""
                        if chunk:
                            yield chunk

    async def _stream_gemini_events(
        self, *, prompt: str, system_prompt: str | None, model: str
    ) -> AsyncIterator[dict[str, str]]:
        payload = {
            "system_instruction": {"parts": [{"text": system_prompt or self._settings.system_prompt}]},
            "contents": [{"role": "user", "parts": [{"text": prompt}]}],
        }
        async with self._client.stream(
            "POST",
            f"{self._settings.gemini_base_url}/models/{model}:streamGenerateContent",
            params={"key": self._settings.gemini_api_key, "alt": "sse"},
            headers={"Content-Type": "application/json", "Accept": "text/event-stream"},
            json=payload,
        ) as response:
            response.raise_for_status()
            async for raw_line in response.aiter_lines():
                if not raw_line:
                    continue
                line = raw_line.strip()
                if not line.startswith("data:"):
                    continue
                data_str = line[len("data:"):].strip()
                if not data_str or data_str == "[DONE]":
                    continue
                try:
                    event_obj = json.loads(data_str)
                except json.JSONDecodeError:
                    continue
                for candidate in event_obj.get("candidates") or []:
                    parts = ((candidate.get("content") or {}).get("parts")) or []
                    for part in parts:
                        chunk = part.get("text") or ""
                        if chunk:
                            yield {"type": "message", "content": str(chunk)}

    async def generate_structured_json(
        self,
        *,
        prompt: str,
        fallback_payload: dict[str, Any],
        model: str | None = None,
    ) -> dict[str, Any]:
        raw = await self.generate_text(
            prompt=f"{prompt}\n\nReturn valid JSON only.",
            system_prompt="You are a JSON-only assistant.",
            model=model,
        )
        parsed = self._extract_json(raw)
        return parsed if parsed is not None else fallback_payload

    async def _generate_openai_text(self, *, prompt: str, system_prompt: str | None, model: str) -> tuple[str, dict[str, int] | None]:
        response = await self._client.post(
            f"{self._settings.openai_base_url}/chat/completions",
            headers={
                "Authorization": f"Bearer {self._settings.openai_api_key}",
                "Content-Type": "application/json",
            },
            json={
                "model": model,
                "messages": [
                    {"role": "system", "content": system_prompt or self._settings.system_prompt},
                    {"role": "user", "content": prompt},
                ],
            },
        )
        response.raise_for_status()
        payload = response.json()
        message = payload["choices"][0]["message"]
        # qwen-35b (DBIZ) may return content=null with reasoning_content when thinking
        text = message.get("content") or message.get("reasoning_content") or ""
        return text, self._extract_openai_usage(payload)

    async def _generate_gemini_text(self, *, prompt: str, system_prompt: str | None, model: str) -> tuple[str, dict[str, int] | None]:
        response = await self._client.post(
            f"{self._settings.gemini_base_url}/models/{model}:generateContent",
            params={"key": self._settings.gemini_api_key},
            json={
                "system_instruction": {"parts": [{"text": system_prompt or self._settings.system_prompt}]},
                "contents": [{"role": "user", "parts": [{"text": prompt}]}],
            },
        )
        response.raise_for_status()
        payload = response.json()
        return payload["candidates"][0]["content"]["parts"][0]["text"], self._extract_gemini_usage(payload)

    async def _generate_openai_embeddings(self, texts: list[str]) -> tuple[list[list[float]], dict[str, int] | None]:
        body = {
            "model": self._settings.openai_embedding_model,
            "input": texts,
        }
        if self._settings.embedding_dimension > 0:
            body["dimensions"] = self._settings.embedding_dimension
        response = await self._client.post(
            f"{self._settings.openai_base_url}/embeddings",
            headers={
                "Authorization": f"Bearer {self._settings.openai_api_key}",
                "Content-Type": "application/json",
            },
            json=body,
        )
        if response.status_code >= 400 and "dimensions" in body:
            body.pop("dimensions", None)
            response = await self._client.post(
                f"{self._settings.openai_base_url}/embeddings",
                headers={
                    "Authorization": f"Bearer {self._settings.openai_api_key}",
                    "Content-Type": "application/json",
                },
                json=body,
            )
        response.raise_for_status()
        payload = response.json()
        return [item["embedding"] for item in payload.get("data", [])], self._extract_openai_usage(payload)

    async def _generate_gemini_embeddings(
        self,
        texts: list[str],
        *,
        model: str,
        task_type: str | None = None,
    ) -> tuple[list[list[float]], dict[str, int] | None]:
        payload = {
            "requests": [
                {
                    "model": f"models/{model}",
                    "content": {"parts": [{"text": text}]},
                    "taskType": task_type or "SEMANTIC_SIMILARITY",
                    "outputDimensionality": self._settings.embedding_dimension,
                }
                for text in texts
            ]
        }
        response = await self._client.post(
            f"{self._settings.gemini_base_url}/models/{model}:batchEmbedContents",
            params={"key": self._settings.gemini_api_key},
            json=payload,
        )
        if response.status_code >= 400:
            payload = {
                "requests": [
                    {
                        "model": f"models/{model}",
                        "content": {"parts": [{"text": text}]},
                        "taskType": task_type or "SEMANTIC_SIMILARITY",
                    }
                    for text in texts
                ]
            }
            response = await self._client.post(
                f"{self._settings.gemini_base_url}/models/{model}:batchEmbedContents",
                params={"key": self._settings.gemini_api_key},
                json=payload,
            )
        response.raise_for_status()
        payload = response.json()
        embeddings = []
        for item in payload.get("embeddings", []):
            values = (item.get("values") or item.get("embedding", {}).get("values") or [])
            if values:
                embeddings.append(values)
        if embeddings:
            return embeddings, self._extract_gemini_usage(payload)

        fallback_embeddings = []
        for text in texts:
            response = await self._client.post(
                f"{self._settings.gemini_base_url}/models/{model}:embedContent",
                params={"key": self._settings.gemini_api_key},
                json={
                    "model": f"models/{model}",
                    "content": {"parts": [{"text": text}]},
                    "taskType": task_type or "SEMANTIC_SIMILARITY",
                },
            )
            response.raise_for_status()
            payload = response.json()
            values = payload.get("embedding", {}).get("values") or []
            if values:
                fallback_embeddings.append(values)
        return fallback_embeddings, None

    def _mock_text(self, prompt: str, model: str) -> str:
        lowered = prompt.lower()
        if "json only" in lowered:
            return json.dumps({"message": "Mock JSON response", "model": model})
        if "recommend" in lowered or "goi y" in lowered:
            return (
                "Toi de xuat bat dau tu khoa co ban, sau do nang cap dan theo muc tieu hoc tap. "
                "Neu ban dang chuyen tu JavaScript sang Python, hay uu tien khoa Python co ban va bai tap thuc hanh."
            )
        return (
            "Day la phan hoi mo phong cua AI gateway FastAPI. "
            "Service van giu nguyen contract de frontend va proxy-client tiep tuc hoat dong."
        )

    @staticmethod
    def _deterministic_embedding(text: str, size: int = 128) -> list[float]:
        if not text:
            return [0.0] * size
        digest = hashlib.sha256(text.encode("utf-8")).digest()
        vector = [0.0] * size
        for index, byte in enumerate(text.encode("utf-8")):
            vector[index % size] += (byte / 255.0) * 0.7
        for index, byte in enumerate(digest):
            vector[index % size] += (byte / 255.0) * 0.3
        norm = sum(value * value for value in vector) ** 0.5 or 1.0
        return [value / norm for value in vector]

    @staticmethod
    def _extract_json(text: str) -> dict[str, Any] | None:
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            match = re.search(r"\{.*\}", text, flags=re.S)
            if not match:
                return None
            try:
                return json.loads(match.group(0))
            except json.JSONDecodeError:
                return None

    @staticmethod
    def _chunk_text(text: str, chunk_size: int = 48) -> list[str]:
        return [text[i : i + chunk_size] for i in range(0, len(text), chunk_size)] or [text]

    async def _emit_text_chunks(self, emitter, text: str) -> None:
        chunk_size = max(1, int(self._settings.stream_emit_chunk_size or 1))
        delay_ms = max(0, int(self._settings.stream_emit_delay_ms or 0))
        delay_seconds = delay_ms / 1000
        for chunk in self._chunk_text(text, chunk_size=chunk_size):
            await emitter.emit("message", text_chunk_event(chunk))
            if delay_seconds > 0:
                await asyncio.sleep(delay_seconds)

    async def _record_chat_usage(
        self,
        *,
        prompt: str,
        response: str,
        provider: str,
        model: str,
        usage: dict[str, int] | None,
    ) -> None:
        prompt_tokens = usage.get("prompt_tokens", 0) if usage else self._estimate_token_count(prompt)
        completion_tokens = usage.get("completion_tokens", 0) if usage else self._estimate_token_count(response)
        runtime_request_context_service.add_chat_usage(
            prompt_tokens=prompt_tokens,
            completion_tokens=completion_tokens,
        )
        await runtime_observability_service.record_token_usage(
            request_id=runtime_request_context_service.current_request_id(),
            scope=runtime_request_context_service.current().scope if runtime_request_context_service.current() else "unknown",
            provider=provider or "mock",
            model=model,
            prompt_tokens=prompt_tokens,
            completion_tokens=completion_tokens,
            embedding_tokens=0,
        )

    async def _record_embedding_usage(
        self,
        *,
        texts: list[str],
        provider: str,
        model: str,
        usage: dict[str, int] | None,
    ) -> None:
        embedding_tokens = usage.get("total_tokens", 0) if usage else sum(self._estimate_token_count(text) for text in texts)
        runtime_request_context_service.add_embedding_usage(tokens=embedding_tokens)
        await runtime_observability_service.record_token_usage(
            request_id=runtime_request_context_service.current_request_id(),
            scope=runtime_request_context_service.current().scope if runtime_request_context_service.current() else "embedding",
            provider=provider or "mock",
            model=model,
            prompt_tokens=0,
            completion_tokens=0,
            embedding_tokens=embedding_tokens,
        )

    def _estimate_token_count(self, text: str) -> int:
        chars_per_token = max(self._settings.token_estimation_chars_per_token, 1.0)
        return max(1, round(len(text or "") / chars_per_token))

    @staticmethod
    def _extract_openai_usage(payload: dict[str, Any]) -> dict[str, int] | None:
        usage = payload.get("usage")
        if not isinstance(usage, dict):
            return None
        return {
            "prompt_tokens": int(usage.get("prompt_tokens") or 0),
            "completion_tokens": int(usage.get("completion_tokens") or 0),
            "total_tokens": int(usage.get("total_tokens") or 0),
        }

    @staticmethod
    def _extract_gemini_usage(payload: dict[str, Any]) -> dict[str, int] | None:
        usage = payload.get("usageMetadata")
        if not isinstance(usage, dict):
            return None
        prompt_tokens = int(usage.get("promptTokenCount") or 0)
        completion_tokens = int(usage.get("candidatesTokenCount") or usage.get("responseTokenCount") or 0)
        total_tokens = int(usage.get("totalTokenCount") or prompt_tokens + completion_tokens)
        return {
            "prompt_tokens": prompt_tokens,
            "completion_tokens": completion_tokens,
            "total_tokens": total_tokens,
        }


switchable_ai_gateway = SwitchableAiGateway()
