# TechHub AI Service - Trạng Thái Thực Thi Và Kế Hoạch Hoàn Thành Nghiệp Vụ

> Rà soát lần cuối: 2026-05-16
> Phạm vi: `TechHub_BE/ai-service-fastapi`, `TechHub_BE/techhub.sql`, và các điểm tích hợp trong `TechHub_BE`
> Quy tắc đánh giá: một chức năng không được xem là hoàn thành chỉ vì endpoint trả về dữ liệu. Chỉ xem là hoàn thành khi chức năng dùng đúng bảng thật, quan hệ thật, người dùng thật, dữ liệu index thật, và qua được kịch bản nghiệp vụ đầu-cuối.

## 1. Kết Luận Hiện Tại

AI Service hiện đã có khung kỹ thuật khá đầy đủ: FastAPI route, chat orchestration, Qdrant, đổi provider, lưu draft, parse file, Kafka consumer và admin endpoint.

Nhưng nếu đo theo bài toán nghiệp vụ thật của TechHub thì chưa hoàn thành. Nhiều phần đang ở mức "đủ để demo" hoặc "có fallback để không lỗi", chưa đủ để chạy đúng với dữ liệu học tập thật.

Các điểm cần sửa cách hiểu:

- Hỏi số liệu đã có semantic layer ở Step 03: metric registry, SQL template, validator theo metric. Đã chạy E2E ngày 2026-05-16 trên PostgreSQL live (`techhub` schema): 9/9 câu hỏi bắt buộc đi qua planner -> validator -> SQL execute thành công (xem `tests/test_step03_analytics_semantic_layer.py`). Một số metric vẫn trả empty vì DB chưa seed `progress/submissions/learning_paths/path_progress/analytics`.
- Qdrant không tự biết join bảng. Các câu hỏi như tiến độ học, đánh giá, bài nộp, doanh thu, hoàn thành lộ trình phải dựa vào PostgreSQL và bản đồ quan hệ nghiệp vụ.
- Recommendation đã có semantic signal layer ở Step 04: rating đúng schema, course history có progress source, không loại nhầm khóa đang học dở, và response ghi rõ pipeline/signal. E2E 2026-05-16 trên DB live cho 2 user thật + 1 cold-start: rating query đúng schema, history bucket/progressSource đúng, 2 user nhận candidate set khác nhau, pipeline resolution + AI payload sanitization PASS (`tests/test_step04_recommendation_signal_layer.py`).
- Learning path và exercise đều có publish flow: Step 06 (LP) và Step 07 (exercise) cùng pattern validator + publisher + status `DRAFT -> PUBLISHING -> PUBLISHED/PUBLISH_FAILED` + audit `result_payload.publish`. Để publish thật cần set `LEARNING_PATH_SERVICE_BASE_URL` / `COURSE_SERVICE_BASE_URL` trong `.env`; nếu thiếu thì draft đứng ở APPROVED và FE có thể vẫn lấy data từ AI service.
- File analysis có Step 08 với hydrate -> parse (DOCX/XLSX/PDF/text) -> chunk -> Qdrant indexing với ownership enforced (user_id + session_id filter), idempotent re-ingest, readiness API, content_hash audit. OCR-gated cho image qua Gemini Vision khi `OCR_ENABLED=true` + `GEMINI_API_KEY`. Bug nghiêm trọng đã fix: `_stable_point_id` từng trả sha1 hex (Qdrant 400) - đã đổi sang UUIDv5 deterministic.
- Chat streaming đã được nối vào trusted identity ở Step 02: WebClient forward `X-User-Id`, `X-User-Email`, `X-User-Roles`; `techhub.sql` chuyển stream sang permission-based policy. Vẫn cần test E2E qua token thật để lên `E2E_VERIFIED`.
- Trạng thái `DONE` cũ che mất fallback. Trả được fallback không có nghĩa là giải quyết đúng nghiệp vụ.

