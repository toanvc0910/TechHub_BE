# REST API Documentation - Learning Path Service

## API Create Learning Path

### API Specs

| Field | Value |
|-------|-------|
| **Name** | Create Learning Path |
| **Description** | Create a new learning path with associated courses |
| **URL** | `https://{{base_url}}/learning-path-service/v1/learning-paths` |
| **Method** | POST |
| **Header** | Content-Type: application/json<br>Authorization: Bearer {token} |
| **Params** | None |

### Request Body

```json
{
  "title": "Full Stack Web Development",
  "description": "Complete path to become a full stack developer",
  "difficulty": "INTERMEDIATE",
  "estimatedDuration": 120,
  "prerequisites": ["Basic Programming", "HTML/CSS"],
  "courseIds": ["course-123", "course-456"]
}
```

### Response

**Status Code: 201**
```json
{
  "meta": {
    "code": "201000",
    "type": "CREATED",
    "message": "Learning path created successfully",
    "service_id": "learning-path-service",
    "extra_meta": {}
  },
  "data": {
    "id": "lp-789",
    "title": "Full Stack Web Development",
    "description": "Complete path to become a full stack developer",
    "difficulty": "INTERMEDIATE",
    "estimatedDuration": 120,
    "prerequisites": ["Basic Programming", "HTML/CSS"],
    "courses": [
      {
        "courseId": "course-123",
        "orderIndex": 0
      },
      {
        "courseId": "course-456",
        "orderIndex": 1
      }
    ],
    "createdAt": "2024-11-18T10:30:00",
    "updatedAt": "2024-11-18T10:30:00"
  }
}
```

**Status Code: 400**
```json
{
  "meta": {
    "code": "400000",
    "type": "BAD_REQUEST",
    "message": "Invalid input data",
    "service_id": "learning-path-service",
    "extra_meta": {}
  },
  "data": null
}
```

**Status Code: 401**
```json
{
  "meta": {
    "code": "401000",
    "type": "UNAUTHORIZED",
    "message": "Authentication is required and has failed or not been provided",
    "service_id": "learning-path-service",
    "extra_meta": {}
  },
  "data": null
}
```

### Sample

```bash
curl --location --request POST 'https://{{base_url}}/learning-path-service/v1/learning-paths' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>' \
--data '{
  "title": "Full Stack Web Development",
  "description": "Complete path to become a full stack developer",
  "difficulty": "INTERMEDIATE",
  "estimatedDuration": 120,
  "prerequisites": ["Basic Programming", "HTML/CSS"],
  "courseIds": ["course-123", "course-456"]
}'
```

### Request Body Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| title | String | ✓ | - | The title of the learning path |
| description | String | ✓ | - | Detailed description of the learning path |
| difficulty | String | ✓ | - | Difficulty level (BEGINNER, INTERMEDIATE, ADVANCED) |
| estimatedDuration | Integer | ✓ | - | Estimated duration in hours |
| prerequisites | Array | - | [] | List of prerequisite topics |
| courseIds | Array | ✓ | - | List of course IDs to include in the path |

### Response Body Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| id | String | ✓ | - | Unique identifier for the learning path |
| title | String | ✓ | - | The title of the learning path |
| description | String | ✓ | - | Detailed description |
| difficulty | String | ✓ | - | Difficulty level |
| estimatedDuration | Integer | ✓ | 0 | Estimated duration in hours |
| prerequisites | Array | - | [] | List of prerequisite topics |
| courses | Array | ✓ | [] | List of courses with order |
| createdAt | String | ✓ | - | Creation timestamp |
| updatedAt | String | ✓ | - | Last update timestamp |

### Errors

| Status Code | Code | Type | Description |
|-------------|------|------|-------------|
| 201 | 201000 | CREATED | Learning path created successfully |
| 400 | 400000 | BAD_REQUEST | Invalid input data |
| 401 | 401000 | UNAUTHORIZED | Authentication required |

---

## API Update Learning Path

### API Specs

| Field | Value |
|-------|-------|
| **Name** | Update Learning Path |
| **Description** | Update an existing learning path |
| **URL** | `https://{{base_url}}/learning-path-service/v1/learning-paths/{pathId}` |
| **Method** | PUT |
| **Header** | Content-Type: application/json<br>Authorization: Bearer {token} |
| **Params** | pathId: string (required) - Learning path ID |

