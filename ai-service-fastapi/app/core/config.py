from __future__ import annotations

import os
from dataclasses import dataclass, field
from functools import lru_cache

from dotenv import load_dotenv

load_dotenv()


def _bool_env(name: str, default: bool) -> bool:
    raw = os.getenv(name)
    if raw is None:
        return default
    return raw.strip().lower() in {"1", "true", "yes", "on"}


def _int_env(name: str, default: int) -> int:
    raw = os.getenv(name)
    if raw is None or raw.strip() == "":
        return default
    try:
        return int(raw)
    except ValueError:
        return default


def _float_env(name: str, default: float) -> float:
    raw = os.getenv(name)
    if raw is None or raw.strip() == "":
        return default
    try:
        return float(raw)
    except ValueError:
        return default


def _jdbc_to_asyncpg(jdbc_url: str) -> str:
    if jdbc_url.startswith("postgresql+asyncpg://"):
        return jdbc_url
    if jdbc_url.startswith("jdbc:postgresql://"):
        return "postgresql+asyncpg://" + jdbc_url.removeprefix("jdbc:postgresql://")
    if jdbc_url.startswith("postgresql://"):
        return "postgresql+asyncpg://" + jdbc_url.removeprefix("postgresql://")
    return jdbc_url


@dataclass(slots=True)
class Settings:
    app_name: str = "AI-SERVICE"
    app_version: str = "0.1.0"
    host: str = field(default_factory=lambda: os.getenv("AI_SERVICE_HOST", "0.0.0.0"))
    port: int = field(default_factory=lambda: _int_env("AI_SERVICE_PORT", 8091))
    environment: str = field(default_factory=lambda: os.getenv("SPRING_PROFILES_ACTIVE", "dev"))
    log_level: str = field(default_factory=lambda: os.getenv("AI_LOG_LEVEL", "INFO").upper())

    database_url: str = field(
        default_factory=lambda: _jdbc_to_asyncpg(
            os.getenv("SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/techhub_db")
        )
    )
    database_username: str = field(default_factory=lambda: os.getenv("SPRING_DATASOURCE_USERNAME", "postgres"))
    database_password: str = field(default_factory=lambda: os.getenv("SPRING_DATASOURCE_PASSWORD", "postgres"))
    database_echo: bool = field(default_factory=lambda: _bool_env("AI_DB_ECHO", False))
    database_auto_create: bool = field(default_factory=lambda: _bool_env("AI_DB_AUTO_CREATE", False))
    database_pool_size: int = field(default_factory=lambda: _int_env("AI_DB_POOL_SIZE", 10))
    database_max_overflow: int = field(default_factory=lambda: _int_env("AI_DB_MAX_OVERFLOW", 20))
    database_pool_recycle_seconds: int = field(
        default_factory=lambda: _int_env("AI_DB_POOL_RECYCLE_SECONDS", 1800)
    )
    database_pool_timeout_seconds: int = field(
        default_factory=lambda: _int_env("AI_DB_POOL_TIMEOUT_SECONDS", 30)
    )
    database_statement_timeout_ms: int = field(
        default_factory=lambda: _int_env("AI_DB_STATEMENT_TIMEOUT_MS", 10000)
    )

    redis_host: str = field(default_factory=lambda: os.getenv("REDIS_HOST", "localhost"))
    redis_port: int = field(default_factory=lambda: _int_env("REDIS_PORT", 6379))
    redis_password: str | None = field(default_factory=lambda: os.getenv("REDIS_PASSWORD"))
    redis_db: int = field(default_factory=lambda: _int_env("REDIS_DB", 0))
    redis_ttl_seconds: int = field(default_factory=lambda: _int_env("AI_REDIS_TTL_SECONDS", 3600))

    eureka_url: str = field(default_factory=lambda: os.getenv("EUREKA_SERVER_URL", "http://localhost:8761/eureka/"))
    eureka_hostname: str = field(default_factory=lambda: os.getenv("EUREKA_INSTANCE_HOSTNAME", "localhost"))
    eureka_enabled: bool = field(default_factory=lambda: _bool_env("AI_EUREKA_ENABLED", True))

    openai_base_url: str = field(default_factory=lambda: os.getenv("OPENAI_BASE_URL", "https://ai-api.dbiz.com/v1"))
    openai_api_key: str | None = field(default_factory=lambda: os.getenv("OPENAI_API_KEY"))
    openai_chat_model: str = field(default_factory=lambda: os.getenv("OPENAI_CHAT_MODEL", "qwen-35b"))
    openai_embedding_model: str = field(
        default_factory=lambda: os.getenv("OPENAI_EMBEDDING_MODEL", "bge-m3")
    )

    gemini_base_url: str = field(
        default_factory=lambda: os.getenv("GEMINI_BASE_URL", "https://generativelanguage.googleapis.com/v1beta")
    )
    gemini_api_key: str | None = field(default_factory=lambda: os.getenv("GEMINI_API_KEY"))
    gemini_chat_model: str = field(default_factory=lambda: os.getenv("GEMINI_CHAT_MODEL", "gemini-2.5-flash"))
    gemini_embedding_model: str = field(
        default_factory=lambda: os.getenv("GEMINI_EMBEDDING_MODEL", "gemini-embedding-001")
    )

    ai_provider: str = field(default_factory=lambda: os.getenv("AI_PROVIDER", "openai").lower())
    orchestration_enabled: bool = field(default_factory=lambda: _bool_env("AI_ORCHESTRATION_V2_ENABLED", True))
    legacy_fallback_enabled: bool = field(default_factory=lambda: _bool_env("AI_LEGACY_FALLBACK_ENABLED", True))
    business_safe_mode_enabled: bool = field(default_factory=lambda: _bool_env("AI_BUSINESS_SAFE_MODE_ENABLED", False))
    intent_semantic_threshold: float = field(default_factory=lambda: _float_env("AI_INTENT_SEMANTIC_THRESHOLD", 0.6))
    learning_path_vector_score_threshold: float = field(
        default_factory=lambda: _float_env("AI_LEARNING_PATH_VECTOR_SCORE_THRESHOLD", 0.55)
    )
    learning_path_require_topic_overlap: bool = field(
        default_factory=lambda: _bool_env("AI_LEARNING_PATH_REQUIRE_TOPIC_OVERLAP", True)
    )
    learning_path_limit_to_user_courses: bool = field(
        default_factory=lambda: _bool_env("AI_LEARNING_PATH_LIMIT_TO_USER_COURSES", False)
    )
    hitl_enabled: bool = field(default_factory=lambda: _bool_env("AI_HITL_ENABLED", True))
    hitl_confidence_threshold: float = field(default_factory=lambda: _float_env("AI_HITL_CONFIDENCE_THRESHOLD", 0.55))
    hitl_risk_threshold: float = field(default_factory=lambda: _float_env("AI_HITL_RISK_THRESHOLD", 0.5))
    hitl_review_data_queries: bool = field(default_factory=lambda: _bool_env("AI_HITL_REVIEW_DATA_QUERIES", True))
    hitl_review_visualizations: bool = field(default_factory=lambda: _bool_env("AI_HITL_REVIEW_VISUALIZATIONS", True))
    embedding_dimension: int = field(default_factory=lambda: _int_env("AI_EMBEDDING_DIMENSION", 1024))
    system_prompt: str = field(
        default_factory=lambda: os.getenv(
            "AI_SYSTEM_PROMPT",
            (
                "Ban la TechHub AI, tro ly hoc tap va phan tich du lieu cua nen tang TechHub. "
                "Khong tu gioi thieu la mo hinh cua Google, OpenAI, Gemini hay bat ky nha cung cap ha tang nao. "
                "Neu nguoi dung hoi 'ban la ai', hay tra loi ban la TechHub AI. "
                "Neu nguoi dung hoi 'toi la ai', 'ten toi la gi' hoac 'ban goi toi la gi', "
                "hay dung thong tin trong saved user preferences/profile context neu co. "
                "Tra loi bang tieng Viet, ro rang, ngan gon, khong hallucinate ve khoa hoc khong ton tai trong du lieu."
            ),
        )
    )

    qdrant_host: str = field(default_factory=lambda: os.getenv("QDRANT_HOST", "http://localhost:6333"))
    qdrant_api_key: str | None = field(default_factory=lambda: os.getenv("QDRANT_API_KEY"))
    qdrant_course_collection: str = field(
        default_factory=lambda: os.getenv("QDRANT_RECOMMENDATION_COLLECTION", "course_embeddings")
    )
    qdrant_lesson_collection: str = field(
        default_factory=lambda: os.getenv("QDRANT_LESSON_COLLECTION", "lesson_embeddings")
    )
    qdrant_blog_collection: str = field(
        default_factory=lambda: os.getenv("QDRANT_BLOG_COLLECTION", "blog_embeddings")
    )
    qdrant_data_contract_collection: str = field(
        default_factory=lambda: os.getenv("QDRANT_DATA_CONTRACT_COLLECTION", "data_contract_embeddings")
    )
    qdrant_profile_collection: str = field(
        default_factory=lambda: os.getenv("QDRANT_PROFILE_COLLECTION", "user_embeddings")
    )
    qdrant_session_file_collection: str = field(
        default_factory=lambda: os.getenv("QDRANT_SESSION_FILE_COLLECTION", "session_file_embeddings")
    )
    qdrant_user_file_collection: str = field(
        default_factory=lambda: os.getenv("QDRANT_USER_FILE_COLLECTION", "user_file_embeddings")
    )
    file_service_base_url: str | None = field(default_factory=lambda: os.getenv("FILE_SERVICE_BASE_URL"))
    learning_path_service_base_url: str | None = field(
        default_factory=lambda: os.getenv("LEARNING_PATH_SERVICE_BASE_URL")
    )
    learning_path_service_timeout_seconds: int = field(
        default_factory=lambda: _int_env("AI_LEARNING_PATH_SERVICE_TIMEOUT_SECONDS", 20)
    )
    course_service_base_url: str | None = field(
        default_factory=lambda: os.getenv("COURSE_SERVICE_BASE_URL")
    )
    course_service_timeout_seconds: int = field(
        default_factory=lambda: _int_env("AI_COURSE_SERVICE_TIMEOUT_SECONDS", 20)
    )
    file_download_timeout_seconds: int = field(default_factory=lambda: _int_env("AI_FILE_DOWNLOAD_TIMEOUT_SECONDS", 15))
    file_max_download_bytes: int = field(default_factory=lambda: _int_env("AI_FILE_MAX_DOWNLOAD_BYTES", 4000000))
    file_max_chars: int = field(default_factory=lambda: _int_env("AI_FILE_MAX_CHARS", 20000))
    file_excerpt_chars: int = field(default_factory=lambda: _int_env("AI_FILE_EXCERPT_CHARS", 2500))
    file_chunk_size: int = field(default_factory=lambda: _int_env("AI_FILE_CHUNK_SIZE", 1200))
    file_chunk_overlap: int = field(default_factory=lambda: _int_env("AI_FILE_CHUNK_OVERLAP", 150))
    stream_emit_chunk_size: int = field(default_factory=lambda: _int_env("AI_STREAM_EMIT_CHUNK_SIZE", 1))
    stream_emit_delay_ms: int = field(default_factory=lambda: _int_env("AI_STREAM_EMIT_DELAY_MS", 8))
    runtime_metrics_window: int = field(default_factory=lambda: _int_env("AI_RUNTIME_METRICS_WINDOW", 200))
    runtime_recent_request_window: int = field(default_factory=lambda: _int_env("AI_RUNTIME_RECENT_REQUEST_WINDOW", 100))
    data_contract_version: str = field(default_factory=lambda: os.getenv("AI_DATA_CONTRACT_VERSION", "techhub_v1"))
    data_contract_cache_ttl_seconds: int = field(
        default_factory=lambda: _int_env("AI_DATA_CONTRACT_CACHE_TTL_SECONDS", 60)
    )
    token_estimation_chars_per_token: float = field(
        default_factory=lambda: _float_env("AI_TOKEN_ESTIMATION_CHARS_PER_TOKEN", 3.8)
    )
    sql_default_limit: int = field(default_factory=lambda: _int_env("AI_SQL_DEFAULT_LIMIT", 100))
    sql_max_limit: int = field(default_factory=lambda: _int_env("AI_SQL_MAX_LIMIT", 250))
    allow_user_model_override: bool = field(default_factory=lambda: _bool_env("AI_ALLOW_USER_MODEL_OVERRIDE", False))
    allow_admin_model_override: bool = field(default_factory=lambda: _bool_env("AI_ALLOW_ADMIN_MODEL_OVERRIDE", True))
    kafka_enabled: bool = field(default_factory=lambda: _bool_env("AI_KAFKA_ENABLED", False))
    kafka_bootstrap_servers: str = field(default_factory=lambda: os.getenv("AI_KAFKA_BOOTSTRAP_SERVERS", ""))
    kafka_group_id: str = field(default_factory=lambda: os.getenv("AI_KAFKA_GROUP_ID", "ai-service-indexer"))
    ocr_enabled: bool = field(default_factory=lambda: _bool_env("AI_OCR_ENABLED", False))
    ocr_timeout_seconds: int = field(default_factory=lambda: _int_env("AI_OCR_TIMEOUT_SECONDS", 30))

    langfuse_enabled: bool = field(default_factory=lambda: _bool_env("LANGFUSE_ENABLED", False))
    langfuse_public_key: str = field(default_factory=lambda: os.getenv("LANGFUSE_PUBLIC_KEY", ""))
    langfuse_secret_key: str = field(default_factory=lambda: os.getenv("LANGFUSE_SECRET_KEY", ""))
    langfuse_host: str = field(default_factory=lambda: os.getenv("LANGFUSE_BASE_URL", "https://cloud.langfuse.com"))

    kafka_topics_csv: str = field(
        default_factory=lambda: os.getenv(
            "AI_KAFKA_TOPICS",
            "course-events,lesson-events,enrollment-events,rating-events,learning-path-events,file-uploaded",
        )
    )

    supported_chat_models: dict[str, list[str]] = field(
        default_factory=lambda: {
            "openai": ["qwen-35b", "gpt-4o-mini", "gpt-4.1-mini", "gpt-4.1"],
            "gemini": ["gemini-2.5-flash-lite-preview-06-17", "gemini-2.5-flash", "gemini-2.5-pro"],
        }
    )

    def active_chat_model(self, provider: str | None = None) -> str:
        actual_provider = (provider or self.ai_provider).lower()
        if actual_provider == "openai":
            return self.openai_chat_model
        return self.gemini_chat_model

    def kafka_topics(self) -> list[str]:
        return [topic.strip() for topic in self.kafka_topics_csv.split(",") if topic.strip()]


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
