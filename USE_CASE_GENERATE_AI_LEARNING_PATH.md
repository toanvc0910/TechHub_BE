# 4.4.1 Generate AI Learning Path

| [1]       | Generate AI Learning Path |
| --------- | ------------------------- |
| **Actor** | Instructor / Admin        |

| **Trigger** | The user (instructor/admin) accesses the Manage Learning Paths page and clicks the "Generate Learning Path with AI" button to create a personalized learning path using artificial intelligence. |

| **Description** | This Use Case allows instructors and administrators to generate personalized learning paths automatically using AI. The system analyzes the user's learning goals, current skill level, target level, and timeframe to create a structured learning path with recommended courses, job positions, and projects. The AI-generated path can then be reviewed, edited, and saved as a draft before publishing. |

| **Pre-Conditions** | - The user is logged in as an Instructor or Admin<br>- The user has access to the Manage Learning Paths page (/manage/learning-paths)<br>- The AI service is available and running<br>- There are published courses in the system for AI to recommend |

| **Post-Conditions** | - An AI-generated learning path draft is created<br>- The learning path includes nodes (courses) and edges (connections)<br>- The task ID is stored for tracking the generation process<br>- The user is redirected to the path designer page<br>- The generated path is saved in the AI Learning Path context<br>- The system displays a success notification |

| **Main Flow** | 1. The user navigates to the Manage Learning Paths page (/manage/learning-paths).<br>2. The user clicks the "Generate Learning Path with AI" button (with Sparkles icon).<br>3. The system opens a modal dialog titled "Generate Learning Path with AI".<br>4. The user fills in the required fields:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Learning Goal (required) - A text description of what they want to achieve<br>&nbsp;&nbsp;&nbsp;&nbsp;b. Timeframe - Select from: 1-3 months, 3-6 months, 6-12 months, 1-2 years (default: 3-6 months)<br>&nbsp;&nbsp;&nbsp;&nbsp;c. Current Level - Select from: Beginner, Intermediate, Advanced (default: Beginner)<br>&nbsp;&nbsp;&nbsp;&nbsp;d. Target Level - Select from: Intermediate, Advanced, Expert (default: Intermediate)<br>&nbsp;&nbsp;&nbsp;&nbsp;e. Language - Select from: Vietnamese, English, Japanese (default: Vietnamese)<br>&nbsp;&nbsp;&nbsp;&nbsp;f. Include Job Positions - Checkbox (default: checked)<br>&nbsp;&nbsp;&nbsp;&nbsp;g. Include Projects - Checkbox (default: checked)<br>5. (Optional) The user modifies generation parameters:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. User changes any field value (goal, timeframe, levels, language).<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System updates the form state in real-time.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. User can adjust parameters before generating.<br>6. (Optional) The user toggles optional features:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. User unchecks "Include Job Positions" or "Include Projects" checkbox.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System updates corresponding parameters to false.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. AI will exclude those features from the generated path.<br>7. The user reviews the notes/hints displayed at the bottom of the form:<br>&nbsp;&nbsp;&nbsp;&nbsp;- "AI will analyze your goals and suggest suitable courses"<br>&nbsp;&nbsp;&nbsp;&nbsp;- "The path will be created as a draft, you can edit it before saving"<br>&nbsp;&nbsp;&nbsp;&nbsp;- "The process may take 10-30 seconds"<br>8. The user clicks the "Generate Path" button.<br>9. The system validates the form:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Learning goal must not be empty<br>&nbsp;&nbsp;&nbsp;&nbsp;- User ID must be available (from authentication)<br>10. The system disables the form and shows a loading spinner on the button.<br>11. The system sends a POST request to the AI service with the generation parameters.<br>12. The AI service processes the request (10-30 seconds):<br>&nbsp;&nbsp;&nbsp;&nbsp;- Analyzes the learning goal using NLP<br>&nbsp;&nbsp;&nbsp;&nbsp;- Searches for relevant courses in the vector database (Qdrant)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Creates a structured learning path with nodes and edges<br>&nbsp;&nbsp;&nbsp;&nbsp;- Generates course sequence based on difficulty progression<br>&nbsp;&nbsp;&nbsp;&nbsp;- Adds job position recommendations (if enabled)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Adds project suggestions (if enabled)<br>13. The AI service returns the generated learning path with:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Task ID for tracking<br>&nbsp;&nbsp;&nbsp;&nbsp;- Path structure (title, description, nodes, edges)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Metadata (total courses, estimated weeks)<br>14. The system stores the generated path in the AI Learning Path context.<br>15. The system displays a success toast: "Learning path created successfully!"<br>16. The system closes the modal dialog.<br>17. The system redirects the user to the path designer page: `/manage/learning-paths/new/designer?fromAi=true`<br>18. The user reviews the AI-generated learning path in the designer.<br>19. (Optional) The user edits the AI-generated path:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. User drags and repositions course nodes on the canvas.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. User adds or removes courses from the path.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. User modifies connections (edges) between courses.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. User updates path title or description.<br>&nbsp;&nbsp;&nbsp;&nbsp;e. See Use Case 4.4.2 - Edit Learning Path in Designer.<br>20. The user saves the learning path (AI-generated or edited version).<br>21. (Optional - Before generating) The user cancels the generation:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. User clicks the "Cancel" button or closes the dialog.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System clears all form fields and resets to default values.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. Dialog is closed without generating anything.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. User returns to the Manage Learning Paths page. |

