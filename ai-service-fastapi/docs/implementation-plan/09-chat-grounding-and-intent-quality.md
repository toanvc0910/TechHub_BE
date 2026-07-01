# Step 09 - Chat Grounding Và Intent Quality

## Trạng thái triển khai

`REAL_DATA_READY` - cập nhật 2026-05-16. E2E `tests/test_step09_chat_grounding_and_intent_quality.py`, **54/54 case PASS** (3 tier-0 + 30 intent prompts + 7 entity + 12 grounding + 2 HITL). Chạy lại:

```bash
python -m tests.test_step09_chat_grounding_and_intent_quality
```

## Thay đổi chính

Entity extraction (`app/orchestration/nodes/entity_extraction_node.py`):

- **`chart_type`** mới: matching word-boundary cho `line` / `pie` / `bar` qua phrase `bieu do duong/cot/tron`, `line/bar/pie chart`, hoặc standalone token (padded space). Không false-positive trên "trong tháng" (đã regression-guarded).
- **`file_scope`** mới: `active` khi câu chứa `file nay`/`tai lieu nay`/`tep nay`/`document nay`; `any` cho file reference chung.
- **`scope`** mở rộng: analytics tokens nay bao gồm `tien do/hoan thanh/completion/progress/lesson/bai hoc`, nên "tiến độ học của tôi" được scope `personal` chính xác (trước đây bị None vì `tien do` không nằm trong analytics token list).

Quality flags (`app/orchestration/state/orchestrator_state.py` + `app/orchestration/nodes/response_compose_node.py`):

- Thêm fields vào `OrchestratorState`: `grounded`, `data_sources`, `fallback_used`, `missing_data`, `grounding_confidence`.
- Helper `compute_quality_flags(state, hitl_active)` áp rule riêng cho từng intent:
  - **data_query/visualization**: grounded khi `query_result.rows` non-empty; `executionMode=deterministic_fallback` → `fallback_used=True` + source `fallback_sql`. Empty rows → `missing_data=[query_rows]`.
  - **knowledge**: grounded khi citations có `kind ∈ {course, lesson}`. Không có citation → `missing_data=[rag_documents]`.
  - **file_analysis**: grounded khi citation có `kind ∈ {file, user_file, session_file}` hoặc fresh file context. Thiếu → `missing_data=[file_chunks]`.
  - **recommendation**: grounded khi citations có `kind=course`. Thiếu → `missing_data=[course_candidates]`.
  - **conversation/clarify**: grounded=True (không cần data ngoài).
  - **HITL clarify active**: grounded=True, `missing_data=[awaiting_user_clarification]`.
- `response_compose_node` chạy `compute_quality_flags` và đưa vào output dict; trace `grounded/dataSources/fallbackUsed/missingData` vào execution trace cho audit.

## Mục tiêu

Chat assistant phải route đúng nghiệp vụ và trả lời từ context thật của TechHub. Khi thiếu dữ liệu, chat phải nói rõ hoặc hỏi lại, không được trả lời chung chung như đã có dữ liệu.

## File cần đọc

- `ai-service-fastapi/app/orchestration/graph/chat_orchestrator_graph.py`
- `ai-service-fastapi/app/orchestration/router/intent_router.py`
- `ai-service-fastapi/app/orchestration/nodes/context_load_node.py`
- `ai-service-fastapi/app/orchestration/nodes/hitl_gate_node.py`
- `ai-service-fastapi/app/orchestration/nodes/entity_extraction_node.py`
- `ai-service-fastapi/app/orchestration/nodes/agents`
- `ai-service-fastapi/app/services/chat_service.py`
- `ai-service-fastapi/app/schemas/chat.py`

## Gap hiện tại

- Intent detection có rule/embedding nhưng còn có thể miss nhiều câu tiếng Việt tự nhiên.
- Entity extraction còn hẹp cho metric, course reference, file reference, time range.
- Context load phụ thuộc catalog/profile/rating/history; các signal này cần các step trước sửa.
- Generic LLM answer có thể che mất việc RAG/file/analytics không có data.
- Streaming emit event nhưng quality gate chưa đủ chặt để đảm bảo grounded answer.

## Intent outcome bắt buộc

| User request | Intent đúng | Data source |
|---|---|---|
| "Tôi nên học gì tiếp?" | recommendation | PostgreSQL profile/history + Qdrant courses/profiles |
| "Tóm tắt khóa React này" | knowledge | Qdrant course/lesson retrieval |
| "Tiến độ học của tôi thế nào?" | data_query hoặc visualization | Analytics semantic layer |
| "Vẽ biểu đồ số học viên theo level" | visualization | SQL rows + chart spec |
| "Tóm tắt file tôi vừa tải" | file_analysis | file chunks trong Qdrant |
| "Tạo bài tập cho lesson này" | exercise generation route | Course/lesson context |
| "Tạo lộ trình học frontend" | learning path route hoặc recommendation flow | catalog/profile/path data |

