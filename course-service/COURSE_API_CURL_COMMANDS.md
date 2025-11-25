# Course Service API - CURL Commands

**Base URL via API Gateway:** `http://localhost:8443/app/api/proxy/courses`  
**Base URL Direct:** `http://localhost:8085/app/api/proxy/courses`

**Note:** Replace `YOUR_JWT_TOKEN` with actual JWT token from login response.

---

## 📚 1. COURSE MANAGEMENT

### 1.1 Get All Courses (Public)
```bash
curl -X GET "http://localhost:8443/app/api/proxy/courses?page=0&size=10&search=javascript" \
  -H "accept: application/json"
````

### 1.2 Get Course by ID (Public)
```bash
curl -X GET "http://localhost:8443/app/api/proxy/courses/COURSE_ID" \
  -H "accept: application/json"
```

### 1.3 Create Course (Requires Auth - Instructor/Admin)
```bash
curl -X POST "http://localhost:8443/app/api/proxy/courses" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Introduction to JavaScript",
    "description": "Learn JavaScript from scratch",
    "thumbnail": "https://example.com/thumbnail.jpg",
    "level": "BEGINNER",
    "status": "DRAFT",
    "price": 49.99,
    "discountedPrice": 29.99,
    "estimatedDuration": 3600,
    "tags": ["javascript", "programming", "web-development"]
  }'
```

### 1.4 Update Course (Requires Auth - Instructor/Admin)
```bash
curl -X PUT "http://localhost:8443/app/api/proxy/courses/COURSE_ID" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Advanced JavaScript",
    "description": "Master JavaScript concepts",
    "level": "ADVANCED",
    "status": "PUBLISHED",
    "price": 99.99
  }'
```

### 1.5 Delete Course (Requires Auth - Instructor/Admin)
```bash
curl -X DELETE "http://localhost:8443/app/api/proxy/courses/COURSE_ID" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 🎓 2. ENROLLMENT

### 2.1 Enroll in Course (Requires Auth)
```bash
curl -X POST "http://localhost:8443/app/api/proxy/courses/COURSE_ID/enroll" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 📖 3. CHAPTER MANAGEMENT

### 3.1 Get Course Chapters (Public)
```bash
curl -X GET "http://localhost:8443/app/api/proxy/courses/COURSE_ID/chapters" \
  -H "accept: application/json"
```

### 3.2 Create Chapter (Requires Auth - Instructor/Admin)
```bash
curl -X POST "http://localhost:8443/app/api/proxy/courses/COURSE_ID/chapters" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Getting Started",
    "description": "Introduction to the course",
    "orderIndex": 1
  }'
```

### 3.3 Update Chapter (Requires Auth - Instructor/Admin)
```bash
curl -X PUT "http://localhost:8443/app/api/proxy/courses/COURSE_ID/chapters/CHAPTER_ID" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Getting Started - Updated",
    "description": "Updated description",
    "orderIndex": 1
  }'
```

### 3.4 Delete Chapter (Requires Auth - Instructor/Admin)
```bash
curl -X DELETE "http://localhost:8443/app/api/proxy/courses/COURSE_ID/chapters/CHAPTER_ID" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Note:** This is a HARD DELETE with CASCADE. All related lessons and assets will be automatically deleted, and remaining chapters will be auto-reordered (1, 2, 3...).

---

## 📝 4. LESSON MANAGEMENT

### 4.1 Get Lesson by ID (Requires Auth - Instructor/Admin)
```bash
curl -X GET "http://localhost:8443/app/api/proxy/courses/COURSE_ID/chapters/CHAPTER_ID/lessons/LESSON_ID/detail" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 4.2 Create Lesson (Requires Auth - Instructor/Admin)
```bash
curl -X POST "http://localhost:8443/app/api/proxy/courses/COURSE_ID/chapters/CHAPTER_ID/lessons" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Variables and Data Types",
    "description": "Learn about variables",
    "contentType": "VIDEO",
    "content": "https://youtube.com/watch?v=example",
    "duration": 600,
    "orderIndex": 1,
    "isFree": false
  }'
```

### 4.3 Update Lesson (Requires Auth - Instructor/Admin)
```bash
curl -X PUT "http://localhost:8443/app/api/proxy/courses/COURSE_ID/chapters/CHAPTER_ID/lessons/LESSON_ID" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Variables and Data Types - Updated",
    "estimatedDuration": 720,
    "orderIndex": 1
  }'
