# Step 04 - Catalog, Profile Và Recommendation

## Trạng thái hiện tại

`REAL_DATA_READY` - cập nhật 2026-05-16. Đã chạy E2E qua `catalog_service` + `RecommendationService` (deterministic core) trên PostgreSQL live (`techhub` schema, `31.220.87.43:5432/techhub`) với 5 real-data case và 5 logic-fixture case, 10/10 PASS.

Bằng chứng đặt tại `tests/test_step04_recommendation_signal_layer.py`. Chạy lại bằng:

```bash
python -m tests.test_step04_recommendation_signal_layer
```

Vì DB hiện chỉ có rating (1 row), enrollment (17 rows, toàn ENROLLED) và 0 row ở `progress` / `learning_paths` / `path_progress`, một số signal phải verify qua synthetic fixture chạy trực tiếp `RecommendationService._rerank_candidates` / `_normalize_ai_payload` / `_resolve_pipeline`. Khi DB seed thêm `progress` (IN_PROGRESS, COMPLETED), `learning_paths`, `path_progress`, các fixture L1/L2 có thể đổi sang real-data path mà không cần đổi code.

## Kết quả đã triển khai

- Course catalog đọc thêm `average_rating`, `rating_count`, `enrollment_count` để có tín hiệu `catalog_quality`.
- Rating đọc đúng schema thật: `ratings.target_id`, `ratings.target_type = 'COURSE'`, `ratings.score`.
- Lỗi rating không còn mất âm thầm: có structured log, runtime counter và recommendation metadata ghi `missingSignals=["ratings"]` khi lỗi hoặc thiếu dữ liệu rating.
- Course history đã tách rõ:
  - `historyBucket=completed`
  - `historyBucket=in_progress`
  - `historyBucket=enrolled`
  - `historyBucket=abandoned`
- Progress có nguồn rõ:
  - `lesson_progress` khi có lesson và progress record.
  - `lesson_progress_empty` khi có lesson nhưng user chưa có progress.
  - `enrollment_status_fallback` khi course chưa có lesson để tính progress.
- Recommendation chỉ exclude khóa đã hoàn thành và khóa client yêu cầu exclude; không tự loại khóa đang học dở.
- Khóa đang học dở được đưa lại vào candidate set với signal `continue_learning`.
- Learning path đang học đưa thêm candidate với signal `path_alignment`.
- Qdrant course/profile chỉ hỗ trợ tìm ứng viên và xếp hạng; PostgreSQL history/rating/path vẫn là nguồn ngữ cảnh chính.
- Metadata response/persist đã có:

  ```text
  pipeline = real_data | partial_data | fallback_catalog
  usedSignals = [...]
  missingSignals = [...]
  candidateCourseIds = [...]
  filteredCourseIds = [...]
  excludedCourseIds = [...]
  ```

## Smoke test đã chạy

E2E 2026-05-16 (`tests/test_step04_recommendation_signal_layer.py`), kết quả 10/10 PASS trên PostgreSQL live:

Real-data cases (gọi trực tiếp `catalog_service` -> DB live):

| Case | Kết quả |
|---|---|
| R1: rating query đọc đúng schema `target_id` / `target_type` / `score` | USER_RICH có 1 rating score=4, mapping về `course_id` thật |
| R2: course history gắn `historyBucket` + `progressSource` cho mọi row | 12 row, bucket hợp lệ (enrolled), source `enrollment_status_fallback` đúng vì lessons trống |
| R3: cold-start user -> pipeline `fallback_catalog` | history/ratings/paths đều rỗng, pipeline resolve đúng |
| R4: 2 user khác nhau -> candidate course IDs khác nhau | rich_active=12, light_active=4, overlap=4 (khác set) |
| R5: missingSignals phản ánh đúng signal vắng mặt | USER_RICH thiếu learning_paths -> `missingSignals=['learning_paths']`, không nhầm với ratings |

Logic-fixture cases (chạy private helper với input synthetic vì DB thiếu seed cho signal tương ứng):

| Case | Kết quả |
|---|---|
| L1: completed bị loại; in_progress giữ lại + có `continue_learning` | completed -> filtered_ids; in_progress -> signals chứa `continue_learning`, `skill_match`, `language_match` |
| L2: candidate trên active learning path -> `path_alignment` | signal `path_alignment` xuất hiện |
| L3: rating>=4 -> `liked_topic` + bump score; rating<=2 -> `avoid_topic` + trừ score | liked=0.99 > avoided=0.90, gap=0.09 |
| L4: AI payload trả courseId không có trong candidate set -> bị loại | fake id bị drop, real id giữ lại |
| L5: pipeline resolution `real_data` / `partial_data` / `fallback_catalog` | cả 3 case resolve đúng |

