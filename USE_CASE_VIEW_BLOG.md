# 4.3.1 View Blog Detail

| [1]       | View Blog Detail |
| --------- | ---------------- |
| **Actor** | Registered User  |

| **Trigger** | The user selects a blog post from the blog listing page or accesses a blog detail page directly via URL (/blog/[slug]). |

| **Description** | This Use Case allows users to view the complete content of a blog post, including title, content, author information, tags, attachments, table of contents, and comments. Users can also interact with the blog by commenting, sharing, and navigating through the content. |

| **Pre-Conditions** | - The user is logged in to the system<br>- The blog post exists in the system and is published<br>- The blog ID can be extracted from the URL slug |

| **Post-Conditions** | - The complete blog content is displayed to the user<br>- Reading time is calculated and displayed<br>- Table of contents is generated from headings<br>- Comments are loaded and displayed<br>- Page title is updated to reflect the blog title |

| **Main Flow** | 1. The user accesses the blog detail page via URL with slug format (e.g., /blog/blog-title-{id}).<br>2. The system extracts the blog ID from the URL slug.<br>3. The system fetches the blog data from the backend API using the extracted ID.<br>4. The system fetches the author information based on the blog's author ID.<br>5. The system fetches all comments for the blog post.<br>6. The system parses the markdown content to HTML.<br>7. The system generates a table of contents from heading tags (h1-h6).<br>8. The system calculates the estimated reading time based on content length.<br>9. The system displays the complete blog with:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Blog title and metadata (reading time, publish date)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Tags as badges<br>&nbsp;&nbsp;&nbsp;&nbsp;- Author information card (avatar, username)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Rendered HTML content from markdown<br>&nbsp;&nbsp;&nbsp;&nbsp;- Attachments section (if available)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Share buttons (copy link, Facebook, Twitter)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Comments section with nested replies<br>10. The system displays a table of contents sidebar (on desktop).<br>11. The system updates the browser's document title to include the blog title.<br>12. The user scrolls through and reads the blog content.<br>13. (Optional) The user clicks on a table of contents item:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. The system smoothly scrolls to the corresponding heading in the content.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. The heading is highlighted or brought into view.<br>14. (Optional) The user clicks a share button:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. If "Copy link" - the system copies the blog URL to clipboard and shows success toast.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. If "Facebook" - opens Facebook share dialog in new tab.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. If "Twitter" - opens Twitter share dialog with blog title and URL in new tab.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. If "Copy for chat" - copies blog title and URL to clipboard.<br>15. (Optional) The user submits a comment or reply (see Use Case 4.3.2 - Add Blog Comment).<br>16. (Optional) The user clicks attachment download:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. The system opens the attachment URL in a new tab.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. The file is downloaded or displayed based on type.<br>17. (Optional) The user clicks "Back to list" link to return to blog listing page (/blog). |

| **Alternate Flow** | **12.1** User accesses blog via direct URL (not from blog listing):<br>&nbsp;&nbsp;&nbsp;&nbsp;a. User pastes or types the full blog URL with slug into browser address bar.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System extracts blog ID from URL slug.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. System fetches and displays blog content (same as Main Flow steps 3-12).<br>&nbsp;&nbsp;&nbsp;&nbsp;d. User successfully views the complete blog detail.<br><br>**12.2** User accesses blog via shared link from social media:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. User clicks a blog link shared on Facebook, Twitter, or messaging app.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. Browser opens the blog detail page directly.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. System loads blog content with metadata for social preview.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. User successfully views the complete blog detail.<br><br>**12.3** User accesses blog via search engine result:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. User searches for topic on Google/search engine.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. User clicks blog post from search results.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. System loads blog with SEO metadata.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. User successfully views the complete blog detail.<br><br>**12.4** User accesses blog via notification or email link:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. User receives notification about new blog or comment.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. User clicks link in notification/email.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. System authenticates user and loads blog.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. User successfully views the complete blog detail. |

| **Exception Flow** | **2.1** If the blog ID cannot be extracted from the slug:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays an error message: "Invalid blog URL"<br>&nbsp;&nbsp;&nbsp;&nbsp;- Shows a "Back to blog list" button.<br>&nbsp;&nbsp;&nbsp;&nbsp;- The user remains on the error page.<br><br>**3.1** If the blog does not exist or has been deleted:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays: "Blog not found"<br>&nbsp;&nbsp;&nbsp;&nbsp;- Shows message: "The link may have changed or the blog has been removed."<br>&nbsp;&nbsp;&nbsp;&nbsp;- Displays a "Back to blog list" button.<br>&nbsp;&nbsp;&nbsp;&nbsp;- The user remains on the error page.<br><br>**3.2** If the blog is in draft status and user is not the author:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays: "Blog not found" (for security, don't reveal it's a draft)<br>&nbsp;&nbsp;&nbsp;&nbsp;- The user remains on the error page.<br><br>**4.1** If the author information cannot be loaded:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays a default avatar with initials from author ID.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Shows a generic username.<br>&nbsp;&nbsp;&nbsp;&nbsp;- The blog content is still displayed.<br><br>**5.1** If comments fail to load:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays an empty comments section.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Users can still attempt to add new comments.<br>&nbsp;&nbsp;&nbsp;&nbsp;- The blog content is still displayed.<br><br>**8.1** If there is a network or server error during initial load:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system shows loading skeletons then error state.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Displays: "Unable to load blog"<br>&nbsp;&nbsp;&nbsp;&nbsp;- The user can refresh the page to retry.<br><br>**9.1** If the clipboard API fails when copying links:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays error toast: "Unable to copy link"<br>&nbsp;&nbsp;&nbsp;&nbsp;- User remains on the page and can retry. |

