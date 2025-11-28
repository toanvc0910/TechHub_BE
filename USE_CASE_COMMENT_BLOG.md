# 4.3.2 Add Comment For Blog

| [1]       | Add Comment For Blog |
| --------- | -------------------- |
| **Actor** | Registered User      |

| **Trigger** | The user views a blog detail page and wants to share their thoughts by writing a comment or replying to an existing comment. |

| **Description** | This Use Case allows a registered user to add comments to a blog post or reply to existing comments, creating nested discussion threads. Users can write text comments with emoji support, submit them, and see them appear in the comments section immediately. |

| **Pre-Conditions** | - The user is logged in to the system<br>- The user is viewing a published blog post<br>- The blog ID is available from the current page context |

| **Post-Conditions** | - A new comment or reply is created in the system<br>- The comment appears immediately in the comments section<br>- The total comment count is updated<br>- A success notification is displayed to the user<br>- The comment input field is cleared |

| **Main Flow** | 1. The user scrolls to the comments section at the bottom of the blog detail page.<br>2. The user clicks in the comment input textarea (placeholder: "Chia sẻ cảm nhận của bạn...").<br>3. The user types their comment content.<br>4. (Optional) The user clicks the emoji button to insert emojis into the comment.<br>5. (Optional) The user selects an emoji from the emoji picker.<br>6. The system inserts the emoji at the cursor position in the textarea.<br>7. The user clicks the "Bình luận" (Comment) button.<br>8. The system validates that the comment content is not empty.<br>9. The system sends a POST request to the backend API with the comment data.<br>10. The backend creates a new comment record with:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Comment content<br>&nbsp;&nbsp;&nbsp;&nbsp;- User ID (from authentication)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Blog ID<br>&nbsp;&nbsp;&nbsp;&nbsp;- Parent ID (null for top-level comments)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Timestamp<br>11. The system displays a success toast: "Đã gửi bình luận" (Comment sent).<br>12. The system clears the comment input field.<br>13. The system refreshes the comments list to show the new comment.<br>14. The new comment appears at the top or bottom (depending on sort order) of the comments section.<br>15. (Optional) The user changes comment sort order:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. User selects "Mới nhất" (Newest) or "Cũ nhất" (Oldest) from sort dropdown.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System re-sorts comments based on creation timestamp.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. Comments list updates to show new order.<br>16. (Optional) The user expands or collapses nested replies:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. User clicks the "X phản hồi" (X replies) toggle button on a parent comment.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System shows or hides the nested replies.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. Toggle button updates to show current state (expanded/collapsed).<br>17. (Optional) The user cancels a reply they started:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. User clicks the "Hủy" (Cancel) button in the reply input area.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System clears the reply content.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. Reply input field is hidden.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. User returns to normal viewing state. |

| **Alternate Flow** | **14.1** User adds a nested reply instead of top-level comment:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. User clicks the "Trả lời" (Reply) button under an existing comment.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. A reply input field appears below that comment.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. User types their reply content.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. (Optional) User adds emojis using the emoji picker.<br>&nbsp;&nbsp;&nbsp;&nbsp;e. User clicks "Phản hồi" (Reply) button.<br>&nbsp;&nbsp;&nbsp;&nbsp;f. System validates the reply content is not empty.<br>&nbsp;&nbsp;&nbsp;&nbsp;g. System sends POST request with parentId set to the parent comment.<br>&nbsp;&nbsp;&nbsp;&nbsp;h. Backend creates new comment with parentId relationship.<br>&nbsp;&nbsp;&nbsp;&nbsp;i. System displays success toast: "Đã gửi phản hồi" (Reply sent).<br>&nbsp;&nbsp;&nbsp;&nbsp;j. Reply input field is cleared and hidden.<br>&nbsp;&nbsp;&nbsp;&nbsp;k. System refreshes comments list.<br>&nbsp;&nbsp;&nbsp;&nbsp;l. New reply appears nested under the parent comment.<br>&nbsp;&nbsp;&nbsp;&nbsp;m. **User successfully shares feedback on the blog** (same outcome as Main Flow).<br><br>**14.2** User adds multi-level nested reply (reply to a reply):<br>&nbsp;&nbsp;&nbsp;&nbsp;a. User clicks "Trả lời" on an already-nested reply comment.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. Reply input appears below that nested comment.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. User types reply content and optionally adds emojis.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. User clicks "Phản hồi" button.<br>&nbsp;&nbsp;&nbsp;&nbsp;e. System validates and sends POST request with deeper parentId.<br>&nbsp;&nbsp;&nbsp;&nbsp;f. System displays success toast and refreshes comments.<br>&nbsp;&nbsp;&nbsp;&nbsp;g. New reply appears at deeper nesting level.<br>&nbsp;&nbsp;&nbsp;&nbsp;h. **User successfully shares feedback on the blog** (same outcome as Main Flow). |

