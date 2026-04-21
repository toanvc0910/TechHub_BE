from __future__ import annotations

import asyncio
import json
import logging
from typing import Any

from app.core.config import get_settings
from app.services.file_context_service import file_context_service
from app.services.observability_service import runtime_observability_service
from app.services.vector_service import vector_service

logger = logging.getLogger(__name__)

# Java topic names → handler mapping
# Producers: CourseEventPublisher (common-service), FileEventPublisher (file-service)
# Payloads: CourseEventPayload, LessonEventPayload, EnrollmentEventPayload, FileUploadedEvent


class IndexingEventConsumer:
    def __init__(self) -> None:
        self._settings = get_settings()
        self._consumer: Any | None = None
        self._task: asyncio.Task[None] | None = None

    async def start(self) -> None:
        if not self._settings.kafka_enabled or not self._settings.kafka_bootstrap_servers.strip():
            logger.info("Kafka indexing consumer disabled by configuration.")
            return
        try:
            from aiokafka import AIOKafkaConsumer
        except Exception as exc:
            logger.warning("Kafka consumer unavailable: %s", exc)
            return

        if self._consumer is not None:
            return
        consumer = AIOKafkaConsumer(
            *self._settings.kafka_topics(),
            bootstrap_servers=self._settings.kafka_bootstrap_servers,
            group_id=self._settings.kafka_group_id,
            enable_auto_commit=True,
            auto_offset_reset="latest",
            value_deserializer=lambda value: value.decode("utf-8"),
        )
        await consumer.start()
        self._consumer = consumer
        self._task = asyncio.create_task(self._consume_loop())
        logger.info(
            "Started Kafka indexing consumer for topics: %s",
            ", ".join(self._settings.kafka_topics()),
        )

    async def stop(self) -> None:
        if self._task is not None:
            self._task.cancel()
            try:
                await self._task
            except asyncio.CancelledError:
                pass
            self._task = None
        if self._consumer is not None:
            await self._consumer.stop()
            self._consumer = None

    async def _consume_loop(self) -> None:
        if self._consumer is None:
            return
        try:
            async for message in self._consumer:
                await self._handle_message(message.topic, message.value)
        except asyncio.CancelledError:
            raise
        except Exception as exc:
            logger.exception("Kafka indexing consumer stopped unexpectedly: %s", exc)
            await runtime_observability_service.record_error(scope="kafka_consumer")

    async def _handle_message(self, topic: str, raw_value: str) -> None:
        try:
            payload = json.loads(raw_value) if raw_value else {}
            if not isinstance(payload, dict):
                payload = {"value": payload}
        except json.JSONDecodeError:
            payload = {"raw": raw_value}

        event_type = str(payload.get("eventType") or payload.get("type") or "").upper()

        try:
            # ─── Topic: course-events ───
            # Java payload: CourseEventPayload {eventType, courseId, title, ...}
            if topic == "course-events":
                course_id = payload.get("courseId") or payload.get("course_id")
                if event_type == "DELETED":
                    logger.info("Course DELETED event for %s — skipping reindex.", course_id)
                    return
                if course_id:
                    await vector_service.reindex_single_course(str(course_id))
                    logger.info("Reindexed course %s (event: %s)", course_id, event_type)
                else:
                    await vector_service.reindex_courses()
                    logger.info("Full course reindex (no courseId in event).")
                return

            # ─── Topic: lesson-events ───
            # Java payload: LessonEventPayload {eventType, lessonId, courseId, ...}
            if topic == "lesson-events":
                lesson_id = payload.get("lessonId") or payload.get("lesson_id")
                if event_type == "DELETED":
                    logger.info("Lesson DELETED event for %s — skipping reindex.", lesson_id)
                    return
                if lesson_id:
                    await vector_service.reindex_single_lesson(str(lesson_id))
                    logger.info("Reindexed lesson %s (event: %s)", lesson_id, event_type)
                else:
                    await vector_service.reindex_lessons()
                    logger.info("Full lesson reindex (no lessonId in event).")
                return

            # ─── Topic: enrollment-events ───
            # Java payload: EnrollmentEventPayload {eventType, enrollmentId, userId, courseId, status, ...}
            # eventType: ENROLLED, PROGRESS_UPDATED, COMPLETED, DROPPED
            if topic == "enrollment-events":
                user_id = payload.get("userId") or payload.get("user_id")
                course_id = payload.get("courseId") or payload.get("course_id")
                logger.info(
                    "Enrollment event: %s user=%s course=%s status=%s",
                    event_type, user_id, course_id, payload.get("status"),
                )
                # Enrollment/progress changes affect user profile embeddings
                await vector_service.reindex_all()
                return

            # ─── Topic: rating-events ───
            # Java payload: RatingEventPayload {eventType, ratingId, userId, courseId, score}
            if topic == "rating-events":
                user_id = payload.get("userId") or payload.get("user_id")
                course_id = payload.get("courseId") or payload.get("course_id")
                score = payload.get("score")
                logger.info(
                    "Rating event: %s user=%s course=%s score=%s",
                    event_type, user_id, course_id, score,
                )
                # Ratings affect user profile embeddings (skill_profile recalculation)
                await vector_service.reindex_all()
                return

            # ─── Topic: learning-path-events ───
            # Java payload: LearningPathEventPayload {eventType, pathId, title, courseCount}
            if topic == "learning-path-events":
                path_id = payload.get("pathId") or payload.get("path_id")
                logger.info(
                    "Learning path event: %s path=%s title=%s",
                    event_type, path_id, payload.get("title"),
                )
                # Path changes affect course ordering and recommendations
                await vector_service.reindex_courses()
                return

            # ─── Topic: file-uploaded ───
            if topic == "file-uploaded":
                await file_context_service.ingest_uploaded_event(payload)
                logger.info("Processed file-uploaded event: %s", payload.get("fileName") or payload.get("name"))
                return

            # ─── Fallback: unknown topic ───
            logger.info("Ignored Kafka event on topic '%s': eventType=%s", topic, event_type)

        except Exception as exc:
            logger.exception("Failed to handle Kafka event on topic '%s': %s", topic, exc)
            await runtime_observability_service.record_error(scope="kafka_event_handler")


indexing_event_consumer = IndexingEventConsumer()
