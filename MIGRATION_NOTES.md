# TechHub BE — Migration Notes

> Tổng hợp các thay đổi **hạ tầng** (ENV, SQL, Docker) trong phiên refactor này.  
> Chỉ ghi nhận hai luồng thay đổi ảnh hưởng đến vận hành:
>
> 1. **Cloudinary → MinIO** (object storage)
> 2. **Hardcoded endpoint security → DB-driven policy** (phân quyền endpoint)

---

## 1. Cloudinary → MinIO (file-service)

### 1.1 Lý do thay đổi

`file-service` trước đây dùng **Cloudinary SDK** để upload / lưu trữ file.  
Đã chuyển sang **MinIO** (self-hosted S3-compatible) để:

- Kiểm soát dữ liệu nội bộ, không phụ thuộc vendor ngoài
- Hỗ trợ video processing pipeline riêng (thumbnail, metadata qua ffprobe/ffmpeg)
- Giảm chi phí khi scale

---

### 1.2 ENV thay đổi — `file-service`

#### Xóa (không còn dùng)

| Tên biến                | Ghi chú               |
| ----------------------- | --------------------- |
| `CLOUDINARY_CLOUD_NAME` | Cloud name Cloudinary |
| `CLOUDINARY_API_KEY`    | API key Cloudinary    |
| `CLOUDINARY_API_SECRET` | API secret Cloudinary |

#### Thêm mới

| Tên biến                    | Ví dụ / Default                | Ghi chú                                                      |
| --------------------------- | ------------------------------ | ------------------------------------------------------------ |
| `MINIO_ENDPOINT`            | `http://minio:9000`            | URL endpoint MinIO (nội bộ Docker)                           |
| `MINIO_ACCESS_KEY`          | `minioadmin`                   | Access key MinIO (= root user)                               |
| `MINIO_SECRET_KEY`          | `minioadmin`                   | Secret key MinIO (= root password)                           |
| `MINIO_BUCKET`              | `techhub`                      | Bucket chứa tất cả file                                      |
| `MINIO_PUBLIC_URL`          | `https://files.yourdomain.com` | Base URL public để truy cập file (reverse-proxy trước MinIO) |
| `MINIO_SECURE`              | `true`                         | `true` nếu endpoint dùng HTTPS                               |
| `KAFKA_BOOTSTRAP_SERVERS`   | `kafka:9092`                   | Kafka cho video processing pipeline                          |
| `KAFKA_FILE_UPLOADED_TOPIC` | `file-uploaded`                | Topic phát event sau khi upload video                        |

> **Lưu ý `MINIO_PUBLIC_URL`**: đây là URL mà client trình duyệt dùng để tải file, không phải endpoint API của MinIO.  
> Thông thường bạn đặt một Nginx/Traefik reverse-proxy trước port `9000` của MinIO và trỏ domain vào đó.

---

### 1.3 SQL thay đổi — bảng `files` (database `techhub_file`)

#### Cột đổi tên

| Cột cũ | Cột mới     | Kiểu              |
| ------ | ----------- | ----------------- |
| `size` | `file_size` | `BIGINT NOT NULL` |

#### Cột thêm mới

```sql
-- Thêm sau cloudinary_secure_url
storage_provider    VARCHAR(50)  NOT NULL DEFAULT 'MINIO',
bucket_name         VARCHAR(255),
object_key          VARCHAR(1000),
public_url          TEXT,
secure_url          TEXT,
thumbnail_object_key VARCHAR(1000),
thumbnail_url       TEXT,
processing_status   VARCHAR(20)  NOT NULL DEFAULT 'READY'
                    CHECK (processing_status IN ('PENDING', 'READY', 'FAILED')),
processing_error    TEXT,
processed_at        TIMESTAMP WITH TIME ZONE,
```

> Các cột `cloudinary_*` **giữ nguyên** để backward-compatible (field mapping trong entity vẫn tồn tại, nhưng storage thực sự là MinIO).

#### Index thêm mới

```sql
CREATE INDEX idx_files_processing_status ON files(processing_status);
CREATE INDEX idx_files_object_key        ON files(object_key);
```

#### Script ALTER (áp dụng nếu database đã tồn tại)