### Request Body

```json
{
  "title": "Advanced Full Stack Web Development",
  "description": "Updated description for full stack path",
  "difficulty": "ADVANCED",
  "estimatedDuration": 150,
  "prerequisites": ["Basic Programming", "HTML/CSS", "JavaScript"]
}
```

### Response

**Status Code: 200**
```json
{
  "meta": {
    "code": "200000",
    "type": "SUCCESS",
    "message": "Learning path updated successfully",
    "service_id": "learning-path-service",
    "extra_meta": {}
  },
  "data": {
    "id": "lp-789",
    "title": "Advanced Full Stack Web Development",
    "description": "Updated description for full stack path",
    "difficulty": "ADVANCED",
    "estimatedDuration": 150,
    "prerequisites": ["Basic Programming", "HTML/CSS", "JavaScript"],
    "updatedAt": "2024-11-18T11:30:00"
  }
}
```

**Status Code: 404**
```json
{
  "meta": {
    "code": "404000",
    "type": "NOT_FOUND",
    "message": "Learning path not found",
    "service_id": "learning-path-service",
    "extra_meta": {}
  },
  "data": null
}
```

### Sample

```bash
curl --location --request PUT 'https://{{base_url}}/learning-path-service/v1/learning-paths/lp-789' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>' \
--data '{
  "title": "Advanced Full Stack Web Development",
  "description": "Updated description for full stack path",
  "difficulty": "ADVANCED",
  "estimatedDuration": 150
}'
```

### Errors

| Status Code | Code | Type | Description |
|-------------|------|------|-------------|
| 200 | 200000 | SUCCESS | Learning path updated successfully |
| 400 | 400000 | BAD_REQUEST | Invalid input data |
| 401 | 401000 | UNAUTHORIZED | Authentication required |
| 404 | 404000 | NOT_FOUND | Learning path not found |

---

## API Get Learning Path by ID

### API Specs

| Field | Value |
|-------|-------|
| **Name** | Get Learning Path |
| **Description** | Retrieve a specific learning path by ID |
| **URL** | `https://{{base_url}}/learning-path-service/v1/learning-paths/{pathId}` |
| **Method** | GET |
| **Header** | Content-Type: application/json<br>Authorization: Bearer {token} |
| **Params** | pathId: string (required) - Learning path ID |

### Request Body

EMPTY

### Response

**Status Code: 200**
```json
{
  "meta": {
    "code": "200000",
    "type": "SUCCESS",
    "message": "Success",
    "service_id": "learning-path-service",
    "extra_meta": {}
  },
  "data": {
    "id": "lp-789",
    "title": "Full Stack Web Development",
    "description": "Complete path to become a full stack developer",
    "difficulty": "INTERMEDIATE",
    "estimatedDuration": 120,
    "prerequisites": ["Basic Programming", "HTML/CSS"],
    "courses": [
      {
        "courseId": "course-123",
        "orderIndex": 0,
        "title": "Introduction to React",
        "duration": 40
      },
      {
        "courseId": "course-456",
        "orderIndex": 1,
        "title": "Node.js Fundamentals",
        "duration": 50
      }
    ],
    "totalEnrollments": 1250,
    "averageRating": 4.5,
    "createdAt": "2024-11-18T10:30:00",
    "updatedAt": "2024-11-18T10:30:00"
  }
}
```

**Status Code: 404**
```json
{
  "meta": {
    "code": "404000",
    "type": "NOT_FOUND",
    "message": "Learning path not found",
    "service_id": "learning-path-service",
    "extra_meta": {}
  },
  "data": null
}
```

### Sample

```bash
curl --location --request GET 'https://{{base_url}}/learning-path-service/v1/learning-paths/lp-789' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>'
```

### Errors

| Status Code | Code | Type | Description |
|-------------|------|------|-------------|
| 200 | 200000 | SUCCESS | Learning path retrieved successfully |
| 401 | 401000 | UNAUTHORIZED | Authentication required |
| 404 | 404000 | NOT_FOUND | Learning path not found |

---

## API Get All Learning Paths

### API Specs