## Mục tiêu

Recommendation phải dùng đúng ngữ cảnh người học: enrollment, lesson progress, rating, skill history, learning path đang học, người học tương đồng và catalog course thật.

Mục tiêu không phải "trả 5 khóa học", mà là "giải thích vì sao những khóa này phù hợp với người học này ở thời điểm này".

## File cần đọc

- `TechHub_BE/techhub.sql`
- `ai-service-fastapi/app/services/catalog_service.py`
- `ai-service-fastapi/app/services/recommendation_service.py`
- `ai-service-fastapi/app/orchestration/nodes/agents/rag_retriever_node.py`
- `ai-service-fastapi/app/orchestration/nodes/agents/rag_response_node.py`
- `course-service/src/main/java/com/techhub/app/courseservice/entity/Rating.java`
- `course-service/src/main/java/com/techhub/app/courseservice/service/impl/CourseRatingServiceImpl.java`

## Gap hiện tại

- Query rating đang sai: code đọc `r.course_id`, `r.rating`; bảng thật dùng `target_id`, `target_type`, `score`.
- Lỗi rating bị catch rồi trả `[]`, làm personalization mất tín hiệu nhưng không ai biết.
- Recommendation loại cả course đã học và đang học; như vậy khó gợi ý "học tiếp khóa đang dở".
- `fetch_course_prerequisites()` dùng bảng không tồn tại.
- Profile vector có history, nhưng chất lượng phụ thuộc progress/rating đúng.
- Fallback từ latest catalog có thể vẫn được đánh dấu `COMPLETED`, che mất việc không cá nhân hóa.

## Task triển khai

1. Sửa rating query:

   ```sql
   SELECT
       r.target_id AS course_id,
       r.score,
       c.title,
       c.level
   FROM ratings r
   JOIN courses c
     ON c.id = r.target_id
    AND r.target_type = 'COURSE'
    AND c.is_active = 'Y'
   WHERE r.user_id = :user_id
     AND r.is_active = 'Y'
   ```

2. Không nuốt lỗi schema âm thầm:

   - log structured error,
   - record runtime counter,
   - metadata phải ghi `missingSignals=["ratings"]` nếu rating lỗi.

3. Làm rõ course history:

   - tách `completed`, `in_progress`, `abandoned`, `enrolled`,
   - tính progress từ `lessons -> progress` khi có lessons,
   - nếu chỉ có enrollment status thì ghi rõ fallback.

4. Sửa logic exclude:

   - exclude completed course theo mặc định,
   - không tự loại in-progress course,
   - nếu user đang học dở, cho phép gợi ý continue/next.

5. Chuẩn hóa recommendation signals:

   | Signal | Nguồn |
   |---|---|
   | `skill_match` | `course_skills`, skill profile |
   | `skill_gap` | goal/current/target level |
   | `liked_topic` | ratings score >= 4 |
   | `avoid_topic` | ratings score <= 2 |
   | `continue_learning` | active enrollment, progress < 1 |
   | `similar_learners` | Qdrant profile search |
   | `path_alignment` | `learning_path_courses`, `path_progress` |
   | `catalog_quality` | enrollment count, average rating |

6. Persist metadata chất lượng:

   ```text
   pipeline = real_data | partial_data | fallback_catalog
   usedSignals = [...]
   missingSignals = [...]
   candidateCourseIds = [...]
   filteredCourseIds = [...]
   ```

7. Test bắt buộc:

   - User đã hoàn thành course frontend phải được gợi ý bước tiếp theo hợp lý.
   - User rating cao một chủ đề phải được ưu tiên chủ đề gần đó.
   - User đang học dở phải có continue/next suggestion.
   - Cold-start user phải được hỏi thêm hoặc fallback catalog có ghi rõ.

## Definition of Done

- Rating đọc đúng schema thật.
- Response phân biệt real-data personalization và fallback catalog.
- Similar profile và course vector chỉ hỗ trợ ranking, không thay thế PostgreSQL history.
- Không trả course ID nằm ngoài candidate set đã validate.
- Step này chỉ đạt `REAL_DATA_READY` khi nhiều user khác nhau nhận gợi ý khác nhau và giải thích được bằng signal thật.
