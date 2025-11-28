# 4.6.1 Manage Permissions

| [1]       | Manage Permissions |
| --------- | ------------------ |
| **Actor** | Admin              |

| **Trigger** | The admin accesses the Manage Permissions page (/manage/permissions) and wants to view, create, edit, or delete permissions in the role-based access control (RBAC) system. |

| **Description** | This Use Case allows administrators to manage permissions that define access rights to various system resources and operations. Each permission specifies an HTTP method (GET, POST, PUT, DELETE, PATCH), a URL endpoint, and a resource type (USERS, ROLES, PERMISSIONS, COURSES, BLOGS, LEARNING_PATHS). Admins can view all permissions in a sortable and filterable table, create new permissions, edit existing ones, and delete permissions that are no longer needed. This forms the foundation of the RBAC system where permissions are assigned to roles. |

| **Pre-Conditions** | - The user is logged in as an Admin<br>- The user has access to the Manage Permissions page (/manage/permissions)<br>- The permission management system is available<br>- Backend API for permissions is operational |

| **Post-Conditions** | - Permission list is displayed with current data from database<br>- New permissions are created and saved in the system<br>- Existing permissions are updated with new information<br>- Deleted permissions are removed from the database<br>- All role assignments using these permissions are affected accordingly<br>- Success or error notifications are displayed to the admin |

| **Main Flow** | 1. The admin navigates to the Manage Permissions page (/manage/permissions).<br>2. The system displays the page header with:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Title: "Quản lý Permissions"<br>&nbsp;&nbsp;&nbsp;&nbsp;- Description: "Quản lý các permissions trong hệ thống phân quyền"<br>3. The system fetches all permissions from the database via GET /permissions endpoint.<br>4. The system displays a data table with permissions containing:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Name column (sortable) - Permission name (e.g., "USER_READ_ALL")<br>&nbsp;&nbsp;&nbsp;&nbsp;- Description column - Optional description of what the permission allows<br>&nbsp;&nbsp;&nbsp;&nbsp;- Method column (filterable) - HTTP method with colored badge (GET/POST/PUT/DELETE/PATCH)<br>&nbsp;&nbsp;&nbsp;&nbsp;- URL column - API endpoint (e.g., "/api/users")<br>&nbsp;&nbsp;&nbsp;&nbsp;- Resource column (filterable) - Resource type badge (USERS/ROLES/PERMISSIONS/COURSES/BLOGS/LEARNING_PATHS)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Actions column - Edit and Delete buttons<br>5. The system provides filter and search controls:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Search input: Filter by permission name<br>&nbsp;&nbsp;&nbsp;&nbsp;- Method dropdown: Filter by HTTP method (All/GET/POST/PUT/DELETE/PATCH)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Resource dropdown: Filter by resource type (All/USERS/ROLES/etc.)<br>&nbsp;&nbsp;&nbsp;&nbsp;- "Thêm Permission" button: Opens add permission dialog<br>6. The admin views the permission list with pagination controls (Previous/Next buttons).<br>7. (Optional) The admin searches for specific permissions:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin types text in the search box.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System filters table to show only matching permission names.<br>8. (Optional) The admin filters by method or resource:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin selects filter value from dropdown.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System updates table to show only matching rows.<br>9. (Optional) The admin sorts the table:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin clicks on "Tên" column header.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System toggles between ascending and descending order.<br>10. (Optional) The admin navigates between pages:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin clicks "Trước" or "Sau" button.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System displays the previous or next page of results.<br>11. **To create a new permission**, the admin clicks "Thêm Permission" button.<br>12. The system opens "Thêm Permission Mới" dialog with form fields:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Tên (required) - Permission name input<br>&nbsp;&nbsp;&nbsp;&nbsp;- Mô tả (optional) - Description textarea<br>&nbsp;&nbsp;&nbsp;&nbsp;- Method (required) - HTTP method dropdown<br>&nbsp;&nbsp;&nbsp;&nbsp;- Resource (required) - Resource type dropdown<br>&nbsp;&nbsp;&nbsp;&nbsp;- URL (required) - API endpoint input<br>&nbsp;&nbsp;&nbsp;&nbsp;- Trạng thái - Active/Inactive toggle switch (default: Active)<br>13. The admin fills in the required fields (name, method, resource, URL).<br>14. The admin clicks "Thêm Permission" button in dialog footer.<br>15. The system validates the form data using Zod schema validation.<br>16. The system sends POST request to /permissions endpoint with permission data.<br>17. The backend creates a new permission record in the database.<br>18. The system displays success toast: "Permission created successfully" (from backend message).<br>19. The system closes the dialog and resets the form.<br>20. The system refreshes the permission list to show the new permission.<br>21. **To edit an existing permission**, the admin clicks the actions menu (three dots) on a permission row.<br>22. The admin selects "Chỉnh sửa" from the dropdown menu.<br>23. The system opens "Chỉnh sửa Permission" dialog with pre-filled form fields.<br>24. The system fetches the permission details via GET /permissions/{id}.<br>25. The system populates the form with current permission data.<br>26. The admin modifies the desired fields (name, description, method, resource, URL, status).<br>27. The admin clicks "Cập nhật Permission" button.<br>28. The system validates the updated data.<br>29. The system sends PUT request to /permissions/{id} with updated data.<br>30. The backend updates the permission record in the database.<br>31. The system displays success toast with update confirmation message.<br>32. The system closes the dialog and refreshes the permission list.<br>33. **To delete a permission**, the admin clicks the actions menu on a permission row.<br>34. The admin selects "Xóa" from the dropdown menu (displayed in red/destructive color).<br>35. The system opens a confirmation AlertDialog:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Title: "Xóa Permission"<br>&nbsp;&nbsp;&nbsp;&nbsp;- Message: "Bạn có chắc chắn muốn xóa permission [name]? Hành động này không thể hoàn tác."<br>&nbsp;&nbsp;&nbsp;&nbsp;- Cancel button: "Hủy"<br>&nbsp;&nbsp;&nbsp;&nbsp;- Confirm button: "Tiếp tục"<br>36. The admin clicks "Tiếp tục" to confirm deletion.<br>37. The system sends DELETE request to /permissions/{id}.<br>38. The backend removes the permission from the database.<br>39. The system displays success toast: "Permission [name] đã được xóa".<br>40. The system closes the dialog and refreshes the permission list. |

