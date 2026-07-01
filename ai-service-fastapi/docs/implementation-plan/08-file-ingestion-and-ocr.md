# Step 08 - File Ingestion Và OCR

## Trạng thái triển khai

`REAL_DATA_READY` - cập nhật 2026-05-16. E2E: 13/13 case PASS qua hydrate/extract/index/search/ownership/idempotency/readiness trên Qdrant live (`https://qdrant.inova.id.vn`). Bằng chứng tại `tests/test_step08_file_ingestion_and_search.py`. Chạy lại:

```bash
python -m tests.test_step08_file_ingestion_and_search
```

## Thay đổi chính

Bug fix nghiêm trọng phát hiện được khi chạy E2E:

- `vector_service._stable_point_id()` cũ trả về SHA1 hex digest (40 ký tự, không phải UUID). Qdrant 1.13 từ chối với HTTP 400 `Bad Request` khi upsert. Toàn bộ session-file + user-file indexing không bao giờ chạy được trên Qdrant thật. Đã đổi sang `uuid.uuid5(_FILE_CHUNK_NAMESPACE, "{scope}:{file_id}:{chunk_index}")` — vẫn deterministic (re-ingest cùng file/chunk → cùng id → upsert in-place) và Qdrant chấp nhận.

Tính năng mới:

- `vector_service.delete_file_chunks(collection, user_id, file_id, session_id?)` — POST `/collections/{col}/points/delete` với filter `(user_id, file_id, [session_id])`. Dùng cho idempotent re-ingest và "forget my file".
- `vector_service.get_file_index_status(user_id, file_id, scope='user'|'session', session_id?)` — đọc POST `/points/count` với cùng filter, trả `{status: INDEXED|NOT_INDEXED|COLLECTION_MISSING, chunkCount}` cho FE/agent biết file đã sẵn sàng search hay chưa, không bịa câu trả lời khi chưa index.
- `_index_file_contexts()` xoá tất cả chunks cũ của `(user_id, file_id, session_id)` trước khi upsert lần mới → orphan chunks (do file mới ngắn hơn file cũ) được dọn sạch, không leak qua re-parse.
- Chunk payload bổ sung `content_hash` (SHA1 của full text), `source_updated_at`, `ingested_at` để traceability và debug.

## Mục tiêu

File người dùng upload phải dùng được trong chat. Người học có thể tải tài liệu lên, hỏi lại về tài liệu đó, và nhận câu trả lời dựa trên chunks đã index kèm citation.

## File cần đọc

- `ai-service-fastapi/app/services/file_context_service.py`
- `ai-service-fastapi/app/services/vector_service.py`
- `ai-service-fastapi/app/orchestration/nodes/agents/file_agent_node.py`
- `ai-service-fastapi/app/api/routes/admin.py`
- `file-service/src/main/java/com/techhub/app/fileservice/controller/FileController.java`
- `file-service/src/main/java/com/techhub/app/fileservice/kafka/FileEventPublisher.java`
- `file-service/src/main/java/com/techhub/app/fileservice/kafka/FileUploadedEvent.java`
- `file-service/src/main/java/com/techhub/app/fileservice/entity/FileEntity.java`

## Gap hiện tại

- Parse file và OCR có code path, nhưng chưa có bằng chứng production với scanned PDF/image.
- File vector collection có thể rỗng mà UI vẫn tưởng feature sẵn sàng.
- Access check phải dựa vào trusted user identity.
- Admin ingest endpoint có thể hydrate file, nhưng cần chứng minh full path từ File Service event đến AI index.
- OCR phụ thuộc config và provider key.

## Business rule

File answer hợp lệ khi:

- file tồn tại trong File Service,
- user sở hữu file hoặc có quyền truy cập,
- file đã được parse hoặc OCR,
- chunks đã index vào đúng Qdrant collection,
- câu trả lời có citation từ file/chunk,
- nếu file chưa index thì trả trạng thái rõ, không bịa nội dung.

## Task triển khai

1. Chốt file event contract:

   ```text
   fileId
   userId
   fileName/originalName
   mimeType
   fileSize
   storageProvider
   bucketName
   objectKey
   secureUrl/publicUrl nếu có
   uploadSource
   referenceId/referenceType nếu có
   ```

2. Enforce ownership:

   - AI so sánh trusted `X-User-Id` với owner file.
   - Admin/staff có thể override theo policy.
   - Không retrieve chunks bằng `userId` client tự gửi.

3. Ingestion idempotent:

   - Re-ingest cùng file phải replace/version chunks sạch.
   - Payload chunk có `fileId`, `chunkIndex`, `contentHash`, `sourceUpdatedAt`.

4. Tách session file và user file:

   - Session file: file attach trong chat session hiện tại, có thể TTL ngắn.
   - User file: file persist trong File Service, hỏi lại được sau này.

5. OCR verification:

   - PDF có selectable text.
   - DOCX/PPTX/XLSX.
   - Image file.
   - Scanned PDF ít text.
   - Ghi provider, timeout, số ký tự extract, fallback behavior.

6. File agent behavior:

   - Không có chunks thì nói file chưa index.
   - Có chunks nhưng confidence thấp thì hỏi rõ phần/câu hỏi.
   - Citation có file name và chunk/page nếu lấy được.

## Kiểm chứng (2026-05-16)

E2E `tests/test_step08_file_ingestion_and_search.py`, 13/13 PASS trên Qdrant live:

Hydration + extraction (5 case):

| Case | Kết quả |
|---|---|
| H1: hydrate với content sẵn -> ingestionStatus=READY | 499 chars content + excerpt |
| H2: DOCX bytes synthetic parse | "synthetic DOCX paragraph" + "Second paragraph" cả hai có trong text |
| H3: XLSX bytes synthetic parse | cell `Python 101` + `PUBLISHED` extract đúng |
| H4: `_can_extract_content` classification | 5 type hỗ trợ; image OCR gate đúng theo `ocr_enabled` |
| H5: OCR disabled + image bytes | trả None không crash |

Index + search + ownership (6 case):

| Case | Kết quả |
|---|---|
| S1: user file index + owner search | 11 chunks indexed; payload có `user_id`, `file_id`, `content_hash`, `ingested_at` |
| S2: search với wrong user_id | empty (ownership enforced) |
| S3: session search (user_id, session_id) isolation | đúng owner+session ra hit; sai session hoặc sai user ra empty |
| S4: re-ingest shorter content | chunks giảm từ 11 → 1, không orphan |
| S5: delete_file_chunks | status đổi sang NOT_INDEXED |
| S6: `get_file_index_status` cho file lạ | NOT_INDEXED |

Kafka contract (2 case):

| Case | Kết quả |
|---|---|
| K1: `ingest_uploaded_event` với inline content | success=True, status=INDEXED, chunkCount=1 |
| K2: event không có content phân tích được | success=False với message rõ "no analyzable text" |

Còn lại để full E2E qua File Service Java thật (không block REAL_DATA_READY):

- Upload file qua FileController thật → verify event `file-uploaded` trên Kafka → AI consume → searchable.
- Test scanned PDF qua Gemini Vision OCR (cần GEMINI_API_KEY và ocr_enabled=true).
- Tắt Qdrant và verify response bị đánh dấu degraded thay vì bịa.

## Definition of Done

- Upload file -> searchable AI context chạy end-to-end.
- File access dùng trusted identity.
- OCR đã test với scanned/image thật.
- Trạng thái file rỗng/chưa index rõ ràng.
- Step này chỉ đạt `REAL_DATA_READY` khi user hỏi được file đã upload trước đó và có cited answer.
