# 4.6.2 Manage Roles

| [1]       | Manage Roles |
| --------- | ------------ |
| **Actor** | Admin        |

| **Trigger** | The admin accesses the Manage Roles page (/manage/roles) and wants to view, create, edit, or delete roles in the role-based access control (RBAC) system. |

| **Description** | This Use Case allows administrators to manage roles that group permissions together for assignment to users. Each role has a name, description, active status, and a collection of permissions. Admins can view all roles in a sortable and filterable table, create new roles with assigned permissions, edit existing roles and their permission assignments, and delete roles that are no longer needed. Roles serve as the middle layer in the RBAC system, connecting permissions to users. |

| **Pre-Conditions** | - The user is logged in as an Admin<br>- The user has access to the Manage Roles page (/manage/roles)<br>- The role management system is available<br>- Backend API for roles is operational<br>- Permissions exist in the system for assignment to roles |

| **Post-Conditions** | - Role list is displayed with current data from database<br>- New roles are created with assigned permissions<br>- Existing roles are updated with new information and permissions<br>- Deleted roles are removed from the database<br>- All user assignments to these roles are affected accordingly<br>- Success or error notifications are displayed to the admin |

| **Main Flow** | 1. The admin navigates to the Manage Roles page (/manage/roles).<br>2. The system displays the page header with:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Title: "Quản lý Roles"<br>&nbsp;&nbsp;&nbsp;&nbsp;- Description: "Quản lý các roles và phân quyền trong hệ thống"<br>3. The system fetches all roles from the database via GET /roles endpoint.<br>4. The system displays a data table with roles containing:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Name column (sortable) - Role name (e.g., "ADMIN", "INSTRUCTOR")<br>&nbsp;&nbsp;&nbsp;&nbsp;- Description column - Optional description of the role<br>&nbsp;&nbsp;&nbsp;&nbsp;- Status column - Active/Inactive badge (green for active, gray for inactive)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Permissions column - Badge showing permission count (e.g., "5 permissions")<br>&nbsp;&nbsp;&nbsp;&nbsp;- Actions column - Edit and Delete buttons<br>5. The system provides search and action controls:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Search input: Filter by role name<br>&nbsp;&nbsp;&nbsp;&nbsp;- "Thêm Role" button: Opens add role dialog<br>6. The admin views the role list with pagination controls (Previous/Next buttons).<br>7. (Optional) The admin searches for specific roles:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin types text in the search box.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System filters table to show only matching role names.<br>8. (Optional) The admin sorts the table:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin clicks on "Tên" column header.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System toggles between ascending and descending order.<br>9. (Optional) The admin navigates between pages:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin clicks "Trước" or "Sau" button.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System displays the previous or next page of results.<br>10. **To create a new role**, the admin clicks "Thêm Role" button.<br>11. The system opens "Thêm Role Mới" dialog with form fields and permission selection:<br>&nbsp;&nbsp;&nbsp;&nbsp;**Basic Information:**<br>&nbsp;&nbsp;&nbsp;&nbsp;- Tên (required) - Role name input<br>&nbsp;&nbsp;&nbsp;&nbsp;- Mô tả (optional) - Description textarea<br>&nbsp;&nbsp;&nbsp;&nbsp;- Trạng thái - Active/Inactive toggle switch (default: Active)<br>&nbsp;&nbsp;&nbsp;&nbsp;**Permissions Selection:**<br>&nbsp;&nbsp;&nbsp;&nbsp;- Grouped by resource type (USERS, ROLES, PERMISSIONS, COURSES, BLOGS, LEARNING_PATHS)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Each group has toggle all switch<br>&nbsp;&nbsp;&nbsp;&nbsp;- Individual permissions with switches showing name, method (color-coded), and URL<br>&nbsp;&nbsp;&nbsp;&nbsp;- Badge showing selected count (e.g., "3 đã chọn")<br>12. The admin fills in the required role name field.<br>13. The admin selects permissions to assign to the role:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin can toggle entire resource groups using group-level switch.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. Admin can toggle individual permissions within each group.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. Selected count updates as permissions are toggled.<br>14. The admin clicks "Thêm Role" button in dialog footer.<br>15. The system validates the form data using Zod schema validation.<br>16. The system sends POST request to /roles endpoint with role data and permissionIds array.<br>17. The backend creates a new role record and assigns permissions in a single transaction.<br>18. The system displays success toast: "Role đã được tạo".<br>19. The system closes the dialog and resets the form.<br>20. The system refreshes the role list to show the new role with permission count.<br>21. **To edit an existing role**, the admin clicks the actions menu (three dots) on a role row.<br>22. The admin selects "Chỉnh sửa" from the dropdown menu.<br>23. The system opens "Chỉnh sửa Role" dialog.<br>24. The system fetches the role details via GET /roles/{id}.<br>25. The system populates the form with current role data:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Name, description, and active status are filled.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Permission switches are checked for currently assigned permissions.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Selected count displays current permission count.<br>26. The admin modifies the desired fields (name, description, status).<br>27. The admin updates permission assignments:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin toggles resource groups or individual permissions.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System updates selected count as changes are made.<br>28. The admin clicks "Cập nhật Role" button.<br>29. The system validates the updated data.<br>30. The system sends PUT request to /roles/{id} with updated role data and permissionIds array.<br>31. The backend updates the role record and permission assignments in the database.<br>32. The system displays success toast: "Role đã được cập nhật".<br>33. The system closes the dialog and refreshes the role list.<br>34. **To delete a role**, the admin clicks the actions menu on a role row.<br>35. The admin selects "Xóa" from the dropdown menu (displayed in red/destructive color).<br>36. The system opens a confirmation AlertDialog:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Title: "Xóa Role"<br>&nbsp;&nbsp;&nbsp;&nbsp;- Message: "Bạn có chắc chắn muốn xóa role [name]? Hành động này không thể hoàn tác."<br>&nbsp;&nbsp;&nbsp;&nbsp;- Cancel button: "Hủy"<br>&nbsp;&nbsp;&nbsp;&nbsp;- Confirm button: "Tiếp tục"<br>37. The admin clicks "Tiếp tục" to confirm deletion.<br>38. The system sends DELETE request to /roles/{id}.<br>39. The backend removes the role and its permission assignments from the database.<br>40. The system displays success toast: "Xóa thành công - Role [name] đã được xóa".<br>41. The system closes the dialog and refreshes the role list. |

