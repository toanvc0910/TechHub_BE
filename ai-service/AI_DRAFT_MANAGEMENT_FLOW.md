# AI Draft Management Flow - Luồng quản lý Exercise & Learning Path Drafts

## 📋 Tổng quan

Hệ thống AI Service tạo **drafts** (bản nháp) cho exercises và learning paths. Admin review và approve trước khi lưu vào database chính.

---

## 🔄 Luồng hoàn chỉnh cho EXERCISE

### 1. **Admin tạo exercise draft**

**Scenario**: Admin click button "AI tạo exercise" tại lesson X

```http
POST /api/ai/exercises/generate
Content-Type: application/json

{
  "lessonId": "uuid-of-lesson",
  "exerciseType": "MULTIPLE_CHOICE",
  "count": 5,
  "difficulty": "MEDIUM"
}
```

**Response**:

```json
{
  "taskId": "uuid-of-draft-task",
  "status": "DRAFT",
  "message": "Exercise draft created successfully. Admin can review and approve.",
  "drafts": {
    "exercises": [
      {
        "type": "MULTIPLE_CHOICE",
        "question": "What is Java?",
        "options": [
          "A programming language",
          "A coffee",
          "An island",
          "A framework"
        ],
        "correctAnswer": 0,
        "explanation": "Java is a high-level programming language..."
      }
    ]
  }
}
```

**Note**:

- Status lưu trong `ai_generation_tasks` là `DRAFT`
- `target_reference` = `lesson_id` để query sau này
- Nếu tạo nhiều lần → sẽ có nhiều drafts cho cùng 1 lesson

---

### 2. **Admin xem danh sách drafts của lesson**

**Use case**: Admin muốn xem tất cả drafts đã tạo cho lesson này

```http
GET /api/ai/drafts/exercises?lessonId={lesson-uuid}
```

**Response**:

```json
{
  "data": [
    {
      "taskId": "draft-1-uuid",
      "status": "DRAFT",
      "createdAt": "2024-11-22T10:00:00Z",
      "resultPayload": { ... }
    },
    {
      "taskId": "draft-2-uuid",
      "status": "DRAFT",
      "createdAt": "2024-11-22T09:00:00Z",
      "resultPayload": { ... }
    }
  ]
}
```

**Or get latest draft only**:

```http
GET /api/ai/drafts/exercises/latest?lessonId={lesson-uuid}
```

---

### 3. **Admin review và approve draft**

**Option A: Approve** (Admin hài lòng với draft)

```http
POST /api/ai/drafts/{taskId}/approve-exercise
```

**Response**:

```json
{
  "success": true,
  "taskId": "draft-uuid",
  "lessonId": "lesson-uuid",
  "message": "Draft approved. Result payload is ready for Course Service API."
}
```

**What happens**:

- Status trong `ai_generation_tasks` → `APPROVED`
- Admin GỬI TIẾP request đến Course Service để lưu exercises:

```http
PUT /api/courses/{courseId}/lessons/{lessonId}/exercise
Content-Type: application/json

{
  "type": "MULTIPLE_CHOICE",
  "question": "What is Java?",
  "options": ["A programming language", "A coffee", "An island", "A framework"],
  "testCases": []
}
```

**Option B: Reject** (Admin không hài lòng)

```http
POST /api/ai/drafts/{taskId}/reject?reason=Questions are too easy
```

**What happens**:

- Status → `REJECTED`
- Admin có thể tạo draft mới

---

## 🔄 Luồng hoàn chỉnh cho LEARNING PATH

### 1. **Admin tạo learning path draft**

```http
POST /api/ai/learning-paths/generate
Content-Type: application/json

{
  "goal": "Become a Full Stack Developer",
  "duration": "6 months",
  "level": "BEGINNER"
}
```

**Response**:

```json
{
  "taskId": "uuid-of-draft-task",
  "status": "DRAFT",
  "title": "Learning Path: Become a Full Stack Developer",
  "nodes": [
    {
      "id": "course-1",
      "title": "HTML & CSS Basics",
      "position": { "x": 0, "y": 0 }
    },
    {
      "id": "course-2",
      "title": "JavaScript Fundamentals",
      "position": { "x": 200, "y": 0 }
    }
  ],
  "edges": [{ "from": "course-1", "to": "course-2" }]
}
```

---

### 2. **Admin xem drafts**

```http
GET /api/ai/drafts/learning-paths
```

---

### 3. **Admin approve**

```http
POST /api/ai/drafts/{taskId}/approve-learning-path
```

Sau đó admin GỬI TIẾP request đến Learning Path Service:

```http
POST /api/v1/learning-paths
Content-Type: application/json

{
  "title": "Become a Full Stack Developer",
  "description": "...",
  "layoutEdges": [...]
}
```

---

## 📊 Database Schema

### Table: `ai_generation_tasks`

