# 4.6.3 Manage Users

| Field     | Value |
| --------- | ----- |
| **Actor** | Admin |

| **Trigger** | The admin accesses the Manage Users page (/manage/accounts) and wants to view, create, edit, or delete user accounts in the system. |

| **Description** | This Use Case allows administrators to manage user accounts in the TechHub system. Each user account has a username, email, profile picture, roles, and status. Admins can view all users in a sortable and searchable table, create new user accounts with role assignments, edit existing user information and roles, and delete user accounts that are no longer needed. User accounts are the foundation of the system, connecting users to roles that define what they can do in the system. |

| **Pre-Conditions** | - The admin is logged in<br>- The admin has permission to view users<br>- The admin has permission to create new users<br>- The admin has permission to edit users<br>- The admin has permission to delete users<br>- The user management system is working<br>- Roles exist in the system for assignment to users |

| **Post-Conditions** | - User list is displayed with current data from database<br>- New users are created with assigned roles and credentials<br>- Existing users are updated with new information and roles<br>- Deleted users are removed from the database<br>- All permissions associated with user roles are affected accordingly<br>- Success or error notifications are displayed to the admin |

| **Main Flow** | 1. The admin navigates to the Manage Users page (/manage/accounts).<br>2. The system displays the page header with:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Title: "Account Management"<br>&nbsp;&nbsp;&nbsp;&nbsp;- Description: "Manage user accounts and permissions"<br>3. The system loads all users from the database with pagination (showing 10 users per page).<br>4. The system displays a table with users containing:<br>&nbsp;&nbsp;&nbsp;&nbsp;- ID column (sortable) - User identification number<br>&nbsp;&nbsp;&nbsp;&nbsp;- Profile Picture column - User profile image<br>&nbsp;&nbsp;&nbsp;&nbsp;- Username column (sortable) - Display name<br>&nbsp;&nbsp;&nbsp;&nbsp;- Email column (sortable) - User email address<br>&nbsp;&nbsp;&nbsp;&nbsp;- Role column (sortable) - Badge showing assigned roles (e.g., "ADMIN", "LEARNER")<br>&nbsp;&nbsp;&nbsp;&nbsp;- Actions column - Edit and Delete buttons (shown based on admin permissions)<br>5. The system provides search and action controls:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Email search input: Filter by email address<br>&nbsp;&nbsp;&nbsp;&nbsp;- Username search input: Filter by username<br>&nbsp;&nbsp;&nbsp;&nbsp;- "Create Account" button: Opens add user dialog (only shown if admin has USER_CREATE permission)<br>6. The admin views the user list with pagination controls:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Previous/Next buttons for navigation<br>&nbsp;&nbsp;&nbsp;&nbsp;- Page indicator showing current page and total pages<br>&nbsp;&nbsp;&nbsp;&nbsp;- Page size selector (10, 20, 50 items per page)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Total count display showing current items and total<br>7. (Optional) The admin searches for specific users:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin types text in the email or username search box.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System filters table to show only matching users.<br>8. (Optional) The admin sorts the table:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin clicks on column headers (ID, Username, Email, Role).<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System toggles between ascending and descending order.<br>9. (Optional) The admin changes page size:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin selects new page size from dropdown (10, 20, or 50).<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System resets to page 1 and refetches data with new page size.<br>10. (Optional) The admin navigates between pages:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin clicks "Previous" or "Next" button.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. System updates URL parameter and fetches the previous or next page of results.<br>11. **To create a new user**, the admin clicks "Create Account" button.<br>12. The system opens "Create Account" dialog with form fields:<br>&nbsp;&nbsp;&nbsp;&nbsp;**Profile Picture Upload:**<br>&nbsp;&nbsp;&nbsp;&nbsp;- Picture preview showing current picture or username initial<br>&nbsp;&nbsp;&nbsp;&nbsp;- Upload button - Opens file selector to upload image from computer<br>&nbsp;&nbsp;&nbsp;&nbsp;- Library button - Opens media library to select existing image<br>&nbsp;&nbsp;&nbsp;&nbsp;- Accepts image files only (jpg, png, gif, etc.)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Maximum file size: 10MB<br>&nbsp;&nbsp;&nbsp;&nbsp;- Uploads to cloud storage<br>&nbsp;&nbsp;&nbsp;&nbsp;**Basic Information:**<br>&nbsp;&nbsp;&nbsp;&nbsp;- Username (required) - Text input with placeholder<br>&nbsp;&nbsp;&nbsp;&nbsp;- Email (required) - Email input with validation<br>&nbsp;&nbsp;&nbsp;&nbsp;- Password (required) - Password input, minimum 6 characters<br>&nbsp;&nbsp;&nbsp;&nbsp;- Confirm Password (required) - Must match password<br>&nbsp;&nbsp;&nbsp;&nbsp;- Role (required) - Dropdown selector showing available roles from system<br>13. The admin fills in the required fields:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin enters username (minimum 1 character).<br>&nbsp;&nbsp;&nbsp;&nbsp;b. Admin enters valid email address.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. Admin enters password (minimum 6 characters).<br>&nbsp;&nbsp;&nbsp;&nbsp;d. Admin confirms password (must match).<br>&nbsp;&nbsp;&nbsp;&nbsp;e. Admin selects at least one role from dropdown.<br>14. (Optional) The admin uploads a profile picture:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin clicks Upload button to select file from computer OR clicks Library button to choose from media library.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. If uploading from computer:<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- System validates file type (must be image).<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- System validates file size (max 10MB).<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- System uploads image to cloud storage.<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- System saves the image location.<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- Picture preview updates with uploaded image.<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- Success message: "Profile picture uploaded successfully".<br>&nbsp;&nbsp;&nbsp;&nbsp;c. If selecting from library:<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- Media library opens showing user's uploaded images.<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- Admin selects an image from the library.<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- System saves the selected image location.<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- Dialog closes and picture preview updates.<br>15. The admin clicks "Add" button to submit the form.<br>16. The system validates the form data:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Email must be valid format<br>&nbsp;&nbsp;&nbsp;&nbsp;- Username must not be empty<br>&nbsp;&nbsp;&nbsp;&nbsp;- Password must be at least 6 characters<br>&nbsp;&nbsp;&nbsp;&nbsp;- Confirm password must match password<br>&nbsp;&nbsp;&nbsp;&nbsp;- At least one role must be selected<br>17. The system saves the new user data to the database.<br>18. The system securely stores the password and assigns the selected role.<br>19. The system displays success message: "Account created successfully".<br>20. The system closes the dialog and clears the form.<br>21. The system refreshes the user table.<br>22. The updated user list shows the newly created user.<br>23. **To edit an existing user**, the admin clicks the actions menu (three dots) on a user row.<br>24. The admin selects "Edit" from the dropdown menu (only visible if admin has USER_UPDATE permission).<br>25. The system opens "Update Account" dialog.<br>26. The system loads the user details from the database.<br>27. The system displays a loading message: "Loading..."<br>28. The system fills the form with current user data:<br>&nbsp;&nbsp;&nbsp;&nbsp;**Profile Picture Upload:**<br>&nbsp;&nbsp;&nbsp;&nbsp;- Same picture upload options as create (Upload button and Library button)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Shows current picture or username initial<br>&nbsp;&nbsp;&nbsp;&nbsp;**Basic Information:**<br>&nbsp;&nbsp;&nbsp;&nbsp;- Username field filled with current username (can be changed)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Email field filled with current email (cannot be changed, grayed out)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Role selector filled with current role (can be changed)<br>&nbsp;&nbsp;&nbsp;&nbsp;**Password Change Section:**<br>&nbsp;&nbsp;&nbsp;&nbsp;- Change Password toggle (default: off)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Password field (only shown when toggle is on)<br>&nbsp;&nbsp;&nbsp;&nbsp;- Confirm Password field (only shown when toggle is on)<br>29. The admin modifies the desired fields:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin can change username.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. Admin can change role selection from dropdown.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. Admin can upload new avatar or select from library.<br>&nbsp;&nbsp;&nbsp;&nbsp;d. Admin CANNOT change email (field is read-only).<br>30. (Optional) The admin changes the user's password:<br>&nbsp;&nbsp;&nbsp;&nbsp;a. Admin toggles "Change Password" switch to ON.<br>&nbsp;&nbsp;&nbsp;&nbsp;b. Password and Confirm Password fields appear.<br>&nbsp;&nbsp;&nbsp;&nbsp;c. Admin enters new password (minimum 6 characters).<br>&nbsp;&nbsp;&nbsp;&nbsp;d. Admin confirms new password (must match).<br>31. The admin clicks "Update Account" button.<br>32. The system validates the updated data:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Username must not be empty<br>&nbsp;&nbsp;&nbsp;&nbsp;- Roles array must contain at least one role<br>&nbsp;&nbsp;&nbsp;&nbsp;- If changePassword is true: password must be at least 6 characters and match confirmation<br>&nbsp;&nbsp;&nbsp;&nbsp;- If changePassword is false: password fields are removed from submission<br>33. The system saves the updated user data to the database.<br>34. The system updates the user information and role assignments.<br>35. The system displays success message: "Account updated successfully".<br>36. The system closes the dialog and clears the form.<br>37. The system refreshes the user list with the latest data.<br>38. **To delete a user**, the admin clicks the actions menu on a user row.<br>39. The admin selects "Delete" from the dropdown menu (only visible if admin has USER_DELETE permission).<br>40. The system opens a confirmation AlertDialog:<br>&nbsp;&nbsp;&nbsp;&nbsp;- Title: "Delete Account"<br>&nbsp;&nbsp;&nbsp;&nbsp;- Message: "Are you sure you want to delete account [username]? This action cannot be undone."<br>&nbsp;&nbsp;&nbsp;&nbsp;- Username is highlighted with background color<br>&nbsp;&nbsp;&nbsp;&nbsp;- Cancel button: "Cancel"<br>&nbsp;&nbsp;&nbsp;&nbsp;- Confirm button: "Continue"<br>41. The admin clicks "Continue" to confirm deletion.<br>42. The system removes the user from the database.<br>43. The system deletes the user and all associated data.<br>44. The system displays success message: "Successfully Deleted - Account has been deleted".<br>45. The system closes the dialog.<br>46. The system refreshes the user list, removing the deleted user from the table. |

