# Step 10 - End-to-End Observability Và Release

## Trạng thái triển khai

`REAL_DATA_READY` - cập nhật 2026-05-16. Step 10 đã có release infrastructure đầy đủ:

- `release_readiness_service.snapshot()` aggregate signal từ Qdrant feature readiness (Step 05), publish counters & timestamps (observability), DB audit của `ai_generation_tasks`, và data-contract validation (Step 01).
- Admin endpoint `GET /api/ai/admin/release-readiness` trả snapshot này, tag mỗi capability bằng status enum `CODE_WIRED | FALLBACK_DEMO | REAL_DATA_READY | E2E_VERIFIED | DEGRADED | FAILED` với reason giải thích.
- Publisher Step 06/07 ghi `record_publish_event(kind, status)` mỗi outcome → counter `publish:learning_path:*` / `publish:exercise:*` + timestamp `publish:last_success:*` / `publish:last_failure:*` trong observability snapshot.
- `scripts/e2e/run_release_smoke.py` drive lần lượt 5 file test Step 03/04/05/06/07, snapshot readiness lần cuối, ghi JSON report `release_smoke_report.json`. Lần chạy mới nhất (2026-05-16): 5/5 step PASS, 63 case tổng cộng.

E2E `tests/test_step10_release_observability.py`: 8/8 PASS (4 observability case + 4 readiness case + admin endpoint qua FastAPI TestClient với trusted headers `proxy-client`/`ADMIN`).

`E2E_VERIFIED` cho từng capability tự nâng lên khi:
- có row `PUBLISHED` thật trong `ai_generation_tasks` (cần `LEARNING_PATH_SERVICE_BASE_URL` / `COURSE_SERVICE_BASE_URL` cấu hình + Java service running),
- Qdrant không còn collection degraded (cần seed `lessons` để `lesson_qa` collection có points).

## Mục tiêu

Chứng minh AI Service hoạt động như một business layer thật của TechHub, không chỉ là tập hợp endpoint. Step này thêm bằng chứng, quality gate và admin visibility để biết chức năng nào thật sự sẵn sàng.

## File cần đọc

- `ai-service-fastapi/app/services/observability_service.py`
- `ai-service-fastapi/app/services/langfuse_service.py`
- `ai-service-fastapi/app/api/routes/admin.py`
- `ai-service-fastapi/app/services/runtime_request_context.py`
- `ai-service-fastapi/app/services/provider_config.py`
- `ai-service-fastapi/app/services/llm_gateway.py`
- `proxy-client/src/main/java/com/techhub/app/proxyclient/controller/AiProxyController.java`
- `proxy-client/src/main/java/com/techhub/app/proxyclient/controller/AiStreamingProxyController.java`

## Gap hiện tại

- Runtime stats có tồn tại nhưng chưa chứng minh answer đúng.
- Request có thể completed dù đang dùng fallback.
- Langfuse trace hỗ trợ debug, nhưng chưa có release checklist gắn với kịch bản nghiệp vụ.
- Status cũ xem source wiring là hoàn thành.

## Observability field bắt buộc

Mỗi request AI lớn nên record:

```text
requestId
userId hoặc user reference đã ẩn danh
intent
capability
provider
model
token usage
latency
data sources used
fallback used
missing data
Qdrant collection và point count nếu liên quan
SQL metric key và tables nếu liên quan
draft task ID nếu liên quan
publish target ID nếu liên quan
error category
```

## Kịch bản release bắt buộc

1. Chat recommendation:
   - login learner,
   - hỏi nên học gì tiếp,
   - response dùng profile/history thật,
   - citations/signals trỏ course thật.

2. Personal analytics:
   - hỏi tiến độ học,
   - SQL dùng trusted user ID,
   - rows khớp DB,
   - chart spec khớp rows.

3. Platform/admin analytics:
   - login admin,
   - hỏi metric toàn hệ thống,
   - query respect allowlist và PII policy.

4. Exercise publish:
   - generate draft cho lesson thật,
   - approve/publish,
   - Course Service trả created exercises.

5. Learning path publish:
   - generate draft,
   - approve/publish,
   - Learning Path Service trả created path.

6. File chat:
   - upload file,
   - consume event,
   - index chunks,
   - hỏi về file,
   - response cite file chunks.

7. Kafka freshness:
   - update course/lesson/rating,
   - consume event,
   - Qdrant point/profile đổi.

8. Provider fallback:
   - giả lập provider lỗi,
   - response degraded rõ,
   - admin stats thấy fallback.

## Task triển khai

1. Thêm status runtime:

   ```text
   code_wired
   fallback_demo
   real_data_ready
   e2e_verified
   degraded
   failed
   ```

2. Thêm smoke/e2e scripts:

   ```text
   TechHub_BE/ai-service-fastapi/scripts/e2e/
   ```

   Script cần:

   - gọi qua proxy route, không chỉ FastAPI trực tiếp,
   - dùng test users,
   - verify DB side effects,
   - verify Qdrant counts,
   - lưu response snippet.

3. Admin quality dashboard:

   - feature readiness,
   - last successful real-data run,
   - last fallback run,
   - collection readiness,
   - publish success/failure counts,
   - top error categories.

4. Cập nhật release documentation:

   - `IMPLEMENTATION_STATUS.md` chỉ đổi status khi có evidence.
   - Mỗi step phải có command, route, request sample, result summary.

5. Retention/cleanup:

   - Langfuse traces retention,
   - runtime stats retention,
   - AI generation task archive policy,
   - file chunk delete/reindex policy khi file bị xóa.

## Definition of Done

- Có bằng chứng cho từng kịch bản nghiệp vụ, không chỉ compile success.
- Admin biết request dùng real data hay fallback.
- Mỗi draft publish trace được từ AI task sang domain record.
- Step này chỉ đạt `E2E_VERIFIED` khi toàn bộ release scenario pass qua proxy-client với data gần production.