## 2. Cách Gọi Trạng Thái Từ Bây Giờ

| Trạng thái | Ý nghĩa |
|---|---|
| `CODE_WIRED` | Đã có route/service/code path, nhưng chưa chứng minh chạy đúng dữ liệu thật. |
| `FALLBACK_DEMO` | Chạy được chủ yếu nhờ fallback, mock, catalog mới nhất, hoặc empty-safe output. |
| `REAL_DATA_READY` | Đã dùng đúng bảng PostgreSQL, quan hệ, Qdrant collection, và user context xác thực. |
| `E2E_VERIFIED` | Đã test qua proxy và AI Service với dữ liệu gần production, đúng kết quả nghiệp vụ. |
| `BLOCKED` | Bị chặn vì thiếu schema, thiếu data, thiếu infra, hoặc thiếu contract với service khác. |

Không đánh dấu `E2E_VERIFIED` nếu chưa test cả happy path và ít nhất một trường hợp rỗng/lỗi.

## 3. Nguồn Sự Thật Dữ Liệu

### 3.1 PostgreSQL Là Nguồn Nghiệp Vụ Chính

`TechHub_BE/techhub.sql` là nguồn sự thật cho bảng và quan hệ. Prompt hoặc vector DB không được tự quyết định quan hệ bảng.

Quan hệ học tập cốt lõi:

```text
users
  -> profiles
  -> enrollments -> courses
  -> ratings(target_id, target_type, score)
  -> progress -> lessons -> chapters -> courses
  -> submissions -> exercises -> lessons
  -> path_progress -> learning_paths -> learning_path_courses -> courses

courses
  -> chapters -> lessons -> exercises -> exercise_test_cases
  -> course_skills -> skills
  -> course_tags -> tags

files
  -> file_usage
  -> AI indexed chunks trong Qdrant

ai_generation_tasks
  -> chỉ lưu draft/kết quả AI, không phải bảng production của exercise/path
```

Các sự thật schema quan trọng:

- `ratings` không có `course_id` hoặc `rating`; bảng thật dùng `target_id`, `target_type`, `score`.
- `course_prerequisites` không tồn tại trong `techhub.sql`.
- Bảng `analytics` có tồn tại nhưng AI analytics hiện chưa đưa vào allowlist.
- `techhub.sql` seed role, permission, endpoint policy; không seed dữ liệu học tập đủ thật như chapters, lessons, progress, submissions, learning paths.
- Vì vậy DB fresh từ `techhub.sql` chưa đủ để chứng minh cá nhân hóa học tập, sinh exercise theo lesson thật, hoặc analytics theo path/progress.

### 3.2 Vai Trò Đúng Của Qdrant

Qdrant dùng để tìm kiếm theo ngữ nghĩa, không phải nguồn sự thật quan hệ.

Nên dùng Qdrant để:

- tìm course candidate theo ý nghĩa,
- tìm lesson/file chunk liên quan câu hỏi,
- tìm người học tương đồng sau khi profile vector đã được build,
- lấy citation/context cho câu trả lời.

Không dùng Qdrant để:

- quyết định SQL join,
- tính số lượng học viên, tiến độ, rating, payment, submission, completion rate,
- thay thế foreign key và business rule trong PostgreSQL.

### 3.3 Vai Trò Đúng Của AI Draft

`ai_generation_tasks` là bảng draft/audit của AI.

Đích publish thật:

- Exercise phải được Course Service tạo vào `exercises` và `exercise_test_cases`.
- Learning path phải được Learning Path Service tạo vào `learning_paths`, `learning_path_courses`, và các bảng liên quan.

Approve draft mà chỉ đổi trạng thái trong `ai_generation_tasks` thì chưa phải publish nghiệp vụ.

## 4. Đánh Giá Chức Năng Hiện Tại

