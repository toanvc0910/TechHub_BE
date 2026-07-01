# Step 03 - Analytics Semantic Layer

## Trạng thái triển khai

`REAL_DATA_READY` - cập nhật 2026-05-16. Đã chạy E2E qua planner → validator → SQL thật trên PostgreSQL live (`techhub` schema, `31.220.87.43:5432/techhub`) với toàn bộ 9 câu hỏi bắt buộc, 9/9 PASS.

Bằng chứng đặt tại `tests/test_step03_analytics_semantic_layer.py`. Chạy lại bằng:

```bash
python -m tests.test_step03_analytics_semantic_layer
```

Bug đã sửa trong lần verify này: `AnalyticsSemanticPlanner._resolve_chart_type` từng pick `pie` cho câu "Doanh thu theo khóa trong tháng này" do `tron` là substring của `trong`. Đã chuyển sang word-boundary matching cho các token ngắn (`tron`, `cot`, `bar`, `pie`, `line`); các phrase dài ("bieu do tron", "pie chart", ...) vẫn substring-match như trước.

Các metric có table rỗng trên DB hiện tại (`progress`, `submissions`, `learning_paths`, `path_progress`, `analytics`, `lessons`, `chapters`, `exercises` đều 0 rows) vẫn được verify ở mức planner/validator/SQL hợp lệ, executes và trả empty rows. Cần seed các bảng này nếu muốn assert non-empty rows. Test script in rõ những case nào đang ở trạng thái empty-data để dễ theo dõi.

## Kết quả đã làm

- Tạo package `app/services/analytics/` gồm:
  - `metric_registry.py`: định nghĩa metric, scope, role, bảng được phép, grain, chart mặc định.
  - `sql_templates.py`: sinh SQL từ template cố định, dùng named params như `:user_id`.
  - `planner.py`: chọn metric bằng luật deterministic trước, không để LLM tự viết SQL cho các metric đã biết.
  - `validator.py`: kiểm tra bảng trong SQL phải thuộc metric definition, personal scope phải có `:user_id`, instructor scope phải filter `c.instructor_id = :user_id`, không cho `SELECT *`, không select cột nhạy cảm khi không có quyền PII.
- `analytics_service.py` ưu tiên semantic planner trước LLM planner cũ.
- Runtime policy cho phép learner đi vào analytics cá nhân, nhưng `analytics_service.py` vẫn chặn platform analytics nếu không phải instructor/staff/admin.
- Data contract metric list đã bổ sung các metric thật: `learner_course_progress`, `average_course_rating`, `learner_submissions`, `learning_path_completion`, `revenue_by_course`, v.v.

## Kiểm chứng đã chạy

```bash
python -m compileall app
python -m tests.test_step03_analytics_semantic_layer
```

E2E test 2026-05-16 chạy trên PostgreSQL live (`techhub` schema). Mỗi case đi qua semantic planner (không LLM), production validator (`analytics_service._validate_sql`), rồi execute SQL bằng `get_db_session()` y hệt runtime.

| Case | Metric chọn | Scope | Chart | Rows trả về |
|---|---|---|---|---|
| "Tiến độ học của tôi theo từng khóa là bao nhiêu?" | `learner_course_progress` | personal | bar | 4 rows (progress=0 nên value=0.00) |
| "Khóa nào có nhiều học viên đang học nhất?" | `active_enrollments_by_course` | platform | bar | 12 rows |
| "Điểm đánh giá trung bình từng khóa?" | `average_course_rating` | platform | bar | 13 rows |
| "Tôi làm bài tập được bao nhiêu điểm?" | `learner_submissions` | personal | bar | 0 rows (submissions trống) |
| "Tỉ lệ hoàn thành các learning path?" | `learning_path_completion` | platform | bar | 0 rows (path_progress trống) |
| "Doanh thu theo khóa trong tháng này?" (admin) | `revenue_by_course` | platform | bar | 5 rows |
| "Doanh thu theo khóa?" (admin, all-time) | `revenue_by_course` | platform | bar | 11 rows |
| "Doanh thu theo khóa của tôi" (instructor) | `revenue_by_course` | platform (owner-filtered) | bar | 10 rows |
| "Vẽ biểu đồ tiến độ của tôi" | `learner_course_progress` | personal | bar | 4 rows |

Mọi case validate tables-vs-metric registry, scope, `:user_id` placeholder cho personal, `instructor_id = :user_id` cho instructor scope, và chart options shape (`availableChartTypes`, `colorPalette`, `emptyState`, `valueAxisLabel`, `categoryAxisLabel`).

## Mục tiêu

Làm cho chức năng "hỏi số liệu" đáng tin cậy. AI phải trả lời bằng metric nghiệp vụ đã định nghĩa và join PostgreSQL hợp lệ, không để LLM tự nghĩ SQL từ một prompt thiếu schema.

Vector DB không giải quyết join. Cần một semantic layer: map metric, dimension, join, filter, quyền truy cập và SQL template.

## File cần đọc