| **Alternate Flow** | **41.1** Admin cancels adding a new role:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin clicks outside the dialog or presses ESC key.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System closes the dialog without saving.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. Form is reset to default values.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. Permission selections are cleared.<br>&nbsp;&nbsp;&nbsp;&nbsp;e. Admin returns to role list.<br>&nbsp;&nbsp;&nbsp;&nbsp;f. **No role is created** (neutral outcome).<br><br>**41.2** Admin uses dialog close button instead of form submit:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin clicks the X button on dialog header.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System triggers onOpenChange event with false value.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. Dialog closes and form resets.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. **No data is saved** (same as cancel).<br><br>**41.3** Admin cancels editing a role:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin clicks outside the edit dialog or presses ESC.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System closes the dialog without updating.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. Original role data and permissions remain unchanged.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. Admin returns to role list.<br>&nbsp;&nbsp;&nbsp;&nbsp;e. **Role is not modified** (neutral outcome).<br><br>**41.4** Admin cancels role deletion:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin clicks "Hủy" button in the confirmation dialog.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System closes the AlertDialog.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. No DELETE request is sent to backend.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. Role and its assignments remain in the database.<br>&nbsp;&nbsp;&nbsp;&nbsp;e. Admin returns to role list.<br>&nbsp;&nbsp;&nbsp;&nbsp;f. **Role is preserved** (neutral outcome).<br><br>**41.5** Admin creates role by bulk selecting resource group:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin toggles resource group switch (e.g., USERS).<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System selects/deselects all permissions in that resource group.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. Selected count updates to reflect changes.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. Admin submits form with bulk-selected permissions.<br>&nbsp;&nbsp;&nbsp;&nbsp;e. **Role is created with all permissions from selected resource groups** (same outcome as individual selection).<br><br>**41.6** Admin modifies role by selecting individual permissions:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin toggles individual permission switches.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. If all permissions in a group are selected, group switch shows checked state.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. If some permissions in a group are selected, group switch shows partial state.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. Admin submits form with custom permission selection.<br>&nbsp;&nbsp;&nbsp;&nbsp;e. **Role is updated with specific permission combination** (same outcome as bulk selection). |