| Chức năng | Trạng thái hiện tại | Khoảng hở thật |
|---|---|---|
| FastAPI foundation | `CODE_WIRED` | Cần checklist release theo infra thật, không chỉ compile. |
| Chat orchestration | `REAL_DATA_READY` | E2E 2026-05-16: 54/54 case PASS (3 tier-0 routing + 30 intent prompts đa ngôn ngữ + 7 entity + 12 grounding + 2 HITL). Quality flags `grounded`/`dataSources`/`fallbackUsed`/`missingData` populated mỗi response. Test: `tests/test_step09_chat_grounding_and_intent_quality.py`. |
| Chat streaming | `REAL_DATA_READY` | Đã forward trusted headers qua WebClient, đổi stream policy sang `AUTHORIZED`, và FastAPI reject `userId` sai. Cần E2E qua proxy/FE để lên `E2E_VERIFIED`. |
| Recommendation | `REAL_DATA_READY` | E2E 2026-05-16: 10/10 case PASS (5 real-data trên DB live + 5 logic-fixture cho signal data-blocked). Rating schema đúng, history bucket/progressSource đúng, 2 user khác nhau -> candidate khác nhau, cold-start -> fallback_catalog, AI payload bịa courseId bị loại. Test: `tests/test_step04_recommendation_signal_layer.py`. |
| Learning path generation | `REAL_DATA_READY` | E2E 2026-05-16: 17/17 case (8 validator + 6 publisher + 3 approve flow) PASS trên DB live + mock HTTP cho Java endpoint. Status flow `DRAFT -> PUBLISHING -> PUBLISHED/PUBLISH_FAILED`, audit `result_payload.publish`. Test: `tests/test_step06_learning_path_publish_flow.py`. Prerequisite vẫn dùng order/level (option B) vì DB chưa có bảng `course_prerequisites`. |
| Exercise generation | `REAL_DATA_READY` | E2E 2026-05-16: 20/20 case (10 validator + 6 publisher + 4 approve flow) PASS. Validator chạy DB live với chapter+lesson seed dưới course thật. Status flow `DRAFT->PUBLISHING->PUBLISHED/PUBLISH_FAILED`, audit `result_payload.publish.createdExerciseIds`. Test: `tests/test_step07_exercise_publish_flow.py`. AI format `mcq/essay/coding` map đúng `MULTIPLE_CHOICE/OPEN_ENDED/CODING`; placeholder test cases bị reject. |
| Analytics/chart | `REAL_DATA_READY` | E2E 2026-05-16: 9/9 câu hỏi bắt buộc PASS qua planner -> validator -> SQL trên PostgreSQL live. Bug `_resolve_chart_type` ("tron" match "trong") đã sửa bằng word-boundary. Test script: `tests/test_step03_analytics_semantic_layer.py`. Metric có table rỗng (progress/submissions/path_progress/analytics) vẫn cần seed để assert non-empty rows. |
| File analysis | `REAL_DATA_READY` | E2E 2026-05-16: 13/13 case PASS qua hydrate/extract(DOCX,XLSX,inline)/index/search/ownership/idempotent re-ingest/readiness API trên Qdrant live. Test: `tests/test_step08_file_ingestion_and_search.py`. Fix bug Qdrant point-ID format (sha1->UUIDv5). |
| Qdrant indexing | `REAL_DATA_READY` | E2E 2026-05-16 trên Qdrant live: 7/7 case PASS. Có `feature_readiness()` cho 7 AI feature, `retrievalMode` tag mỗi search result, observability timestamps cho full/incremental reindex và last_error theo collection. Test: `tests/test_step05_vector_index_and_events.py`. |
| Kafka freshness | `REAL_DATA_READY` | Đã chuyển enrollment/rating/learning-path event sang targeted reindex (`reindex_single_profile` / `reindex_single_course`), không còn gọi `reindex_all()` từ event handler. Missing-userId được log + counter, không trigger global reindex. Contract routing verified bằng spies trên 5 topic. |
| Runtime policy | `REAL_DATA_READY` | Đã bỏ quyền từ request body, chỉ nhận role/user từ trusted headers; learner chỉ được analytics cá nhân qua Step 03, không tự bật platform analytics/PII/model override. Cần thêm policy theo tenant ở step sau. |
| Observability | `REAL_DATA_READY` | Step 10 hoàn thành: `release_readiness_service` + admin `GET /api/ai/admin/release-readiness` tag mỗi capability bằng status enum + reason. Publisher Step 06/07 ghi `record_publish_event` → counters `publish:<kind>:PUBLISHED/PUBLISH_FAILED` + timestamps. Release runner `scripts/e2e/run_release_smoke.py` 5/5 step PASS (63 case). Test: `tests/test_step10_release_observability.py` 8/8 PASS. |