| **Alternate Flow** | **40.1** Admin cancels adding a new permission:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin clicks outside the dialog or presses ESC key.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System closes the dialog without saving.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. Form is reset to default values.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. Admin returns to permission list.<br>&nbsp;&nbsp;&nbsp;&nbsp;e. **No permission is created** (neutral outcome).<br><br>**40.2** Admin uses dialog close button instead of form submit:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin clicks the X button on dialog header.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System triggers onOpenChange event with false value.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. Dialog closes and form resets.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. **No data is saved** (same as cancel).<br><br>**40.3** Admin cancels editing a permission:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin clicks outside the edit dialog or presses ESC.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System closes the dialog without updating.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. Original permission data remains unchanged.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. Admin returns to permission list.<br>&nbsp;&nbsp;&nbsp;&nbsp;e. **Permission is not modified** (neutral outcome).<br><br>**40.4** Admin cancels permission deletion:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin clicks "Hủy" button in the confirmation dialog.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System closes the AlertDialog.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. No DELETE request is sent to backend.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. Permission remains in the database.<br>&nbsp;&nbsp;&nbsp;&nbsp;e. Admin returns to permission list.<br>&nbsp;&nbsp;&nbsp;&nbsp;f. **Permission is preserved** (neutral outcome). |

| **Exception Flow** | **15.1** If required fields are empty or invalid:<br>&nbsp;&nbsp;&nbsp;&nbsp;- System displays field-level validation errors under each field.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Error messages: "Name is required", "URL is required", etc.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Form submit button may be disabled.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin must correct errors before resubmitting.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin remains in the dialog.<br><br>**16.1** If permission name already exists (duplicate):<br>&nbsp;&nbsp;&nbsp;&nbsp;- Backend returns 400 Bad Request error.<br>&nbsp;&nbsp;&nbsp;&nbsp;- System displays error toast: "Lỗi - Không thể tạo permission".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Possible message: "Permission name already exists".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Form remains open with data preserved.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin can modify name and retry.<br><br>**16.2** If network error occurs during creation:<br>&nbsp;&nbsp;&nbsp;&nbsp;- System catches the error in try-catch block.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Displays error toast: "Lỗi - Không thể tạo permission".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Form data is preserved.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin can retry submission.<br><br>**24.1** If permission details fail to load (edit mode):<br>&nbsp;&nbsp;&nbsp;&nbsp;- Dialog shows "Đang tải..." message.<br>&nbsp;&nbsp;&nbsp;&nbsp;- If fetch fails, error is caught.<br>&nbsp;&nbsp;&nbsp;&nbsp;- System may show error message or empty form.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin can close dialog and retry.<br><br>**29.1** If permission update fails (validation error):<br>&nbsp;&nbsp;&nbsp;&nbsp;- Backend returns 400 error with validation details.<br>&nbsp;&nbsp;&nbsp;&nbsp;- System uses handleErrorApi to set field errors.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Displays error toast: "Lỗi - Không thể cập nhật permission".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Form remains open for corrections.<br><br>**29.2** If permission is being used by roles (cannot update critical fields):<br>&nbsp;&nbsp;&nbsp;&nbsp;- Backend may return 409 Conflict error.<br>&nbsp;&nbsp;&nbsp;&nbsp;- System displays error toast with explanation.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin must reassign roles before making changes.<br><br>**37.1** If permission is assigned to active roles (cannot delete):<br>&nbsp;&nbsp;&nbsp;&nbsp;- Backend returns 409 Conflict error.<br>&nbsp;&nbsp;&nbsp;&nbsp;- System displays error toast: "DeleteFailed".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Error message: Permission is in use, cannot be deleted.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Confirmation dialog closes.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin must remove permission from roles first.<br><br>**37.2** If network error during deletion:<br>&nbsp;&nbsp;&nbsp;&nbsp;- System catches error in mutation.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Displays error toast with error message.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Permission remains in database and table.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin can retry deletion.<br><br>**Initial Load Error** If permissions fail to load on page load:<br>&nbsp;&nbsp;&nbsp;&nbsp;- System shows error message: "Lỗi: [error.message]".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Table shows error state instead of data.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin can refresh page to retry.<br><br>**Empty Result** If no permissions exist in the system:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Table shows empty state message: "Không có kết quả".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin can create the first permission using "Thêm Permission" button. |