```sql
-- Đổi tên cột size → file_size
ALTER TABLE files RENAME COLUMN size TO file_size;

-- Thêm các cột MinIO
ALTER TABLE files
    ADD COLUMN IF NOT EXISTS storage_provider    VARCHAR(50)  NOT NULL DEFAULT 'MINIO',
    ADD COLUMN IF NOT EXISTS bucket_name         VARCHAR(255),
    ADD COLUMN IF NOT EXISTS object_key          VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS public_url          TEXT,
    ADD COLUMN IF NOT EXISTS secure_url          TEXT,
    ADD COLUMN IF NOT EXISTS thumbnail_object_key VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS thumbnail_url       TEXT,
    ADD COLUMN IF NOT EXISTS processing_status   VARCHAR(20)  NOT NULL DEFAULT 'READY',
    ADD COLUMN IF NOT EXISTS processing_error    TEXT,
    ADD COLUMN IF NOT EXISTS processed_at        TIMESTAMP WITH TIME ZONE;

-- Thêm constraint check
ALTER TABLE files
    ADD CONSTRAINT chk_processing_status
    CHECK (processing_status IN ('PENDING', 'READY', 'FAILED'));

-- Thêm index
CREATE INDEX IF NOT EXISTS idx_files_processing_status ON files(processing_status);
CREATE INDEX IF NOT EXISTS idx_files_object_key        ON files(object_key);
```

---

### 1.4 Docker / Portainer thay đổi

#### MinIO service thêm vào `portainer-stack.yml`

```yaml
minio:
  image: minio/minio:RELEASE.2025-02-28T09-55-16Z
  container_name: minio
  command: server /data --console-address ":9001"
  environment:
    - MINIO_ROOT_USER=${MINIO_ACCESS_KEY}
    - MINIO_ROOT_PASSWORD=${MINIO_SECRET_KEY}
  volumes:
    - minio_data:/data
  ports:
    - "9002:9000" # S3 API
    - "9001:9001" # MinIO Console UI
  networks:
    - do-an-net
  cpus: "0.30"
  mem_limit: 512m
  restart: always

volumes:
  minio_data:
```

> **Port mapping**: `9002` trên host → `9000` trong container.  
> Console quản lý MinIO truy cập tại `http://host:9001`.

#### Checklist setup MinIO sau khi deploy

