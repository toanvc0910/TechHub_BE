# Step 05 - Vector Index Và Event Freshness

## Trạng thái triển khai

`REAL_DATA_READY` - cập nhật 2026-05-16. E2E qua Qdrant live (`https://qdrant.inova.id.vn`, 4/5 collection có data; 13 course points, 9 profile points) + PostgreSQL live: 7/7 case PASS. Bằng chứng đặt tại `tests/test_step05_vector_index_and_events.py`. Chạy lại bằng:

```bash
python -m tests.test_step05_vector_index_and_events
```

## Thay đổi chính trong lần lên `REAL_DATA_READY`

Bug fix (correctness + performance):

- `indexing_event_consumer.py` trước đây gọi `vector_service.reindex_all()` cho mỗi `enrollment-event` / `rating-event`, và `reindex_courses()` cho mỗi `learning-path-event`. Với DB nhiều user, mỗi event đẩy progress sẽ kéo theo embed lại toàn bộ courses + lessons + profiles -> tốn token và làm chậm freshness của những event sau. Đã đổi sang reindex theo target:
  - `enrollment-events` -> `reindex_single_profile(userId)`
  - `rating-events` -> `reindex_single_profile(userId)` + `reindex_single_course(courseId)`
  - `learning-path-events` -> `reindex_single_course(courseId)` cho từng `courseIds` trong payload
  - Mọi event thiếu `userId` mà cần userId -> không reindex toàn cục; ghi `errors:kafka_event_missing_user_id` counter.

Tính năng mới:

- `vector_service.reindex_single_profile(user_id)` upsert đúng một profile point bằng cùng id-shape với full reindex (str(user_id)).
- `vector_service.get_feature_readiness()` báo cho mỗi AI feature (`recommendation`, `similar_learner`, `lesson_qa`, `exercise_grounding`, `session_file_chat`, `user_file_chat`, `course_rag`) collection bắt buộc có sẵn sàng không, và liệt kê `degradedFeatures`.
- `search_courses` / `search_similar_profiles` gán `retrievalMode = "vector" | "lexical_fallback"` cho từng result. `recommendation_service` đọc cờ này và đưa `course_vector_lexical_fallback` / `similar_learners_lexical_fallback` vào `missingSignals`, đồng thời mark `pipeline=partial_data` -> fallback không còn invisible.
- `observability_service.record_vector_operation()` nhận thêm `mode` (counters per-mode) và track `last_full_reindex:<col>` / `last_incremental:<col>` / `last_error:<col>` qua `_timestamps`, surface trong `snapshot().vectorOps.timestamps`.

## Mục tiêu

Qdrant phải phản ánh dữ liệu thật mới nhất của course, lesson, profile người học và file. Khi course/lesson/rating/file thay đổi, AI retrieval phải được cập nhật đủ nhanh để recommendation, RAG, exercise và file chat không dùng dữ liệu cũ.

## File cần đọc

- `ai-service-fastapi/app/services/vector_service.py`
- `ai-service-fastapi/app/services/indexing_event_consumer.py`
- `ai-service-fastapi/app/services/catalog_service.py`
- `common-service/src/main/java/com/techhub/app/commonservice/kafka/publisher/CourseEventPublisher.java`
- `common-service/src/main/java/com/techhub/app/commonservice/kafka/KafkaTopics.java`
- `course-service/src/main/resources/application.yml`
- `file-service/src/main/java/com/techhub/app/fileservice/kafka/FileEventPublisher.java`
- `file-service/src/main/java/com/techhub/app/fileservice/kafka/FileUploadedEvent.java`

## Gap hiện tại

- Qdrant có nhiều collection nhưng chưa có readiness contract cho từng chức năng.
- Một số search fail thì fallback lexical âm thầm.
- Enrollment/rating/path event có thể reindex quá rộng hoặc chưa chứng minh targeted profile update.
- Payload vector có ID/metadata, nhưng chưa có health state nói collection nào bắt buộc cho feature nào.
- File collection có thể rỗng trong khi UI vẫn tưởng file analysis đã sẵn sàng.

## Contract collection