| **Alternate Flow** | None |

| **Exception Flow** | **16.1** If username is empty:<br>&nbsp;&nbsp;&nbsp;&nbsp;- System displays validation error: "Username is required".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin must enter username to submit form.<br><br>**16.2** If email is invalid format:<br>&nbsp;&nbsp;&nbsp;&nbsp;- System displays validation error: "Invalid email address".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin must enter valid email format.<br><br>**16.3** If password is too short (less than 6 characters):<br>&nbsp;&nbsp;&nbsp;&nbsp;- System displays validation error: "Password must be at least 6 characters".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin must enter longer password.<br><br>**16.4** If passwords don't match:<br>&nbsp;&nbsp;&nbsp;&nbsp;- System displays validation error: "Passwords do not match".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin must enter matching passwords.<br><br>**16.5** If no role is selected:<br>&nbsp;&nbsp;&nbsp;&nbsp;- System displays validation error: "At least one role is required".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin must select a role from dropdown.<br><br>**14.1** If avatar file is not an image:<br>&nbsp;&nbsp;&nbsp;&nbsp;- System displays error: "Please select an image file".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin must select valid image file.<br><br>**14.2** If avatar file exceeds 10MB:<br>&nbsp;&nbsp;&nbsp;&nbsp;- System displays error: "File size must be less than 10MB".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin must select smaller file.<br><br>**17.1** If email already exists:<br>&nbsp;&nbsp;&nbsp;&nbsp;- System displays error: "Email already exists in the system".<br>&nbsp;&nbsp;&nbsp;&nbsp;- Admin must use different email address. |