| **Exception Flow** | **15.1** If role name is empty or invalid:<br>&nbsp;&nbsp;&nbsp;&nbsp;- System displays field-level validation error under name field.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Error message: "Name is required" or "Name too long (max 50 characters)".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Form submit button may be disabled.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin must correct error before resubmitting.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin remains in the dialog.<br><br>**16.1** If role name already exists (duplicate):<br>&nbsp;&nbsp;&nbsp;&nbsp;- Backend returns 400 Bad Request error.<br>&nbsp;&nbsp;&nbsp;&nbsp;- System displays error toast: "Lỗi - Không thể tạo role".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Possible message: "Role name already exists".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Form remains open with data preserved.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin can modify name and retry.<br><br>**16.2** If network error occurs during creation:<br>&nbsp;&nbsp;&nbsp;&nbsp;- System catches the error in try-catch block.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Displays error toast: "Lỗi - Không thể tạo role".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Form data and permission selections are preserved.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin can retry submission.<br><br>**16.3** If permission assignment fails after role creation:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Backend may rollback role creation (transaction).<br>&nbsp;&nbsp;&nbsp;&nbsp;- System displays error toast with details.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin can retry creating role with permissions.<br><br>**24.1** If role details fail to load (edit mode):<br>&nbsp;&nbsp;&nbsp;&nbsp;- Dialog shows "Đang tải..." message while fetching.<br>&nbsp;&nbsp;&nbsp;&nbsp;- If fetch fails, error is caught.<br>&nbsp;&nbsp;&nbsp;&nbsp;- System may show error message or empty form.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin can close dialog and retry.<br><br>**30.1** If role update fails (validation error):<br>&nbsp;&nbsp;&nbsp;&nbsp;- Backend returns 400 error with validation details.<br>&nbsp;&nbsp;&nbsp;&nbsp;- System uses handleErrorApi to set field errors.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Displays error toast: "Lỗi - Không thể cập nhật role".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Form remains open for corrections.<br><br>**30.2** If permission assignment update fails:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Backend may return error if permissions don't exist.<br>&nbsp;&nbsp;&nbsp;&nbsp;- System displays error toast with explanation.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Original permissions remain assigned.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin can adjust permission selection and retry.<br><br>**38.1** If role is assigned to active users (cannot delete):<br>&nbsp;&nbsp;&nbsp;&nbsp;- Backend returns 409 Conflict error.<br>&nbsp;&nbsp;&nbsp;&nbsp;- System displays error toast: "Xóa thất bại".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Error message: "Role is assigned to users and cannot be deleted".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Confirmation dialog closes.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin must remove role from users first.<br><br>**38.2** If network error during deletion:<br>&nbsp;&nbsp;&nbsp;&nbsp;- System catches error in mutation.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Displays error toast with error message.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Role remains in database and table.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin can retry deletion.<br><br>**Initial Load Error** If roles fail to load on page load:<br>&nbsp;&nbsp;&nbsp;&nbsp;- System shows error message: "Lỗi: [error.message]".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Table shows error state instead of data.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin can refresh page to retry.<br><br>**Empty Result** If no roles exist in the system:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Table shows empty state message: "Không có kết quả".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin can create the first role using "Thêm Role" button.<br><br>**No Permissions Available** If no permissions exist when creating/editing role:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Permission selection section shows empty state.<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin must create permissions first before assigning to roles.<br>&nbsp;&nbsp;&nbsp;&nbsp;- System may display message: "No permissions available. Please create permissions first." |

---

## Related Use Cases

This use case is part of the RBAC (Role-Based Access Control) system:

- **Manage Permissions** (Use Case 4.6.1): Roles contain permissions that define access rights.
- **Manage Users** (Use Case 4.6.3): Roles are assigned to users for access control.
- **View Audit Logs** (Use Case 4.6.4): Role changes are logged for security auditing.

---

## API Endpoints

**Frontend Proxy Route:**

```
GET /app/api/proxy/roles
GET /app/api/proxy/roles/{id}
POST /app/api/proxy/roles
PUT /app/api/proxy/roles/{id}
DELETE /app/api/proxy/roles/{id}
POST /app/api/proxy/roles/{id}/permissions
DELETE /app/api/proxy/roles/{roleId}/permissions/{permissionId}
```