| **Alternate Flow** | **16.1** User creates learning path manually without AI (traditional method):<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Instead of clicking "Generate with AI", user clicks "Create New Path" button.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System opens the path designer with blank canvas.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. User manually enters path title and description.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. User manually searches and adds courses as nodes.<br>&nbsp;&nbsp;&nbsp;&nbsp;e. User manually connects courses with edges (prerequisites).<br>&nbsp;&nbsp;&nbsp;&nbsp;f. User manually positions nodes on canvas.<br>&nbsp;&nbsp;&nbsp;&nbsp;g. User saves the manually-created learning path as draft.<br>&nbsp;&nbsp;&nbsp;&nbsp;h. **User successfully creates a new learning path** (same outcome as Main Flow).<br><br>**16.2** User duplicates existing learning path and modifies it:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. User views list of existing learning paths.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. User clicks "Duplicate" button on a published path.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. System creates a copy with all nodes and edges.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. System opens designer with duplicated content.<br>&nbsp;&nbsp;&nbsp;&nbsp;e. User modifies title, courses, or structure as needed.<br>&nbsp;&nbsp;&nbsp;&nbsp;f. User saves the modified path as new learning path.<br>&nbsp;&nbsp;&nbsp;&nbsp;g. **User successfully creates a new learning path** (same outcome as Main Flow).<br><br>**16.3** User imports learning path from template or file:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. User clicks "Import Template" button.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System displays available templates (e.g., "Web Dev Bootcamp", "Data Science Path").<br>&nbsp;&nbsp;&nbsp;&nbsp;c. User selects a template or uploads JSON file.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. System loads template structure into designer.<br>&nbsp;&nbsp;&nbsp;&nbsp;e. User reviews and customizes the imported path.<br>&nbsp;&nbsp;&nbsp;&nbsp;f. User saves the customized path as new learning path.<br>&nbsp;&nbsp;&nbsp;&nbsp;g. **User successfully creates a new learning path** (same outcome as Main Flow). |

| **Exception Flow** | **7.1** If learning goal field is empty:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays error toast with title "Error".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Message: "Learning Goal is required" or localized equivalent.<br>&nbsp;&nbsp;&nbsp;&nbsp;- The "Generate Path" button remains disabled.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User must enter a goal before proceeding.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User remains in the dialog.<br><br>**7.2** If user ID is not available (authentication issue):<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays error toast: "User authentication error".<br>&nbsp;&nbsp;&nbsp;&nbsp;- User is prompted to log in again.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Dialog is closed.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User returns to login page.<br><br>**10.1** If AI service is unavailable or times out:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays error toast: "Failed to generate learning path".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Additional message: "AI service is currently unavailable. Please try again later."<br>&nbsp;&nbsp;&nbsp;&nbsp;- The form is re-enabled for retry.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Loading spinner is removed.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User can modify parameters and retry.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User remains in the dialog.<br><br>**10.2** If AI generation fails due to insufficient data:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays error toast: "Could not generate path".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Message: "No suitable courses found for your learning goal. Try modifying your goal or parameters."<br>&nbsp;&nbsp;&nbsp;&nbsp;- User can edit the goal and retry.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User remains in the dialog.<br><br>**11.1** If AI service returns error response:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system parses the error message from the response.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Displays error toast with specific error message.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Form is re-enabled for retry.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User can adjust parameters and try again.<br><br>**11.2** If network error occurs during generation:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays error toast: "Network error".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Message: "Please check your connection and try again."<br>&nbsp;&nbsp;&nbsp;&nbsp;- Form is re-enabled.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User can retry the operation.<br><br>**12.1** If generated path has no courses (empty path):<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays warning toast: "Path generated with no courses".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Message: "Please refine your learning goal for better results."<br>&nbsp;&nbsp;&nbsp;&nbsp;- User is still redirected to designer.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User can manually add courses in the designer. |