---

## Related Use Cases

This use case is part of the RBAC (Role-Based Access Control) system:

- **Manage Roles** (Use Case 4.6.2): Permissions are assigned to roles for access control.
- **Manage Users** (Use Case 4.6.3): Users are assigned roles which contain permissions.
- **View Audit Logs** (Use Case 4.6.4): Permission changes are logged for security auditing.

---

## API Endpoints

**Frontend Proxy Route:**

```
GET /app/api/proxy/permissions
GET /app/api/proxy/permissions/{id}
POST /app/api/proxy/permissions
PUT /app/api/proxy/permissions/{id}
DELETE /app/api/proxy/permissions/{id}
```

**Backend API:**

```
GET /permissions
GET /permissions/{id}
POST /permissions
PUT /permissions/{id}
DELETE /permissions/{id}
```

**Get All Permissions Request:**

```
GET /permissions
```

**Get All Permissions Response (200 OK):**

```json
{
  "success": true,
  "status": "OK",
  "message": "Permissions retrieved successfully",
  "timestamp": "2024-11-27T10:00:00Z",
  "code": 200,
  "path": "/permissions",
  "payload": {
    "data": [
      {
        "id": "perm-123",
        "name": "USER_READ_ALL",
        "description": "Get all users in the system",
        "url": "/api/users",
        "method": "GET",
        "resource": "USERS"
      },
      {
        "id": "perm-456",
        "name": "USER_CREATE",
        "description": "Create new user",
        "url": "/api/users",
        "method": "POST",
        "resource": "USERS"
      },
      {
        "id": "perm-789",
        "name": "COURSE_UPDATE",
        "description": "Update course information",
        "url": "/api/courses/{id}",
        "method": "PUT",
        "resource": "COURSES"
      }
    ]
  }
}
```

**Get Permission By ID Request:**

```
GET /permissions/perm-123
```

**Get Permission By ID Response (200 OK):**

```json
{
  "success": true,
  "status": "OK",
  "message": "Permission retrieved successfully",
  "timestamp": "2024-11-27T10:00:00Z",
  "code": 200,
  "path": "/permissions/perm-123",
  "payload": {
    "data": {
      "id": "perm-123",
      "name": "USER_READ_ALL",
      "description": "Get all users in the system",
      "url": "/api/users",
      "method": "GET",
      "resource": "USERS"
    }
  }
}
```