**Backend API:**

```
GET /roles
GET /roles/{id}
POST /roles
PUT /roles/{id}
DELETE /roles/{id}
POST /roles/{id}/permissions
DELETE /roles/{roleId}/permissions/{permissionId}
```

**Get All Roles Request:**

```
GET /roles
```

**Get All Roles Response (200 OK):**

```json
{
  "success": true,
  "status": "OK",
  "message": "Roles retrieved successfully",
  "timestamp": "2024-11-27T10:00:00Z",
  "code": 200,
  "path": "/roles",
  "payload": {
    "data": [
      {
        "id": "role-123",
        "name": "ADMIN",
        "description": "Administrator with full system access",
        "isActive": true,
        "permissionIds": ["perm-1", "perm-2", "perm-3", "perm-4", "perm-5"]
      },
      {
        "id": "role-456",
        "name": "INSTRUCTOR",
        "description": "Course instructor with teaching permissions",
        "isActive": true,
        "permissionIds": ["perm-10", "perm-11", "perm-12"]
      },
      {
        "id": "role-789",
        "name": "STUDENT",
        "description": "Student with learning access",
        "isActive": true,
        "permissionIds": ["perm-20", "perm-21"]
      }
    ]
  }
}
```

**Get Role By ID Request:**

```
GET /roles/role-123
```

**Get Role By ID Response (200 OK):**

```json
{
  "success": true,
  "status": "OK",
  "message": "Role retrieved successfully",
  "timestamp": "2024-11-27T10:00:00Z",
  "code": 200,
  "path": "/roles/role-123",
  "payload": {
    "data": {
      "id": "role-123",
      "name": "ADMIN",
      "description": "Administrator with full system access",
      "isActive": true,
      "permissions": [
        {
          "id": "perm-1",
          "name": "USER_READ_ALL",
          "description": "Get all users",
          "url": "/api/users",
          "method": "GET",
          "resource": "USERS"
        },
        {
          "id": "perm-2",
          "name": "USER_CREATE",
          "description": "Create new user",
          "url": "/api/users",
          "method": "POST",
          "resource": "USERS"
        }
      ]
    }
  }
}
```

**Create Role Request:**

```json
POST /roles
{
  "name": "MODERATOR",
  "description": "Content moderator",
  "active": true,
  "permissionIds": [
    "perm-blog-read",
    "perm-blog-update",
    "perm-blog-delete",
    "perm-course-read"
  ]
}
```

**Create Role Response (201 Created):**

```json
{
  "success": true,
  "status": "CREATED",
  "message": "Role created successfully",
  "timestamp": "2024-11-27T10:05:00Z",
  "code": 201,
  "path": "/roles",
  "payload": {
    "data": {
      "id": "role-new",
      "name": "MODERATOR",
      "description": "Content moderator",
      "isActive": true,
      "permissionIds": [
        "perm-blog-read",
        "perm-blog-update",
        "perm-blog-delete",
        "perm-course-read"
      ]
    }
  }
}
```

**Update Role Request:**

```json
PUT /roles/role-123
{
  "name": "ADMIN_UPDATED",
  "description": "Super administrator with enhanced access",
  "active": true,
  "permissionIds": [
    "perm-1",
    "perm-2",
    "perm-3",
    "perm-4",
    "perm-5",
    "perm-6"
  ]
}
```

**Update Role Response (200 OK):**

```json
{
  "success": true,
  "status": "OK",
  "message": "Role updated successfully",
  "timestamp": "2024-11-27T10:10:00Z",
  "code": 200,
  "path": "/roles/role-123",
  "payload": {
    "data": {
      "id": "role-123",
      "name": "ADMIN_UPDATED",
      "description": "Super administrator with enhanced access",
      "isActive": true,
      "permissionIds": [
        "perm-1",
        "perm-2",
        "perm-3",
        "perm-4",
        "perm-5",
        "perm-6"
      ]
    }
  }
}
```

**Delete Role Request:**

```
DELETE /roles/role-789
```

**Delete Role Response (200 OK):**

```json
{
  "success": true,
  "status": "OK",
  "message": "Role deleted successfully",
  "timestamp": "2024-11-27T10:15:00Z",
  "code": 200,
  "path": "/roles/role-789",
  "payload": {
    "data": null
  }
}
```