---

## Related Use Cases

Users can perform additional actions while viewing a blog:

- **Add Blog Comment** (Use Case 4.3.2): The user can write and submit comments on the blog post.
- **Reply to Comment** (Use Case 4.3.3): The user can reply to existing comments, creating nested comment threads.
- **Share Blog** (Use Case 4.3.4): The user can share the blog via social media or copy the link.

---

## API Endpoints

**Frontend Proxy Route:**

```
GET /app/api/proxy/blogs/{id}
GET /app/api/proxy/blogs/{blogId}/comments
```

**Backend API:**

```
GET /blogs/{id}
GET /blogs/{blogId}/comments
```

**Request Parameters:**

- `id`: Blog ID (extracted from URL slug)
- `blogId`: Blog ID for fetching comments

**Blog Detail Success Response (200 OK):**

```json
{
  "success": true,
  "status": "OK",
  "message": "Blog retrieved successfully",
  "timestamp": "2024-11-26T10:30:00Z",
  "code": 200,
  "payload": {
    "data": {
      "id": "blog123",
      "title": "Introduction to React Hooks",
      "content": "# React Hooks\n\nReact Hooks are...",
      "thumbnail": "https://example.com/image.jpg",
      "authorId": "user123",
      "tags": ["react", "javascript", "tutorial"],
      "status": "PUBLISHED",
      "attachments": [
        {
          "url": "https://example.com/file.pdf",
          "type": "document",
          "caption": "React Hooks Guide"
        }
      ],
      "created": "2024-11-20T10:00:00Z",
      "updated": "2024-11-25T15:30:00Z"
    }
  }
}
```

**Comments Success Response (200 OK):**

```json
{
  "success": true,
  "status": "OK",
  "message": "Comments retrieved successfully",
  "timestamp": "2024-11-26T10:30:00Z",
  "code": 200,
  "payload": {
    "data": [
      {
        "id": "comment1",
        "blogId": "blog123",
        "userId": "user456",
        "content": "Great article!",
        "parentId": null,
        "created": "2024-11-21T12:00:00Z",
        "updated": "2024-11-21T12:00:00Z",
        "replies": [
          {
            "id": "comment2",
            "blogId": "blog123",
            "userId": "user789",
            "content": "I agree!",
            "parentId": "comment1",
            "created": "2024-11-22T10:00:00Z",
            "updated": "2024-11-22T10:00:00Z",
            "replies": []
          }
        ]
      }
    ]
  }
}
```

**Error Response (404 Not Found):**

```json
{
  "success": false,
  "status": "NOT_FOUND",
  "message": "Blog not found",
  "timestamp": "2024-11-26T10:30:00Z",
  "code": 404
}
```

---

## UI Components

**Page Location:** `/blog/[slug]`

**Key Components:**

1. **Breadcrumb Navigation**

   - "← Back to list" link
   - Returns to blog listing page

2. **Blog Header Section**

   - Blog title (large, responsive font)
   - Metadata: reading time, publish date
   - Tags displayed as badges

3. **Author Information Card**

   - Author avatar (or initials if no avatar)
   - Username
   - "Author" label

4. **Blog Content Area**

   - Rendered HTML from markdown
   - Proper typography with prose styling
   - Images with rounded corners
   - Headings with auto-generated IDs for TOC linking
   - Smooth scroll to headings

5. **Table of Contents Sidebar** (Desktop only)

   - Generated from h1-h6 headings
   - Clickable items for navigation
   - Fixed position during scroll
   - Shows hierarchy of headings

6. **Attachments Section** (if available)

   - List of downloadable files
   - Icons indicating file type (image/document)
   - Download/view buttons for each attachment

7. **Share Section**

   - Copy link button
   - Facebook share button
   - Twitter share button
   - Copy for chat button
   - Toast notifications for copy actions

8. **Comments Section**

   - Comment count with icon
   - Sort dropdown (newest/oldest)
   - New comment input with emoji picker
   - Comments list with nested replies
   - User avatars for each comment
   - Like/dislike buttons (UI only)
   - Reply functionality