```

### 4.4 Delete Lesson (Requires Auth - Instructor/Admin)
```bash
curl -X DELETE "http://localhost:8443/app/api/proxy/courses/COURSE_ID/chapters/CHAPTER_ID/lessons/LESSON_ID" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Note:** This is a HARD DELETE with CASCADE. All related lesson assets will be automatically deleted, and remaining lessons will be auto-reordered (1, 2, 3...).

---

## 📎 5. LESSON ASSETS

### 5.1 Create Lesson Asset (Requires Auth - Instructor/Admin)
```bash
curl -X POST "http://localhost:8443/app/api/proxy/courses/COURSE_ID/chapters/CHAPTER_ID/lessons/LESSON_ID/assets" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "assetType": "DOCUMENT",
    "title": "Lecture Slides",
    "externalUrl": "https://example.com/slides.pdf",
    "orderIndex": 1
  }'
```

### 5.2 Update Lesson Asset (Requires Auth - Instructor/Admin)
```bash
curl -X PUT "http://localhost:8443/app/api/proxy/courses/COURSE_ID/chapters/CHAPTER_ID/lessons/LESSON_ID/assets/ASSET_ID" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Updated Lecture Slides",
    "orderIndex": 1
  }'
```

### 5.3 Delete Lesson Asset (Requires Auth - Instructor/Admin)
```bash
curl -X DELETE "http://localhost:8443/app/api/proxy/courses/COURSE_ID/chapters/CHAPTER_ID/lessons/LESSON_ID/assets/ASSET_ID" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 📊 6. PROGRESS TRACKING

### 6.1 Get Course Progress (Requires Auth)
```bash
curl -X GET "http://localhost:8443/app/api/proxy/courses/COURSE_ID/progress" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 6.2 Update Lesson Progress (Requires Auth)
```bash
curl -X PUT "http://localhost:8443/app/api/proxy/courses/COURSE_ID/lessons/LESSON_ID/progress" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "watchedDuration": 300,
    "lastPosition": 300
  }'
```

### 6.3 Mark Lesson as Complete (Requires Auth)
```bash
curl -X POST "http://localhost:8443/app/api/proxy/courses/COURSE_ID/lessons/LESSON_ID/progress/complete" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## ⭐ 7. RATINGS

### 7.1 Get Course Ratings (Public)
```bash
curl -X GET "http://localhost:8443/app/api/proxy/courses/COURSE_ID/ratings" \
  -H "accept: application/json"
```

### 7.2 Submit Course Rating (Requires Auth)
```bash
curl -X POST "http://localhost:8443/app/api/proxy/courses/COURSE_ID/ratings" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "rating": 5,
    "comment": "Excellent course! Highly recommended."
  }'
```

---

## 💬 8. COMMENTS

### 8.1 Get Course Comments (Public)
```bash
curl -X GET "http://localhost:8443/app/api/proxy/courses/COURSE_ID/comments" \
  -H "accept: application/json"
```

### 8.2 Add Course Comment (Requires Auth)
```bash
curl -X POST "http://localhost:8443/app/api/proxy/courses/COURSE_ID/comments" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Great course so far!",
    "parentCommentId": null
  }'
```

### 8.3 Get Lesson Comments (Public)
```bash
curl -X GET "http://localhost:8443/app/api/proxy/courses/COURSE_ID/lessons/LESSON_ID/comments" \
  -H "accept: application/json"
```

### 8.4 Add Lesson Comment (Requires Auth)
```bash
curl -X POST "http://localhost:8443/app/api/proxy/courses/COURSE_ID/lessons/LESSON_ID/comments" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Can you explain this part again?",
    "parentCommentId": null
  }'
```

### 8.5 Get Workspace Comments (Requires Auth)
```bash
curl -X GET "http://localhost:8443/app/api/proxy/courses/COURSE_ID/lessons/LESSON_ID/workspace/comments" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 8.6 Add Workspace Comment (Requires Auth)
```bash
curl -X POST "http://localhost:8443/app/api/proxy/courses/COURSE_ID/lessons/LESSON_ID/workspace/comments" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "My code is not working as expected",
    "codeSnippet": "const x = 10;"
  }'
```