1. Truy cập MinIO Console (`http://host:9001`), đăng nhập bằng `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY`
2. Tạo bucket tên bằng giá trị `MINIO_BUCKET` (ví dụ `techhub`)
3. Đặt bucket policy **Public Read** cho object GET (để file có thể truy cập qua URL trực tiếp):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::techhub/*"
    }
  ]
}
```

---

## 2. DB-driven Endpoint Security (proxy-client + user-service)

### 2.1 Lý do thay đổi

Trước đây `proxy-client` hardcode danh sách endpoint PUBLIC/private trực tiếp trong `JwtAuthenticationFilter.java` và `SecurityConfig.java`.  
Đã chuyển sang model **DB-driven**: danh sách endpoint và mức bảo mật lưu trong bảng `endpoint_security_policies`, proxy-client load cache lúc startup và reload qua Kafka event khi có thay đổi.

**Ba mức bảo mật:**
| Level | JWT | RBAC Permission |
|---|---|---|
| `PUBLIC` | Không cần | Không cần |
| `AUTHENTICATED` | Bắt buộc | Không cần |
| `AUTHORIZED` | Bắt buộc | Bắt buộc (mặc định nếu không match policy nào) |

---

### 2.2 ENV thay đổi

**Không có biến ENV mới** — tính năng này dùng lại Kafka và DB đã có sẵn.  
Chỉ cần đảm bảo `user-service` đã có `KAFKA_BOOTSTRAP_SERVERS` (đã tồn tại).

---

### 2.3 SQL thay đổi — database `techhub` (user-service DB)

Chuẩn hiện tại:

- **First-time init**: chỉ cần chạy `techhub.sql` (đã bao gồm `security_level` + `endpoint_security_policies` + seed dữ liệu).
- **Upgrade/incremental**: giữ file migrate riêng `endpoint_security_policies.sql` để áp dụng thay đổi mới trên DB đã tồn tại.

File migrate: [`endpoint_security_policies.sql`](./endpoint_security_policies.sql)

#### Tạo enum type

```sql
CREATE TYPE security_level AS ENUM ('PUBLIC', 'AUTHENTICATED', 'AUTHORIZED');
```

#### Tạo bảng

```sql
CREATE TABLE endpoint_security_policies (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    url_pattern  VARCHAR(500) NOT NULL,         -- Ant-style pattern, VD: /api/users/**
    method       VARCHAR(10)  NOT NULL DEFAULT '*', -- HTTP method hoặc '*' = mọi method
    security_level security_level NOT NULL,
    description  VARCHAR(500),
    is_active    VARCHAR(1)   NOT NULL DEFAULT 'Y' CHECK (is_active IN ('Y', 'N')),
    created      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by   UUID REFERENCES users(id),
    updated_by   UUID REFERENCES users(id)
);

CREATE INDEX idx_esp_security_level ON endpoint_security_policies(security_level);
CREATE INDEX idx_esp_is_active       ON endpoint_security_policies(is_active);
```

#### Seed data — PUBLIC (17 patterns)

```sql
INSERT INTO endpoint_security_policies (url_pattern, method, security_level, description) VALUES
    ('/api/auth/**',                    '*',    'PUBLIC', 'Auth endpoints'),
    ('/api/users',                      'POST', 'PUBLIC', 'User registration'),
    ('/api/users/forgot-password',      '*',    'PUBLIC', 'Forgot password'),
    ('/api/users/reset-password/**',    '*',    'PUBLIC', 'Reset password'),
    ('/api/users/resend-reset-code/**', '*',    'PUBLIC', 'Resend reset code'),
    ('/api/users/public/**',            '*',    'PUBLIC', 'Public user endpoints'),
    ('/actuator/**',                    '*',    'PUBLIC', 'Spring Actuator'),
    ('/swagger-ui/**',                  '*',    'PUBLIC', 'Swagger UI'),
    ('/v3/api-docs/**',                 '*',    'PUBLIC', 'OpenAPI docs'),
    ('/oauth2/**',                      '*',    'PUBLIC', 'OAuth2 flow'),
    ('/api/files/**',                   '*',    'PUBLIC', 'File access'),
    ('/api/folders/**',                 '*',    'PUBLIC', 'Folder access'),
    ('/api/file-usage/**',              '*',    'PUBLIC', 'File usage stats'),
    ('/api/ai/chat/stream/**',          '*',    'PUBLIC', 'AI Chat SSE streaming'),
    ('/api/payment/**',                 '*',    'PUBLIC', 'Payment callbacks'),
    ('/api/payments/**',                '*',    'PUBLIC', 'Payment queries'),
    ('/api/transactions/**',            '*',    'PUBLIC', 'Transaction queries');
```

#### Seed data — AUTHENTICATED (3 patterns)

```sql
INSERT INTO endpoint_security_policies (url_pattern, method, security_level, description) VALUES
    ('/api/users/profile',         '*',    'AUTHENTICATED', 'Current user profile'),
    ('/api/users/change-password', 'POST', 'AUTHENTICATED', 'Change own password'),
    ('/api/users/{userId}',        'GET',  'AUTHENTICATED', 'View user by ID');
```

> Mọi endpoint **không khớp** bất kỳ pattern nào sẽ tự động thuộc level **`AUTHORIZED`** (JWT + RBAC).

---

### 2.4 Quản lý policy sau khi deploy

Admin có thể CRUD policy qua API (JWT + role ADMIN):

| Method   | URL                                          | Mô tả                                   |
| -------- | -------------------------------------------- | --------------------------------------- |
| `GET`    | `/api/internal/endpoint-security-policies`   | Lấy danh sách (proxy-client gọi nội bộ) |
| `POST`   | `/api/admin/endpoint-security-policies`      | Tạo policy mới                          |
| `PUT`    | `/api/admin/endpoint-security-policies/{id}` | Cập nhật policy                         |
| `DELETE` | `/api/admin/endpoint-security-policies/{id}` | Xóa mềm policy                          |

Sau mỗi thay đổi, `user-service` tự động publish Kafka event → `proxy-client` reload cache mà không cần restart.

---

## 3. Checklist triển khai

### Thứ tự thực hiện khi lên môi trường mới

```
1. [ ] Chạy SQL trên DB user-service (techhub):
  - techhub.sql  (baseline đầy đủ, đã gồm endpoint security policies)

2. [ ] Chạy SQL trên DB file-service (techhub_file):
  - file-service/schema.sql

3. [ ] Bổ sung ENV vào Portainer / .env:
       MINIO_ENDPOINT=http://minio:9000
       MINIO_ACCESS_KEY=...
       MINIO_SECRET_KEY=...
       MINIO_BUCKET=techhub
       MINIO_PUBLIC_URL=https://files.yourdomain.com
       MINIO_SECURE=false          # true nếu endpoint HTTPS
       KAFKA_FILE_UPLOADED_TOPIC=file-uploaded

4. [ ] Deploy stack portainer-stack.yml (đã có service minio + volume minio_data)

5. [ ] Vào MinIO Console (port 9001), tạo bucket và set Public Read policy

6. [ ] Restart file-service sau khi MinIO sẵn sàng

7. [ ] Restart proxy-client — sẽ tự load endpoint security policies từ user-service lúc startup
```

### Thứ tự khi developer đang code tiếp (sau baseline)

```
1. [ ] Giữ techhub.sql làm baseline chuẩn để onboard môi trường mới
2. [ ] Mỗi thay đổi schema mới tạo thêm file migrate tương ứng
3. [ ] Khi pull code mới, chỉ chạy các file migrate chưa áp dụng
4. [ ] Không chỉnh lịch sử migrate đã chạy ở môi trường shared
```

### Thứ tự khi upgrade môi trường đang chạy (có data cũ)

```
1. [ ] Backup DB trước
2. [ ] Chạy ALTER TABLE (không DROP) cho bảng files
3. [ ] Chạy endpoint_security_policies.sql
4. [ ] Thêm ENV mới, bỏ ENV Cloudinary cũ
5. [ ] Deploy minio service
6. [ ] Rolling restart file-service, proxy-client
7. [ ] Kiểm tra file upload mới → MinIO
       (file cũ trên Cloudinary vẫn accessible qua cloudinary_url cũ trong DB)
```

---

## 4. Tóm tắt nhanh

| Thay đổi                    | Service bị ảnh hưởng           | ENV mới                | SQL mới                                   |
| --------------------------- | ------------------------------ | ---------------------- | ----------------------------------------- |
| Cloudinary → MinIO          | `file-service`                 | 8 biến (xóa 3, thêm 8) | ALTER `files` + thêm 10 cột + 2 index     |
| MinIO container             | Docker infra                   | —                      | —                                         |
| DB-driven endpoint security | `user-service`, `proxy-client` | Không                  | Tạo mới bảng `endpoint_security_policies` |

---

## 5. AI Provider Switching (OpenAI ↔ Gemini) — ai-service

### 5.1 Mục tiêu

Cho phép chuyển provider AI theo ENV/config mà **không cần sửa lại code service**.

### 5.2 Các thay đổi chính

- Thêm interface chung `AiGateway`:
  - `generateStructuredJson(...)`
  - `generateStreamingResponse(...)`
  - `generateStreamingResponseWithHistory(...)`
- `OpenAiGateway` và `GeminiGateway` cùng implement `AiGateway`
- Thêm `SwitchableAiGateway` (`@Primary`) để route theo `ai.provider`
  - `openai` → dùng `OpenAiGateway`
  - `gemini` (default) → dùng `GeminiGateway`
- Các service gọi AI đã chuyển sang inject `SwitchableAiGateway`:
  - `ChatStreamingService`
  - `ChatOrchestrationServiceImpl`
  - `LearningPathAiServiceImpl`
  - `AiRecommendationServiceImpl`
  - `AiExerciseServiceImpl`
- `EmbeddingService` cũng switch provider theo `ai.provider`
  - OpenAI: `/embeddings`
  - Gemini: `:embedContent` / `:batchEmbedContents`

### 5.3 ENV mới/quan trọng

```bash
AI_PROVIDER=gemini   # hoặc openai
```

Trong `application.yml`:

```yaml
ai:
  provider: ${AI_PROVIDER:gemini}

chatbot:
  embedding:
    model-id: ${CHATBOT_EMBEDDING_MODEL:}
    openai-model-id: ${CHATBOT_OPENAI_EMBEDDING_MODEL:text-embedding-3-small}
    gemini-model-id: ${CHATBOT_GEMINI_EMBEDDING_MODEL:text-embedding-004}
```

### 5.4 Runtime Admin API (không cần restart)

Đã bổ sung API admin để đổi provider ngay khi hệ thống đang chạy:

- `GET /api/ai/admin/provider-config` → lấy provider hiện tại
- `POST /api/ai/admin/provider-config` với body:

```json
{
  "provider": "openai",
  "chatModel": "gpt-4o-mini"
}
```

Giá trị hợp lệ: `openai`, `gemini`.

Model hiện hỗ trợ:

- OpenAI: `gpt-4o-mini`, `gpt-4.1-mini`, `gpt-4.1`
- Gemini: `gemini-2.5-flash`, `gemini-2.5-pro`

---

## 6. Verification Snapshot (2026-03-16)

### 6.1 BE compile smoke test

- ✅ `file-service`: `mvn -q compile -DskipTests` → `FILE_OK`
- ✅ `proxy-client`: `mvn -q compile -DskipTests` → `PROXY_OK`
- ✅ `user-service`: `mvn -q compile -DskipTests` → `USER_OK`
- ✅ `ai-service`: `AI_PROVIDER=openai mvn -q compile -DskipTests` → `AI_OPENAI_OK`

### 6.2 FE status

- ✅ Đã có UI admin trên `manage/dashboard` để chọn provider (`Gemini` / `OpenAI`) **và model theo provider**, lưu runtime.
- ✅ FE đã thêm proxy route: `/app/api/proxy/ai/admin/provider-config` (GET/POST).
- ✅ FE API hooks đã hỗ trợ đọc/đổi provider config.
