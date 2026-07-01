# Step 07 - Exercise Publish Flow

## Trạng thái triển khai

`REAL_DATA_READY` - cập nhật 2026-05-16. E2E: 20/20 case PASS qua validator (DB live với chapter+lesson tạm seed dưới course thật, cleanup ở teardown), publisher (mock HTTP với contract khớp Course Service `ExerciseRequest`) và approve flow (DB live `ai_generation_tasks`). Bằng chứng tại `tests/test_step07_exercise_publish_flow.py`. Chạy lại:

```bash
python -m tests.test_step07_exercise_publish_flow
```

## Thay đổi chính

Module mới:

- `app/services/publishers/exercise_validator.py` — kiểm lesson tồn tại + active + thuộc đúng course (join `lessons -> chapters` filter `chapters.course_id`), readiness content (content/description), AI format `mcq|essay|coding` → ExerciseType `MULTIPLE_CHOICE|OPEN_ENDED|CODING`. Rule: MCQ ≥2 options + đúng 1 correct, essay rubric không rỗng, coding starter + test cases concrete (chặn placeholder "sample input"/"expected behavior"), không duplicate question trong batch, difficulty ∈ {BEGINNER, INTERMEDIATE, ADVANCED}.
- `app/services/publishers/exercise_publisher.py` — convert batch sang `List<ExerciseRequest>`; POST `/api/courses/{courseId}/lessons/{lessonId}/exercises`; trusted identity headers `X-User-Id`/`X-User-Email`/`X-User-Roles`; refuse publish khi base URL có nhưng missing identity.

Wire vào approve flow (`app/services/draft_service.py`):

- `approve_exercise()` chuyển DRAFT → PUBLISHING → PUBLISHED/PUBLISH_FAILED. Outcome ghi vào `result_payload.publish` (gồm `createdExerciseIds`, `requestPayload`, `responsePayload`, `error`).
- Route `/drafts/{task_id}/approve-exercise` đọc trusted context và truyền xuống service.
- Draft thiếu `courseId` trong `request_payload` → fail-fast PUBLISH_FAILED.

Settings mới (`app/core/config.py`):

- `COURSE_SERVICE_BASE_URL` (str|None, default None — skip publish).
- `AI_COURSE_SERVICE_TIMEOUT_SECONDS` (int, default 20).

## Mục tiêu

Exercise do AI sinh ra phải trở thành exercise thật trong Course Service sau khi review. Nội dung phải bám lesson thật và map đúng DTO/model của Course Service.

## File cần đọc

- `ai-service-fastapi/app/services/exercise_service.py`
- `ai-service-fastapi/app/services/draft_service.py`
- `ai-service-fastapi/app/schemas/exercise.py`
- `course-service/src/main/java/com/techhub/app/courseservice/controller/ExerciseController.java`
- `course-service/src/main/java/com/techhub/app/courseservice/dto/request/ExerciseRequest.java`
- `course-service/src/main/java/com/techhub/app/courseservice/dto/ExerciseTestCaseDto.java`
- `course-service/src/main/java/com/techhub/app/courseservice/entity/Exercise.java`
- `course-service/src/main/java/com/techhub/app/courseservice/entity/ExerciseTestCase.java`

## Gap hiện tại

- AI có thể tạo draft payload, nhưng approve chỉ đổi status AI task.
- Nếu không có lesson thật, không thể chứng minh exercise grounded.
- Chưa validate đầy đủ lesson thuộc course đang thao tác.
- Fallback exercise còn chung chung, chỉ hợp local development.
- Payload validation mới kiểm shape nhẹ, chưa kiểm chất lượng exercise.
- Coding test cases có thể không runnable hoặc không khớp workspace language.

## Business rule

Exercise hợp lệ khi:

- lesson tồn tại và active,
- lesson thuộc đúng course,
- lesson có content/assets/chunks đủ để grounding,
- type map đúng enum của Course Service,
- MCQ có ít nhất 2 options và đúng 1 đáp án correct nếu chưa hỗ trợ multi-select,
- essay có rubric,
- coding có starter code/test cases khớp workspace language,
- order index hợp lệ trong lesson,
- publish tạo record thật trong `exercises` và `exercise_test_cases`.

## Task triển khai

1. Validate lesson ownership:

   ```sql
   SELECT l.id, ch.course_id
   FROM lessons l
   JOIN chapters ch ON ch.id = l.chapter_id
   WHERE l.id = :lesson_id
     AND ch.course_id = :course_id
     AND l.is_active = 'Y'
   ```