**Assign Permissions to Role Request:**

```json
POST /roles/role-123/permissions
{
  "permissionIds": [
    "perm-new-1",
    "perm-new-2",
    "perm-new-3"
  ]
}
```

**Assign Permissions Response (200 OK):**

```json
{
  "success": true,
  "status": "OK",
  "message": "Permissions assigned to role successfully",
  "timestamp": "2024-11-27T10:20:00Z",
  "code": 200,
  "path": "/roles/role-123/permissions",
  "payload": {
    "data": {
      "roleId": "role-123",
      "permissionIds": ["perm-new-1", "perm-new-2", "perm-new-3"]
    }
  }
}
```

**Remove Permission from Role Request:**

```
DELETE /roles/role-123/permissions/perm-5
```

**Remove Permission Response (200 OK):**

```json
{
  "success": true,
  "status": "OK",
  "message": "Permission removed from role successfully",
  "timestamp": "2024-11-27T10:25:00Z",
  "code": 200,
  "path": "/roles/role-123/permissions/perm-5",
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
  "message": "Role name is required",
  "timestamp": "2024-11-27T10:30:00Z",
  "code": 400,
  "path": "/roles"
}
```

**Error Response (404 Not Found):**

```json
{
  "success": false,
  "status": "NOT_FOUND",
  "message": "Role not found",
  "timestamp": "2024-11-27T10:35:00Z",
  "code": 404,
  "path": "/roles/invalid-id"
}
```

**Error Response (409 Conflict - In Use):**

```json
{
  "success": false,
  "status": "CONFLICT",
  "message": "Role is assigned to active users and cannot be deleted",
  "timestamp": "2024-11-27T10:40:00Z",
  "code": 409,
  "path": "/roles/role-123"
}
```

---

## UI Components

**Page Location:** `/manage/roles`

**Key Components:**

1. **Page Header Card**

   - Title: "Quản lý Roles"
   - Description: "Quản lý các roles và phân quyền trong hệ thống"
   - Card layout with header and content sections

2. **Search and Action Controls**

   - **Search Input**:
     - Placeholder: "Tìm theo tên..."
     - Max width: 384px (sm)
     - Filters by role name
   - **Add Button**:
     - Text: "Thêm Role"
     - Icon: PlusCircle
     - Position: Right-aligned (ml-auto)
     - Size: Small

3. **Roles Data Table**

   - **Columns**:
     - **Tên** (sortable):
       - Font: Medium weight
       - Shows role name
       - Click header to toggle sort
       - CaretSortIcon for sorting indicator
     - **Mô tả**:
       - Color: Muted foreground
       - Shows description or "-" if null
     - **Trạng thái**:
       - Badge component with variant:
         - Active: Default variant (green)
         - Inactive: Secondary variant (gray)
       - Text: "Active" or "Inactive"
     - **Permissions**:
       - Badge with outline variant
       - Shows count: "[count] permissions"
       - Count from permissionIds array length
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
   - Space between buttons: space-x-2

5. **Add/Edit Role Dialog**

   - **Title**:
     - Add: "Thêm Role Mới"
     - Edit: "Chỉnh sửa Role"
   - **Description**:
     - Add: "Tạo role mới và gán permissions"
     - Edit: "Cập nhật thông tin role"
   - **Max Width**: 800px
   - **Max Height**: 90vh
   - **Flex Layout**: Vertical with scrollable content

   - **Form Fields (Basic Information)**:

     - **Tên** (required):
       - Input component
       - Placeholder: "ADMIN"
       - Red asterisk indicator
       - Max length: 50 characters
     - **Mô tả** (optional):
       - Textarea component
       - Placeholder: "Quản trị viên hệ thống"
     - **Trạng thái**:
       - Switch toggle
       - Label: "Trạng thái"
       - Shows "Active" or "Inactive"
       - Default: true (Active)
       - Flex layout with gap-2

   - **Permissions Selection Section**:

     - **Header**:
       - Label: "Permissions"
       - Badge showing selected count: "[count] đã chọn"
       - Flex justify-between layout
     - **Permission Groups Container**:
       - Border rounded-lg with padding
       - Scrollable area (part of flex-1 overflow-y-auto)
       - Space-y-4 between groups
     - **Each Resource Group**:
       - **Group Header**:
         - Switch for select/deselect all in group
         - Resource name in bold large text (font-semibold text-lg)
         - Flex items-center gap-2
       - **Permission List**:
         - Indented with ml-8 (margin-left)
         - Grid layout with 1 column
         - Gap-2 between items
       - **Each Permission Item**:
         - Flex items-center gap-2
         - Switch for individual permission
         - Permission details in flex-1:
           - Name in text-sm
           - Dash separator
           - Method in text-xs with color coding:
             - GET: Green (text-green-600 font-semibold)
             - POST: Orange (text-orange-600 font-semibold)
             - PUT: Blue (text-blue-600 font-semibold)
             - DELETE: Red (text-red-600 font-semibold)
             - PATCH: Purple (text-purple-600 font-semibold)
           - URL in code format (text-xs text-muted-foreground)

   - **Footer**:
     - Submit button: "Thêm Role" or "Cập nhật Role"
     - Disabled while mutation pending
     - Form ID: "role-form" for external submission

