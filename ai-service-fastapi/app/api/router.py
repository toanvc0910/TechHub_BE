from __future__ import annotations

from fastapi import APIRouter

from app.api.routes import admin, chat, drafts, exercises, learning_paths, recommendations, system

api_router = APIRouter()
api_router.include_router(system.router, tags=["system"])
api_router.include_router(chat.router, prefix="/api/ai/chat", tags=["chat"])
api_router.include_router(admin.router, prefix="/api/ai/admin", tags=["admin"])
api_router.include_router(exercises.router, prefix="/api/ai/exercises", tags=["exercises"])
api_router.include_router(learning_paths.router, prefix="/api/ai/learning-paths", tags=["learning-paths"])
api_router.include_router(recommendations.router, prefix="/api/ai/recommendations", tags=["recommendations"])
api_router.include_router(drafts.router, prefix="/api/ai/drafts", tags=["drafts"])
