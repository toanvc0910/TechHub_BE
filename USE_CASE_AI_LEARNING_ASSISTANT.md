# 4.5.1 AI Learning Assistant

| [1]       | AI Learning Assistant |
| --------- | --------------------- |
| **Actor** | Registered User       |

| **Trigger** | The user accesses the AI Chat page (/ai-chat) and wants to ask questions about programming, get learning advice, or receive personalized course recommendations from the AI assistant. |

| **Description** | This Use Case allows registered users to interact with an AI-powered chatbot that can answer general programming questions or provide personalized learning advice. The system supports two modes: General Mode (answers general programming questions) and Advisor Mode (provides personalized advice based on user's learning progress). Chat history is saved in sessions, allowing users to continue previous conversations and manage multiple chat sessions. |

| **Pre-Conditions** | - The user is logged in to the system<br>- The user has access to the AI Chat page (/ai-chat)<br>- The AI service is available and running<br>- User ID is available from authentication |

| **Post-Conditions** | - A new chat message is created and saved in the current session<br>- The AI response is generated and displayed to the user<br>- The chat history is updated in the database<br>- The session is saved with all messages for future reference<br>- User can view, continue, or delete chat sessions |

| **Main Flow** | 1. The user navigates to the AI Chat page (/ai-chat).<br>2. The system loads user ID from localStorage (from userInfo).<br>3. The system fetches all existing chat sessions for the user from the database.<br>4. The system auto-selects the most recent session (if exists).<br>5. The system fetches all messages for the selected session from the database.<br>6. The system displays the chat interface with:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Left sidebar: Settings panel with mode selection, progress toggle, suggested questions<br>&nbsp;&nbsp;&nbsp;&nbsp;- Main area: Chat messages and input box<br>&nbsp;&nbsp;&nbsp;&nbsp;- Header: Session selector and "New Session" button<br>7. The user selects chat mode:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. General Mode - For general programming questions<br>&nbsp;&nbsp;&nbsp;&nbsp;b. Advisor Mode - For personalized learning advice<br>8. (Optional - Advisor Mode only) The user enables "Use My Progress" checkbox to include learning progress in AI context.<br>9. The user types a message in the textarea input field.<br>10. The user presses Enter or clicks the Send button.<br>11. The system validates that the message is not empty.<br>12. The system adds the user message to the chat display.<br>13. The system shows a typing indicator ("...") from the AI assistant.<br>14. The system sends a POST request to the AI service with:<br>&nbsp;&nbsp;&nbsp;&nbsp;- sessionId (if continuing existing session, or undefined for new session)<br>&nbsp;&nbsp;&nbsp;&nbsp;- userId<br>&nbsp;&nbsp;&nbsp;&nbsp;- mode (GENERAL or ADVISOR)<br>&nbsp;&nbsp;&nbsp;&nbsp;- message (user's question)<br>&nbsp;&nbsp;&nbsp;&nbsp;- context (includes progress data if enabled)<br>15. The AI service processes the request:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Analyzes the question using NLP<br>&nbsp;&nbsp;&nbsp;&nbsp;- If General Mode: Searches general programming knowledge<br>&nbsp;&nbsp;&nbsp;&nbsp;- If Advisor Mode: Searches course/lesson database, considers user's progress<br>&nbsp;&nbsp;&nbsp;&nbsp;- Generates an appropriate response using LLM<br>&nbsp;&nbsp;&nbsp;&nbsp;- Creates or updates chat session in database<br>&nbsp;&nbsp;&nbsp;&nbsp;- Saves both user message and AI response<br>16. The AI service returns:<br>&nbsp;&nbsp;&nbsp;&nbsp;- sessionId (new session ID if first message, or existing session ID)<br>&nbsp;&nbsp;&nbsp;&nbsp;- messageId (ID of the saved message)<br>&nbsp;&nbsp;&nbsp;&nbsp;- message/answer (AI's response text)<br>&nbsp;&nbsp;&nbsp;&nbsp;- timestamp<br>17. The system removes the typing indicator.<br>18. The system displays the AI response with:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Bot avatar icon<br>&nbsp;&nbsp;&nbsp;&nbsp;- Response text (with clickable links if present)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Timestamp<br>&nbsp;&nbsp;&nbsp;&nbsp;- Copy button<br>19. The system updates the sessionId if it was a new session.<br>20. The system refreshes the session list to show the new session.<br>21. The system clears the input field.<br>22. The user can continue the conversation by typing another message (repeat from step 9).<br>23. (Optional) The user clicks a suggested question:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. System auto-fills the input field with the suggested question.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. User can edit or send directly.<br>24. (Optional) The user copies an AI response:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. User clicks the Copy button on a message.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System copies message content to clipboard.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. Copy icon changes to CheckCircle icon for 2 seconds.<br>25. (Optional) The user switches between chat modes:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. User selects different mode from dropdown (General ↔ Advisor).<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System updates mode state and suggested questions.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. Next messages use the new mode. |

| **Alternate Flow** | **25.1** User creates a new chat session instead of continuing existing one:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. User clicks "New Session" button in the header.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System sends POST request to create empty session with userId and mode.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. Backend creates new session record and returns session ID.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. System sets the new sessionId as current.<br>&nbsp;&nbsp;&nbsp;&nbsp;e. System clears all messages in the chat area.<br>&nbsp;&nbsp;&nbsp;&nbsp;f. System displays success toast: "Created a new chat session".<br>&nbsp;&nbsp;&nbsp;&nbsp;g. System adds new session to session list.<br>&nbsp;&nbsp;&nbsp;&nbsp;h. User can start chatting in the new session (same as Main Flow).<br>&nbsp;&nbsp;&nbsp;&nbsp;i. **User successfully starts a conversation with AI** (same outcome as Main Flow).<br><br>**25.2** User switches to a previous chat session:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. User selects a different session from the session dropdown.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System updates current sessionId.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. System fetches messages for the selected session from database.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. System displays all previous messages in chronological order.<br>&nbsp;&nbsp;&nbsp;&nbsp;e. User can continue the conversation from where they left off.<br>&nbsp;&nbsp;&nbsp;&nbsp;f. User sends new messages (same as Main Flow steps 9-22).<br>&nbsp;&nbsp;&nbsp;&nbsp;g. **User successfully continues a previous conversation** (same outcome as Main Flow).<br><br>**25.3** User uses suggested question instead of typing manually:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. User clicks one of the suggested questions in the sidebar.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System auto-fills the input field with the question text.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. User reviews the question (can edit if needed).<br>&nbsp;&nbsp;&nbsp;&nbsp;d. User clicks Send button or presses Enter.<br>&nbsp;&nbsp;&nbsp;&nbsp;e. System processes the message (same as Main Flow steps 11-22).<br>&nbsp;&nbsp;&nbsp;&nbsp;f. **User successfully gets AI response** (same outcome as Main Flow). |

| **Exception Flow** | **11.1** If message input is empty or only whitespace:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The Send button remains disabled.<br>&nbsp;&nbsp;&nbsp;&nbsp;- No request is sent to the AI service.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User must type a valid message.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User remains on the page.<br><br>**11.2** If user is not logged in (userId is empty):<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays error toast: "Please login before chatting with AI".<br>&nbsp;&nbsp;&nbsp;&nbsp;- The Send button is disabled.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Banner message is shown: "Login Required - Please login to use AI chat feature and save your chat history".<br>&nbsp;&nbsp;&nbsp;&nbsp;- User should navigate to login page.<br><br>**15.1** If AI service is unavailable or times out:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system removes the typing indicator.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Displays error toast: "Could not send message".<br>&nbsp;&nbsp;&nbsp;&nbsp;- User's message remains in the chat (not lost).<br>&nbsp;&nbsp;&nbsp;&nbsp;- User can retry sending the message.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User remains on the page.<br><br>**15.2** If AI service returns error response:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system removes the typing indicator.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Displays error toast with specific error message from server.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User's message is displayed but no AI response is added.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User can send another message.<br><br>**16.1** If AI response is empty or invalid:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays fallback message: "Sorry, I cannot answer this question."<br>&nbsp;&nbsp;&nbsp;&nbsp;- User is encouraged to rephrase the question.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Conversation can continue.<br><br>**16.2** If network error occurs during message sending:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system removes typing indicator.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Displays error toast: "Could not send message".<br>&nbsp;&nbsp;&nbsp;&nbsp;- User can check connection and retry.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User's message remains visible.<br><br>**19.1** If session creation fails (new conversation):<br>&nbsp;&nbsp;&nbsp;&nbsp;- System displays error toast: "Failed to create session".<br>&nbsp;&nbsp;&nbsp;&nbsp;- User can try clicking "New Session" again.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Existing sessions are not affected.<br><br>**Database Error** If fetching sessions or messages fails:<br>&nbsp;&nbsp;&nbsp;&nbsp;- System shows empty message list.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User can still send messages (will create new session).<br>&nbsp;&nbsp;&nbsp;&nbsp;- Error is logged to console.<br><br>**Delete Session Error** If deleting session fails:<br>&nbsp;&nbsp;&nbsp;&nbsp;- System displays error toast: "Failed to delete session".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Session remains in the list.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User can try again or contact support. |

---

## Related Use Cases

This use case connects to the AI-powered learning features:

- **View Learning Progress** (Use Case 4.2.1): User's progress data is used in Advisor Mode for personalized recommendations.
- **View Course List** (Use Case 4.1.1): AI can recommend courses from the available course catalog.
- **Generate AI Learning Path** (Use Case 4.4.1): Similar AI technology used for learning path generation.
- **Manage AI Drafts** (Use Case 4.6.1): AI-generated content management system.

---

## API Endpoints

**Frontend Proxy Route:**

```
POST /app/api/proxy/ai/chat/messages
POST /app/api/proxy/ai/chat/sessions
GET /app/api/proxy/ai/chat/sessions
GET /app/api/proxy/ai/chat/sessions/{sessionId}/messages
DELETE /app/api/proxy/ai/chat/sessions/{sessionId}
```

**Backend API:**

```
POST /ai/chat/messages
POST /ai/chat/sessions
GET /ai/chat/sessions
GET /ai/chat/sessions/{sessionId}/messages
DELETE /ai/chat/sessions/{sessionId}
```

**Send Chat Message Request:**

```json
POST /ai/chat/messages
{
  "sessionId": "123e4567-e89b-12d3-a456-426614174000",
  "userId": "987e6543-e21b-12d3-a456-426614174111",
  "mode": "ADVISOR",
  "message": "Which course should I take next?",
  "context": {
    "includeProgress": true
  }
}
```

**Send Chat Message Response (200 OK):**

```json
{
  "success": true,
  "status": "OK",
  "message": "Chat processed",
  "timestamp": "2024-11-27T10:30:00Z",
  "code": 200,
  "payload": {
    "data": {
      "sessionId": "123e4567-e89b-12d3-a456-426614174000",
      "messageId": "msg-456",
      "timestamp": "2024-11-27T10:30:05Z",
      "mode": "ADVISOR",
      "message": "Based on your current progress in JavaScript, I recommend taking the React.js course next. You've completed the fundamentals and are ready for frontend frameworks. [View React Course](/courses/react-basics-course-uuid)",
      "metadata": {
        "tokensUsed": 150,
        "model": "gpt-4"
      }
    }
  }
}
```

**Create Session Request:**

```json
POST /ai/chat/sessions?userId=987e6543-e21b-12d3-a456-426614174111&mode=GENERAL
{}
```

**Create Session Response (201 Created):**

```json
{
  "success": true,
  "status": "CREATED",
  "message": "Session created successfully",
  "timestamp": "2024-11-27T10:00:00Z",
  "code": 201,
  "payload": {
    "data": {
      "id": "new-session-789",
      "userId": "987e6543-e21b-12d3-a456-426614174111",
      "startedAt": "2024-11-27T10:00:00Z",
      "endedAt": null,
      "context": null
    }
  }
}
```

**Get User Sessions Response (200 OK):**

```json
{
  "success": true,
  "status": "OK",
  "message": "Sessions retrieved successfully",
  "timestamp": "2024-11-27T10:00:00Z",
  "code": 200,
  "payload": {
    "data": [
      {
        "id": "session-1",
        "userId": "987e6543-e21b-12d3-a456-426614174111",
        "startedAt": "2024-11-27T09:00:00Z",
        "endedAt": null,
        "context": null
      },
      {
        "id": "session-2",
        "userId": "987e6543-e21b-12d3-a456-426614174111",
        "startedAt": "2024-11-26T14:00:00Z",
        "endedAt": "2024-11-26T15:30:00Z",
        "context": null
      }
    ]
  }
}
```

**Get Session Messages Response (200 OK):**

```json
{
  "success": true,
  "status": "OK",
  "message": "Messages retrieved successfully",
  "timestamp": "2024-11-27T10:00:00Z",
  "code": 200,
  "payload": {
    "data": [
      {
        "id": "msg-1",
        "sessionId": "session-1",
        "sender": "USER",
        "content": "What is the difference between let and const?",
        "timestamp": "2024-11-27T09:05:00Z"
      },
      {
        "id": "msg-2",
        "sessionId": "session-1",
        "sender": "BOT",
        "content": "The main difference is that 'let' allows reassignment while 'const' creates a constant reference...",
        "timestamp": "2024-11-27T09:05:03Z"
      }
    ]
  }
}
```

**Delete Session Response (200 OK):**

```json
{
  "success": true,
  "status": "OK",
  "message": "Session deleted successfully",
  "timestamp": "2024-11-27T10:30:00Z",
  "code": 200,
  "payload": {
    "data": null
  }
}
```

**Error Response (400 Bad Request):**

```json
{
  "success": false,
  "status": "BAD_REQUEST",
  "message": "Message cannot be empty",
  "timestamp": "2024-11-27T10:30:00Z",
  "code": 400
}
```

**Error Response (401 Unauthorized):**

```json
{
  "success": false,
  "status": "UNAUTHORIZED",
  "message": "Please login to use AI chat",
  "timestamp": "2024-11-27T10:30:00Z",
  "code": 401
}
```

---

## UI Components

**Page Location:** `/ai-chat`

**Key Components:**

1. **Authentication Banner** (when not logged in)

   - Yellow/amber background with warning
   - MessageCircle icon
   - Title: "Login Required"
   - Description: "Please login to use AI chat feature and save your chat history"

2. **Page Header**

   - Title: "AI Assistant" with MessageCircle icon
   - Description: Brief explanation of AI chat feature
   - Session selector dropdown (200px width)
   - Delete session button (trash icon, shown when session is selected)
   - "New Session" button (outline variant)

3. **Left Sidebar - Settings Panel** (1 column on mobile, 1/4 on desktop)

   - Title: "Settings"
   - **Mode Selection**:
     - Label: "Mode"
     - Select dropdown with two options:
       - General (MessageSquare icon) - "Answer general programming questions"
       - Advisor (GraduationCap icon) - "Advise based on your learning progress"
   - **Progress Toggle** (Advisor mode only):
     - Checkbox: "Use my progress"
     - Enables/disables including user progress in AI context
   - **Session Info**:
     - Label: "Current Session"
     - Shows: Session ID (first 8 chars) and message count
     - Or: "New Session" if no session selected
   - **Suggested Questions**:
     - Label: "Suggested Questions"
     - 4 preset questions based on mode
     - Clickable buttons that auto-fill input
     - Small text, multi-line, outline variant

4. **Main Chat Area** (3 columns on desktop)

   - **Header**:
     - Title: "Chat"
     - Description: "Mode: [General/Advisor]" with badge
   - **Messages ScrollArea** (height: 500px):
     - Empty state (when no messages):
       - Bot icon (large, faded)
       - Text: "No messages yet"
       - Subtext: "Send a message to start chatting"
     - Message items (for each message):
       - Avatar (user: blue, bot: purple)
       - Message bubble (user: right-aligned blue, bot: left-aligned gray)
       - Content with clickable markdown links
       - Timestamp (small, muted)
       - Copy button (bot messages only)
       - Typing indicator (3 bouncing dots when loading)
   - **Input Area**:
     - Textarea (3 rows, flexible height)
     - Placeholder: "Type your message..."
     - Send button (with Send icon or Loader2 when pending)
     - Disabled when: empty input, pending request, or not logged in
     - Keyboard shortcut: Enter to send, Shift+Enter for new line
     - Helper text: "Press Enter to send, Shift + Enter for new line"

5. **Session Management**

   - Session dropdown (SelectTrigger)
   - Shows session labels: "Session 1", "Session 2", etc.
   - Auto-selects most recent session on load
   - Delete button (Trash2 icon) next to dropdown

6. **Message Rendering**

   - User messages: Right-aligned, blue background, white text
   - Bot messages: Left-aligned, gray background, dark text
   - Markdown links: Rendered as clickable `<Link>` components
   - Links open in new tab with blue color and underline

7. **Loading States**

   - Typing indicator: 3 bouncing dots with staggered animation
   - Send button: Loader2 spinning icon when mutation pending
   - Disabled inputs during message sending

8. **Toast Notifications**
   - Success: "Created a new chat session"
   - Success: "Session deleted successfully"
   - Success: Copy confirmation (via icon change)
   - Error: "Please login before chatting with AI"
   - Error: "Could not send message"
   - Error: "Failed to create session"
   - Error: "Failed to delete session"

---

## Validation Rules

**Message Input:**

- Must not be empty
- Must not be only whitespace
- Trimmed before sending
- No maximum length enforced (reasonable limits on backend)

**User Authentication:**

- User must be logged in
- Valid user ID (UUID format) required
- User ID extracted from localStorage userInfo

**Session Management:**

- Session ID must be valid UUID (if provided)
- Auto-create session if none exists
- Session belongs to authenticated user

**Mode Selection:**

- Must be "GENERAL" or "ADVISOR"
- Default: "GENERAL"
- Affects AI response generation logic

---

## Business Rules

1. **Session Management:**

   - Each user can have multiple chat sessions
   - Sessions are created automatically on first message (if no session selected)
   - Sessions can be explicitly created via "New Session" button
   - Most recent session is auto-selected on page load
   - Sessions are labeled sequentially: "Session 1", "Session 2", etc.
   - Deleting current session clears messages and deselects session

2. **Chat Modes:**

   - **General Mode**:
     - Answers general programming questions
     - No access to user's learning data
     - Searches general knowledge base
     - Suitable for conceptual questions
   - **Advisor Mode**:
     - Provides personalized learning advice
     - Can access user's course progress (if enabled)
     - Searches course/lesson database
     - Recommends next courses based on progress
     - Suggests learning paths

3. **Progress Context (Advisor Mode):**

   - "Use My Progress" checkbox must be enabled
   - System includes user's completed courses, current enrollments
   - AI considers skill level and learning pace
   - Recommendations are personalized to user's journey

4. **Message History:**

   - All messages saved to database immediately
   - Messages persist across sessions
   - Can view/continue previous conversations
   - Messages include timestamp and sender (USER/BOT)

5. **AI Response Generation:**

   - Uses LLM (GPT-4 or similar) for response generation
   - Searches vector database (Qdrant) for relevant context
   - General Mode: Searches programming knowledge
   - Advisor Mode: Searches courses, lessons, learning paths
   - Response time: 2-5 seconds typical
   - Timeout: 30 seconds maximum

6. **Link Rendering:**

   - AI can include markdown-style links in responses: `[text](url)`
   - Links are parsed and rendered as clickable components
   - Course links navigate to course detail pages
   - External links open in new tab

7. **Suggested Questions:**

   - 4 preset questions per mode
   - General Mode: Basic programming concepts
   - Advisor Mode: Progress-related questions
   - Questions are translatable (i18n support)
   - Clicking suggestion auto-fills input

8. **Performance:**

   - Messages loaded from database on session select
   - React Query caching for sessions and messages
   - Auto-scroll to bottom when new message arrives
   - Optimistic UI updates (show user message immediately)

9. **Security:**
   - User can only access their own sessions
   - User ID verified on backend
   - Messages filtered by userId
   - Rate limiting on AI service (prevent abuse)

---

## User Experience Flow

**First-Time User Journey:**

1. **Landing**

   - User navigates to /ai-chat
   - Sees welcome screen with no messages
   - Bot icon and "Send a message to start chatting"

2. **Mode Selection**

   - User reads mode descriptions
   - Chooses General or Advisor
   - Reviews suggested questions

3. **First Message**

   - User types question or clicks suggestion
   - Presses Send
   - Sees typing indicator
   - Receives AI response

4. **Conversation**

   - User reads response
   - Asks follow-up questions
   - Chat flows naturally
   - Session auto-created

5. **Session Management**
   - User creates new session for different topic
   - Switches between sessions
   - Continues previous conversations
   - Deletes old sessions

---

## User Messages

**Success Messages:**

- "Created a new chat session"
- "Session deleted successfully"
- (Implicit) Copy success via CheckCircle icon

**Error Messages:**

- "Please login before chatting with AI"
- "Could not send message"
- "Failed to create session"
- "Failed to delete session"
- "Sorry, I cannot answer this question." (AI fallback)

**Info Messages:**

- "Login Required - Please login to use AI chat feature and save your chat history"
- "No messages yet - Send a message to start chatting"
- "Press Enter to send, Shift + Enter for new line"

**Mode Descriptions:**

- General: "Answer general programming questions"
- Advisor: "Advise based on your learning progress"

**Suggested Questions (General Mode):**

- "What is programming?"
- "Difference between JavaScript and TypeScript?"
- "How to learn programming effectively?"
- "Which framework is suitable for beginners?"

**Suggested Questions (Advisor Mode):**

- "Which course should I take next?"
- "How is my learning progress?"
- "Suggest a learning path for me"
- "Should I learn more about AI?"

---

## Chat Flow Diagram

```
AI Chat Page (/ai-chat)
        ↓
Load User ID from localStorage
        ↓
Fetch User Sessions from DB
        ↓
Auto-select Most Recent Session
        ↓
Fetch Messages for Session
        ↓
Display Chat Interface
        ↓
    ┌─────────────────────────┐
    │   User Actions          │
    │   - Select Mode         │
    │   - Enable Progress     │
    │   - Type Message        │
    │   - Click Suggestion    │
    └─────────────────────────┘
        ↓
    User Sends Message
        ↓
    ┌─────────────────────────┐
    │   Validation            │
    │   - Message not empty?  │
    │   - User logged in?     │
    └─────────────────────────┘
        ↓ (Valid)
    Show User Message + Typing Indicator
        ↓
    POST /ai/chat/messages
        ↓
    ┌─────────────────────────┐
    │   AI Processing         │
    │   - Analyze question    │
    │   - Search knowledge    │
    │   - Generate response   │
    │   - Save to database    │
    └─────────────────────────┘
        ↓
    Receive AI Response
        ↓
    Remove Typing Indicator
        ↓
    Display AI Message
        ↓
    Update Session List
        ↓
    Clear Input Field
        ↓
    User Continues Conversation
        ↓
    ┌─────────────────────────┐
    │   Session Management    │
    │   - New Session         │
    │   - Switch Session      │
    │   - Delete Session      │
    └─────────────────────────┘
```

---

## Technical Notes

1. **State Management:**

   - React useState for local UI state (messages, input, mode, session)
   - React Query for server state (sessions, messages)
   - localStorage for user authentication persistence
   - useRef for ScrollArea auto-scroll

2. **Data Fetching:**

   - useGetUserSessions: Fetches all sessions for user
   - useGetSessionMessages: Fetches messages for specific session
   - Queries enabled conditionally (when userId/sessionId available)
   - Auto-refetch disabled (manual refresh via query invalidation)

3. **Mutations:**

   - useSendChatMessageMutation: Send message to AI
   - useCreateSessionMutation: Create new empty session
   - useDeleteSessionMutation: Delete session by ID
   - All mutations invalidate relevant queries on success

4. **Message Rendering:**

   - Custom `renderMessageContent` function parses markdown links
   - Regex: `/\[([^\]]+)\]\(([^)]+)\)/g`
   - Links rendered as Next.js `<Link>` components
   - External links: `target="_blank" rel="noopener noreferrer"`

5. **Auto-Scroll Behavior:**

   - useEffect watches messages array
   - Scrolls to bottom when messages change
   - Uses scrollAreaRef.current.scrollHeight
   - Smooth scrolling for better UX

6. **Typing Indicator:**

   - Temporary message with id="typing"
   - Role: "assistant", content: "..."
   - 3 bouncing dots with staggered animation delay
   - Removed before displaying actual AI response

7. **Session Labeling:**

   - Sessions labeled by reverse index: "Session N"
   - Most recent = Session 1
   - Older sessions numbered sequentially
   - Label generated client-side from sessions array

8. **Keyboard Shortcuts:**

   - Enter: Send message
   - Shift + Enter: New line in textarea
   - Prevented via onKeyDown event handler

9. **Copy to Clipboard:**

   - Uses navigator.clipboard.writeText()
   - Visual feedback: Copy icon → CheckCircle icon
   - Timeout: 2 seconds before reverting

10. **Internationalization:**

    - All UI text from useTranslations("AiChat")
    - Supports en, vi, ja languages
    - Suggested questions translatable
    - Mode descriptions translatable

11. **Error Handling:**

    - Try-catch blocks for async operations
    - Toast notifications for user feedback
    - Graceful degradation (empty states)
    - Console logging for debugging

12. **Performance Optimizations:**

    - React Query caching (sessions, messages)
    - Conditional query enabling (prevent unnecessary fetches)
    - Query invalidation only when needed
    - useRef to avoid re-renders for scroll

13. **Accessibility:**

    - Semantic HTML structure
    - Proper labels for form controls
    - Keyboard navigation support
    - ARIA attributes for icons
    - Focus management for inputs

14. **Responsive Design:**
    - Mobile: Stacked layout (sidebar below chat)
    - Desktop: Side-by-side layout (1/4 sidebar, 3/4 chat)
    - Responsive grid: `grid-cols-1 lg:grid-cols-4`
    - Touch-friendly buttons and inputs

---

## Instructor: Dr. Mai Anh Tho

## Project: TechHub - Software Engineering

## Page: 45