---

## Related Use Cases

This use case is part of the RBAC (Role-Based Access Control) system:

- **Manage Permissions** (Use Case 4.6.1): Permissions define access rights for user actions.
- **Manage Roles** (Use Case 4.6.2): Users are assigned roles that contain permissions.
- **View Audit Logs** (Use Case 4.6.4): User changes are logged for security auditing.
- **User Profile Management**: Users can view and edit their own profile information.

---

## User Interface Components

**Page Location:** `/manage/accounts`

**Key Components:**

1. **Page Header Card**

   - Title: "Account Management"
   - Description: "Manage user accounts and permissions"
   - Card layout with header and content sections
   - Uses CardHeader, CardTitle, CardDescription, CardContent components

2. **Search and Action Controls**

   - **Email Search Input**:
     - Placeholder: "Filter by email"
     - Width: max-w-sm w-[150px]
     - Filters email column using TanStack Table
     - Real-time client-side filtering
   - **Username Search Input**:
     - Placeholder: "Filter by name" (though column is "fullName")
     - Width: max-w-sm w-[150px]
     - Filters username column
   - **Create Account Button**:
     - Text: "Create Account"
     - Icon: PlusCircle (h-3.5 w-3.5)
     - Size: Small (h-7 gap-1)
     - Only shown if admin has USER_CREATE permission
     - Opens AddEmployee dialog

