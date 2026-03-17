# TechHub Deployment Runbook (SQL + BE + FE)

## 1) Prerequisites

- External Docker network exists: `techhub`
- PostgreSQL is reachable and has target databases
- Kafka, Redis, Qdrant reachable from containers
- Portainer stack env has all variables in `.env`

## 2) Deploy SQL first (required)

> First-time deploy = **init DB schema + seed**, KHÔNG chạy migrate/alter.

### 2.1 User-service DB (`techhub`) — first-time

Run:

- `techhub.sql`

Purpose:

- Create `security_level` enum
- Create `endpoint_security_policies` table
- Seed PUBLIC/AUTHENTICATED endpoint rules

### 2.2 File-service DB (`techhub_file`) — first-time

Run:

- `file-service/schema.sql`

### 2.3 Only if upgrading existing DB (đã có dữ liệu cũ)

Then run migration scripts (including `endpoint_security_policies.sql` if needed) as documented in:

- `MIGRATION_NOTES.md`

### 2.4 Recommended lifecycle

- New environment / new partner onboarding: run only `techhub.sql` for DB `techhub`.
- Ongoing development: keep `techhub.sql` as baseline, apply migration SQL files only for new changes after baseline.

## 3) Deploy Backend stack in Portainer

Use:

- `portainer-stack.yml`

Important:

- Stack now maps to external network `techhub`
- `ai-service` env now includes:
  - `AI_PROVIDER`
  - `GEMINI_BASE_URL`, `GEMINI_API_KEY`, `GEMINI_CHAT_MODEL`, `GEMINI_CHAT_MAX_OUTPUT_TOKENS`
  - `CHATBOT_OPENAI_EMBEDDING_MODEL`, `CHATBOT_GEMINI_EMBEDDING_MODEL`

Recommended initial AI env:

- `AI_PROVIDER=gemini`
- `GEMINI_CHAT_MODEL=gemini-2.5-flash`
- `CHATBOT_OPENAI_EMBEDDING_MODEL=text-embedding-3-small`
- `CHATBOT_GEMINI_EMBEDDING_MODEL=text-embedding-004`

## 4) Deploy Frontend

### Option A — Local Docker Compose build

Use file:

- `../TechHub_FE/compose.yml`

Required env:

- `NEXT_PUBLIC_API_ENDPOINT`
- `NEXT_PUBLIC_URL`
- `NEXT_PUBLIC_WS_BASE`

### Option B — Portainer stack (image-based)

Use file:

- `../TechHub_FE/portainer-stack.yml`

Set env in Portainer:

- `TECHHUB_FE_IMAGE` (optional, default `toandeptroai/techhub-fe:0.1.0`)
- `NEXT_PUBLIC_API_ENDPOINT`
- `NEXT_PUBLIC_URL`
- `NEXT_PUBLIC_WS_BASE`

## 5) Quick verification checklist

- FE opens at `http://<host>:3000`
- API proxy works from FE to BE (login + basic fetch)
- MinIO health OK and bucket exists (`techhub`)
- File upload returns MinIO-backed URLs
- AI dashboard can:
  - switch provider
  - switch model
- Endpoint security table contains seed data and proxy auth works

## 6) Common pitfalls

- Network mismatch: stack not attached to external `techhub`
- SQL not applied before starting services
- `NEXT_PUBLIC_API_ENDPOINT` incorrect (FE cannot call BE)
- Missing MinIO bucket policy (public GET)
- Missing `GEMINI_API_KEY` while `AI_PROVIDER=gemini`
