# Step 02 - Trusted Identity Và Runtime Policy

## Trạng thái triển khai

`REAL_DATA_READY` - đã triển khai ngày 2026-05-14.

Step này đã chuyển AI Service từ kiểu "tin `userId` trong body" sang kiểu "chỉ dùng danh tính do proxy đã xác thực". Phần còn lại chưa gọi là `E2E_VERIFIED` vì chưa chạy qua token thật trên frontend, nhưng code path và policy đã được nối đúng.

## Kết quả đã làm

- Tạo `app/api/dependencies/trusted_context.py` để đọc `X-User-Id`, `X-User-Email`, `X-User-Roles`, `X-Request-Source`.
- `chat/messages`, `chat/stream`, `chat/sessions`, `recommendations`, `learning-paths` đều kiểm tra `userId` gửi lên phải khớp `X-User-Id`.
- `chat/sessions/{session_id}/messages` giờ lọc theo chủ session, không chỉ theo `session_id`.
- `drafts/*`, `exercises/generate`, `admin/*` có gate role server-side.
- `AiStreamingProxyController` đã forward trusted headers cho SSE giống Feign.
- `techhub.sql` và migration `V4__normalize_rbac_permissions.sql` đã đổi stream chat sang `AUTHORIZED`, thêm permission `AI_CHAT_STREAM` và `AI_CHAT_STREAM_SIMPLE`.
- `runtime_policy_service.py` không còn tin `userRole`, `permissions`, `tenantPolicy`, `piiAccess`, `sqlMaxRows`, `allowDataQuery`, `modelOverride` do client tự gửi. `modelOverride` chỉ được giữ khi role trusted là admin/staff.
- Sau Step 03, learner được đi vào analytics cá nhân nhưng không được tự bật platform analytics, PII hoặc model override bằng body.

## Kiểm chứng đã chạy

```bash
python -m compileall app
mvn -pl proxy-client -am -DskipTests compile
```

Smoke test Python đã kiểm tra:

- trusted user khớp thì qua,
- body `userId` khác `X-User-Id` bị reject `403`,
- body giả `SUPER_ADMIN`, `AI_PII_ACCESS`, `tenantPolicy`, `modelOverride` bị loại với user thường,
- runtime policy trả role thật là `LEARNER`, không nhận PII/model override từ body; platform analytics được chặn ở semantic layer của Step 03.

## Mục tiêu

AI Service phải dùng danh tính người dùng do server xác thực, không tin `userId` client tự gửi trong body/query. Đây là điều kiện bắt buộc trước khi mở personal analytics, recommendation history, file search, chat session history và draft approval.

## File cần đọc

- `proxy-client/src/main/java/com/techhub/app/proxyclient/security/JwtAuthenticationFilter.java`
- `proxy-client/src/main/java/com/techhub/app/proxyclient/config/FeignConfig.java`
- `proxy-client/src/main/java/com/techhub/app/proxyclient/controller/AiStreamingProxyController.java`
- `proxy-client/src/main/java/com/techhub/app/proxyclient/controller/AiProxyController.java`
- `TechHub_BE/techhub.sql`
- `ai-service-fastapi/app/api/routes/chat.py`
- `ai-service-fastapi/app/services/runtime_policy_service.py`
- `ai-service-fastapi/app/schemas/chat.py`

## Gap hiện tại

- Feign có thể forward `X-User-Id`, `X-User-Email`, `X-User-Roles`, nhưng FastAPI route chưa enforce đồng nhất.
- SSE dùng `AiStreamingProxyController` với WebClient, hiện chỉ forward `X-Request-Source`.
- Trong `techhub.sql`, `/api/ai/chat/stream/**` đang là `PUBLIC`, không phù hợp nếu stream có dữ liệu cá nhân.
- `runtime_policy_service` đọc role/permission từ request body `context`, dễ bị client tác động nếu không normalize từ proxy.
- Chat sessions/history nhận `userId` từ query mà chưa chứng minh khớp authenticated user.

## Task triển khai

1. Sửa streaming proxy:

   - Thêm `HttpServletRequest` vào `streamChat`.
   - Lấy `userId`, `userEmail`, `userRoles` từ request attributes do `JwtAuthenticationFilter` set.
   - Forward các header:

     ```text
     X-User-Id
     X-User-Email
     X-User-Roles
     X-Request-Source
     ```

2. Sửa endpoint policy:

   - Không để `/api/ai/chat/stream/**` là public nếu stream đọc dữ liệu cá nhân.
   - Chuyển sang `AUTHENTICATED` hoặc permission-based policy.
   - Chỉ giữ public cho health/simple endpoint không đọc user data.

3. Tạo dependency đọc trusted context trong FastAPI:

   ```text
   app/api/dependencies/trusted_context.py
   ```

   Dependency này đọc:

   ```text
   X-User-Id
   X-User-Email
   X-User-Roles
   X-Request-Source
   ```

4. Enforce user match:

   - Nếu body có `userId`, phải khớp `X-User-Id`.
   - Nếu thiếu trusted user ở endpoint personal, trả `401` hoặc `403`.
   - Admin endpoint phải kiểm role admin/staff hoặc permission tương ứng.

5. Normalize runtime policy server-side:

   - Role/permission lấy từ trusted headers hoặc permission service.
   - `context` trong body chỉ là preference, không phải authority.
   - Bỏ qua `modelOverride`, `allowDataQuery`, `piiAccess`, role giả trong body nếu user không đủ quyền.

6. Cập nhật route:

   - `chat/messages`
   - `chat/stream`
   - `chat/sessions`
   - `recommendations/history`
   - `recommendations/realtime`
   - `drafts/*`
   - `admin/*`

## Kiểm chứng

- User A không xem được chat sessions của User B.
- User A đổi body `userId` thành User B thì personal analytics bị reject.
- Stream route nhận được `X-User-Id`.
- Normal user không đổi được provider config.

## Definition of Done

- Mọi câu trả lời cá nhân đều dùng trusted user identity.
- SSE và non-SSE có cùng rule identity.
- `runtime_policy_service` không còn tin role/permission từ body.
- Endpoint policy trong `techhub.sql` khớp behavior thật.
- Step này chỉ đạt `REAL_DATA_READY` khi request sai `userId` bị reject.