3. **Users Data Table**

   - **Columns**:

     - **ID** (sortable):
       - Displays user identification number
     - **Profile Picture**:
       - Shows user's profile image
       - Shows username initial if no picture
     - **Username** (sortable):
       - Displays user's display name
     - **Email** (sortable):
       - Displays user's email address
     - **Role** (sortable):
       - Displays role badges
       - Multiple roles shown as separate badges
       - Shows "-" if no roles
     - **Actions**:
       - Menu with Edit and Delete options
       - Edit shown only if admin has update permission
       - Delete shown only if admin has delete permission

   - **Table Features**:
     - Sortable columns
     - Searchable by email and username
     - Shows loading animation while loading data
     - Shows "No results" when empty

4. **Page Navigation**

   - **Info Display**:
     - Shows: "Showing X of Y results"
   - **Navigation Buttons**:
     - Previous button: "Previous"
     - Page indicator: "Page X of Y"
     - Next button: "Next"
     - Disabled when no more pages
   - **Page Size Selector**:
     - Options: 10, 20, 50 users per page
     - Resets to page 1 when changed

5. **Add User Form**

   - **Title**: "Create Account"
   - **Description**: "Add a new user account to the system"

   - **Form Fields**:

     - **Profile Picture Upload**:

       - Picture preview
       - Upload button - Uploads image from computer
       - Library button - Selects from existing images
       - Shows "Uploading..." status when uploading

     - **Username** (required):

       - Text input field

     - **Email** (required):

       - Email input field

     - **Password** (required):

       - Password input field
       - Minimum 6 characters

     - **Confirm Password** (required):

       - Password input field
       - Must match password

     - **Role** (required):
       - Role selection dropdown
       - Shows "Loading roles..." while loading

   - **Action Buttons**:
     - Submit button: "Add" or "Submitting..."
     - Disabled when uploading or submitting

6. **Edit User Form**

   - **Title**: "Update Account"
   - Shows loading message: "Loading..." while loading data

   - **Form Fields**:

     - **Profile Picture Upload**:

       - Same as Add User form

     - **Username** (can be changed):

       - Text input field

     - **Email** (cannot be changed):

       - Grayed out text field

     - **Role** (can be changed):

       - Role selection dropdown

     - **Change Password** (toggle):

       - On/Off switch
       - Default: off

     - **Password** (shown only when toggle is on):

       - Password input field

     - **Confirm Password** (shown only when toggle is on):
       - Password input field

   - **Action Buttons**:
     - Submit button: "Update Account" or "Submitting..."
     - Disabled when uploading or submitting

7. **Delete Confirmation**

   - **Title**: "Delete Account"
   - **Message**: "Are you sure you want to delete account [username]? This action cannot be undone."
   - Username is highlighted
   - **Buttons**:
     - Cancel button: "Cancel"
     - Confirm button: "Continue"

