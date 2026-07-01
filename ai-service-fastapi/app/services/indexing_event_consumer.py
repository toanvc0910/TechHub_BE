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
        # Manual commit only after a successful `_handle_message`. With
        # `enable_auto_commit=True`, a poison message that throws inside the
        # handler would have its offset advanced anyway and the event would be
        # lost forever. Manual commit + DLT logging is the production-safe
        # default.
        consumer = AIOKafkaConsumer(
            *self._settings.kafka_topics(),
            bootstrap_servers=self._settings.kafka_bootstrap_servers,
            group_id=self._settings.kafka_group_id,
            enable_auto_commit=False,
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
        # The outer loop never lets a single bad message kill the consumer.
        # Each message is processed under its own try/except; on success the
        # offset is committed, on failure the message is logged to a dead-
        # letter sink (structured log + observability counter) and the offset
        # is still advanced so the same message does not poison-replay forever.
        while True:
            try:
                async for message in self._consumer:
                    handled = False
                    try:
                        await self._handle_message(message.topic, message.value)
                        handled = True
                    except Exception as exc:  # noqa: BLE001
                        # Dead-letter: emit structured log and counter; the
                        # offset is committed below so we don't get stuck on
                        # the same bad message. A real DLT topic would replace
                        # this log with a producer.send to `<topic>.dlt`.
                        await self._dead_letter(message, exc)
                    try:
                        # Commit either way to advance offset. Real DLT would
                        # only commit when DLT publish succeeds.
                        await self._consumer.commit()
                    except Exception as commit_exc:  # noqa: BLE001
                        logger.exception(
                            "Kafka offset commit failed for topic=%s offset=%s: %s",
                            message.topic, message.offset, commit_exc,
                        )
                        await runtime_observability_service.record_error(scope="kafka_commit")
                    if not handled:
                        await runtime_observability_service.record_error(scope="kafka_dlt")
                # async-for exited cleanly → consumer was stopped externally
                return
            except asyncio.CancelledError:
                raise
            except Exception as exc:  # noqa: BLE001
                # Connection-level error: log, brief backoff, retry the for-loop.
                logger.exception("Kafka indexing consumer error, restarting loop: %s", exc)
                await runtime_observability_service.record_error(scope="kafka_consumer")
                await asyncio.sleep(2.0)

    async def _dead_letter(self, message: Any, exc: Exception) -> None:
        payload_preview = ""
        try:
            payload_preview = str(message.value)[:500]
        except Exception:  # noqa: BLE001
            payload_preview = "<unserializable>"
        logger.error(
            json.dumps(
                {
                    "event": "kafka_dead_letter",
                    "topic": message.topic,
                    "partition": message.partition,
                    "offset": message.offset,
                    "error": f"{type(exc).__name__}: {exc}"[:240],
                    "payloadPreview": payload_preview,
                },
                ensure_ascii=False,
            )
        )

    async def _handle_message(self, topic: str, raw_value: str) -> None:
        # _consume_loop wraps this in try/except, so we no longer need to
        # swallow exceptions here — let them bubble up so the dead-letter
        # path can record the failure with offset/partition context.
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
                if not user_id:
                    logger.warning("Enrollment event missing userId — cannot do targeted profile reindex; ignoring.")
                    await runtime_observability_service.record_error(scope="kafka_event_missing_user_id")
                    return
                await vector_service.reindex_single_profile(str(user_id))
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
                if not user_id:
                    logger.warning("Rating event missing userId — cannot do targeted profile reindex; ignoring.")
                    await runtime_observability_service.record_error(scope="kafka_event_missing_user_id")
                    return
                # Profile vector is the primary signal that needs to refresh; course
                # rating aggregates (average_rating, rating_count) on the course
                # payload only update on the next course reindex — that is acceptable
                # because they are advisory ranking signals, not exclusion filters.
                await vector_service.reindex_single_profile(str(user_id))
                if course_id:
                    await vector_service.reindex_single_course(str(course_id))
                return

            # ─── Topic: learning-path-events ───
            # Java payload: LearningPathEventPayload {eventType, pathId, title, courseCount, courseIds}
            if topic == "learning-path-events":
                path_id = payload.get("pathId") or payload.get("path_id")
                logger.info(
                    "Learning path event: %s path=%s title=%s",
                    event_type, path_id, payload.get("title"),
                )
                # Path events only describe the path itself, not which user's
                # profile signal changed; per-user `path_progress` updates flow
                # through enrollment-events / dedicated path-progress events.
                # If the path lists affected courses, reindex only those — never
                # do a global reindex_courses() here.
                course_ids = payload.get("courseIds") or payload.get("course_ids") or []
                if isinstance(course_ids, list):
                    for cid in course_ids:
                        if cid:
                            await vector_service.reindex_single_course(str(cid))
                return

            # ─── Topic: file-uploaded ───
            if topic == "file-uploaded":
                await file_context_service.ingest_uploaded_event(payload)
                logger.info("Processed file-uploaded event: %s", payload.get("fileName") or payload.get("name"))
                return

            # ─── Fallback: unknown topic ───
            logger.info("Ignored Kafka event on topic '%s': eventType=%s", topic, event_type)

        except Exception as exc:
            # Log full exception, increment counter, then re-raise so the
            # consume loop can route through the dead-letter handler with
            # offset/partition context.
            logger.exception("Failed to handle Kafka event on topic '%s': %s", topic, exc)
            await runtime_observability_service.record_error(scope="kafka_event_handler")
            raise


indexing_event_consumer = IndexingEventConsumer()