| Field | Value |
|-------|-------|
| **Name** | Get All Learning Paths |
| **Description** | Retrieve a list of all learning paths with filtering and pagination |
| **URL** | `https://{{base_url}}/learning-path-service/v1/learning-paths` |
| **Method** | GET |
| **Header** | Content-Type: application/json<br>Authorization: Bearer {token} |
| **Params** | difficulty: string (optional)<br>page: int (optional, default = 0)<br>size: int (optional, default = 10)<br>sort_by: string (optional, default = "createdAt")<br>order: string (optional, default = "desc") |

### Request Body

EMPTY

### Response

**Status Code: 200**
```json
{
  "meta": {
    "code": "200000",
    "type": "SUCCESS",
    "message": "Success",
    "service_id": "learning-path-service",
    "extra_meta": {}
  },
  "data": {
    "learningPaths": [
      {
        "id": "lp-789",
        "title": "Full Stack Web Development",
        "description": "Complete path to become a full stack developer",
        "difficulty": "INTERMEDIATE",
        "estimatedDuration": 120,
        "totalEnrollments": 1250,
        "averageRating": 4.5,
        "createdAt": "2024-11-18T10:30:00"
      }
    ],
    "pagination": {
      "total_pages": 5,
      "total_items": 45,
      "current_page": 0,
      "page_size": 10
    }
  }
}
```

### Sample

```bash
curl --location --request GET 'https://{{base_url}}/learning-path-service/v1/learning-paths?difficulty=INTERMEDIATE&page=0&size=10&sort_by=createdAt&order=desc' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>'
```

### Response Body Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| learningPaths | Array | ✓ | [] | List of learning paths |
| total_pages | Integer | ✓ | 0 | Total number of pages |
| total_items | Integer | ✓ | 0 | Total number of items |
| current_page | Integer | ✓ | 0 | Current page number |
| page_size | Integer | ✓ | 10 | Number of items per page |

---

## API Update Course Order

### API Specs

| Field | Value |
|-------|-------|
| **Name** | Update Course Order |
| **Description** | Update the order of courses within a learning path |
| **URL** | `https://{{base_url}}/learning-path-service/v1/learning-paths/{pathId}/courses/order` |
| **Method** | PUT |
| **Header** | Content-Type: application/json<br>Authorization: Bearer {token} |
| **Params** | pathId: string (required) - Learning path ID |

### Request Body

```json
{
  "courseOrders": [
    {
      "courseId": "course-456",
      "orderIndex": 0
    },
    {
      "courseId": "course-123",
      "orderIndex": 1
    }
  ]
}
```

### Response

**Status Code: 200**
```json
{
  "meta": {
    "code": "200000",
    "type": "SUCCESS",
    "message": "Course order updated successfully",
    "service_id": "learning-path-service",
    "extra_meta": {}
  },
  "data": {
    "pathId": "lp-789",
    "courses": [
      {
        "courseId": "course-456",
        "orderIndex": 0
      },
      {
        "courseId": "course-123",
        "orderIndex": 1
      }
    ]
  }
}
```

### Sample

```bash
curl --location --request PUT 'https://{{base_url}}/learning-path-service/v1/learning-paths/lp-789/courses/order' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>' \
--data '{
  "courseOrders": [
    {"courseId": "course-456", "orderIndex": 0},
    {"courseId": "course-123", "orderIndex": 1}
  ]
}'
```

---

## API Update Path Progress

### API Specs

| Field | Value |
|-------|-------|
| **Name** | Update Path Progress |
| **Description** | Update user progress for a learning path |
| **URL** | `https://{{base_url}}/learning-path-service/v1/progress/{pathId}` |
| **Method** | PUT |
| **Header** | Content-Type: application/json<br>Authorization: Bearer {token} |
| **Params** | pathId: string (required) - Learning path ID |

### Request Body

```json
{
  "userId": "user-123",
  "completedCourses": ["course-123"],
  "currentCourseId": "course-456",
  "progressPercentage": 45.5,
  "timeSpent": 54
}
```

### Response

**Status Code: 200**
```json
{
  "meta": {
    "code": "200000",
    "type": "SUCCESS",
    "message": "Progress updated successfully",
    "service_id": "learning-path-service",
    "extra_meta": {}
  },
  "data": {
    "id": "progress-001",
    "userId": "user-123",
    "learningPathId": "lp-789",
    "completedCourses": ["course-123"],
    "currentCourseId": "course-456",
    "progressPercentage": 45.5,
    "timeSpent": 54,
    "isCompleted": false,
    "lastAccessedAt": "2024-11-18T12:30:00",
    "updatedAt": "2024-11-18T12:30:00"
  }
}
```