6. **Delete Role AlertDialog**

   - **Title**: "Xóa Role"
   - **Description**: "Bạn có chắc chắn muốn xóa role [name]? Hành động này không thể hoàn tác."
   - Role name highlighted with colored background (bg-foreground text-primary-foreground rounded px-1)
   - **Actions**:
     - Cancel button: "Hủy"
     - Confirm button: "Tiếp tục"

7. **Loading State**

   - TableSkeleton component shown while fetching data
   - Skeleton animation for better UX

8. **Error State**

   - Red text displaying error message
   - Format: "Lỗi: [error.message]"

9. **Toast Notifications**
   - Success:
     - Create: "Role đã được tạo"
     - Update: "Role đã được cập nhật"
     - Delete: "Xóa thành công - Role [name] đã được xóa"
   - Error:
     - Create: "Lỗi - Không thể tạo role"
     - Update: "Lỗi - Không thể cập nhật role"
     - Delete: "Xóa thất bại - [error message]"
   - Toast with title and description
   - Destructive variant for errors

---

## Validation Rules

**Role Name:**

- Required field (cannot be empty)
- Minimum length: 1 character
- Maximum length: 50 characters
- Should follow naming convention (e.g., ADMIN, INSTRUCTOR, STUDENT)
- Must be unique across all roles
- Typically uppercase for consistency

**Description:**

- Optional field
- Can be null or empty string
- No strict maximum length (reasonable limit)
- Should describe role purpose

**Active Status:**

- Boolean field
- Default: true
- Determines if role is active in the system
- Inactive roles cannot be assigned to new users

**Permission IDs:**

- Optional array during creation (can create role without permissions)
- Each ID must be valid UUID
- All permission IDs must exist in the system
- Minimum 1 permission when using assign endpoint
- No duplicates in the array

---

## Business Rules

1. **Role Structure:**

   - Roles group related permissions for easier assignment
   - Role name should clearly indicate purpose (ADMIN, INSTRUCTOR, MODERATOR, etc.)
   - Description helps explain role scope and responsibilities
   - Active status controls whether role can be assigned to users

2. **Permission Assignment:**

   - Permissions can be assigned during role creation or later
   - Multiple permissions can be assigned at once
   - Permissions can be added or removed from roles
   - Same permission can belong to multiple roles
   - Removing a permission from a role affects all users with that role

3. **Role-User Relationship:**

   - Users are assigned roles, not individual permissions
   - One user can have multiple roles
   - User's effective permissions = union of all role permissions
   - Changing role permissions immediately affects all users with that role

4. **CRUD Operations:**

   - **Create**: New roles can be created with or without permissions
   - **Read**: All roles visible to admins, including permission count
   - **Update**: Can update name, description, status, and permissions together
   - **Delete**: Only possible if role is not assigned to any active user

5. **Data Integrity:**

   - Role names must be unique (no duplicates)
   - Cannot delete roles that are assigned to users
   - Soft delete may be preferred for audit trail
   - Role changes are logged for security compliance

6. **Permission Grouping:**

   - Permissions grouped by resource type for easier selection
   - Group-level toggle for bulk selection/deselection
   - Visual color coding by HTTP method for clarity
   - Selected count badge for progress tracking