## 5. Roadmap Hoàn Thành Nghiệp Vụ

Triển khai theo thứ tự dưới đây. Mỗi step có một file chi tiết trong `docs/implementation-plan`.

| Step | File chi tiết | Mục tiêu | Trạng thái sau khi xong |
|---|---|---|---|
| 01 | `docs/implementation-plan/01-real-data-contract.md` | Tạo data contract từ `techhub.sql`: bảng, join, metric, owner service, yêu cầu seed data. | `REAL_DATA_READY` - đã triển khai và `validate_data_contract()` pass |
| 02 | `docs/implementation-plan/02-trusted-identity-and-policy.md` | AI dùng user identity/policy đã xác thực từ proxy/JWT, không tin `userId` client gửi. | `REAL_DATA_READY` - đã triển khai, compile và smoke test pass |
| 03 | `docs/implementation-plan/03-analytics-semantic-layer.md` | Thay SQL ad-hoc bằng metric/join map và SQL template cho analytics thật. | `REAL_DATA_READY` - E2E 2026-05-16 chạy planner+validator+SQL trên DB live, 9/9 câu hỏi bắt buộc PASS |
| 04 | `docs/implementation-plan/04-catalog-profile-recommendation.md` | Sửa catalog/profile/rating/history để recommendation cá nhân hóa bằng dữ liệu thật. | `REAL_DATA_READY` - E2E 2026-05-16 trên DB live, 10/10 case (5 real-data + 5 logic-fixture) PASS |
| 05 | `docs/implementation-plan/05-vector-index-and-events.md` | Làm Qdrant/Kafka phản ánh đúng course, lesson, profile, file mới nhất. | `REAL_DATA_READY` - E2E 2026-05-16 trên Qdrant + DB live, 7/7 case PASS; consumer reindex theo target |
| 06 | `docs/implementation-plan/06-learning-path-publish-flow.md` | Generate, validate, approve, publish learning path vào domain thật. | `REAL_DATA_READY` - E2E 2026-05-16, 17/17 case PASS, status `DRAFT->PUBLISHING->PUBLISHED/PUBLISH_FAILED` |
| 07 | `docs/implementation-plan/07-exercise-publish-flow.md` | Generate, validate, approve, publish exercise vào Course Service. | `REAL_DATA_READY` - E2E 2026-05-16, 20/20 case PASS, status `DRAFT->PUBLISHING->PUBLISHED/PUBLISH_FAILED` |
| 08 | `docs/implementation-plan/08-file-ingestion-and-ocr.md` | Kiểm chứng upload-to-index-to-chat cho file người dùng, gồm OCR. | `REAL_DATA_READY` - E2E 2026-05-16 trên Qdrant live, 13/13 case PASS, bug Qdrant point-ID format đã fix |
| 09 | `docs/implementation-plan/09-chat-grounding-and-intent-quality.md` | Chat route đúng intent, có citation, không trả lời bịa khi thiếu data. | `REAL_DATA_READY` - E2E 2026-05-16, 54/54 case PASS, quality flags grounded/dataSources/fallbackUsed/missingData wire qua response_compose |
| 10 | `docs/implementation-plan/10-e2e-observability-and-release.md` | Thêm kịch bản end-to-end, quality gate, admin observability để release. | `REAL_DATA_READY` - 2026-05-16: release runner 5/5 step PASS (63 case), admin `/release-readiness` endpoint + publish counters + status enum. `E2E_VERIFIED` cần Java services running + Qdrant lesson collection có data. |

