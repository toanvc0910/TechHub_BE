# 4.2.2 Forgot Password

| [1]       | Forgot Password |
| --------- | --------------- |
| **Actor** | Registered User |

| **Trigger** | The user selects the "Forgot Password" option from the Login screen or accesses the forgot password page directly. |

| **Description** | This Use Case allows a registered user who has forgotten their password to initiate a password reset process by receiving an OTP (One-Time Password) code via email. |

| **Pre-Conditions** | - The user has a registered account in the system<br>- The user's email is verified and active |

| **Post-Conditions** | - An OTP code is generated and sent to the user's registered email<br>- The system creates a temporary reset token<br>- The user is redirected to the Reset Password page |

| **Main Flow** | 1. The user enters their registered email address in the forgot password form.<br>2. The user submits the form.<br>3. The system validates the email format.<br>4. The system verifies that the email exists in the database.<br>5. The system generates a 6-digit OTP code.<br>6. The system sends the OTP code to the user's email address.<br>7. The system displays a success message "Please check your email to receive OTP code".<br>8. The system redirects the user to the Reset Password page with the email parameter after 2 seconds. |

| **Alternate Flow** | N/A - There is only one method to reset password (via email OTP). |

| **Exception Flow** | **1.1** If the email field is empty:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays: "Email is required"<br>&nbsp;&nbsp;&nbsp;&nbsp;- The user remains on the form.<br><br>**1.2** If the email format is invalid:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays: "Invalid email address"<br>&nbsp;&nbsp;&nbsp;&nbsp;- The user remains on the form.<br><br>**3.1** If the email is not registered in the system:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays an error message: "Email not found" or "This email is not registered."<br>&nbsp;&nbsp;&nbsp;&nbsp;- The user remains on the forgot password form.<br><br>**4.1** If the email service fails to send the OTP:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays an error message: "Failed to send email. Please try again."<br>&nbsp;&nbsp;&nbsp;&nbsp;- The user can retry the operation.<br><br>**4.2** If there is a network or server error:<br>&nbsp;&nbsp;&nbsp;&nbsp;- The system displays: "An error occurred, please try again"<br>&nbsp;&nbsp;&nbsp;&nbsp;- The user remains on the form. |

---

## Related Use Cases

After successfully requesting a password reset, the user proceeds to:

- **Reset Password** (Use Case 4.2.3): The user enters the OTP code and new password to complete the password reset process.

---

## API Endpoint

**Frontend Proxy Route:**

```
POST /app/api/proxy/users/forgot-password
```

**Backend API:**

```
POST /users/forgot-password
```

**Request Body:**

```json
{
  "email": "user@example.com"
}
```

**Success Response (200 OK):**

```json
{
  "success": true,
  "status": "OK",
  "message": "OTP has been sent to your email",
  "timestamp": "2024-11-26T10:30:00Z",
  "code": 200
}
```

**Error Response (404 Not Found):**

```json
{
  "success": false,
  "status": "NOT_FOUND",
  "message": "Email not found",
  "timestamp": "2024-11-26T10:30:00Z",
  "code": 404
}
```

---

## UI Components

**Page Location:** `/forgot-password`

**Key Components:**

1. **Email Input Field**

   - Type: Email
   - Validation: Required, valid email format
   - Placeholder: User's email address

2. **Reset Password Button**

   - Disabled when: Form is submitting or email has been sent
   - Shows loading indicator during submission
   - Text changes to "Email Sent" after successful submission

3. **Back to Login Link**

   - Navigates user back to login page
   - Accessible at all times

4. **Visual Elements**
   - Split screen layout (form on left, image on right for desktop)
   - Responsive design for mobile devices
   - Clear instructions: "Enter your email below and we'll send you instructions on how to reset your password."

---

## Validation Rules

**Email Field:**

- Must not be empty
- Must be a valid email format (contains @ and domain)
- Must be registered in the system

---

## Business Rules

1. **OTP Generation:**

   - OTP code is 6 digits long
   - OTP has an expiration time (typically 10-15 minutes)
   - Only one active OTP per user at a time

2. **Security:**

   - Rate limiting: Prevent multiple rapid requests from same email
   - Do not reveal whether email exists (optional, for security)
   - Log all password reset attempts for audit purposes

3. **Email Delivery:**

   - Email contains OTP code
   - Email includes instructions for password reset
   - Email contains link to reset password page
   - Email has expiration notice

4. **User Experience:**
   - Clear feedback messages
   - Automatic redirect to reset password page
   - Email field is disabled after successful submission

---

## User Messages

**Success Messages:**

- "Please check your email to receive OTP code"
- "Email Sent"

**Error Messages:**

- "Email is required"
- "Invalid email address"
- "Email not found"
- "Failed to send email. Please try again"
- "An error occurred, please try again"

---

## Navigation Flow

```
Login Page → Forgot Password Page → Reset Password Page → Login Page
     ↑              ↓                        ↓                ↓
     └──────────────┘                        └────────────────┘
   (Back to Login)                      (After successful reset)
```

---

## Technical Notes

1. **State Management:**

   - Uses React Hook Form for form handling
   - Zod schema validation for email input
   - React Query mutation for API calls

2. **Loading States:**

   - Button shows loading spinner during submission
   - Button is disabled during API call
   - Form inputs disabled after successful submission

3. **Error Handling:**

   - Catches network errors
   - Displays user-friendly error messages
   - Logs errors to console for debugging

4. **Internationalization:**
   - All text labels are translatable
   - Supports multiple languages (English, Vietnamese, Japanese)

---

## Instructor: Dr. Mai Anh Tho

## Project: TechHub - Software Engineering

## Page: 41