- `TechHub_BE/techhub.sql`
- `ai-service-fastapi/app/services/analytics_service.py`
- `ai-service-fastapi/app/orchestration/nodes/agents/sql_agent_node.py`
- `ai-service-fastapi/app/orchestration/nodes/agents/viz_agent_node.py`
- `ai-service-fastapi/app/schemas/analytics_contract.py`
- `proxy-client/src/main/java/com/techhub/app/proxyclient/controller/AnalyticsProxyController.java`
- `proxy-client/src/main/java/com/techhub/app/proxyclient/controller/PaymentProxyController.java`

## Gap hiện tại

- `analytics_service.py` có `_allowed_tables` và `_schema_context` viết cứng.
- Allowlist thiếu nhiều bảng có ích: `ratings`, `submissions`, `analytics`, transaction/payment tables.
- `_collect_tables()` dùng regex đơn giản, validation còn nông.
- Fallback SQL chỉ phủ vài case: progress, lesson mix, path completion, enrollment by level.
- LLM planner có thể sinh SQL nghe đúng nhưng không đúng metric nghiệp vụ.

## Metric cần làm trước

| Metric key | Câu hỏi nghiệp vụ | Join cần dùng | Scope |
|---|---|---|---|
| `learner_course_progress` | "Tiến độ học của tôi?" | `enrollments -> courses -> chapters -> lessons -> progress` | personal |
| `course_completion_rate` | "Khóa nào hoàn thành tốt?" | `courses -> enrollments`, cộng progress rollup nếu có lessons | platform/admin |
| `active_enrollments_by_level` | "Người học đang học level nào?" | `enrollments -> courses` | platform/admin |
| `average_course_rating` | "Khóa nào được đánh giá cao?" | `ratings(target_type='COURSE') -> courses` | platform/admin |
| `learner_submissions` | "Kết quả làm bài của tôi?" | `submissions -> exercises -> lessons -> chapters -> courses` | personal |
| `exercise_grade_distribution` | "Điểm bài tập phân bố thế nào?" | `submissions -> exercises -> lessons` | instructor/admin |
| `learning_path_completion` | "Lộ trình nào hoàn thành tốt?" | `path_progress -> learning_paths` | personal/platform |
| `study_time_by_course` | "Thời gian học theo khóa?" | `analytics -> courses` | personal/platform |
| `revenue_by_course` | "Doanh thu theo khóa?" | `transactions -> transaction_items -> courses -> payments` | admin/instructor |

## Task triển khai

1. Tạo metric registry:

   ```text
   app/services/analytics/
     __init__.py
     metric_registry.py
     sql_templates.py
     planner.py
     validator.py
   ```

2. Mỗi metric cần metadata rõ:

   ```python
   MetricDefinition(
       key="learner_course_progress",
       scope="personal",
       required_role=["USER", "ADMIN", "SUPER_ADMIN"],
       tables=["enrollments", "courses", "chapters", "lessons", "progress"],
       default_chart="bar",
       required_params=["user_id"],
       sql_template="...",
   )
   ```

3. Planner phải deterministic trước:

   - Rule/embedding/LLM chỉ chọn metric, dimension, time range.
   - SQL được build từ template, không để LLM tự viết toàn bộ SQL ở bản đầu.

4. Build SQL bằng named params:

   - Dùng `:user_id`, `:course_id`, `:from_date`.
   - Thêm `is_active = 'Y'` cho bảng có cột này.
   - Luôn có `LIMIT`.
   - Scope personal luôn filter bằng trusted user ID.

5. Thay `_schema_context` bằng data contract từ Step 01.

6. Tăng validation:

   - Bảng phải thuộc metric definition.
   - Cột select phải nằm trong allowlist.
   - Không trả PII nếu policy không cho phép.
   - Ưu tiên SQL parser hoặc query builder thay vì regex-only.

7. Giữ contract chart:

   - Vẫn trả `QueryResult`, `ChartSpec`, `ChartOptions`.
   - Chart data phải được build từ rows thật.
   - Data rỗng phải là empty state rõ ràng, không được tạo insight giả.

## Câu hỏi test bắt buộc

- "Tiến độ học của tôi theo từng khóa là bao nhiêu?"
- "Khóa nào có nhiều học viên đang học nhất?"
- "Điểm đánh giá trung bình từng khóa?"
- "Tôi làm bài tập được bao nhiêu điểm?"
- "Tỉ lệ hoàn thành các learning path?"
- "Doanh thu theo khóa trong tháng này?" với admin/instructor.
- "Vẽ biểu đồ tiến độ của tôi" phải reuse metric và trả chart spec.

## Definition of Done

- Analytics dùng metric/join đã biết.
- SQL được build từ template hoặc validate chặt theo metric registry.
- Personal analytics không chạy nếu thiếu trusted user ID.
- Empty data không bị diễn giải thành insight thành công.
- Step này chỉ đạt `REAL_DATA_READY` khi toàn bộ câu hỏi test trả đúng rows và chart metadata.