| **Exception Flow** | **8.1** If comment content is empty or only whitespace:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays error toast: "Vui lòng nhập nội dung bình luận" (Please enter comment content).<br>&nbsp;&nbsp;&nbsp;&nbsp;- The comment button remains disabled.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User remains on the page with focus on input field.<br><br>**8.2** If comment content is too long (exceeds character limit):<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays error toast: "Comment is too long".<br>&nbsp;&nbsp;&nbsp;&nbsp;- User must shorten the comment.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User remains on the page.<br><br>**9.1** If user is not authenticated:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays error toast: "Không thể gửi bình luận. Vui lòng đăng nhập và thử lại." (Cannot send comment. Please login and try again).<br>&nbsp;&nbsp;&nbsp;&nbsp;- User remains on the page.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User should be redirected to login page.<br><br>**9.2** If network error occurs during submission:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays error toast: "Không thể gửi bình luận. Vui lòng đăng nhập và thử lại." (Cannot send comment. Please login and try again).<br>&nbsp;&nbsp;&nbsp;&nbsp;- Comment content remains in the input field.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User can retry submission.<br><br>**10.1** If blog post is locked or comments are disabled:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays error message: "Comments are disabled for this blog".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Comment input is disabled or hidden.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User can only view existing comments.<br><br>**10.2** If server returns validation error:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays error toast with specific error message from server.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Comment content remains in input field.<br>&nbsp;&nbsp;&nbsp;&nbsp;- User can correct and resubmit. |

---

## Related Use Cases

This use case is part of the blog engagement workflow:

- **View Blog Detail** (Use Case 4.3.1): User must be viewing a blog to access the comment feature.
- **Edit Comment** (Use Case 4.3.3): User can edit their own comments after posting (if implemented).
- **Delete Comment** (Use Case 4.3.4): User can delete their own comments (if implemented).

---

## API Endpoints

**Frontend Proxy Route:**

```
POST /app/api/proxy/blogs/{blogId}/comments
GET /app/api/proxy/blogs/{blogId}/comments
```

**Backend API:**

```
POST /blogs/{blogId}/comments
GET /blogs/{blogId}/comments
```

**Add Comment Request:**

```json
POST /blogs/{blogId}/comments
{
  "content": "This is a great article! Thanks for sharing.",
  "parentId": null
}
```

**Add Reply Request:**

```json
POST /blogs/{blogId}/comments
{
  "content": "I totally agree with you!",
  "parentId": "comment123"
}
```

**Success Response (201 Created):**

```json
{
  "success": true,
  "status": "CREATED",
  "message": "Comment added successfully",
  "timestamp": "2024-11-27T10:30:00Z",
  "code": 201,
  "path": "/blogs/blog123/comments",
  "data": {
    "id": "comment456",
    "content": "This is a great article! Thanks for sharing.",
    "userId": "user789",
    "parentId": null,
    "created": "2024-11-27T10:30:00Z",
    "replies": []
  }
}
```

**Get Comments Response (200 OK):**

```json
{
  "success": true,
  "status": "OK",
  "message": "Comments retrieved successfully",
  "timestamp": "2024-11-27T10:30:00Z",
  "code": 200,
  "path": "/blogs/blog123/comments",
  "data": [
    {
      "id": "comment1",
      "content": "Great article!",
      "userId": "user456",
      "parentId": null,
      "created": "2024-11-21T12:00:00Z",
      "replies": [
        {
          "id": "comment2",
          "content": "I agree!",
          "userId": "user789",
          "parentId": "comment1",
          "created": "2024-11-22T10:00:00Z",
          "replies": []
        }
      ]
    }
  ]
}
```