8. **Media Library**

   - Opens when Library button clicked
   - Title: "Select Avatar"
   - Shows user's uploaded images
   - Closes automatically after selection

9. **Loading and Error States**

   - **Loading**: Shows loading animation
   - **Error**: Shows error message in red text
   - **Empty**: Shows "No results" when no users found

10. **Notification Messages**

    - **Success**:
      - Create: "Account created successfully"
      - Update: "Account updated successfully"
      - Delete: "Successfully Deleted - Account has been deleted"
      - Avatar Upload: "Avatar uploaded successfully"
    - **Error**:
      - Create: "Error - Failed to create account"
      - Update: "Failed to update account"
      - Delete: Error message from backend
      - Validation: "Validation Error - Please fix the errors in the form"
      - Avatar Upload: "Error - Failed to upload avatar"
      - File Type: "Error - Please select an image file"
      - File Size: "Error - File size must be less than 10MB"
    - Toast with title and description
    - Destructive variant for errors

---

## Validation Rules

**Username:**

- Required field (cannot be empty)
- Minimum length: 1 character
- No maximum length specified in schema
- Must be unique is not enforced client-side

**Email:**

- Required field
- Must be valid email format
- Must be unique across all users (enforced by backend)
- Cannot be changed after user creation (read-only in edit mode)

**Password:**

- Required field when creating new user
- Optional when editing user (only if changePassword is true)
- Minimum length: 6 characters
- No complexity requirements specified

**Confirm Password:**

- Required field when password is present
- Minimum length: 6 characters
- Must exactly match password field
- Validated using Zod refine function

**Roles:**

- Required field
- Must be array with at least one role
- Each role must exist in the system
- Single role selection via dropdown (array with one element)

**Avatar:**

- Optional field
- If uploading: must be image file type
- If uploading: maximum size 10MB
- Accepts URL string from Cloudinary
- Can be selected from media library or uploaded locally

---

## Business Rules

1. **User Structure:**

   - Each user has unique email (primary identifier)
   - Username is display name (can be changed)
   - Users must have at least one role
   - Avatar is optional but recommended
   - Status and isActive track account state

2. **Role Assignment:**

   - Users can have multiple roles (array)
   - UI currently supports single role selection
   - Roles determine user permissions
   - Changing roles immediately affects user's access rights
   - Cannot remove all roles from a user

3. **Password Management:**

   - Passwords are hashed before storage (backend)
   - Password changes are optional during edit
   - changePassword flag controls password update
   - If changePassword is false, existing password hash is kept
   - No password recovery flow in this use case

4. **Avatar Management:**

   - Avatars stored in Cloudinary
   - Can upload new image (max 10MB)
   - Can select from existing media library
   - Library filtered by userId and IMAGE type
   - Fallback to username initial if no avatar

5. **CRUD Operations:**

   - **Create**: Requires all mandatory fields
   - **Read**: Paginated list with search and sort
   - **Update**: Email is immutable, other fields editable
   - **Delete**: Immediate removal with confirmation

6. **Permissions-Based Access:**

   - USER_CREATE: Required to see "Create Account" button
   - USER_UPDATE: Required to see "Edit" in actions menu
   - USER_DELETE: Required to see "Delete" in actions menu
   - Permissions checked using usePermissions hook
   - Checks both permission name and method+URL combination

7. **Search and Filter:**

   - Client-side filtering on email and username
   - Case-insensitive search
   - Real-time filtering as user types
   - Does not require backend call

8. **Pagination:**

   - Default page size: 10 items
   - Selectable page sizes: 10, 20, 50
   - Server-side pagination via API
   - URL parameter tracks current page
   - Changing page size resets to page 1

9. **Sorting:**

   - Client-side sorting via TanStack Table
   - Sortable columns: ID, Username, Email, Role
   - Toggle between ascending/descending
   - Sorting state preserved during navigation

10. **Data Integrity:**

    - Email uniqueness enforced by backend
    - Role IDs must exist in roles table
    - Avatar URLs validated before storage
    - Form validation prevents invalid submissions

---

## User Experience Flow

