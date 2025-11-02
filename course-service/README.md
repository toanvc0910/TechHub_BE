# TechHub Course Service

A comprehensive Spring Boot microservice for managing online courses, lessons, exercises, enrollments, and progress tracking.

## 📋 Table of Contents
- [Features](#features)
- [Architecture](#architecture)
- [Technologies](#technologies)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Configuration](#configuration)

## ✨ Features

### Core Modules
- **Course Management**: CRUD operations for courses with categories, tags, and pricing
- **Chapter Management**: Organize courses into chapters with ordering
- **Lesson Management**: Multiple content types (video, document, code, quiz)
- **Exercise Management**: Support for multiple choice, coding, and short answer questions
- **Progress Tracking**: Track user completion percentage for each lesson
- **Enrollment Management**: Handle course enrollments and status tracking
- **Submission Management**: Manage exercise submissions and grading
- **User Code Management**: Save and retrieve user code snippets per lesson
- **Rating System**: Rate and review courses with average calculation

## 🏗️ Architecture

This service follows a **layered architecture** pattern:

```
┌─────────────────────┐
│   Controllers       │  ← REST API endpoints
├─────────────────────┤
│   Services          │  ← Business logic
├─────────────────────┤
│   Repositories      │  ← Data access layer
├─────────────────────┤
│   Models/Entities   │  ← JPA entities
└─────────────────────┘
```

### Key Design Principles
✅ **Constructor-Based Dependency Injection** (No @Autowired)
✅ **DTO Pattern** for clean API contracts
✅ **Repository Pattern** with Spring Data JPA
✅ **Global Exception Handling**
✅ **RESTful API Design**
✅ **UUID as Primary Keys**
✅ **Soft Delete Pattern** (is_active flag)

## 🛠️ Technologies

- **Spring Boot 3.4.8**
- **Java 17**
- **Spring Data JPA** with Hibernate
- **PostgreSQL** database
- **Lombok** for reducing boilerplate
- **ModelMapper** for DTO conversion
- **SpringDoc OpenAPI 3** (Swagger UI)
- **Jakarta Validation**
- **Maven** build tool

## 📁 Project Structure

```
course-service/
├── src/
│   └── main/
│       ├── java/com/techhub/app/courseservice/
│       │   ├── controller/          # REST Controllers
│       │   │   ├── CourseController.java
│       │   │   ├── ChapterController.java
│       │   │   ├── LessonController.java
│       │   │   ├── ExerciseController.java
│       │   │   ├── ProgressController.java
│       │   │   ├── EnrollmentController.java
│       │   │   ├── SubmissionController.java
│       │   │   ├── UserCodeController.java
│       │   │   └── RatingController.java
│       │   │
│       │   ├── service/              # Business Logic
│       │   │   ├── CourseService.java
│       │   │   ├── ChapterService.java
│       │   │   ├── LessonService.java
│       │   │   ├── ExerciseService.java
│       │   │   ├── ProgressService.java
│       │   │   ├── EnrollmentService.java
│       │   │   ├── SubmissionService.java
│       │   │   ├── UserCodeService.java
│       │   │   └── RatingService.java
│       │   │
│       │   ├── repository/           # Data Access
│       │   │   ├── CourseRepository.java
│       │   │   ├── ChapterRepository.java
│       │   │   ├── LessonRepository.java
│       │   │   ├── ExerciseRepository.java
│       │   │   ├── ProgressRepository.java
│       │   │   ├── EnrollmentRepository.java
│       │   │   ├── SubmissionRepository.java
│       │   │   ├── UserCodeRepository.java
│       │   │   └── RatingRepository.java
│       │   │
│       │   ├── model/                # JPA Entities
│       │   │   ├── Course.java
│       │   │   ├── Chapter.java
│       │   │   ├── Lesson.java
│       │   │   ├── Exercise.java
│       │   │   ├── Progress.java
│       │   │   ├── Enrollment.java
│       │   │   ├── Submission.java
│       │   │   ├── UserCode.java
│       │   │   └── Rating.java
│       │   │
│       │   ├── dto/                  # Data Transfer Objects
│       │   │   ├── CourseDTO.java
│       │   │   ├── CreateCourseDTO.java
│       │   │   ├── UpdateCourseDTO.java
│       │   │   └── ... (similar for other entities)
│       │   │
│       │   ├── config/               # Configuration
│       │   │   ├── AppConfig.java
│       │   │   └── SwaggerConfig.java
│       │   │
│       │   ├── exception/            # Exception Handling
│       │   │   ├── GlobalExceptionHandler.java
│       │   │   └── ResourceNotFoundException.java
│       │   │
│       │   ├── utils/                # Utilities
│       │   │   ├── ResponseWrapper.java
│       │   │   └── MapperUtil.java
│       │   │
│       │   └── CourseserviceApplication.java
│       │
│       └── resources/
│           ├── application.yml       # Configuration
│           └── schema.sql            # Database schema
│
├── pom.xml
└── README.md
```

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- PostgreSQL 12+
- Maven 3.8+

### Database Setup

1. Create a PostgreSQL database:
```sql
CREATE DATABASE techhub_courses;
```

2. Update database credentials in `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/techhub_courses
    username: your_username
    password: your_password
```

### Running the Application

1. **Clone the repository**
```bash
git clone <repository-url>
cd course-service
```

2. **Build the project**
```bash
./mvnw clean install
```

3. **Run the application**
```bash
./mvnw spring-boot:run
```

The service will start on **http://localhost:8082**

## 📚 API Documentation

Once the application is running, access the interactive API documentation:

- **Swagger UI**: http://localhost:8082/api/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8082/api/v3/api-docs

### Sample API Endpoints

#### Courses
- `POST /api/courses` - Create a new course
- `GET /api/courses` - Get all courses
- `GET /api/courses/{id}` - Get course by ID
- `GET /api/courses/instructor/{instructorId}` - Get courses by instructor
- `PUT /api/courses/{id}` - Update course
- `DELETE /api/courses/{id}` - Soft delete course

#### Chapters
- `POST /api/chapters` - Create a new chapter
- `GET /api/chapters/course/{courseId}` - Get chapters by course

#### Lessons
- `POST /api/lessons` - Create a new lesson
- `GET /api/lessons/chapter/{chapterId}` - Get lessons by chapter

#### Enrollments
- `POST /api/enrollments` - Enroll in a course
- `GET /api/enrollments/user/{userId}` - Get user enrollments
- `PATCH /api/enrollments/{id}/status?status=COMPLETED` - Update enrollment status

#### Progress
- `POST /api/progress` - Create/update progress
- `GET /api/progress/user/{userId}` - Get user progress
- `GET /api/progress/user/{userId}/lesson/{lessonId}` - Get specific lesson progress

#### Submissions
- `POST /api/submissions` - Submit an exercise answer
- `PATCH /api/submissions/{id}/grade?grade=85.5` - Grade a submission

#### User Codes
- `POST /api/user-codes` - Save user code snippet
- `GET /api/user-codes/user/{userId}` - Get user's saved codes

#### Ratings
- `POST /api/ratings` - Create a rating
- `GET /api/ratings/target/{targetId}?targetType=COURSE` - Get ratings for target
- `GET /api/ratings/target/{targetId}/average?targetType=COURSE` - Get average rating

### Request/Response Examples

**Create a Course:**
```json
POST /api/courses
{
  "title": "Spring Boot Masterclass",
  "description": "Complete guide to Spring Boot development",
  "price": 99.99,
  "instructorId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "PUBLISHED",
  "categories": ["Programming", "Backend"],
  "tags": ["Java", "Spring", "Microservices"]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Course created successfully",
  "data": {
    "id": "987e6543-e21b-12d3-a456-426614174000",
    "title": "Spring Boot Masterclass",
    "description": "Complete guide to Spring Boot development",
    "price": 99.99,
    "instructorId": "123e4567-e89b-12d3-a456-426614174000",
    "status": "PUBLISHED",
    "categories": ["Programming", "Backend"],
    "tags": ["Java", "Spring", "Microservices"],
    "createdAt": "2025-10-18T12:00:00",
    "updatedAt": "2025-10-18T12:00:00",
    "isActive": true
  },
  "timestamp": "2025-10-18T12:00:00"
}
```

## 🗄️ Database Schema

The service uses the following main entities:

- **courses** - Course information
- **chapters** - Course chapters
- **lessons** - Lesson content
- **exercises** - Practice exercises
- **progress** - User progress tracking
- **enrollments** - Course enrollments
- **submissions** - Exercise submissions
- **user_codes** - Saved code snippets
- **ratings** - Course ratings

See `src/main/resources/schema.sql` for complete schema.

## ⚙️ Configuration

### Application Properties

Key configuration in `application.yml`:

```yaml
server:
  port: 8082                    # Service port
  servlet:
    context-path: /api          # Base URL path

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/techhub_courses
    username: postgres
    password: postgres
  
  jpa:
    hibernate:
      ddl-auto: update          # Auto-create/update schema
    show-sql: true              # Log SQL queries

springdoc:
  swagger-ui:
    path: /swagger-ui.html      # Swagger UI path
```

## 🔒 Best Practices Implemented

1. **Constructor-Based DI**: All dependencies injected via constructors (no @Autowired)
2. **DTO Pattern**: Separate DTOs for input/output to decouple API from domain model
3. **Validation**: Jakarta Validation annotations on DTOs
4. **Exception Handling**: Global exception handler for consistent error responses
5. **Soft Delete**: Uses `is_active` flag instead of hard deletes
6. **Auditing**: Automatic timestamps with @CreationTimestamp and @UpdateTimestamp
7. **UUID Primary Keys**: Better for distributed systems
8. **Transactional Operations**: @Transactional on write operations

## 📝 Example Usage Flow

1. **Create a Course** → Returns course ID
2. **Add Chapters** to the course
3. **Add Lessons** to each chapter
4. **Add Exercises** to lessons
5. **User Enrolls** in the course
6. **Track Progress** as user completes lessons
7. **Submit Answers** to exercises
8. **Grade Submissions**
9. **Rate the Course**

## 🧪 Testing

Run tests with:
```bash
./mvnw test
```

## 📄 License

This project is part of the TechHub microservices architecture.

## 👥 Contributors

- Backend Team - TechHub Development

## 📞 Support

For issues and questions, please contact: support@techhub.com

---

**Built with ❤️ using Spring Boot 3 and modern microservices architecture**
-- TechHub Course Service Database Schema

-- Courses Table
CREATE TABLE courses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    price DOUBLE PRECISION NOT NULL,
    instructor_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Course Categories Table (ElementCollection)
CREATE TABLE course_categories (
    course_id UUID NOT NULL,
    category VARCHAR(255),
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- Course Tags Table (ElementCollection)
CREATE TABLE course_tags (
    course_id UUID NOT NULL,
    tag VARCHAR(255),
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- Chapters Table
CREATE TABLE chapters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    chapter_order INTEGER NOT NULL,
    course_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- Lessons Table
CREATE TABLE lessons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    lesson_order INTEGER NOT NULL,
    chapter_id UUID NOT NULL,
    content_type VARCHAR(50),
    video_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE
);

-- Lesson Document URLs Table (ElementCollection)
CREATE TABLE lesson_document_urls (
    lesson_id UUID NOT NULL,
    document_url VARCHAR(500),
    FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
);

-- Exercises Table
CREATE TABLE exercises (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(50) NOT NULL,
    question TEXT NOT NULL,
    test_cases TEXT,
    lesson_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
);

-- Exercise Options Table (ElementCollection)
CREATE TABLE exercise_options (
    exercise_id UUID NOT NULL,
    option TEXT,
    FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE CASCADE
);

-- Progress Table
CREATE TABLE progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    lesson_id UUID NOT NULL,
    completion DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
);

-- Enrollments Table
CREATE TABLE enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    course_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    enrolled_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- Submissions Table
CREATE TABLE submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    exercise_id UUID NOT NULL,
    answer TEXT NOT NULL,
    grade DOUBLE PRECISION,
    graded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE CASCADE
);

-- User Codes Table
CREATE TABLE user_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    lesson_id UUID NOT NULL,
    code TEXT NOT NULL,
    language VARCHAR(50) NOT NULL,
    saved_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE
);

-- Ratings Table
CREATE TABLE ratings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    target_id UUID NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Create Indexes for Better Performance
CREATE INDEX idx_courses_instructor ON courses(instructor_id);
CREATE INDEX idx_courses_status ON courses(status);
CREATE INDEX idx_courses_is_active ON courses(is_active);

CREATE INDEX idx_chapters_course ON chapters(course_id);
CREATE INDEX idx_lessons_chapter ON lessons(chapter_id);
CREATE INDEX idx_exercises_lesson ON exercises(lesson_id);

CREATE INDEX idx_progress_user ON progress(user_id);
CREATE INDEX idx_progress_lesson ON progress(lesson_id);
CREATE INDEX idx_progress_user_lesson ON progress(user_id, lesson_id);

CREATE INDEX idx_enrollments_user ON enrollments(user_id);
CREATE INDEX idx_enrollments_course ON enrollments(course_id);
CREATE INDEX idx_enrollments_user_course ON enrollments(user_id, course_id);

CREATE INDEX idx_submissions_user ON submissions(user_id);
CREATE INDEX idx_submissions_exercise ON submissions(exercise_id);

CREATE INDEX idx_user_codes_user ON user_codes(user_id);
CREATE INDEX idx_user_codes_lesson ON user_codes(lesson_id);

CREATE INDEX idx_ratings_user ON ratings(user_id);
CREATE INDEX idx_ratings_target ON ratings(target_id, target_type);