### 8.7 Delete Comment (Requires Auth)
```bash
curl -X DELETE "http://localhost:8443/app/api/proxy/courses/COURSE_ID/comments/COMMENT_ID" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 🏋️ 9. EXERCISES

### 9.1 Get Lesson Exercise (Requires Auth)
```bash
curl -X GET "http://localhost:8443/app/api/proxy/courses/COURSE_ID/lessons/LESSON_ID/exercise" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 9.2 Create/Update Exercise (Requires Auth - Instructor/Admin)
```bash
curl -X PUT "http://localhost:8443/app/api/proxy/courses/COURSE_ID/lessons/LESSON_ID/exercise" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "CODING",
    "title": "Sum Two Numbers",
    "description": "Write a function that returns sum of two numbers",
    "initialCode": "function sum(a, b) {\n  // Your code here\n}",
    "solutionCode": "function sum(a, b) {\n  return a + b;\n}",
    "language": "JAVASCRIPT",
    "testCases": [
      {
        "input": "1,2",
        "expectedOutput": "3",
        "isHidden": false
      },
      {
        "input": "10,20",
        "expectedOutput": "30",
        "isHidden": true
      }
    ]
  }'
```

### 9.3 Submit Exercise Solution (Requires Auth)
```bash
curl -X POST "http://localhost:8443/app/api/proxy/courses/COURSE_ID/lessons/LESSON_ID/exercise/submissions" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "function sum(a, b) { return a + b; }",
    "language": "JAVASCRIPT"
  }'
```

---

## 💻 10. USER WORKSPACE

### 10.1 Get User Workspace (Requires Auth)
```bash
curl -X GET "http://localhost:8443/app/api/proxy/courses/COURSE_ID/lessons/LESSON_ID/workspace" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 10.2 Save User Workspace Code (Requires Auth)
```bash
curl -X PUT "http://localhost:8443/app/api/proxy/courses/COURSE_ID/lessons/LESSON_ID/workspace" \
  -H "accept: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "const greeting = \"Hello World\";\nconsole.log(greeting);",
    "language": "JAVASCRIPT"
  }'
```

---

## 🔐 Authentication Required

Most endpoints require JWT authentication. First, login to get a token:

```bash
# Login
curl -X POST "http://localhost:8443/app/api/proxy/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "yourpassword"
  }'
```

Response will include `accessToken`. Use it in subsequent requests:
```bash
-H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE"
```

---

## 📌 Notes

1. **Replace placeholder IDs:**
   - `COURSE_ID` → actual UUID of course
   - `CHAPTER_ID` → actual UUID of chapter
   - `LESSON_ID` → actual UUID of lesson
   - `ASSET_ID` → actual UUID of asset
   - `COMMENT_ID` → actual UUID of comment

2. **Course Levels:** `BEGINNER`, `INTERMEDIATE`, `ADVANCED`

3. **Course Status:** `DRAFT`, `PUBLISHED`, `ARCHIVED`

4. **Content Types:** `VIDEO`, `TEXT`, `QUIZ`, `CODING`

5. **Exercise Types:** `CODING`, `QUIZ`, `MULTIPLE_CHOICE`

6. **Languages:** `JAVASCRIPT`, `PYTHON`, `JAVA`, `CPP`, `GO`, etc.

7. **Asset Types:** `VIDEO`, `DOCUMENT`, `LINK`, `IMAGE`, `CODE`

8. **⚠️ IMPORTANT - Field Name Changes:**
   - Use `orderIndex` (NOT `order`) for chapters, lessons, and assets
   - Use `estimatedDuration` (NOT `duration`) when updating lessons
   - Use `assetType` (NOT `type`) for assets
   - Use `externalUrl` (NOT `url`) for assets

9. **🗑️ DELETE Operations:**
   - Chapter and Lesson deletes are HARD DELETE with CASCADE
   - Deleting a chapter removes all its lessons and assets
   - Deleting a lesson removes all its assets
   - Remaining items are auto-reordered (1, 2, 3...)

---

## 🧪 Quick Test Flow

1. **Create a course** → Get `COURSE_ID`
2. **Create a chapter** → Get `CHAPTER_ID`
3. **Create a lesson** → Get `LESSON_ID`
4. **Enroll in course**
5. **Mark lesson complete**
6. **Submit rating**
7. **Add comment**