## Task triển khai

1. Mở rộng intent test set:

   - tiếng Việt có dấu,
   - tiếng Việt không dấu,
   - câu trộn thuật ngữ như chart, SQL, file, lesson, course,
   - follow-up ngắn.

2. Thêm confidence gate:

   - Intent confidence thấp và thiếu data thì hỏi clarify.
   - Analytics thiếu scope/user identity thì hỏi lại hoặc default an toàn.
   - File request không có active/indexed file thì báo chưa có file.

3. Cải thiện entity extraction:

   - course title/course ID,
   - lesson ID,
   - file ID/active file,
   - metric key,
   - time range,
   - scope personal/platform/instructor,
   - chart type.

4. Grounding contract theo agent:

   | Agent | Bắt buộc có |
   |---|---|
   | RAG | retrieved docs và citations |
   | SQL | metric key, SQL, row count, tables |
   | Viz | chart spec từ query result |
   | File | file citations hoặc not-indexed state |
   | Recommendation | course candidates và signals |
   | Conversation | không bịa course/file/stat |

5. Thêm quality flags:

   ```text
   grounded=true/false
   dataSources=[postgres,qdrant,file,llm]
   fallbackUsed=true/false
   missingData=[lessons,progress,file_chunks]
   confidence=...
   ```

6. SSE metadata:

   - `planning_step` khi có ý nghĩa,
   - `artifact` cho chart/structured data,
   - `citation` cho RAG/file,
   - `hitl_question` cho clarify,
   - `done` có quality flags.

## Kiểm chứng (2026-05-16)

E2E `tests/test_step09_chat_grounding_and_intent_quality.py`, **54/54 PASS**:

**Tier-0 routing (3 case):**

| Case | Kết quả |
|---|---|
| `has_fresh_file_context=True` + empty utterance | route file_analysis, confidence=0.98, rule `tier0:file-context` |
| active analysis + chart swap utterance ("đổi sang biểu đồ đường") | visualization, rule `tier0:active-analysis-refine:chart` |
| active analysis + filter utterance ("chỉ lấy beginner") | data_query, rule `tier0:active-analysis-refine:filter` |

**Intent classification (30 prompts):**

- 5 conversation, 5 recommendation, 5 knowledge, 5 data_query, 5 visualization, 5 file_analysis
- Trộn tiếng Việt có/không dấu + Anh mixed (`recommend`, `report`, `dashboard`, `who is`, `how`)
- Tất cả hit tier-1 regex deterministic (`confidence=0.86`), test không phụ thuộc LLM/embedding network

**Entity extraction (7 case):**

| Case | Kết quả |
|---|---|
| chart_type=line ("đổi sang biểu đồ đường") | line |
| regression guard: "trong tháng này" KHÔNG match pie/tron | chart_type=None |
| chart_type=pie ("biểu đồ tròn") | pie |
| "tiến độ học của tôi theo từng khóa" | metric=progress, scope=personal |
| "thống kê doanh thu tháng này" | time_range=this_month, scope=platform |
| "tóm tắt tài liệu này" | file_scope=active |
| UUID trong câu | reference_id extracted |

**Grounding flags (12 case):**

| Case | grounded | data_sources | fallback_used | missing |
|---|---|---|---|---|
| data_query có rows | True | postgres | False | [] |
| data_query rows=0 | False | postgres, llm | False | [query_rows] |
| data_query deterministic_fallback | True | postgres, fallback_sql | True | [] |
| visualization có chart_spec | True | postgres, chart | False | [] |
| knowledge có course citation | True | qdrant | False | [] |
| knowledge thiếu citation | False | llm | False | [rag_documents] |
| file_analysis có file citation | True | file | False | [] |
| file_analysis thiếu citation+context | False | llm | False | [file_chunks] |
| recommendation có course citations | True | postgres, qdrant | False | [] |
| recommendation thiếu citations | False | llm | False | [course_candidates] |
| conversation | True | llm | False | [] |
| HITL clarify active | True | llm | False | [awaiting_user_clarification] |

**HITL gate (2 case):**

| Case | Kết quả |
|---|---|
| confidence=0.1 < threshold | hitl_clarify_active=True, intent=`clarify`, sub_intent=`low_confidence` |
| confidence=0.9 + non-recommendation | passes through, không trigger clarify |

Còn lại để full E2E qua chat thật (không block REAL_DATA_READY):

- Chạy chat orchestrator full graph với LLM gateway thật cho mỗi prompt và assert response text bám citations.
- Drive 5+ chart follow-up scenarios qua chat_service với streaming SSE để verify `planning_step/citation/done` events có quality flags.

## Definition of Done

- Chat routing được test bằng câu tiếng Việt gần thực tế sản phẩm.
- Mọi grounded answer expose data source.
- Thiếu data thì hỏi lại hoặc từ chối có ích.
- Step này chỉ đạt `REAL_DATA_READY` khi test chứng minh route đúng và output grounded.
