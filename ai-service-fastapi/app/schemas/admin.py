from __future__ import annotations

from typing import Any

from pydantic import BaseModel


class ReindexStats(BaseModel):
    indexed: int
    failed: int
    duration: str | None = None


class ReindexResponse(BaseModel):
    success: bool
    message: str
    stats: ReindexStats | None = None


class QdrantCollectionStats(BaseModel):
    vectorCount: int
    pointsCount: int
    status: str


class QdrantStatsResponse(BaseModel):
    collections: dict[str, QdrantCollectionStats]
    healthy: bool
    version: str | None = None


class ProviderConfigRequest(BaseModel):
    provider: str | None = None
    chatModel: str | None = None
    embeddingModel: str | None = None


class FileUploadedEventRequest(BaseModel):
    fileId: str | None = None
    userId: str | None = None
    bucketName: str | None = None
    objectKey: str | None = None
    fileType: str | None = None
    mimeType: str | None = None
    publicUrl: str | None = None
    secureUrl: str | None = None
    name: str | None = None
    originalName: str | None = None


class ProviderConfigResponse(BaseModel):
    provider: str
    activeChatModel: str
    activeEmbeddingModel: str | None = None
    models: dict[str, str]
    supportedProviders: list[str]
    supportedChatModels: dict[str, list[str]]
    metadata: dict[str, Any] | None = None