**Create Permission Request:**

```json
POST /permissions
{
  "name": "BLOG_DELETE",
  "description": "Delete blog post",
  "url": "/api/blogs/{id}",
  "method": "DELETE",
  "resource": "BLOGS",
  "active": true
}
```

**Create Permission Response (201 Created):**

```json
{
  "success": true,
  "status": "CREATED",
  "message": "Permission created successfully",
  "timestamp": "2024-11-27T10:05:00Z",
  "code": 201,
  "path": "/permissions",
  "payload": {
    "data": {
      "id": "perm-new",
      "name": "BLOG_DELETE",
      "description": "Delete blog post",
      "url": "/api/blogs/{id}",
      "method": "DELETE",
      "resource": "BLOGS"
    }
  }
}
```

**Update Permission Request:**

```json
PUT /permissions/perm-123
{
  "name": "USER_READ_ALL_UPDATED",
  "description": "Get all users with pagination",
  "url": "/api/users?page=1",
  "method": "GET",
  "resource": "USERS",
  "active": true
}
```

**Update Permission Response (200 OK):**

```json
{
  "success": true,
  "status": "OK",
  "message": "Permission updated successfully",
  "timestamp": "2024-11-27T10:10:00Z",
  "code": 200,
  "path": "/permissions/perm-123",
  "payload": {
    "data": {
      "id": "perm-123",
      "name": "USER_READ_ALL_UPDATED",
      "description": "Get all users with pagination",
      "url": "/api/users?page=1",
      "method": "GET",
      "resource": "USERS"
    }
  }
}
```

**Delete Permission Request:**

```
DELETE /permissions/perm-789
```

**Delete Permission Response (200 OK):**

```json
{
  "success": true,
  "status": "OK",
  "message": "Permission deleted successfully",
  "timestamp": "2024-11-27T10:15:00Z",
  "code": 200,
  "path": "/permissions/perm-789",
  "payload": {
    "data": null
  }
}
```

**Error Response (400 Bad Request - Validation):**

```json
{
  "success": false,
  "status": "BAD_REQUEST",
  "message": "Permission name is required",
  "timestamp": "2024-11-27T10:20:00Z",
  "code": 400,
  "path": "/permissions"
}
```

**Error Response (404 Not Found):**

```json
{
  "success": false,
  "status": "NOT_FOUND",
  "message": "Permission not found",
  "timestamp": "2024-11-27T10:25:00Z",
  "code": 404,
  "path": "/permissions/invalid-id"
}
```

**Error Response (409 Conflict - In Use):**

```json
{
  "success": false,
  "status": "CONFLICT",
  "message": "Permission is assigned to active roles and cannot be deleted",
  "timestamp": "2024-11-27T10:30:00Z",
  "code": 409,
  "path": "/permissions/perm-123"
}
```

---

## UI Components

**Page Location:** `/manage/permissions`

**Key Components:**

1. **Page Header Card**

   - Title: "Quản lý Permissions"
   - Description: "Quản lý các permissions trong hệ thống phân quyền"
   - Card layout with header and content sections

2. **Filter and Search Controls**

   - **Search Input**:
     - Placeholder: "Tìm theo tên..."
     - Max width: 320px
     - Filters by permission name
   - **Method Filter Dropdown**:
     - Label: "Lọc Method"
     - Width: 150px
     - Options: Tất cả, GET, POST, PUT, DELETE, PATCH
   - **Resource Filter Dropdown**:
     - Label: "Lọc Resource"
     - Width: 150px
     - Options: Tất cả, USERS, ROLES, PERMISSIONS, COURSES, BLOGS, LEARNING_PATHS
   - **Add Button**:
     - Text: "Thêm Permission"
     - Icon: PlusCircledIcon
     - Position: Right-aligned
     - Size: Small