---

## Related Use Cases

This use case connects to the learning path management workflow:

- **View Learning Paths** (Use Case 4.4.0): User views all learning paths before generating new ones.
- **Edit Learning Path in Designer** (Use Case 4.4.2): User edits the AI-generated path after generation.
- **Save Learning Path** (Use Case 4.4.3): User saves the finalized learning path after editing.
- **Manage Courses** (Use Case 4.5.1): Courses in the system are used by AI for recommendations.

---

## API Endpoints

**Frontend Proxy Route:**

```
POST /app/api/proxy/ai/learning-paths/generate
```

**Backend API:**

```
POST /ai/learning-paths/generate
```

**Request Body:**

```json
{
  "goal": "Learn full-stack web development to become a professional developer",
  "timeframe": "3-6 months",
  "language": "vi",
  "currentLevel": "BEGINNER",
  "targetLevel": "INTERMEDIATE",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "preferredCourseIds": [],
  "includePositions": true,
  "includeProjects": true,
  "duration": "3-6 months",
  "level": "BEGINNER"
}
```

**Success Response (200 OK):**

```json
{
  "payload": {
    "data": {
      "taskId": "task-789abc",
      "status": "COMPLETED",
      "path": {
        "title": "Full-Stack Web Development Path",
        "description": "A comprehensive learning path to become a full-stack web developer",
        "estimatedDuration": "5 months",
        "nodes": [
          {
            "id": "node-1",
            "type": "course",
            "data": {
              "courseId": "course-123",
              "title": "HTML & CSS Fundamentals",
              "description": "Learn the basics of web development",
              "estimatedWeeks": 2
            },
            "position": {
              "x": 100,
              "y": 100
            }
          },
          {
            "id": "node-2",
            "type": "course",
            "data": {
              "courseId": "course-456",
              "title": "JavaScript Essentials",
              "description": "Master JavaScript programming",
              "estimatedWeeks": 4
            },
            "position": {
              "x": 300,
              "y": 100
            }
          },
          {
            "id": "node-3",
            "type": "course",
            "data": {
              "courseId": "course-789",
              "title": "React.js Complete Guide",
              "description": "Build modern web applications with React",
              "estimatedWeeks": 6
            },
            "position": {
              "x": 500,
              "y": 100
            }
          }
        ],
        "edges": [
          {
            "id": "edge-1",
            "source": "node-1",
            "target": "node-2",
            "type": "default"
          },
          {
            "id": "edge-2",
            "source": "node-2",
            "target": "node-3",
            "type": "default"
          }
        ],
        "metadata": {
          "totalCourses": 3,
          "estimatedWeeks": 20
        }
      }
    }
  }
}
```

**Error Response (400 Bad Request):**

```json
{
  "success": false,
  "message": "Goal must be at least 5 characters",
  "timestamp": "2024-11-27T10:30:00Z",
  "code": 400
}
```

**Error Response (500 Internal Server Error):**

```json
{
  "success": false,
  "message": "AI service is currently unavailable",
  "timestamp": "2024-11-27T10:30:00Z",
  "code": 500
}
```

---

## UI Components

**Location:** Manage Learning Paths Page - Modal Dialog

**Key Components:**

1. **Trigger Button**

   - Text: "Generate Learning Path with AI"
   - Icon: Sparkles (indicating AI feature)
   - Positioned in the page header/toolbar
   - Primary button styling