**Error Response (400 Bad Request):**

```json
{
  "success": false,
  "status": "BAD_REQUEST",
  "message": "Comment content is required",
  "timestamp": "2024-11-27T10:30:00Z",
  "code": 400
}
```

**Error Response (401 Unauthorized):**

```json
{
  "success": false,
  "status": "UNAUTHORIZED",
  "message": "Please login to add comments",
  "timestamp": "2024-11-27T10:30:00Z",
  "code": 401
}
```

---

## UI Components

**Location:** Blog Detail Page - Comments Section

**Key Components:**

1. **Comments Section Header**

   - Comment count icon (MessageCircle)
   - Total comment count (includes all nested replies)
   - Sort dropdown (Newest/Oldest)

2. **Main Comment Input Area**

   - Large textarea (3 rows minimum)
   - Placeholder: "Chia sẻ cảm nhận của bạn..." (Share your thoughts...)
   - Emoji button (Smile icon) positioned bottom-right of textarea
   - Character counter (optional)
   - Submit button: "Bình luận" (Comment)
   - Button shows loading spinner when submitting
   - Button is disabled when:
     - Content is empty/whitespace only
     - Submission is in progress

3. **Emoji Picker**

   - Opens when emoji button is clicked
   - Positioned absolutely above the textarea
   - Allows emoji selection
   - Closes after emoji selection
   - Inserts emoji at cursor position

4. **Comment Item Display**

   - User avatar (or initials if no avatar)
   - Username (formatted as @username)
   - Timestamp (formatted as dd/MM/yyyy)
   - Comment content text
   - Action buttons:
     - Like (ThumbsUp icon) - UI only
     - Dislike (ThumbsDown icon) - UI only
     - Reply button ("Trả lời")

5. **Reply Input Area** (appears when replying)

   - Textarea (3 rows)
   - Placeholder: "Viết phản hồi..." (Write reply...)
   - Emoji button
   - Action buttons:
     - Cancel button: "Hủy"
     - Submit button: "Phản hồi" (Reply)
   - Indented under parent comment

6. **Nested Replies Display**

   - Indented 12px more than parent (ml-12 class)
   - Show/hide toggle button
   - Display count: "X phản hồi" (X replies)
   - Recursive nesting supported
   - Collapse/expand icon (ChevronUp/ChevronDown)

7. **Loading States**

   - Skeleton loaders (3 items) while fetching comments
   - Spinner on submit button during submission
   - Disabled state for inputs during submission

8. **Empty State**
   - Message: "Hãy là người đầu tiên chia sẻ cảm nghĩ của bạn." (Be the first to share your thoughts)
   - Shown when no comments exist
   - Centered text with muted styling

---

## Validation Rules

**Comment Content:**

- Must not be empty
- Must not be only whitespace
- Minimum length: 1 character (after trimming)
- Maximum length: 5000 characters (typical limit)
- XSS protection: HTML tags are escaped/sanitized

**User Authentication:**

- User must be logged in
- Valid authentication token required
- User ID extracted from session/token

**Blog Post:**

- Blog must exist and be published
- Blog must allow comments (not locked)
- Blog ID must be valid UUID format

---

## Business Rules

1. **Comment Creation:**

   - Each comment has unique ID (UUID)
   - Timestamp automatically set to current server time
   - User ID from authenticated session
   - Comments are public and visible to all users
   - Comments cannot be anonymous

2. **Nested Replies:**

   - Unlimited nesting depth supported (recursive structure)
   - Each reply references parent via parentId
   - Top-level comments have parentId = null
   - Replies inherit blog association from parent

3. **Comment Display:**

   - Sort order: Newest first (default) or Oldest first
   - Total count includes all nested replies (recursive)
   - Replies can be collapsed/expanded
   - Show reply count on parent comments

4. **User Experience:**

   - Immediate feedback via toast notifications
   - Optimistic UI updates (comment appears before server confirmation)
   - Input cleared after successful submission
   - Auto-scroll to new comment (optional)