3. **Permissions Data Table**

   - **Columns**:
     - **Tên** (sortable):
       - Font: Medium weight
       - Shows permission name
       - Click header to toggle sort
     - **Mô tả**:
       - Color: Muted foreground
       - Shows description or "-" if null
     - **Method** (filterable):
       - Badge component with color coding:
         - GET: Green (bg-green-100 text-green-800)
         - POST: Orange (bg-orange-100 text-orange-800)
         - PUT: Blue (bg-blue-100 text-blue-800)
         - DELETE: Red (bg-red-100 text-red-800)
         - PATCH: Purple (bg-purple-100 text-purple-800)
     - **URL**:
       - Displayed as code block
       - Small text size
       - Muted background with padding
     - **Resource** (filterable):
       - Badge with outline variant
       - Shows resource type
     - **Hành động**:
       - Dropdown menu (three dots icon)
       - Menu items: Chỉnh sửa, Xóa
       - Delete option in red/destructive color
   - **Table Features**:
     - Rounded border
     - Responsive layout
     - Row hover effect
     - Empty state: "Không có kết quả"

4. **Pagination Controls**

   - Previous button: "Trước"
   - Next button: "Sau"
   - Outline variant, small size
   - Disabled when no more pages
   - Position: Bottom-right

5. **Add Permission Dialog**

   - **Title**: "Thêm Permission Mới"
   - **Description**: "Tạo permission mới cho hệ thống phân quyền"
   - **Max Width**: 600px
   - **Form Fields**:
     - **Tên** (required):
       - Input component
       - Placeholder: "USER_READ_ALL"
       - Red asterisk indicator
     - **Mô tả** (optional):
       - Textarea component
       - Placeholder: "Get all users"
     - **Method** (required):
       - Select dropdown
       - Options: GET, POST, PUT, DELETE, PATCH
       - Default: GET
     - **Resource** (required):
       - Select dropdown
       - Options: USERS, ROLES, PERMISSIONS, COURSES, BLOGS, LEARNING_PATHS
       - Default: USERS
     - **URL** (required):
       - Input component
       - Placeholder: "/api/users"
     - **Trạng thái**:
       - Switch toggle
       - Label: "Trạng thái"
       - Shows "Active" or "Inactive"
       - Default: true (Active)
   - **Grid Layout**: 2 columns for Method and Resource fields
   - **Footer**:
     - Submit button: "Thêm Permission"
     - Disabled while mutation pending

6. **Edit Permission Dialog**

   - **Title**: "Chỉnh sửa Permission"
   - **Description**: "Cập nhật thông tin permission trong hệ thống phân quyền"
   - Same form structure as Add dialog
   - Pre-filled with existing permission data
   - Loading state: "Đang tải..." while fetching
   - **Footer**:
     - Submit button: "Cập nhật Permission"
     - Disabled while loading or mutation pending

7. **Delete Permission AlertDialog**

   - **Title**: "Xóa Permission"
   - **Description**: "Bạn có chắc chắn muốn xóa permission [name]? Hành động này không thể hoàn tác."
   - Permission name highlighted with colored background
   - **Actions**:
     - Cancel button: "Hủy"
     - Confirm button: "Tiếp tục"

8. **Loading State**

   - TableSkeleton component shown while fetching data
   - Skeleton animation for better UX

9. **Error State**

   - Red text displaying error message
   - Format: "Lỗi: [error.message]"

10. **Toast Notifications**
    - Success: Permission created/updated/deleted messages
    - Error: "Lỗi - Không thể [action] permission"
    - Shows backend message when available

---

## Validation Rules

**Permission Name:**

- Required field (cannot be empty)
- Minimum length: 1 character
- Maximum length: 100 characters
- Should follow naming convention (e.g., RESOURCE_ACTION_SCOPE)
- Must be unique across all permissions

**Description:**

- Optional field
- Can be null or empty string
- No maximum length restriction (reasonable backend limit)

**URL:**

- Required field (cannot be empty)
- Maximum length: 255 characters
- Should be valid API endpoint path
- Can include path parameters (e.g., /api/users/{id})

**Method:**

- Required field
- Must be one of: GET, POST, PUT, DELETE, PATCH
- Default: GET

**Resource:**

- Required field
- Must be one of: USERS, ROLES, PERMISSIONS, COURSES, BLOGS, LEARNING_PATHS
- Default: USERS

**Active Status:**

- Boolean field
- Default: true
- Determines if permission is active in the system

---

## Business Rules

1. **Permission Structure:**

   - Each permission defines access to a specific HTTP endpoint
   - Combination of Method + URL should ideally be unique
   - Permission name should clearly indicate the action (e.g., USER_CREATE, COURSE_READ)
   - Resource categorization helps organize permissions logically