7. **Search and Filter:**

   - Search by role name (case-insensitive)
   - Client-side filtering for better performance
   - Sorting by role name (alphabetical)

8. **Status Badge Colors:**

   - Active: Green/Default variant - Role is currently in use
   - Inactive: Gray/Secondary variant - Role is disabled

9. **Pagination:**
   - Default page size configured in table settings
   - Maintains search state across pages
   - Previous/Next navigation

---

## User Experience Flow

**Admin Creating New Role:**

1. **Access**

   - Navigate to /manage/roles
   - View existing roles table

2. **Initiate Creation**

   - Click "Thêm Role" button
   - Dialog opens with empty form

3. **Fill Basic Info**

   - Enter role name (required)
   - Add description (optional, recommended)
   - Toggle active status if needed

4. **Assign Permissions**

   - Browse grouped permissions by resource
   - Toggle entire resource groups for bulk selection
   - OR toggle individual permissions for custom setup
   - Watch selected count update

5. **Submit**

   - Click "Thêm Role"
   - See loading state on button
   - Receive success toast

6. **Verification**
   - Dialog closes automatically
   - Table refreshes
   - New role appears with permission count
   - Can immediately assign to users

**Admin Editing Role:**

1. **Select**

   - Locate role in table
   - Click three-dots menu
   - Select "Chỉnh sửa"

2. **Review**

   - Dialog opens with loading state
   - Form populates with current data
   - Permission switches show current assignments
   - Review existing values

3. **Modify**

   - Update desired fields
   - Add/remove permissions by toggling
   - Watch selected count update
   - Keep other fields unchanged

4. **Save**
   - Click "Cập nhật Role"
   - Receive confirmation
   - Changes applied immediately to all users with this role

**Admin Deleting Role:**

1. **Select**

   - Find role to delete
   - Click actions menu
   - Click "Xóa" (red option)

2. **Confirm**

   - Read warning message
   - Understand action is irreversible
   - Understand users will lose this role's permissions
   - Click "Tiếp tục" or "Hủy"

3. **Complete**
   - If confirmed: Role deleted
   - Success toast displayed
   - Table updates automatically
   - Users previously having this role lose its permissions

---

## User Messages

**Success Messages:**

- "Role đã được tạo" (role created)
- "Role đã được cập nhật" (role updated)
- "Xóa thành công - Role [name] đã được xóa" (role deleted)
- "Permissions assigned to role successfully" (from backend)

**Error Messages:**

- "Lỗi - Không thể tạo role" (cannot create role)
- "Lỗi - Không thể cập nhật role" (cannot update role)
- "Xóa thất bại" (delete failed)
- "Name is required"
- "Role name already exists"
- "Role is assigned to active users and cannot be deleted"
- "No permissions available. Please create permissions first."

**Info Messages:**

- "Đang tải..." (loading state in edit dialog)
- "Không có kết quả" (empty table state)
- "[count] đã chọn" (selected permission count)
- "[count] permissions" (permission count in table)

**Placeholder Text:**

- "Tìm theo tên..." (search input)
- "ADMIN" (name field)
- "Quản trị viên hệ thống" (description field)

**Dialog Descriptions:**

- "Tạo role mới và gán permissions"
- "Cập nhật thông tin role"
- "Bạn có chắc chắn muốn xóa role [name]? Hành động này không thể hoàn tác."

**Status Labels:**

- "Active" (active role)
- "Inactive" (inactive role)

---

## Role Management Flow Diagram

```
Manage Roles Page
        ↓
Load Roles from Database
        ↓
Display Data Table (with permission counts)
        ↓
    ┌─────────────────────────────────┐
    │   Admin Actions                 │
    │   - Search by name              │
    │   - Sort by name                │
    │   - Paginate                    │
    └─────────────────────────────────┘
        ↓
    ┌─────────────────────────────────┐
    │   Create Role                   │
    │   - Click Add Button            │
    │   - Fill Basic Info             │
    │   - Select Permissions          │
    │     * By resource group         │
    │     * Individual permissions    │
    │   - Submit                      │
    │   - POST /roles                 │
    └─────────────────────────────────┘
        ↓
    Success Toast → Refresh Table
        ↓
    ┌─────────────────────────────────┐
    │   Edit Role                     │
    │   - Click Edit Menu             │
    │   - GET /roles/{id}             │
    │   - Load current permissions    │
    │   - Modify Basic Info           │
    │   - Update Permissions          │
    │   - PUT /roles/{id}             │
    └─────────────────────────────────┘
        ↓
    Success Toast → Refresh Table
        ↓
    ┌─────────────────────────────────┐
    │   Delete Role                   │
    │   - Click Delete Menu           │
    │   - Confirm Dialog              │
    │   - DELETE /roles/{id}          │
    │   - Remove from users           │
    └─────────────────────────────────┘
        ↓
    Success Toast → Refresh Table
```

