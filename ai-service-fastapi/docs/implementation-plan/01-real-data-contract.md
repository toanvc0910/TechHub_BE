# Step 01 - Real Data Contract

## Mục tiêu

Tạo một data contract chung cho AI Service để mọi chức năng AI biết rõ:

- bảng PostgreSQL nào là nguồn sự thật,
- quan hệ join nào được phép dùng,
- metric nghiệp vụ nào được tính từ bảng nào,
- service nào sở hữu quyền ghi dữ liệu production,
- dữ liệu tối thiểu nào phải có trước khi gọi một chức năng là hoàn thành.

Step này tránh tình trạng AI trả lời nghe hợp lý nhưng không bám schema thật của TechHub.

## File cần đọc

- `TechHub_BE/techhub.sql`
- `TechHub_BE/ai-service-fastapi/app/services/catalog_service.py`
- `TechHub_BE/ai-service-fastapi/app/services/analytics_service.py`
- `TechHub_BE/ai-service-fastapi/app/services/vector_service.py`
- `TechHub_BE/course-service/src/main/java/com/techhub/app/courseservice/entity`
- `TechHub_BE/learning-path-service/src/main/java/com/techhub/app/learningpathservice/entity`
- `TechHub_BE/file-service/src/main/java/com/techhub/app/fileservice/entity`

## Quan hệ dữ liệu thật

Lấy map này làm nền:

```text
users.id
  -> profiles.user_id
  -> enrollments.user_id
  -> ratings.user_id
  -> progress.user_id
  -> submissions.user_id
  -> path_progress.user_id
  -> files.user_id

courses.id
  -> chapters.course_id
  -> enrollments.course_id
  -> learning_path_courses.course_id
  -> course_skills.course_id
  -> course_tags.course_id
  -> analytics.course_id

chapters.id
  -> lessons.chapter_id

lessons.id
  -> exercises.lesson_id
  -> progress.lesson_id
  -> user_codes.lesson_id
  -> lesson_assets.lesson_id

exercises.id
  -> exercise_test_cases.exercise_id
  -> submissions.exercise_id

learning_paths.id
  -> learning_path_courses.path_id
  -> learning_path_skills.path_id
  -> path_progress.path_id

ratings.target_id
  -> courses.id khi ratings.target_type = 'COURSE'
```

## Gap hiện tại

- `catalog_service.fetch_user_ratings()` đang đọc `r.course_id` và `r.rating`, nhưng bảng thật là `target_id`, `target_type`, `score`.
- `catalog_service.fetch_course_prerequisites()` đọc bảng `course_prerequisites`, nhưng bảng này không tồn tại trong `techhub.sql`.
- `analytics_service` có schema string và allowlist viết cứng, dễ lệch với DB.
- Bảng `analytics` có trong `techhub.sql`, nhưng chưa được đưa vào AI analytics allowlist.
- `techhub.sql` không seed dữ liệu học tập thật, nên không đủ để chứng minh personalization, exercise, path, file.

## Task triển khai

1. Tạo module data contract trong AI Service:

   ```text
   app/services/data_contract/
     __init__.py
     tables.py
     joins.py
     metrics.py
     owners.py
   ```

2. Khai báo metadata bảng an toàn cho AI đọc:

   ```python
   TABLES = {
       "courses": {
           "owner": "course-service",
           "columns": ["id", "title", "description", "level", "language", "status", "created", "is_active"],
           "pii": [],
       },
       "ratings": {
           "owner": "course-service",
           "columns": ["id", "user_id", "target_id", "target_type", "score", "created", "is_active"],
           "pii": [],
       },
   }
   ```

3. Khai báo join hợp lệ:

   ```python
   JOINS = [
       {"from": "courses.id", "to": "chapters.course_id", "type": "one_to_many"},
       {"from": "chapters.id", "to": "lessons.chapter_id", "type": "one_to_many"},
       {"from": "lessons.id", "to": "progress.lesson_id", "type": "one_to_many"},
       {"from": "users.id", "to": "progress.user_id", "type": "one_to_many"},
       {"from": "courses.id", "to": "ratings.target_id", "condition": "ratings.target_type = 'COURSE'"},
   ]
   ```

4. Khai báo service owner cho quyền ghi:

   | Dữ liệu | Service sở hữu | AI được ghi trực tiếp? |
   |---|---|---|
   | `ai_generation_tasks` | AI Service | Có |
   | `chat_sessions`, `chat_messages` | AI Service | Có |
   | `exercises`, `exercise_test_cases` | Course Service | Không |
   | `learning_paths`, `learning_path_courses` | Learning Path Service | Không |
   | `files`, `file_usage` | File Service | Không |

5. Tạo readiness check cho dữ liệu:

   ```text
   courses > 0
   chapters > 0
   lessons > 0
   enrollments > 0
   progress > 0 cho personal analytics
   ratings > 0 cho recommendation
   learning_paths > 0 và path_progress > 0 cho path analytics
   files > 0 và indexed chunks > 0 cho file chat
   ```

6. Cho analytics, recommendation, learning path, exercise import data contract thay vì tự giữ schema riêng.

## Kiểm chứng

- Contract check so sánh bảng/cột khai báo với `information_schema`.
- Fail fast nếu cột đã khai báo không tồn tại.
- Xác nhận rating chỉ dùng `ratings.target_id`, `ratings.target_type`, `ratings.score`.
- Xác nhận `course_prerequisites` bị bỏ khỏi code hoặc được tạo bằng migration/schema thật.

## Kết quả thực hiện 2026-05-14

- Đã tạo module `app/services/data_contract/` với `tables.py`, `joins.py`, `metrics.py`, `owners.py`, `validation.py`.
- Đã đổi `analytics_service.py` sang dùng allowlist/schema context từ data contract, không giữ schema string riêng.
- Đã sửa `catalog_service.fetch_user_ratings()` theo schema thật: `ratings.target_id`, `ratings.target_type = 'COURSE'`, `ratings.score`.
- Đã bỏ truy vấn bảng ảo `course_prerequisites`; hàm hiện trả danh sách rỗng có chủ ý cho đến khi schema thật được bổ sung.
- Đã thêm endpoint admin `GET /api/ai/admin/data-contract` và `GET /api/ai/admin/data-contract/validate`.
- Đã thêm route proxy tương ứng `GET /api/proxy/ai/admin/data-contract` và `GET /api/proxy/ai/admin/data-contract/validate`.
- Đã thêm permission seed `AI_DATA_CONTRACT_READ` và `AI_DATA_CONTRACT_VALIDATE` trong `techhub.sql`.

Kiểm chứng đã chạy:

```text
python -m compileall app
mvn -pl proxy-client -am -DskipTests compile
validate_data_contract() -> DATA_CONTRACT_OK
missing_tables -> []
missing_columns_count -> 0
feature readiness -> recommendation, learning_path_generation, exercise_generation, file_analysis, analytics, chat_history, draft_approval = True
```

## Definition of Done

- AI Service có một data contract duy nhất cho bảng, join, metric và service owner.
- Không còn giả định schema mâu thuẫn giữa analytics, recommendation, path và exercise.
- Thiếu bảng/cột phải hiện ra health/check failure, không bị nuốt lỗi âm thầm.
- Step này chỉ đạt `REAL_DATA_READY` khi contract pass với dev database đang chạy.