2. **Permission Assignment:**

   - Permissions are assigned to roles, not directly to users
   - Users inherit permissions from their assigned roles
   - A permission can be assigned to multiple roles
   - Changing a permission affects all roles and users that have it

3. **CRUD Operations:**

   - **Create**: New permissions are immediately available for role assignment
   - **Read**: All permissions visible to admins, filtered by active status in production
   - **Update**: Changes propagate to all role assignments
   - **Delete**: Only possible if permission is not assigned to any active role

4. **Data Integrity:**

   - Permission names must be unique (no duplicates)
   - Cannot delete permissions that are in use by roles
   - Soft delete may be preferred over hard delete for audit purposes
   - Permission changes are logged for security auditing

5. **HTTP Method Colors:**

   - Visual color coding helps admins quickly identify operation types
   - GET (Read): Green - Safe operations
   - POST (Create): Orange - Data creation
   - PUT (Update): Blue - Data modification
   - DELETE (Remove): Red - Data deletion
   - PATCH (Partial): Purple - Partial updates

6. **Resource Categories:**

   - USERS: User management permissions
   - ROLES: Role and permission management
   - PERMISSIONS: Permission CRUD operations
   - COURSES: Course content management
   - BLOGS: Blog post management
   - LEARNING_PATHS: Learning path administration

7. **Search and Filter:**

   - Search is case-insensitive
   - Filters can be combined (Method + Resource + Name search)
   - Filtering is client-side for better performance
   - Sorting applies to filtered results

8. **Pagination:**
   - Default page size configured in table settings
   - Maintains filter/search state across pages
   - Shows total count if available

---

## User Experience Flow

**Admin Creating New Permission:**

1. **Access**

   - Navigate to /manage/permissions
   - View existing permissions table

2. **Initiate Creation**

   - Click "Thêm Permission" button
   - Dialog opens with empty form

3. **Fill Form**

   - Enter permission name (required)
   - Add description (optional, recommended)
   - Select HTTP method from dropdown
   - Select resource category
   - Enter API endpoint URL
   - Toggle active status if needed

4. **Submit**

   - Click "Thêm Permission"
   - See loading state on button
   - Receive success toast

5. **Verification**
   - Dialog closes automatically
   - Table refreshes
   - New permission appears in list
   - Can immediately assign to roles

**Admin Editing Permission:**

1. **Select**

   - Locate permission in table
   - Click three-dots menu
   - Select "Chỉnh sửa"

2. **Review**

   - Dialog opens with loading state
   - Form populates with current data
   - Review existing values

3. **Modify**

   - Update desired fields
   - Keep other fields unchanged
   - Validate changes

4. **Save**
   - Click "Cập nhật Permission"
   - Receive confirmation
   - Changes applied immediately

**Admin Deleting Permission:**

1. **Select**

   - Find permission to delete
   - Click actions menu
   - Click "Xóa" (red option)

2. **Confirm**

   - Read warning message
   - Understand action is irreversible
   - Click "Tiếp tục" or "Hủy"

3. **Complete**
   - If confirmed: Permission deleted
   - Success toast displayed
   - Table updates automatically

---

## User Messages

**Success Messages:**

- "Permission created successfully" (from backend)
- "Permission updated successfully"
- "Permission [name] đã được xóa"

**Error Messages:**

- "Lỗi - Không thể tạo permission"
- "Lỗi - Không thể cập nhật permission"
- "DeleteFailed" (from translation key)
- "Name is required"
- "URL is required"
- "Permission name already exists"
- "Permission is assigned to active roles and cannot be deleted"

**Info Messages:**

- "Đang tải..." (loading state in edit dialog)
- "Không có kết quả" (empty table state)

**Placeholder Text:**

- "Tìm theo tên..." (search input)
- "USER_READ_ALL" (name field)
- "Get all users" (description field)
- "/api/users" (URL field)
- "Chọn method" (method dropdown)
- "Chọn resource" (resource dropdown)

**Dialog Descriptions:**

- "Tạo permission mới cho hệ thống phân quyền"
- "Cập nhật thông tin permission trong hệ thống phân quyền"
- "Bạn có chắc chắn muốn xóa permission [name]? Hành động này không thể hoàn tác."

---

## Permission Management Flow Diagram