9. **Scroll to Top Button**

   - Appears after scrolling down
   - Smooth scroll to page top
   - Fixed position bottom-right

10. **Loading States**
    - Skeleton loaders for initial content
    - Spinner for comment submissions
    - Loading states for async operations

---

## Display Rules

**Content Rendering:**

- Markdown is parsed to HTML using a markdown parser
- Code blocks have syntax highlighting
- Images are responsive and rounded
- Links open in new tabs for external URLs
- Headings are auto-numbered in TOC

**Reading Time Calculation:**

- Estimated based on average reading speed (200 words per minute)
- Calculated from content word count
- Displayed in minutes

**Table of Contents:**

- Only shown on desktop (lg breakpoint)
- Generated from h1-h6 tags in content
- Maintains heading hierarchy
- Sticky positioning during scroll

**Comments Display:**

- Nested up to multiple levels (recursive)
- Show/hide replies toggle for parent comments
- Chronological ordering (newest or oldest first)
- Total count includes all replies recursively

---

## Business Rules

1. **Blog Visibility:**

   - Only published blogs are visible to public users
   - Draft blogs only visible to authors
   - Deleted blogs return 404 error

2. **Content Processing:**

   - Markdown content converted to sanitized HTML
   - XSS protection applied to user-generated content
   - Images lazy-loaded for performance

3. **Comments:**

   - Users can comment only if logged in
   - Comments are public and visible to all
   - Nested replies supported (parent-child relationship)
   - Real-time updates via query invalidation

4. **Sharing:**

   - Share URLs use canonical blog URL
   - Social media sharing includes OG meta tags
   - Copy to clipboard requires HTTPS

5. **Performance:**

   - Blog content and comments loaded in parallel
   - Table of contents generated client-side
   - Images optimized and lazy-loaded
   - Skeleton loaders prevent layout shift

6. **SEO:**
   - Page title includes blog title
   - Meta descriptions from blog excerpt
   - Canonical URLs for sharing
   - Structured data for search engines

---

## User Interactions

**Reading Experience:**

- Smooth scrolling throughout the page
- Responsive typography for readability
- Dark mode support via theme provider
- Print-friendly styling

**Navigation:**

- TOC click scrolls to heading
- Back button returns to blog list
- Browser back/forward navigation supported
- Deep linking to specific sections via URL hash (optional)

**Engagement:**

- Comment submission with validation
- Reply to comments (nested threads)
- Share on social media
- Download attachments

**Feedback:**

- Toast notifications for actions
- Loading states for async operations
- Error messages for failed requests
- Success confirmations for submissions

---

## User Messages

**Success Messages:**

- "Blog loaded successfully" (implicit - content shown)
- "Link copied to clipboard"
- "Comment submitted successfully"

**Error Messages:**

- "Blog not found"
- "The link may have changed or the blog has been removed"
- "Unable to load blog"
- "Unable to copy link"
- "Unable to load comments"
- "Invalid blog URL"

**Loading Messages:**

- Loading skeletons (no text)
- Spinner with "Submitting..." for comments

---

## Navigation Flow

```
Blog List Page → Blog Detail Page
      ↑                ↓
      └────────────────┘
   (Back to List Link)

Blog Detail Page → Share → Social Media / Clipboard
                ↓
            Comments → Add Comment / Reply
                ↓
          Attachments → Download File
                ↓
         TOC Click → Scroll to Section
```

---

## Technical Notes

1. **URL Structure:**

   - Uses slug format: `/blog/{title}-{id}`
   - ID extracted using `extractIdFromSlug()` helper
   - Supports SEO-friendly URLs

2. **State Management:**

   - React Query for data fetching and caching
   - Local state for UI interactions (modals, emoji pickers)
   - Optimistic updates for comment submissions

3. **Content Processing:**

   - Markdown parsed with custom parser
   - HTML sanitization for security
   - TOC generation from heading tags
   - Heading IDs auto-generated for anchors

4. **Loading Strategy:**

   - Initial: Show skeleton loaders
   - Parallel fetching: Blog data, author info, comments
   - Progressive enhancement: Core content first, then comments
   - Error boundaries for graceful failure

5. **Responsive Design:**

   - Mobile-first approach
   - TOC hidden on mobile/tablet
   - Grid layout adapts to screen size
   - Touch-friendly buttons and interactions

6. **Performance Optimizations:**

   - Dynamic imports for emoji picker (avoid SSR)
   - Memoized calculations (reading time, TOC, sorted comments)
   - Lazy loading for images
   - Query caching with React Query

7. **Internationalization:**

   - All UI text translatable
   - Date formatting locale-aware
   - Supports multiple languages (English, Vietnamese, Japanese)

8. **Accessibility:**
   - Semantic HTML structure
   - ARIA labels for interactive elements
   - Keyboard navigation support
   - Focus management for modals

---

## Instructor: Dr. Mai Anh Tho

## Project: TechHub - Software Engineering

## Page: 42