---

## Technical Notes

1. **State Management:**

   - React useState for local UI state (dialogs, search, sorting)
   - React Query (TanStack Query) for server state
   - React Context (RoleTableContext) for sharing state between components
   - Query caching with automatic invalidation on mutations

2. **Data Fetching:**

   - useGetRoles: Fetches all roles on mount (returns permissionIds array)
   - useGetRole: Fetches single role for editing (returns full permission objects)
   - Queries enabled conditionally (e.g., only when ID exists)
   - Loading states handled gracefully with skeletons

3. **Mutations:**

   - useCreateRoleMutation: POST new role with permissions
   - useUpdateRoleMutation: PUT role by ID (includes permission updates)
   - useDeleteRoleMutation: DELETE role by ID
   - useAssignPermissionsToRoleMutation: POST permissions to role
   - useRemovePermissionFromRoleMutation: DELETE permission from role
   - All mutations invalidate ["roles"] query key on success

4. **Permission Management:**

   - Grouped by resource type for better UX
   - Set data structure for efficient toggle operations
   - Group-level toggles check if all permissions selected
   - Individual toggles update set and trigger re-render
   - Color-coded methods for visual distinction

5. **Table Features (TanStack Table):**

   - Sorting: Client-side sorting by name
   - Filtering: Column filter for name search
   - Pagination: getPaginationRowModel
   - Responsive column configuration

6. **Form Handling:**

   - React Hook Form for form state management
   - Zod resolver for schema validation
   - Field-level validation with error messages
   - Form reset on dialog close or successful submit
   - Separate state for permission selections

7. **Validation:**

   - Zod schemas: CreateRoleBody, UpdateRoleBody
   - Client-side validation before submission
   - Server-side validation errors mapped to form fields via handleErrorApi
   - Permission IDs validated as UUID array

8. **Dialog Management:**

   - Controlled dialogs with open/onOpenChange props
   - AddModal: open state from parent component
   - EditModal: open when roleIdEdit is defined
   - DeleteDialog: open when roleDelete is not null
   - Dialog content scrollable (max-h-90vh)

9. **Error Handling:**

   - Try-catch blocks for all async operations
   - handleErrorApi utility for mapping backend errors
   - Toast notifications for user feedback
   - Error state display in table
   - Specific error messages for different scenarios

10. **Performance Optimizations:**

    - React Query caching reduces API calls
    - Client-side filtering and sorting
    - Conditional rendering for dialogs
    - Set data structure for O(1) permission lookups
    - Permission grouping done once during render

11. **Accessibility:**

    - Semantic HTML structure
    - ARIA attributes from shadcn/ui components
    - Keyboard navigation in dialogs and dropdowns
    - Focus management in dialogs
    - Switch components for boolean values

12. **Responsive Design:**

    - Table scrolls horizontally on small screens
    - Dialog adapts to screen size (sm:max-w-800px)
    - Responsive form layouts
    - Touch-friendly button sizes
    - Max dialog height (90vh) prevents overflow

13. **Internationalization:**

    - useTranslations("ManageRole") for i18n support
    - Translation keys for all UI text
    - Supports multiple languages (vi, en, ja)

14. **Security Considerations:**

    - Admin-only access (role check on backend)
    - Role changes affect all users immediately
    - Audit logging recommended for all operations
    - Cannot delete roles assigned to users
    - Transaction-based creation (role + permissions)

15. **Data Flow:**
    - Create: Single request with role data + permissionIds
    - Edit: Single request updates both role and permissions
    - Form maintains permissionIds as Set for efficient operations
    - Backend handles permission assignment atomically

---

## Instructor: Dr. Mai Anh Tho

## Project: TechHub - Software Engineering

## Page: 47