```
Manage Permissions Page
        ↓
Load Permissions from Database
        ↓
Display Data Table
        ↓
    ┌─────────────────────────┐
    │   Admin Actions         │
    │   - Search/Filter       │
    │   - Sort Columns        │
    │   - Paginate            │
    └─────────────────────────┘
        ↓
    ┌─────────────────────────┐
    │   Create Permission     │
    │   - Click Add Button    │
    │   - Fill Form           │
    │   - Submit              │
    │   - POST /permissions   │
    └─────────────────────────┘
        ↓
    Success Toast → Refresh Table
        ↓
    ┌─────────────────────────┐
    │   Edit Permission       │
    │   - Click Edit Menu     │
    │   - GET /permissions/id │
    │   - Modify Form         │
    │   - PUT /permissions/id │
    └─────────────────────────┘
        ↓
    Success Toast → Refresh Table
        ↓
    ┌─────────────────────────┐
    │   Delete Permission     │
    │   - Click Delete Menu   │
    │   - Confirm Dialog      │
    │   - DELETE /permissions │
    └─────────────────────────┘
        ↓
    Success Toast → Refresh Table
```

---

## Technical Notes

1. **State Management:**

   - React useState for local UI state (dialogs, filters, sorting)
   - React Query (TanStack Query) for server state
   - React Context (PermissionTableContext) for sharing state between components
   - Query caching with automatic invalidation on mutations

2. **Data Fetching:**

   - useGetPermissions: Fetches all permissions on mount
   - useGetPermissionById: Fetches single permission for editing
   - Queries enabled conditionally (e.g., only when ID exists)
   - Loading states handled gracefully with skeletons

3. **Mutations:**

   - useCreatePermissionMutation: POST new permission
   - useUpdatePermissionMutation: PUT permission by ID
   - useDeletePermissionMutation: DELETE permission by ID
   - All mutations invalidate ["permissions"] query key on success

4. **Table Features (TanStack Table):**

   - Sorting: Client-side sorting with state persistence
   - Filtering: Column filters for method, resource, name
   - Pagination: getPaginationRowModel
   - Visibility: Column visibility toggle (if needed)
   - Row selection: Checkbox selection (currently not used)

5. **Form Handling:**

   - React Hook Form for form state management
   - Zod resolver for schema validation
   - Field-level validation with error messages
   - Form reset on dialog close or successful submit

6. **Validation:**

   - Zod schemas: CreatePermissionBody, UpdatePermissionBody
   - Client-side validation before submission
   - Server-side validation errors mapped to form fields via handleErrorApi

7. **Dialog Management:**

   - Controlled dialogs with open/onOpenChange props
   - AddPermission: open state from parent
   - EditPermission: open when permissionIdEdit is defined
   - DeletePermission: open when permissionDelete is not null

8. **Error Handling:**

   - Try-catch blocks for all async operations
   - handleErrorApi utility for mapping backend errors to form fields
   - Toast notifications for user feedback
   - Error state display in table

9. **Performance Optimizations:**

   - React Query caching reduces unnecessary API calls
   - Client-side filtering and sorting (no server round-trips)
   - Conditional rendering for dialogs (not mounted when closed)
   - Memoized table data with useMemo (implicitly via TanStack Table)

10. **Accessibility:**

    - Semantic HTML structure
    - Proper ARIA attributes from shadcn/ui components
    - Keyboard navigation support in dialogs and dropdowns
    - Focus management (dialog focus trap)

11. **Responsive Design:**

    - Table scrolls horizontally on small screens
    - Dialog adapts to screen size (sm:max-w-600px)
    - Grid layout for form fields (responsive columns)
    - Touch-friendly button sizes

12. **Internationalization:**

    - useTranslations("ManagePermission") for i18n
    - Translation keys for success/error messages
    - Supports multiple languages (vi, en, ja)

13. **Color Coding:**

    - HTTP methods have distinct colors for visual clarity
    - Badge components for method and resource
    - Destructive variant for delete actions

14. **Security Considerations:**
    - Admin-only access (role check on backend)
    - Permissions affect RBAC system security
    - Audit logging recommended for all CRUD operations
    - Cannot delete in-use permissions (data integrity)

---

## Instructor: Dr. Mai Anh Tho

## Project: TechHub - Software Engineering

## Page: 46