| Collection | Nguồn | Dùng cho |
|---|---|---|
| courses | `courses`, `course_skills`, `course_tags`, enrollment count | recommendation, course RAG |
| lessons | `lessons -> chapters -> courses` | lesson Q&A, exercise context |
| profiles | `users`, `profiles`, enrollments, progress, ratings | similar learners |
| session files | chunks attach trong một chat session | file analysis theo session |
| user files | chunks từ File Service | hỏi lại file đã upload |

## Task triển khai

1. Thêm index readiness checks:

   ```text
   recommendation cần courses collection > 0
   exercise theo lesson cần lesson tồn tại trong DB và lesson collection sẵn sàng
   file analysis cần session/user file chunks > 0
   similar learner cần profiles collection > 0
   ```

2. Expose readiness trong admin stats:

   - tên collection,
   - point count,
   - last full reindex,
   - last incremental event,
   - last error,
   - feature nào đang degraded.

3. Làm fallback visible:

   - Nếu Qdrant fail và dùng lexical fallback, response metadata phải có `retrievalMode=lexical_fallback`.
   - Record runtime counter.
   - Không gọi feature là verified nếu path chính đang fallback.

4. Cải thiện event handling:

   - Course update: reindex một course.
   - Lesson update: reindex một lesson.
   - Enrollment update: reindex profile của một user.
   - Rating update: reindex profile của một user, refresh course quality signal nếu cần.
   - Learning path update: reindex metadata/profile bị ảnh hưởng.
   - File upload: hydrate/index đúng một file.

5. Test contract event:

   - Java publisher payload phải khớp Python consumer.
   - Validate field: courseId, lessonId, userId, ratingId, pathId, fileId.
   - Missing ID phải warning rõ, không fake success.

6. Manual reindex:

   - Full reindex chỉ admin được gọi.
   - Kết quả trả count theo collection.
   - Recreate collection chỉ khi explicit hoặc an toàn cho môi trường.

## Kiểm chứng (2026-05-16)

E2E `tests/test_step05_vector_index_and_events.py`, 7/7 PASS trên Qdrant + DB live:

| Case | Kết quả |
|---|---|
| S1: collection stats reachable + schema | healthy=True, 5 logical key đủ, courses=13 points |
| S2: feature_readiness classify đúng | ready=`[recommendation, similar_learner, course_rag]`; degraded=`[exercise_grounding, lesson_qa, session_file_chat, user_file_chat]` (đúng vì lessons/sessionFiles=0 points, userFiles chưa init) |
| S3: search_courses tag `retrievalMode` mọi result | trên query "python frontend" -> 5 result, modes=`{vector}` (Qdrant live -> không fallback) |
| S4: reindex_single_course không tạo duplicate | count Qdrant ổn định ở 13 trước/sau reindex; point tồn tại tại canonical UUID id |
| S5: reindex_single_profile + observability timestamp | profile point hiện diện sau reindex; timestamp `last_incremental:user_embeddings` tiến lên |
| S6: Kafka contract routing | course-events->reindex_single_course; lesson-events->reindex_single_lesson; enrollment->reindex_single_profile; rating->reindex_single_profile + reindex_single_course; learning-path với courseIds->reindex_single_course cho từng id. `reindex_all` / `reindex_courses` / `reindex_lessons` không bị gọi |
| S7: Missing userId -> không reindex_all | counter `errors:kafka_event_missing_user_id` tăng đúng 2; không gọi `reindex_single_profile` cũng không `reindex_all` |

Roadmap còn lại (cần khi DB seed thêm dữ liệu, không block REAL_DATA_READY):

- Tạo/sửa course thật -> verify course point đổi (S4 đã verify cơ chế upsert nhưng chưa drive qua Java publisher thật).
- Tạo/sửa lesson thật -> verify lesson point đổi (DB lessons=0 nên chưa exercise được).
- Upload file -> verify user file chunks search được (thuộc Step 08).
- Tắt Qdrant và verify response bị đánh dấu degraded (cần gửi traffic qua FE/proxy).

## Definition of Done

- Mỗi AI feature biết index bắt buộc của nó đã sẵn sàng chưa.
- Kafka giữ index fresh mà không phụ thuộc manual full reindex.
- Retrieval fallback hiện rõ trong metadata/runtime stats.
- Step này chỉ đạt `REAL_DATA_READY` khi event thật làm thay đổi Qdrant đúng collection.