### Sample

```bash
curl --location --request PUT 'https://{{base_url}}/learning-path-service/v1/progress/lp-789' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>' \
--data '{
  "userId": "user-123",
  "completedCourses": ["course-123"],
  "currentCourseId": "course-456",
  "progressPercentage": 45.5
}'
```

### Request Body Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| userId | String | ✓ | - | User ID |
| completedCourses | Array | - | [] | List of completed course IDs |
| currentCourseId | String | - | - | Current course being studied |
| progressPercentage | Double | - | 0.0 | Overall progress percentage |
| timeSpent | Integer | - | 0 | Time spent in hours |

---

## API Get User Progress

### API Specs

| Field | Value |
|-------|-------|
| **Name** | Get User Progress |
| **Description** | Retrieve user's progress for a specific learning path |
| **URL** | `https://{{base_url}}/learning-path-service/v1/progress/{pathId}/user/{userId}` |
| **Method** | GET |
| **Header** | Content-Type: application/json<br>Authorization: Bearer {token} |
| **Params** | pathId: string (required)<br>userId: string (required) |

### Request Body

EMPTY

### Response

**Status Code: 200**
```json
{
  "meta": {
    "code": "200000",
    "type": "SUCCESS",
    "message": "Success",
    "service_id": "learning-path-service",
    "extra_meta": {}
  },
  "data": {
    "id": "progress-001",
    "userId": "user-123",
    "learningPathId": "lp-789",
    "learningPathTitle": "Full Stack Web Development",
    "completedCourses": ["course-123"],
    "currentCourseId": "course-456",
    "progressPercentage": 45.5,
    "timeSpent": 54,
    "isCompleted": false,
    "startedAt": "2024-11-01T10:00:00",
    "lastAccessedAt": "2024-11-18T12:30:00",
    "estimatedCompletionDate": "2024-12-15T00:00:00"
  }
}
```

**Status Code: 404**
```json
{
  "meta": {
    "code": "404000",
    "type": "NOT_FOUND",
    "message": "Progress not found",
    "service_id": "learning-path-service",
    "extra_meta": {}
  },
  "data": null
}
```

### Sample

```bash
curl --location --request GET 'https://{{base_url}}/learning-path-service/v1/progress/lp-789/user/user-123' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>'
```

---

## API Get Leaderboard

### API Specs

| Field | Value |
|-------|-------|
| **Name** | Get Leaderboard |
| **Description** | Retrieve leaderboard rankings based on type |
| **URL** | `https://{{base_url}}/learning-path-service/v1/leaderboard` |
| **Method** | GET |
| **Header** | Content-Type: application/json<br>Authorization: Bearer {token} |
| **Params** | type: string (optional, default = "TOTAL_COURSES")<br>page: int (optional, default = 0)<br>size: int (optional, default = 20) |

### Request Body

EMPTY

### Response

**Status Code: 200**
```json
{
  "meta": {
    "code": "200000",
    "type": "SUCCESS",
    "message": "Success",
    "service_id": "learning-path-service",
    "extra_meta": {}
  },
  "data": {
    "leaderboardType": "TOTAL_COURSES",
    "rankings": [
      {
        "rank": 1,
        "userId": "user-123",
        "userName": "John Doe",
        "score": 25,
        "totalPathsCompleted": 5,
        "totalCoursesCompleted": 25,
        "totalTimeSpent": 300,
        "lastUpdated": "2024-11-18T12:00:00"
      },
      {
        "rank": 2,
        "userId": "user-456",
        "userName": "Jane Smith",
        "score": 22,
        "totalPathsCompleted": 4,
        "totalCoursesCompleted": 22,
        "totalTimeSpent": 280,
        "lastUpdated": "2024-11-18T11:30:00"
      }
    ],
    "pagination": {
      "total_pages": 3,
      "total_items": 50,
      "current_page": 0,
      "page_size": 20
    }
  }
}
```

### Sample

```bash
curl --location --request GET 'https://{{base_url}}/learning-path-service/v1/leaderboard?type=TOTAL_COURSES&page=0&size=20' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>'
```