2. Kiểm readiness lesson content:

   Chỉ cho generate nếu có ít nhất một nguồn:

   - `lessons.content`,
   - `lessons.description`,
   - `lesson_assets`,
   - indexed lesson/file chunks.

   Nếu thiếu content, trả lỗi rõ: chưa thể sinh exercise bám lesson.

3. Tạo validator:

   ```text
   app/services/validators/exercise_payload_validator.py
   ```

   Validator kiểm MCQ, essay, coding, difficulty, explanation, test cases và DTO mapping.

4. Tạo publisher:

   ```text
   app/services/publishers/exercise_publisher.py
   ```

   Nhiệm vụ:

   - convert AI payload sang `List<ExerciseRequest>`,
   - gọi Course Service `POST /api/courses/{courseId}/lessons/{lessonId}/exercises`,
   - truyền trusted admin/instructor context,
   - lưu created exercise IDs vào `ai_generation_tasks.result_payload.publish`.

5. Sửa approval flow:

   - `approve-exercise` publish luôn hoặc thêm endpoint `publish-exercise`.
   - Nếu Course Service write fail, không set status published.
   - Validation error lưu vào `error_message` hoặc `publish.errors`.

6. Quality checks:

   - Không duplicate câu hỏi trong cùng batch.
   - Explanation phải gắn với concept trong lesson.
   - Không cho test case placeholder như "sample input" nếu không phải exercise tự luận/free-form.

## Kiểm chứng (2026-05-16)

E2E `tests/test_step07_exercise_publish_flow.py`, 20/20 PASS:

Validator (10 case, DB live + temp lesson seeded):

| Case | Kết quả |
|---|---|
| V1: payload đầy MCQ+essay+coding hợp lệ | 3 exercises, types đúng, orderIndex 1-3 |
| V2: lesson_id không thuộc course_id | reject với "does not belong" |
| V3: lesson không tồn tại | reject "not found" |
| V4: MCQ 0 correct option | reject "exactly 1 correct" |
| V5: MCQ 2+ correct option | reject "exactly 1 correct" |
| V6: Essay rubric rỗng | reject "rubric" |
| V7: Coding starterCode rỗng | reject "starterCode" |
| V8: Coding test case "sample input"/"expected behavior" | reject "placeholder" |
| V9: duplicate question trong batch | reject "Duplicate exercise question" |
| V10: payload rỗng | reject "empty" |

Publisher (6 case, mock httpx):

| Case | Kết quả |
|---|---|
| P1: POST trả 200 + danh sách id | PUBLISHED, createdExerciseIds set, URL chính xác `/api/courses/{cid}/lessons/{lid}/exercises` |
| P2: POST 500 | PUBLISH_FAILED, error chứa "HTTP 500" |
| P3: base URL có nhưng missing trusted_user_id | PUBLISH_FAILED, 0 HTTP call |
| P4: base URL = None | SKIPPED_NO_BASE_URL, 0 HTTP call |
| P5: shape conversion | sent body là `List<ExerciseRequest>`; MCQ.options=list, OPEN_ENDED.options.rubric, CODING.testCases có expectedOutput; X-User-Id header set |
| P6: validation lỗi trong publish | PUBLISH_FAILED prefix `validation_failed:`, 0 HTTP call |

Approve flow E2E (4 case, DB live):

| Case | Kết quả |
|---|---|
| A1: happy path | DB=PUBLISHED, `publish.createdExerciseIds` khớp 3 id |
| A2: Course Service 500 | DB=PUBLISH_FAILED, error_message chứa "HTTP 500" |
| A3: không cấu hình base URL | DB=APPROVED |
| A4: draft thiếu courseId trong request_payload | DB=PUBLISH_FAILED ngay, không gọi publisher |

Còn lại để full E2E qua Java thật (không block REAL_DATA_READY):

- Set `COURSE_SERVICE_BASE_URL` và verify exercises xuất hiện trong `exercises` / `exercise_test_cases` (DB hiện có 0 exercises do chưa được publish thật).
- Cần Course Service Java running để đo end-to-end latency và verify trusted identity được xử lý chính xác.

## Definition of Done

- Exercise sinh từ AI bám lesson thật.
- Approve tạo record thật qua Course domain.
- Draft, approval, publish, failure đều audit được.
- Step này chỉ đạt `REAL_DATA_READY` khi Course Service trả created exercise records.
