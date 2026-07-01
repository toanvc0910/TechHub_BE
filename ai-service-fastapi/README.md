# TechHub AI Service FastAPI

FastAPI replacement for `AI-SERVICE`, keeping the existing `/api/ai/...` contract used by `proxy-client` and the frontend.

Highlights:

- Keeps service discovery name `AI-SERVICE`
- Preserves chat, admin, recommendation, draft, exercise, and learning-path endpoints
- Introduces a graph-based orchestration flow for chat
- Uses Redis as hot-path conversation memory with in-process fallback
- Supports OpenAI/Gemini provider switching with deterministic fallback responses for local development

Run locally:

```bash
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8091
```
