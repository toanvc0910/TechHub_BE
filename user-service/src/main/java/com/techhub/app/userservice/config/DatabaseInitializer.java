package com.techhub.app.userservice.config;

import com.techhub.app.userservice.entity.Permission;
import com.techhub.app.userservice.entity.Role;
import com.techhub.app.userservice.entity.RolePermission;
import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.entity.UserRole;
import com.techhub.app.userservice.enums.PermissionMethod;
import com.techhub.app.userservice.enums.UserStatus;
import com.techhub.app.userservice.repository.PermissionRepository;
import com.techhub.app.userservice.repository.RolePermissionRepository;
import com.techhub.app.userservice.repository.RoleRepository;
import com.techhub.app.userservice.repository.UserRepository;
import com.techhub.app.userservice.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Create Roles - Always ensure default roles exist
        Role learnerRole = roleRepository.findByName("LEARNER").orElseGet(() -> {
            Role role = new Role();
            role.setName("LEARNER");
            role.setDescription("Học viên");
            role.setIsActive(true);
            role.setCreated(LocalDateTime.now());
            role.setUpdated(LocalDateTime.now());
            return roleRepository.save(role);
        });

        Role instructorRole = roleRepository.findByName("INSTRUCTOR").orElseGet(() -> {
            Role role = new Role();
            role.setName("INSTRUCTOR");
            role.setDescription("Giảng viên");
            role.setIsActive(true);
            role.setCreated(LocalDateTime.now());
            role.setUpdated(LocalDateTime.now());
            return roleRepository.save(role);
        });

        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
            Role role = new Role();
            role.setName("ADMIN");
            role.setDescription("Quản trị viên");
            role.setIsActive(true);
            role.setCreated(LocalDateTime.now());
            role.setUpdated(LocalDateTime.now());
            return roleRepository.save(role);
        });

        System.out.println("=== ROLES INITIALIZED ===");
        System.out.println("LEARNER role ID: " + learnerRole.getId());
        System.out.println("INSTRUCTOR role ID: " + instructorRole.getId());
        System.out.println("ADMIN role ID: " + adminRole.getId());
        System.out.println("========================");

        // 2. Create Permissions chi tiết cho từng API endpoint
        List<Permission> permissions = null;
        if (permissionRepository.count() == 0) {
            // User Management Permissions
            Permission createUserPerm = createPermission("USER_CREATE", "Create user", "/api/users",
                    PermissionMethod.POST, "USERS");
            Permission getUsersPerm = createPermission("USER_READ_ALL", "Get all users", "/api/users",
                    PermissionMethod.GET, "USERS");
            Permission getUserByIdPerm = createPermission("USER_READ", "Get user by ID", "/api/users/{id}",
                    PermissionMethod.GET, "USERS");
            Permission getUserByEmailPerm = createPermission("USER_READ_EMAIL", "Get user by email",
                    "/api/users/email/{email}", PermissionMethod.GET, "USERS");
            Permission getUserByUsernamePerm = createPermission("USER_READ_USERNAME", "Get user by username",
                    "/api/users/username/{username}", PermissionMethod.GET, "USERS");
            Permission updateUserPerm = createPermission("USER_UPDATE", "Update user", "/api/users/{id}",
                    PermissionMethod.PUT, "USERS");
            Permission deleteUserPerm = createPermission("USER_DELETE", "Delete user", "/api/users/{id}",
                    PermissionMethod.DELETE, "USERS");
            Permission getUserProfilePerm = createPermission("USER_PROFILE", "Get user profile", "/api/users/profile",
                    PermissionMethod.GET, "USERS");

            // User Status Management Permissions
            Permission activateUserPerm = createPermission("USER_ACTIVATE", "Activate user", "/api/users/{id}/activate",
                    PermissionMethod.POST, "USERS");
            Permission deactivateUserPerm = createPermission("USER_DEACTIVATE", "Deactivate user",
                    "/api/users/{id}/deactivate", PermissionMethod.POST, "USERS");
            Permission changeUserStatusPerm = createPermission("USER_STATUS_CHANGE", "Change user status",
                    "/api/users/{id}/status/{status}", PermissionMethod.PUT, "USERS");

            // Password Management Permissions
            Permission changePasswordPerm = createPermission("USER_CHANGE_PASSWORD", "Change password",
                    "/api/users/{id}/change-password", PermissionMethod.POST, "USERS");

            // Blog Permissions (để demo phân quyền chi tiết)
            Permission createBlogPerm = createPermission("BLOG_CREATE", "Create blog", "/api/blogs",
                    PermissionMethod.POST, "BLOGS");
            Permission readBlogPerm = createPermission("BLOG_READ", "Read blogs", "/api/blogs", PermissionMethod.GET,
                    "BLOGS");
            Permission updateBlogPerm = createPermission("BLOG_UPDATE", "Update blog", "/api/blogs/{id}",
                    PermissionMethod.PUT, "BLOGS");
            Permission deleteBlogPerm = createPermission("BLOG_DELETE", "Delete blog", "/api/blogs/{id}",
                    PermissionMethod.DELETE, "BLOGS");

            // Course Permissions
            Permission createCoursePerm = createPermission("COURSE_CREATE", "Create course", "/api/courses",
                    PermissionMethod.POST, "COURSES");
            Permission readCoursePerm = createPermission("COURSE_READ", "Read courses", "/api/courses",
                    PermissionMethod.GET, "COURSES");
            Permission updateCoursePerm = createPermission("COURSE_UPDATE", "Update course", "/api/courses/{id}",
                    PermissionMethod.PUT, "COURSES");
            Permission deleteCoursePerm = createPermission("COURSE_DELETE", "Delete course", "/api/courses/{id}",
                    PermissionMethod.DELETE, "COURSES");

            permissions = Arrays.asList(
                    // User permissions
                    createUserPerm, getUsersPerm, getUserByIdPerm, getUserByEmailPerm, getUserByUsernamePerm,
                    updateUserPerm, deleteUserPerm, getUserProfilePerm, activateUserPerm, deactivateUserPerm,
                    changeUserStatusPerm, changePasswordPerm,
                    // Blog permissions
                    createBlogPerm, readBlogPerm, updateBlogPerm, deleteBlogPerm,
                    // Course permissions
                    createCoursePerm, readCoursePerm, updateCoursePerm, deleteCoursePerm);
            permissionRepository.saveAll(permissions);
        } else {
            permissions = permissionRepository.findAll();
        }

        // 3. Assign Permissions to Roles với phân quyền chi tiết
        if (rolePermissionRepository.count() == 0 && permissions != null) {
            // ADMIN có tất cả permissions
            for (Permission permission : permissions) {
                createRolePermission(adminRole.getId(), permission.getId());
            }

            // INSTRUCTOR có permissions hạn chế
            String[] instructorPermissions = {
                    "USER_READ_ALL", "USER_READ", "USER_PROFILE", "USER_READ_EMAIL", "USER_READ_USERNAME",
                    "BLOG_CREATE", "BLOG_READ", "BLOG_UPDATE", // Instructor có thể tạo và sửa blog của mình
                    "COURSE_CREATE", "COURSE_READ", "COURSE_UPDATE" // Instructor có thể tạo và quản lý khóa học
            };

            assignPermissionsByCode(instructorRole.getId(), permissions, instructorPermissions);

            // LEARNER chỉ có permissions cơ bản
            String[] learnerPermissions = {
                    "USER_PROFILE", "USER_CHANGE_PASSWORD", // Chỉ xem và đổi mật khẩu của chính mình
                    "BLOG_READ", // Chỉ đọc blog
                    "COURSE_READ" // Chỉ xem khóa học
            };

            assignPermissionsByCode(learnerRole.getId(), permissions, learnerPermissions);
        }

        // 4. Create Sample Users
        if (userRepository.count() == 0) {
            // Admin User
            User adminUser = createUser("admin@techhub.com", "admin", "TechHub Admin", "admin123");
            adminUser = userRepository.save(adminUser);
            createUserRole(adminUser.getId(), adminRole.getId());

            // Instructor User
            User instructorUser = createUser("instructor@techhub.com", "instructor", "TechHub Instructor",
                    "instructor123");
            instructorUser = userRepository.save(instructorUser);
            createUserRole(instructorUser.getId(), instructorRole.getId());

            // Learner User
            User learnerUser = createUser("learner@techhub.com", "learner", "TechHub Learner", "learner123");
            learnerUser = userRepository.save(learnerUser);
            createUserRole(learnerUser.getId(), learnerRole.getId());

            System.out.println("=== SAMPLE USERS CREATED ===");
            System.out.println("Admin: admin@techhub.com / admin123");
            System.out.println("Instructor: instructor@techhub.com / instructor123");
            System.out.println("Learner: learner@techhub.com / learner123");
            System.out.println("===============================");
        }
    }

    private Permission createPermission(String name, String description, String url, PermissionMethod method,
            String resource) {
        Permission permission = new Permission();
        permission.setName(name);
        permission.setDescription(description);
        permission.setUrl(url);
        permission.setMethod(method);
        permission.setResource(resource);
        permission.setIsActive(true);
        return permission;
    }

    private User createUser(String email, String username, String fullName, String password) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(UserStatus.ACTIVE);
        user.setIsActive(true);
        user.setCreated(LocalDateTime.now());
        user.setUpdated(LocalDateTime.now());
        return user;
    }

    private void createUserRole(UUID userId, UUID roleId) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRole.setIsActive(true);
        userRole.setAssignedAt(LocalDateTime.now());
        userRole.setCreated(LocalDateTime.now());
        userRole.setUpdated(LocalDateTime.now());
        userRoleRepository.save(userRole);
    }

    private void createRolePermission(UUID roleId, UUID permissionId) {
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRoleId(roleId);
        rolePermission.setPermissionId(permissionId);
        rolePermission.setIsActive(true);
        rolePermissionRepository.save(rolePermission);
    }

    private void assignPermissionsByCode(UUID roleId, List<Permission> permissions, String[] permissionCodes) {
        for (String code : permissionCodes) {
            Permission permission = permissions.stream().filter(p -> p.getName().equals(code)).findFirst().orElse(null);
            if (permission != null) {
                createRolePermission(roleId, permission.getId());
            }
        }
    }
}