2. **Modal Dialog**

   - Title: "Generate Learning Path with AI" with Sparkles icon
   - Description: Brief explanation of the feature
   - Max width: 600px
   - Centered on screen
   - Overlay backdrop

3. **Learning Goal Input**

   - Label: "Learning Goal" with red asterisk (required)
   - Component: Textarea (3 rows)
   - Placeholder: Localized placeholder text
   - Character validation: Minimum 5 characters
   - Real-time validation

4. **Timeframe Selector**

   - Label: "Timeframe"
   - Component: Select dropdown
   - Options:
     - 1-3 months
     - 3-6 months (default)
     - 6-12 months
     - 1-2 years

5. **Level Selectors (Grid Layout)**

   - Two columns: Current Level and Target Level
   - Current Level options: Beginner, Intermediate, Advanced
   - Target Level options: Intermediate, Advanced, Expert
   - Default: Beginner → Intermediate

6. **Language Selector**

   - Label: "Language"
   - Component: Select dropdown
   - Options:
     - Tiếng Việt (vi)
     - English (en)
     - 日本語 (ja)
   - Default: Vietnamese

7. **Options Checkboxes**

   - Label: "Options"
   - Checkbox 1: "Include Job Positions" (default: checked)
   - Checkbox 2: "Include Projects" (default: checked)
   - Aligned vertically with spacing

8. **Information Panel**

   - Background: Light blue (bg-blue-50)
   - Icon: 💡 (lightbulb)
   - Title: "Note:"
   - Three bullet points with helpful information
   - Small font size
   - Positioned at bottom of form

9. **Action Buttons (Footer)**

   - Cancel Button:
     - Text: "Cancel"
     - Variant: Outline
     - Disabled during generation
   - Generate Button:
     - Text: "Generate Path"
     - Icon: Sparkles
     - Disabled when: goal is empty OR generation is in progress
     - Shows loading spinner when generating
     - Text changes to "Loading..." during generation

10. **Loading States**
    - Button spinner during generation
    - Disabled form inputs during generation
    - Visual feedback for async operation

---

## Validation Rules

**Learning Goal:**

- Required field (cannot be empty)
- Minimum length: 5 characters
- Maximum length: 1000 characters (reasonable limit)
- Trimmed before validation (whitespace removed)

**User Authentication:**

- User must be logged in
- User must have Instructor or Admin role
- Valid user ID (UUID format)

**Timeframe:**

- Must select one of the predefined options
- Cannot be empty
- Default: "3-6 months"

**Levels:**

- Current level must be lower than or equal to target level
- Both must be selected
- Cannot have Beginner → Beginner (no progression)

**Language:**

- Must be one of: vi, en, ja
- Default: vi (Vietnamese)

---

## Business Rules

1. **AI Generation Process:**

   - Uses Natural Language Processing (NLP) to analyze learning goal
   - Searches courses using vector similarity (Qdrant)
   - Ranks courses by relevance to goal
   - Creates logical progression from current to target level
   - Estimates time based on course durations and timeframe

2. **Path Structure:**

   - Each node represents a course or milestone
   - Edges represent dependencies/prerequisites
   - Nodes positioned automatically by AI
   - Can include non-course nodes (projects, positions)

3. **Course Selection:**

   - Only PUBLISHED courses are considered
   - Courses must match selected language (preferred)
   - Difficulty increases progressively
   - Prerequisites are respected
   - Maximum 10-15 courses per path (prevents overwhelming)

4. **Performance:**

   - Generation time: 10-30 seconds typical
   - Timeout: 60 seconds maximum
   - Asynchronous processing with task ID
   - Results cached temporarily

5. **Draft Management:**

   - Generated paths start as DRAFT status
   - Stored in AI Learning Path context
   - Task ID tracked for status monitoring
   - User can edit before finalizing
   - Auto-saved in context for session persistence

6. **Job Positions & Projects:**
   - Included only if corresponding checkbox is checked
   - Positions suggested based on target level and skills
   - Projects matched to learning goals
   - Optional features, can be disabled

---

## User Experience Flow

**Step-by-Step Journey:**