**Admin Viewing User List:**

1. **Access**

   - Navigate to /manage/accounts
   - View page header with title and description

2. **Browse Users**

   - View paginated table of users
   - See ID, avatar, username, email, and roles
   - Use Previous/Next for pagination
   - Select page size (10, 20, 50)

3. **Search Users**

   - Type in email or username search box
   - See filtered results in real-time
   - Clear search to see all users again

4. **Sort Users**
   - Click column headers to sort
   - Toggle ascending/descending order
   - View sorted data immediately

**Admin Creating New User:**

1. **Initiate Creation**

   - Click "Create Account" button
   - Dialog opens with empty form

2. **Fill Basic Info**

   - Enter username
   - Enter email address
   - Enter password and confirmation
   - Select role from dropdown

3. **Upload Avatar (Optional)**

   - Click Upload button to select local file
   - OR click Library button to select from media library
   - Wait for upload: "Uploading..."
   - See success: "Avatar uploaded successfully"
   - Preview updates with selected/uploaded image

4. **Submit**

   - Click "Add" button
   - See "Submitting..." state
   - Receive success toast: "Account created successfully"
   - Dialog closes automatically
   - New user appears in table

5. **Verification**
   - Table refreshes with new user
   - Can immediately edit or assign additional roles if needed

**Admin Editing User:**

1. **Select**

   - Click three dots menu on user row
   - Click "Edit" option

2. **Review**

   - Dialog opens with "Loading..." state
   - Form populates with current user data
   - Email field is grayed out (read-only)

3. **Modify**

   - Change username if needed
   - Change role selection if needed
   - Upload new avatar or select from library
   - Optionally toggle "Change Password" to ON
   - If changing password: enter new password and confirmation

4. **Save**
   - Click "Update Account"
   - See "Submitting..." state
   - Success toast: "Account updated successfully"
   - Dialog closes
   - Table refreshes with updated data

**Admin Deleting User:**

1. **Select**

   - Click three dots menu on user row
   - Click "Delete" option

2. **Confirm**

   - Confirmation dialog appears
   - Read message with highlighted username
   - Click "Continue" or "Cancel"

3. **Complete**
   - If confirmed: User deleted
   - Success toast: "Successfully Deleted - Account has been deleted"
   - Table refreshes
   - User removed from list

---

## User Messages

**Success Messages:**

- "Account created successfully"
- "Account updated successfully"
- "Successfully Deleted - Account has been deleted"
- "Avatar uploaded successfully"

**Error Messages:**

- "Error - Failed to create account"
- "Failed to update account"
- "Validation Error"
- "Please fix the errors in the form"
- "Username is required"
- "Invalid email address"
- "Password must be at least 6 characters"
- "Passwords do not match"
- "At least one role is required"
- "Role is required"
- "Error - Please select an image file"
- "Error - File size must be less than 10MB"
- "Error - Failed to upload avatar"
- "Email already exists in the system"
- "User not found"

**Info Messages:**

- "Loading..." (dialog loading state)
- "Uploading..." (avatar upload in progress)
- "No results" (empty table state)
- "Loading roles..." (role dropdown loading)
- "Showing X of Y results" (pagination info)
- "Page X of Y" (page indicator)

**Placeholder Text:**

- "Filter by email"
- "Filter by name"
- "Enter username"
- "Enter email"
- "Enter password"
- "Confirm password"
- "Select a role"

**Dialog Descriptions:**

- "Add a new user account to the system"
- "Are you sure you want to delete account [username]? This action cannot be undone."

**Button Labels:**

- "Create Account"
- "Add" / "Submitting..."
- "Update Account" / "Submitting..."
- "Edit"
- "Delete"
- "Cancel"
- "Continue"
- "Previous"
- "Next"

**Field Labels:**

- "Username"
- "Email"
- "Password"
- "Confirm Password"
- "Role"
- "Change Password"
- "Profile Picture"

---

## Instructor: Dr. Mai Anh Tho

## Project: TechHub - Software Engineering

## Use Case: 4.6.3 Manage Users

## Page: /manage/accounts
