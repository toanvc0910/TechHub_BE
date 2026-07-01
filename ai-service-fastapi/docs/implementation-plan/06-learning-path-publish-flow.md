# Step 06 - Learning Path Publish Flow

## Trạng thái triển khai

`REAL_DATA_READY` - cập nhật 2026-05-16. E2E: 17/17 case PASS qua validator (DB live cho 3 published course), publisher (mock HTTP với contract khớp Java DTO) và approve flow (DB live `ai_generation_tasks`). Bằng chứng tại `tests/test_step06_learning_path_publish_flow.py`. Chạy lại:

```bash
python -m tests.test_step06_learning_path_publish_flow
```

## Thay đổi chính

Status enum mới (`app/core/enums.py`):

- `PUBLISHING` - draft đang trong quá trình gọi Learning Path Service.
- `PUBLISHED` - record thật đã tồn tại trong `learning_paths` (audit trong `result_payload.publish.learningPathId`).
- `PUBLISH_FAILED` - publish thất bại; `error_message` lưu mã HTTP / lý do, `result_payload.publish.learningPathId` lưu pathId đã tạo nếu addCourses lỗi sau create thành công.

Module mới:

- `app/services/publishers/learning_path_validator.py` - validate draft trước khi publish: courseId tồn tại + active + PUBLISHED trong PostgreSQL, không duplicate course/order, isOptional ∈ {Y, N}, mọi node map về course, mọi edge source/target trỏ vào node thật, không self-loop, không cycle (DFS với màu unseen/on-stack/done).
- `app/services/publishers/learning_path_publisher.py` - convert draft sang `LearningPathRequestDTO` shape (`title`, `description`, `skills`, `layoutEdges`, `createdBy`, `updatedBy`); POST `/api/v1/learning-paths` rồi POST `/{pathId}/courses` với `AddCoursesToPathRequestDTO`. Trusted identity forward qua `X-User-Id` / `X-User-Email` / `X-User-Roles`. Trả `LearningPathPublishOutcome` với `status`, `learningPathId`, `error`, `requestPayload`, `responsePayload`, `startedAt`, `finishedAt`.

Wire vào approve flow (`app/services/draft_service.py`):

- `approve_learning_path()` chuyển DRAFT -> PUBLISHING (DB flush) -> gọi publisher -> DRAFT/`PUBLISHED`/`PUBLISH_FAILED`/`APPROVED` (nếu base URL chưa cấu hình thì giữ ở APPROVED). Outcome luôn ghi vào `result_payload.publish` để audit.
- Route `/drafts/{task_id}/approve-learning-path` đọc `X-User-Id` / `X-User-Email` / `X-User-Roles` từ trusted context và truyền xuống service.
- Missing trusted user ID + base URL có config -> `PUBLISH_FAILED` thay vì publish bừa với createdBy null.

Settings mới (`app/core/config.py`):

- `LEARNING_PATH_SERVICE_BASE_URL` (str|None, default None - skip publish).
- `AI_LEARNING_PATH_SERVICE_TIMEOUT_SECONDS` (int, default 20).

## Mục tiêu

Learning path do AI sinh ra phải trở thành learning path thật của TechHub sau khi được review. AI chỉ sinh draft; Learning Path Service phải sở hữu record production cuối cùng.

## File cần đọc

- `ai-service-fastapi/app/services/learning_path_service.py`
- `ai-service-fastapi/app/services/draft_service.py`
- `ai-service-fastapi/app/schemas/learning_path.py`
- `learning-path-service/src/main/java/com/techhub/app/learningpathservice/controller/LearningPathController.java`
- `learning-path-service/src/main/java/com/techhub/app/learningpathservice/dto/LearningPathRequestDTO.java`
- `learning-path-service/src/main/java/com/techhub/app/learningpathservice/dto/CourseInPathDTO.java`
- `learning-path-service/src/main/java/com/techhub/app/learningpathservice/entity/LearningPath.java`
- `learning-path-service/src/main/java/com/techhub/app/learningpathservice/entity/LearningPathCourse.java`

## Gap hiện tại

- AI generation lưu draft trong `ai_generation_tasks`.
- `approve_learning_path()` chỉ đổi status và trả payload, chưa tạo learning path thật.
- Code có nhắc prerequisite nhưng schema thật không có `course_prerequisites`.
- Thứ tự path chưa gắn với prerequisite/skill progression đáng tin cậy.
- Chưa có audit link từ `ai_generation_tasks.id` sang `learning_paths.id`.

## Business rule

Path hợp lệ khi:

- mọi course ID tồn tại, active/published,
- thứ tự có rule giải thích được,
- edge chỉ trỏ đến node có trong path,
- node position hợp lệ cho frontend,
- optional course được đánh dấu rõ,
- path khớp goal/current level/target level/duration,
- publish tạo record thật trong `learning_paths` và `learning_path_courses`.

## Task triển khai

1. Chốt prerequisite model:

   Option A: thêm bảng domain thật:

   ```text
   course_prerequisites(course_id, prerequisite_id)
   ```

   Option B: chưa thêm bảng, derive thứ tự từ:

   - course `level`,
   - `course_skills`,
   - course `requirements`,
   - learning path order đã có,
   - preferred courses admin chọn.

   Chọn option nào thì cập nhật vào Step 01 data contract.

2. Validate draft payload:

   - không có unknown course ID,
   - không duplicate node ID,
   - không edge đến node thiếu,
   - không cycle nếu UI không hỗ trợ,
   - đủ field cho `LearningPathRequestDTO`,
   - layout edges hợp với `LearningPath.LayoutEdge`.

3. Tạo publisher:

   ```text
   app/services/publishers/learning_path_publisher.py
   ```

   Nhiệm vụ:

   - convert AI draft sang `LearningPathRequestDTO`,
   - gọi Learning Path Service qua proxy/service discovery hoặc client riêng,
   - gửi trusted user/admin context,
   - lưu publish result vào `ai_generation_tasks.result_payload.publish`.

4. Sửa approval flow:

   - `approve-learning-path` publish luôn hoặc tạo endpoint riêng `publish-learning-path`.
   - Nếu publish fail, không set final status thành published.
   - Nên có status:

     ```text
     DRAFT
     APPROVED
     PUBLISHING
     PUBLISHED
     REJECTED
     PUBLISH_FAILED
     ```

5. Rollback/error:

   - Nếu Learning Path Service tạo path nhưng add course fail, trả publish failure kèm path ID đã tạo.
   - Ưu tiên để domain service xử lý transaction.

## Kiểm chứng (2026-05-16)

E2E `tests/test_step06_learning_path_publish_flow.py`, 17/17 PASS trên DB live + mock HTTP cho Java publish endpoint:

Validator (8 case, đều fail-fast trước khi publish):

| Case | Kết quả |
|---|---|
| V1: draft hợp lệ với 3 course PUBLISHED thật | 3 courses, 2 edges, 2 skills - pass |
| V2: courseId không có trong DB | `Unknown courseIds: [<uuid>]` |
| V3: duplicate courseId | `Duplicate courseId: <id>` |
| V4: duplicate order | `Duplicate order=1` |
| V5: isOptional='MAYBE' | `isOptional must be 'Y' or 'N'` |
| V6: edge target không thuộc courses | `target <uuid> not in courses` |
| V7: edge self-loop | `forms a self-loop` |
| V8: cycle A->B->C->A | `Cycle detected ... back-edge` |

Publisher (6 case, mock httpx):

| Case | Kết quả |
|---|---|
| P1: create + addCourses đều OK | status=PUBLISHED, learningPathId set, 2 POST calls |
| P2: create 500 | PUBLISH_FAILED, không gọi addCourses, learningPathId=None |
| P3: addCourses fail sau create OK | PUBLISH_FAILED nhưng learningPathId được lưu (partial state cho admin reconcile) |
| P4: missing trusted_user_id + base URL có | PUBLISH_FAILED ngay, 0 HTTP call |
| P5: base URL = None | SKIPPED_NO_BASE_URL, 0 HTTP call |
| P6: validation lỗi trong publish | PUBLISH_FAILED prefix `validation_failed:`, 0 HTTP call |

Approve flow E2E trên DB live (3 case):

| Case | Kết quả |
|---|---|
| A1: happy path | DRAFT row -> DB status=PUBLISHED, `result_payload.publish.learningPathId` đúng |
| A2: LP service trả 500 | DB status=PUBLISH_FAILED, `error_message` chứa "HTTP 500" |
| A3: không cấu hình base URL | DB status=APPROVED (không phải PUBLISH_FAILED) |

Còn lại để full E2E qua Java thật (không block REAL_DATA_READY):

- Chạy đầy đủ qua Learning Path Service thật khi service được expose URL ra dev gateway và verify record xuất hiện trong `learning_paths`/`learning_path_courses`.
- Drive end-to-end từ FE generate -> approve để kiểm chứng prompt-to-publish.

## Definition of Done

- Approve AI draft tạo learning path thật hoặc ghi rõ chỉ là draft.
- Trạng thái phân biệt approval và publishing.
- Created learning path ID được lưu để audit.
- Step này chỉ đạt `REAL_DATA_READY` khi path publish xem được qua Learning Path Service API.