```sql
CREATE TABLE ai_generation_tasks (
    id UUID PRIMARY KEY,
    task_type VARCHAR(64) NOT NULL,           -- 'EXERCISE_GENERATION', 'LEARNING_PATH_GENERATION'
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT', -- 'DRAFT', 'APPROVED', 'REJECTED'
    target_reference VARCHAR(255),            -- lesson_id (for exercises) hoặc goal (for learning path)
    result_payload JSONB,                     -- Draft content từ AI
    request_payload JSONB,                    -- Request gốc
    prompt TEXT,                              -- Prompt đã dùng
    created TIMESTAMP WITH TIME ZONE,
    updated TIMESTAMP WITH TIME ZONE,
    is_active VARCHAR(1) DEFAULT 'Y'
);

CREATE INDEX idx_ai_generation_tasks_target_status ON ai_generation_tasks(target_reference, status, task_type);
```

---

## 🎯 Key Points

### 1. **Tại sao cần DRAFT status?**

- AI có thể tạo content không chính xác
- Admin cần review trước khi publish
- Cho phép tạo nhiều version và chọn version tốt nhất

### 2. **Làm sao biết lấy draft nào khi có nhiều drafts?**

- **Solution 1**: Lấy draft mới nhất (default) → API `GET /api/ai/drafts/exercises/latest?lessonId=xxx`
- **Solution 2**: Hiển thị list cho admin chọn → API `GET /api/ai/drafts/exercises?lessonId=xxx`
- **Solution 3**: Admin chỉ giữ 1 draft (xóa cũ khi tạo mới) → Not implemented yet

### 3. **Format data trong result_payload**

**For EXERCISE_GENERATION**:

```json
{
  "exercises": [
    {
      "type": "MULTIPLE_CHOICE",
      "question": "What is the capital of France?",
      "options": ["London", "Paris", "Berlin", "Madrid"],
      "correctAnswer": 1,
      "explanation": "Paris is the capital and largest city of France."
    },
    {
      "type": "CODING",
      "question": "Write a function to reverse a string",
      "testCases": [
        { "input": "hello", "expectedOutput": "olleh" },
        { "input": "world", "expectedOutput": "dlrow" }
      ],
      "explanation": "Use string manipulation methods..."
    }
  ]
}
```

**For LEARNING_PATH_GENERATION**:

```json
{
  "title": "Full Stack Developer Path",
  "description": "Complete path from beginner to professional",
  "nodes": [
    {
      "id": "course-1",
      "title": "HTML Basics",
      "position": { "x": 0, "y": 0 },
      "level": "BEGINNER"
    },
    {
      "id": "course-2",
      "title": "CSS Styling",
      "position": { "x": 200, "y": 0 },
      "level": "BEGINNER"
    }
  ],
  "edges": [{ "from": "course-1", "to": "course-2", "label": "Next" }]
}
```

---

## 🔧 Future Enhancements

1. **Auto-save to Course Service**: Thêm Feign Client để tự động gọi Course Service API khi approve
2. **Draft versioning**: Track version history của drafts
3. **Batch approve**: Approve nhiều drafts cùng lúc
4. **AI re-generation**: Cho phép AI regenerate draft dựa trên feedback
5. **Preview mode**: Preview exercises trước khi approve

---

## 📝 API Summary

| Endpoint                                         | Method | Description                   |
| ------------------------------------------------ | ------ | ----------------------------- |
| `/api/ai/exercises/generate`                     | POST   | Tạo exercise draft            |
| `/api/ai/learning-paths/generate`                | POST   | Tạo learning path draft       |
| `/api/ai/drafts/exercises?lessonId={id}`         | GET    | List tất cả drafts của lesson |
| `/api/ai/drafts/exercises/latest?lessonId={id}`  | GET    | Get draft mới nhất            |
| `/api/ai/drafts/{taskId}`                        | GET    | Get detail 1 draft            |
| `/api/ai/drafts/{taskId}/approve-exercise`       | POST   | Approve exercise draft        |
| `/api/ai/drafts/{taskId}/approve-learning-path`  | POST   | Approve learning path draft   |
| `/api/ai/drafts/{taskId}/reject?reason={reason}` | POST   | Reject draft                  |

---

## ✅ Checklist cho Admin

**Khi tạo Exercise cho Lesson**:

1. ✅ Click "AI tạo exercise" tại lesson
2. ✅ Nhận taskId và draft content
3. ✅ Review draft content (có thể tạo lại nhiều lần)
4. ✅ Approve draft → status = APPROVED
5. ✅ Copy `result_payload` và call Course Service API để lưu exercises
6. ✅ Done!

**Khi tạo Learning Path**:

1. ✅ Click "AI tạo learning path"
2. ✅ Nhập goal, duration, level
3. ✅ Review draft (nodes, edges)
4. ✅ Approve draft
5. ✅ Copy data và call Learning Path Service API
6. ✅ Done!