### Response Body Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| leaderboardType | String | ✓ | - | Type of leaderboard (TOTAL_COURSES, TOTAL_PATHS, TIME_SPENT) |
| rankings | Array | ✓ | [] | List of user rankings |
| rank | Integer | ✓ | - | User's rank position |
| userId | String | ✓ | - | User ID |
| userName | String | ✓ | - | User's display name |
| score | Integer | ✓ | 0 | User's score based on leaderboard type |
| totalPathsCompleted | Integer | ✓ | 0 | Total learning paths completed |
| totalCoursesCompleted | Integer | ✓ | 0 | Total courses completed |
| totalTimeSpent | Integer | ✓ | 0 | Total time spent in hours |

### Errors

| Status Code | Code | Type | Description |
|-------------|------|------|-------------|
| 200 | 200000 | SUCCESS | Leaderboard retrieved successfully |
| 400 | 400000 | BAD_REQUEST | Invalid leaderboard type |
| 401 | 401000 | UNAUTHORIZED | Authentication required |

---

## API Delete Learning Path

### API Specs

| Field | Value |
|-------|-------|
| **Name** | Delete Learning Path |
| **Description** | Delete a learning path by ID |
| **URL** | `https://{{base_url}}/learning-path-service/v1/learning-paths/{pathId}` |
| **Method** | DELETE |
| **Header** | Content-Type: application/json<br>Authorization: Bearer {token} |
| **Params** | pathId: string (required) - Learning path ID |

### Request Body

EMPTY

### Response

**Status Code: 200**
```json
{
  "meta": {
    "code": "200000",
    "type": "SUCCESS",
    "message": "Learning path deleted successfully",
    "service_id": "learning-path-service",
    "extra_meta": {}
  },
  "data": null
}
```

**Status Code: 404**
```json
{
  "meta": {
    "code": "404000",
    "type": "NOT_FOUND",
    "message": "Learning path not found",
    "service_id": "learning-path-service",
    "extra_meta": {}
  },
  "data": null
}
```

### Sample

```bash
curl --location --request DELETE 'https://{{base_url}}/learning-path-service/v1/learning-paths/lp-789' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>'
```

### Errors

| Status Code | Code | Type | Description |
|-------------|------|------|-------------|
| 200 | 200000 | SUCCESS | Learning path deleted successfully |
| 401 | 401000 | UNAUTHORIZED | Authentication required |
| 404 | 404000 | NOT_FOUND | Learning path not found |

---

## Common Error Codes

| Status Code | Code | Type | Description |
|-------------|------|------|-------------|
| 200 | 200000 | SUCCESS | Request successful |
| 201 | 201000 | CREATED | Resource created successfully |
| 400 | 400000 | BAD_REQUEST | Invalid request data |
| 401 | 401000 | UNAUTHORIZED | Authentication required or failed |
| 403 | 403000 | FORBIDDEN | Insufficient permissions |
| 404 | 404000 | NOT_FOUND | Resource not found |
| 409 | 409000 | CONFLICT | Resource conflict |
| 500 | 500000 | INTERNAL_SERVER_ERROR | Internal server error |

---

## Data Models

### LearningPath

```json
{
  "id": "string",
  "title": "string",
  "description": "string",
  "difficulty": "BEGINNER | INTERMEDIATE | ADVANCED",
  "estimatedDuration": "integer",
  "prerequisites": ["string"],
  "courses": [
    {
      "courseId": "string",
      "orderIndex": "integer"
    }
  ],
  "totalEnrollments": "integer",
  "averageRating": "double",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

### PathProgress

```json
{
  "id": "string",
  "userId": "string",
  "learningPathId": "string",
  "completedCourses": ["string"],
  "currentCourseId": "string",
  "progressPercentage": "double",
  "timeSpent": "integer",
  "isCompleted": "boolean",
  "startedAt": "datetime",
  "lastAccessedAt": "datetime",
  "completedAt": "datetime"
}
```

### Leaderboard

```json
{
  "rank": "integer",
  "userId": "string",
  "userName": "string",
  "score": "integer",
  "totalPathsCompleted": "integer",
  "totalCoursesCompleted": "integer",
  "totalTimeSpent": "integer",
  "lastUpdated": "datetime"
}
```
```