## 6. Kịch Bản Bắt Buộc Phải Qua

| Kịch bản | Kết quả đúng |
|---|---|
| Learner hỏi "tôi nên học gì tiếp?" | Recommendation dùng profile, enrollment, progress, rating, skill, catalog; không chỉ trả course mới nhất trừ khi ghi rõ fallback. |
| Learner hỏi tiến độ học | SQL dùng `enrollments -> courses -> chapters -> lessons -> progress` với trusted user ID. |
| Admin hỏi analytics toàn hệ thống | SQL dùng metric template, bảng allowlist, policy đúng quyền. |
| Learner hỏi file đã upload | AI hydrate metadata, kiểm tra quyền, retrieve indexed chunks, có citation từ file. |
| Instructor sinh exercise cho lesson | Lesson tồn tại, thuộc course đúng, có content, payload map được sang Course Service DTO. |
| Admin approve exercise draft | Tạo record thật trong `exercises` và `exercise_test_cases`. |
| Admin sinh learning path | Course IDs thật, node/edge hợp lệ, thứ tự có rule giải thích được. |
| Admin approve learning path draft | Tạo record thật trong `learning_paths` và `learning_path_courses`. |
| Course/lesson/rating/progress đổi | Kafka event cập nhật đúng Qdrant collection/profile vector. |
| Provider/Qdrant lỗi | User nhận degraded response rõ ràng, admin thấy counter fallback/error. |

## 7. Nguyên Tắc Bắt Buộc Khi Triển Khai

- Không tin `userId` trong body cho dữ liệu cá nhân nếu không khớp trusted identity.
- Không để LLM tự bịa join SQL. LLM chỉ được chọn trong metric đã biết hoặc bị validate chặt.
- Không xem Qdrant là nguồn sự thật cho số liệu.
- Không gọi draft là published nếu service owner chưa tạo record thật.
- Không nuốt lỗi schema âm thầm khi ảnh hưởng personalization hoặc analytics.
- Không đánh dấu fallback là hoàn thành nếu không expose `executionMode=fallback` hoặc metadata tương đương.
- Không dùng dữ liệu giả làm bằng chứng, trừ khi test ghi rõ đó là fixture.

## 8. Kiểm Chứng Tối Thiểu

Sau mỗi step:

```bash
python -m compileall TechHub_BE/ai-service-fastapi/app
mvn -pl proxy-client,course-service,learning-path-service,file-service -am test
```

Sau toàn roadmap cần có bằng chứng:

- Qdrant stats cho courses, lessons, profiles, session files, user files.
- DB counts cho courses, chapters, lessons, enrollments, progress, ratings, submissions, learning paths, path progress, files, AI tasks.
- Analytics request mẫu có SQL, rows, chart spec, policy snapshot.
- Recommendation mẫu có real signals và không fallback ngoài ý muốn.
- Exercise approval tạo record Course Service.
- Learning path approval tạo record Learning Path Service.
- File upload event tạo searchable chunks.
- Chat streaming có trusted user context.

## 9. Cách Dùng Kế Hoạch Này

Khi giao việc cho AI agent khác, đưa file này kèm đúng file step cần làm. Mỗi file step có:

- mục tiêu nghiệp vụ,
- file source cần đọc,
- quan hệ bảng thật,
- gap hiện tại,
- task triển khai,
- case kiểm chứng,
- định nghĩa hoàn thành.

Chỉ cập nhật trạng thái trong file này sau khi có bằng chứng kiểm chứng thật.
