# TechHub - Sequence Diagram: Change Password

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Các thành phần chính](#2-các-thành-phần-chính)
3. [API Endpoints](#3-api-endpoints)
4. [Data Structures](#4-data-structures)
5. [Chi tiết luồng xử lý](#5-chi-tiết-luồng-xử-lý)
6. [Sequence Diagram](#6-sequence-diagram)
7. [Error Handling](#7-error-handling)
8. [Business Rules](#8-business-rules)

---

## 1. Tổng quan

Luồng **Change Password** cho phép user đã đăng nhập thay đổi mật khẩu của họ. Yêu cầu:

- User phải đăng nhập (có JWT token)
- Phải nhập đúng mật khẩu hiện tại
- Mật khẩu mới phải có ít nhất 6 ký tự
- Xác nhận mật khẩu phải khớp với mật khẩu mới

### Phân biệt với các luồng khác

| Luồng           | Mô tả                                    | Yêu cầu Auth |
| --------------- | ---------------------------------------- | ------------ |
| Change Password | User đổi mật khẩu khi đã đăng nhập       | ✅ Required  |
| Forgot Password | User quên mật khẩu, gửi OTP qua email    | ❌ No        |
| Reset Password  | User đặt lại mật khẩu sau khi verify OTP | ❌ No        |

---

## 2. Các thành phần chính

| Component         | Service      | Vai trò                             |
| ----------------- | ------------ | ----------------------------------- |
| `UserController`  | user-service | REST API endpoint                   |
| `UserService`     | user-service | Interface định nghĩa business logic |
| `UserServiceImpl` | user-service | Implementation của UserService      |
| `UserRepository`  | user-service | CRUD User entity                    |
| `PasswordEncoder` | user-service | Mã hóa và verify password (BCrypt)  |
| `User`            | user-service | Entity đại diện cho user            |

---

## 3. API Endpoints

| Method | Endpoint                     | Mô tả             | Auth Required |
| ------ | ---------------------------- | ----------------- | ------------- |
| POST   | `/api/users/change-password` | Đổi mật khẩu user | Yes (JWT)     |

### Request Headers

| Header        | Mô tả                           | Required |
| ------------- | ------------------------------- | -------- |
| Authorization | Bearer {jwt_token}              | Yes      |
| X-User-Id     | UUID của user (từ Proxy-Client) | Yes      |

---

## 4. Data Structures

### 4.1 Request DTO

#### ChangePasswordRequest

```java
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}
```

### 4.2 User Entity (relevant fields)

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    private UUID id;

    private String email;
    private String username;
    private String passwordHash;  // BCrypt encoded

    private LocalDateTime created;
    private LocalDateTime updated;

    private Boolean isActive;
}
```

### 4.3 API Response

#### Success Response

```json
{
  "status": "success",
  "message": "Password changed successfully",
  "data": null,
  "path": "/api/users/change-password"
}
```

#### Error Response

```json
{
  "status": "error",
  "message": "Current password is incorrect",
  "code": 400,
  "path": "/api/users/change-password"
}
```

---

## 5. Chi tiết luồng xử lý

### 5.1 Change Password Flow

1. **User gửi request** với current password, new password, confirm password
2. **Validate request** - kiểm tra các trường required, min length
3. **Extract User ID** từ X-User-Id header
4. **Tìm User** trong database theo ID
5. **Verify current password** - so sánh với passwordHash bằng BCrypt
6. **Validate confirm password** - phải khớp với new password
7. **Encode new password** bằng BCrypt
8. **Update user** với password mới
9. **Return success response**

---

## 6. Sequence Diagram

### Change Password - Complete Flow

```mermaid
sequenceDiagram
    autonumber
    participant User
    participant ProxyClient
    participant APIGateway
    participant UserController
    participant UserService as UserServiceImpl
    participant PasswordEncoder
    participant UserRepository
    participant Database

    rect rgb(240, 248, 255)
        Note over User,Database: CHANGE PASSWORD - POST /api/users/change-password

        User->>+ProxyClient: POST /api/users/change-password<br/>Authorization: Bearer {jwt_token}<br/>{<br/>  currentPassword: "oldPass123",<br/>  newPassword: "newPass456",<br/>  confirmPassword: "newPass456"<br/>}

        ProxyClient->>ProxyClient: Extract userId from JWT token

        ProxyClient->>+APIGateway: POST /api/users/change-password<br/>X-User-Id: {userId}<br/>Authorization: Bearer {jwt_token}<br/>{request body}

        APIGateway->>+UserController: changePassword(request, X-User-Id)

        rect rgb(255, 248, 220)
            Note over UserController: Validate X-User-Id Header

            alt X-User-Id header missing
                UserController-->>APIGateway: 400 Bad Request
                APIGateway-->>ProxyClient: "Authentication required - missing X-User-Id header"
                ProxyClient-->>User: ❌ Error: Missing authentication
            else X-User-Id present
                UserController->>UserController: userId = UUID.fromString(X-User-Id)
            end
        end

        UserController->>+UserService: changePassword(userId, request)

        rect rgb(220, 255, 220)
            Note over UserService,Database: 1. Find Active User

            UserService->>+UserRepository: findByIdAndIsActiveTrue(userId)
            UserRepository->>Database: SELECT * FROM users<br/>WHERE id = ? AND is_active = true
            Database-->>UserRepository: User or null
            UserRepository-->>-UserService: Optional<User>

            alt User not found
                UserService-->>UserController: ❌ throw NotFoundException<br/>"User not found"
                UserController-->>APIGateway: 404 Not Found
                APIGateway-->>ProxyClient: "User not found"
                ProxyClient-->>User: ❌ Error: User not found
            end
        end

        rect rgb(255, 240, 245)
            Note over UserService,PasswordEncoder: 2. Verify Current Password

            UserService->>+PasswordEncoder: matches(currentPassword, user.passwordHash)
            PasswordEncoder->>PasswordEncoder: BCrypt.checkpw(rawPassword, hashedPassword)
            PasswordEncoder-->>-UserService: boolean (true/false)

            alt Current password incorrect
                UserService-->>UserController: ❌ throw UnauthorizedException<br/>"Current password is incorrect"
                UserController-->>APIGateway: 400 Bad Request
                APIGateway-->>ProxyClient: "Current password is incorrect"
                ProxyClient-->>User: ❌ Error: Wrong current password
            end
        end

        rect rgb(255, 250, 240)
            Note over UserService: 3. Validate Password Confirmation

            UserService->>UserService: newPassword.equals(confirmPassword)?

            alt Passwords don't match
                UserService-->>UserController: ❌ throw BadRequestException<br/>"Password confirmation does not match"
                UserController-->>APIGateway: 400 Bad Request
                APIGateway-->>ProxyClient: "Password confirmation does not match"
                ProxyClient-->>User: ❌ Error: Passwords don't match
            end
        end

        rect rgb(220, 255, 220)
            Note over UserService,Database: 4. Update Password

            UserService->>+PasswordEncoder: encode(newPassword)
            PasswordEncoder->>PasswordEncoder: BCrypt.hashpw(newPassword, salt)
            PasswordEncoder-->>-UserService: hashedPassword

            UserService->>UserService: user.setPasswordHash(hashedPassword)<br/>user.setUpdated(now())

            UserService->>+UserRepository: save(user)
            UserRepository->>Database: UPDATE users<br/>SET password_hash = ?,<br/>    updated = ?<br/>WHERE id = ?
            Database-->>UserRepository: Updated
            UserRepository-->>-UserService: User saved

            UserService->>UserService: log.info("Password changed for user {}", userId)
        end

        UserService-->>-UserController: void (success)
        UserController-->>-APIGateway: GlobalResponse<Void><br/>"Password changed successfully"
        APIGateway-->>-ProxyClient: 200 OK
        ProxyClient-->>-User: ✅ Password changed successfully
    end
```

---

## 7. Error Handling

| Error Case                 | HTTP Status | Message                                            |
| -------------------------- | ----------- | -------------------------------------------------- |
| Missing X-User-Id header   | 400         | Authentication required - missing X-User-Id header |
| Invalid UUID format        | 400         | Invalid user ID format                             |
| User not found             | 404         | User not found                                     |
| User inactive              | 404         | User not found                                     |
| Current password incorrect | 401/400     | Current password is incorrect                      |
| Passwords don't match      | 400         | Password confirmation does not match               |
| New password too short     | 400         | Password must be at least 6 characters             |
| Current password empty     | 400         | Current password is required                       |
| New password empty         | 400         | New password is required                           |
| Confirm password empty     | 400         | Confirm password is required                       |

---

## 8. Business Rules

### 8.1 Validation Rules

| Field           | Rule                                |
| --------------- | ----------------------------------- |
| currentPassword | Required, không được empty          |
| newPassword     | Required, minimum 6 characters      |
| confirmPassword | Required, phải khớp với newPassword |

### 8.2 Security

1. **Password Hashing**: Sử dụng BCrypt với salt
2. **Verify before change**: Phải verify đúng current password trước khi đổi
3. **JWT Required**: User phải đăng nhập với JWT token hợp lệ
4. **X-User-Id Header**: Proxy-Client extract userId từ JWT và truyền qua header

### 8.3 Password Flow Comparison

```mermaid
flowchart TD
    subgraph "Change Password (Authenticated)"
        A1[User logged in] --> A2[Enter current + new password]
        A2 --> A3[Verify current password]
        A3 --> A4[Update password]
    end

    subgraph "Forgot Password (Not Authenticated)"
        B1[User not logged in] --> B2[Enter email]
        B2 --> B3[Send OTP via email]
        B3 --> B4[User enters OTP + new password]
        B4 --> B5[Verify OTP]
        B5 --> B6[Reset password]
    end
```

### 8.4 State Diagram

```mermaid
stateDiagram-v2
    [*] --> RequestReceived: POST /change-password

    RequestReceived --> ValidatingHeader: Check X-User-Id

    ValidatingHeader --> HeaderError: Missing/Invalid header
    ValidatingHeader --> FindingUser: Valid header

    HeaderError --> [*]: 400 Error

    FindingUser --> UserNotFound: User not found
    FindingUser --> VerifyingPassword: User found

    UserNotFound --> [*]: 404 Error

    VerifyingPassword --> WrongPassword: Password incorrect
    VerifyingPassword --> ValidatingConfirm: Password correct

    WrongPassword --> [*]: 401 Error

    ValidatingConfirm --> ConfirmMismatch: Don't match
    ValidatingConfirm --> UpdatingPassword: Match

    ConfirmMismatch --> [*]: 400 Error

    UpdatingPassword --> Success: Password updated
    Success --> [*]: 200 OK
```

---

## 9. Database Schema

### users Table (relevant columns)

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,  -- BCrypt hash

    created TIMESTAMP NOT NULL,
    updated TIMESTAMP NOT NULL,

    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_is_active ON users(is_active);
```

---

## Tóm tắt các thành phần

| Component         | Service      | Vai trò                        |
| ----------------- | ------------ | ------------------------------ |
| `UserController`  | user-service | REST API endpoint              |
| `UserServiceImpl` | user-service | Business logic implementation  |
| `UserRepository`  | user-service | Data access layer              |
| `PasswordEncoder` | user-service | BCrypt password hashing        |
| `User`            | user-service | User entity                    |
| `ProxyClient`     | proxy-client | JWT validation, extract userId |