5. **Security:**

   - Rate limiting: Max 10 comments per minute per user
   - Spam detection (duplicate content check)
   - Content moderation (profanity filter - optional)
   - User can only edit/delete own comments

6. **Performance:**
   - Comments loaded with blog detail (parallel fetch)
   - Pagination for large comment threads (if >100 comments)
   - Lazy loading for deeply nested replies
   - React Query cache for comment data

---

## User Interactions

**Adding Comments:**

1. Click in textarea to focus
2. Type comment content
3. (Optional) Add emojis via picker
4. Click "Bình luận" button
5. See success toast
6. See new comment appear in list

**Adding Replies:**

1. Click "Trả lời" on a comment
2. Reply input appears below comment
3. Type reply content
4. (Optional) Add emojis
5. Click "Phản hồi" or "Hủy"
6. See reply nested under parent

**Managing Comments:**

1. View comment count
2. Change sort order (newest/oldest)
3. Expand/collapse reply threads
4. Scroll through comments
5. See user avatars and names

---

## User Messages

**Success Messages:**

- "Đã gửi bình luận" (Comment sent)
- "Đã gửi phản hồi" (Reply sent)

**Error Messages:**

- "Vui lòng nhập nội dung bình luận" (Please enter comment content)
- "Vui lòng nhập nội dung phản hồi" (Please enter reply content)
- "Không thể gửi bình luận. Vui lòng đăng nhập và thử lại." (Cannot send comment. Please login and try again)
- "Không thể gửi phản hồi. Vui lòng đăng nhập và thử lại." (Cannot send reply. Please login and try again)
- "Comment is too long"
- "Comments are disabled for this blog"

**Empty State Message:**

- "Hãy là người đầu tiên chia sẻ cảm nghĩ của bạn." (Be the first to share your thoughts)

---

## Comment Display Flow

```
View Blog Detail Page
        ↓
Scroll to Comments Section
        ↓
    ┌───────────────────────┐
    │   Add New Comment     │ ← Main comment input
    │   (Top-level)         │
    └───────────────────────┘
        ↓
    Submit Comment
        ↓
    Success/Error Toast
        ↓
    Comment Appears in List
        ↓
    ┌───────────────────────┐
    │   Comment Display     │
    │   └── Reply Button    │ ← Click to reply
    └───────────────────────┘
        ↓
    Reply Input Appears
        ↓
    Submit Reply
        ↓
    Reply Nested Under Parent
        ↓
    ┌───────────────────────┐
    │   Parent Comment      │
    │   └── Reply 1         │ ← Nested reply
    │       └── Reply 2     │ ← Deeper nesting
    └───────────────────────┘
```

---

## Technical Notes

1. **State Management:**

   - React Hook Form NOT used (direct state management)
   - Local state for comment/reply content
   - React Query mutation for API calls
   - Optimistic updates with rollback on error

2. **Emoji Integration:**

   - Dynamic import of emoji-picker-react (avoid SSR issues)
   - Emoji inserted at cursor position (not just appended)
   - Textarea ref used for cursor position tracking
   - requestAnimationFrame for cursor repositioning

3. **Comment Structure:**

   - Recursive data structure for nested replies
   - Each comment can have array of reply comments
   - Depth tracking for indentation (depth \* 12px)
   - Maximum practical depth: 5 levels

4. **User Information:**

   - Separate API call to fetch user details per comment
   - Caching prevents duplicate requests
   - Fallback to user ID if user fetch fails
   - Avatar or initials displayed

5. **Real-time Updates:**

   - React Query cache invalidation after mutations
   - Automatic refetch of comments list
   - No websockets (polling via cache revalidation)

6. **Sorting Implementation:**

   - Client-side sorting using useMemo
   - Sorts by created timestamp
   - Preserves nested structure
   - Re-sorts on sort order change

7. **Performance Optimizations:**

   - Memoized total comment count (recursive calculation)
   - Memoized sorted comments
   - Lazy loading for emoji picker
   - Conditional rendering for reply inputs

8. **Accessibility:**
   - Semantic HTML (textarea, button)
   - ARIA labels for emoji buttons
   - Keyboard navigation support
   - Focus management for reply inputs

---

## Instructor: Dr. Mai Anh Tho

## Project: TechHub - Software Engineering

## Page: 43