1. **Discovery**

   - User sees "Generate with AI" button
   - Recognizes Sparkles icon as AI feature
   - Clicks to explore

2. **Input**

   - Reads clear instructions
   - Fills in learning goal (main input)
   - Adjusts optional parameters
   - Reviews helpful notes

3. **Generation**

   - Clicks "Generate Path"
   - Sees loading feedback (10-30s)
   - Understands process is ongoing

4. **Result**

   - Receives success notification
   - Automatically redirected to designer
   - Sees visual path layout
   - Can edit and customize

5. **Finalization**
   - Reviews AI suggestions
   - Makes adjustments if needed
   - Saves as learning path
   - Publishes for students

---

## User Messages

**Success Messages:**

- "Learning path created successfully!"
- "AI has generated {count} courses for your path"
- "Path saved to drafts"

**Error Messages:**

- "Learning Goal is required"
- "Please enter at least 5 characters"
- "Failed to generate learning path"
- "AI service is currently unavailable. Please try again later."
- "No suitable courses found for your learning goal. Try modifying your goal or parameters."
- "Network error. Please check your connection and try again."
- "User authentication error"

**Loading Message:**

- "Loading..." (on button during generation)

**Information Notes:**

- "💡 Note:"
- "AI will analyze your goals and suggest suitable courses"
- "The path will be created as a draft, you can edit it before saving"
- "The process may take 10-30 seconds"

---

## Generation Flow Diagram

```
Manage Learning Paths Page
        ↓
Click "Generate with AI"
        ↓
    ┌─────────────────────────┐
    │   Open Modal Dialog     │
    │   - Input Learning Goal │
    │   - Select Parameters   │
    │   - Configure Options   │
    └─────────────────────────┘
        ↓
    Click "Generate Path"
        ↓
    ┌─────────────────────────┐
    │   Validation Check      │
    │   - Goal not empty?     │
    │   - User authenticated? │
    └─────────────────────────┘
        ↓ (Valid)
    ┌─────────────────────────┐
    │   Send to AI Service    │
    │   Processing (10-30s)   │
    │   - Analyze goal (NLP)  │
    │   - Search courses      │
    │   - Create structure    │
    └─────────────────────────┘
        ↓
    ┌─────────────────────────┐
    │   Receive Response      │
    │   - Task ID             │
    │   - Path structure      │
    │   - Nodes & Edges       │
    └─────────────────────────┘
        ↓
    Save to Context
        ↓
    Success Toast
        ↓
    Redirect to Designer
        ↓
    /manage/learning-paths/new/designer?fromAi=true
        ↓
    ┌─────────────────────────┐
    │   Path Designer         │
    │   - Visual layout       │
    │   - Edit nodes/edges    │
    │   - Save as draft       │
    └─────────────────────────┘
```

---

## Technical Notes

1. **State Management:**

   - React useState for form fields
   - AI Learning Path Context for generated path
   - React Query mutation for API calls
   - Local storage for draft persistence (optional)

2. **AI Technology Stack:**

   - NLP for goal analysis
   - Vector database (Qdrant) for course search
   - Similarity scoring for course ranking
   - Graph algorithms for path structuring

3. **Form Handling:**

   - Direct state management (not React Hook Form)
   - Real-time validation on input change
   - Disabled state during submission
   - Auto-reset on dialog close

4. **Navigation:**

   - Next.js router for page transitions
   - Query parameter `fromAi=true` to indicate AI source
   - Context preserves data across navigation
   - Designer page checks context for AI path

5. **Performance Optimizations:**

   - Debounced goal input (avoid excessive validation)
   - Memoized select options
   - Lazy loading of designer page
   - Task ID for async tracking

6. **Error Handling:**

   - Try-catch blocks for API calls
   - User-friendly error messages
   - Toast notifications for feedback
   - Console logging for debugging

7. **Internationalization:**

   - All text translatable via useTranslations
   - Supports vi, en, ja languages
   - Dynamic placeholder text
   - Localized error messages

8. **Security:**
   - User authentication required
   - Role-based access (Instructor/Admin only)
   - Input sanitization on backend
   - Rate limiting on AI service (prevent abuse)

---

## Instructor: Dr. Mai Anh Tho

## Project: TechHub - Software Engineering

## Page: 44
