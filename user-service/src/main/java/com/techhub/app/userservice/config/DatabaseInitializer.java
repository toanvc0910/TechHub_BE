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
                // Set this to true to force re-initialize permissions (will delete all existing
                // permissions and role assignments)
                boolean FORCE_REINIT_PERMISSIONS = true;

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

                // Force re-initialize if flag is set
                if (FORCE_REINIT_PERMISSIONS && permissionRepository.count() > 0) {
                        System.out.println("🔄 FORCE_REINIT_PERMISSIONS = true");
                        System.out.println("🗑️ Deleting all existing role_permission and permission data...");
                        rolePermissionRepository.deleteAll();
                        permissionRepository.deleteAll();
                        System.out.println("✅ Old data deleted. Creating new permissions...");
                }

                if (permissionRepository.count() == 0) {
                        // ==================== USER MANAGEMENT ====================
                        Permission createUserPerm = createPermission("USER_CREATE", "Create user", "/api/users",
                                        PermissionMethod.POST, "USERS");
                        Permission getUsersPerm = createPermission("USER_READ_ALL", "Get all users", "/api/users",
                                        PermissionMethod.GET, "USERS");
                        Permission getUserByIdPerm = createPermission("USER_READ", "Get user by ID", "/api/users/{id}",
                                        PermissionMethod.GET, "USERS");
                        Permission getUserByEmailPerm = createPermission("USER_READ_EMAIL", "Get user by email",
                                        "/api/users/email/{email}", PermissionMethod.GET, "USERS");
                        Permission getUserByUsernamePerm = createPermission("USER_READ_USERNAME",
                                        "Get user by username",
                                        "/api/users/username/{username}", PermissionMethod.GET, "USERS");
                        Permission updateUserPerm = createPermission("USER_UPDATE", "Update user", "/api/users/{id}",
                                        PermissionMethod.PUT, "USERS");
                        Permission deleteUserPerm = createPermission("USER_DELETE", "Delete user", "/api/users/{id}",
                                        PermissionMethod.DELETE, "USERS");
                        Permission getUserProfilePerm = createPermission("USER_PROFILE", "Get user profile",
                                        "/api/users/profile",
                                        PermissionMethod.GET, "USERS");
                        Permission activateUserPerm = createPermission("USER_ACTIVATE", "Activate user",
                                        "/api/users/{id}/activate",
                                        PermissionMethod.POST, "USERS");
                        Permission deactivateUserPerm = createPermission("USER_DEACTIVATE", "Deactivate user",
                                        "/api/users/{id}/deactivate", PermissionMethod.POST, "USERS");
                        Permission changeUserStatusPerm = createPermission("USER_STATUS_CHANGE", "Change user status",
                                        "/api/users/{id}/status/{status}", PermissionMethod.PUT, "USERS");
                        Permission changePasswordPerm = createPermission("USER_CHANGE_PASSWORD", "Change password",
                                        "/api/users/change-password", PermissionMethod.POST, "USERS");

                        // ==================== ADMIN - PERMISSION MANAGEMENT ====================
                        Permission listPermissionsPerm = createPermission("ADMIN_PERMISSION_LIST",
                                        "List all permissions",
                                        "/api/admin/permissions", PermissionMethod.GET, "ADMIN");
                        Permission getPermissionPerm = createPermission("ADMIN_PERMISSION_READ", "Get permission by ID",
                                        "/api/admin/permissions/{id}", PermissionMethod.GET, "ADMIN");
                        Permission createPermissionPerm = createPermission("ADMIN_PERMISSION_CREATE",
                                        "Create permission",
                                        "/api/admin/permissions", PermissionMethod.POST, "ADMIN");
                        Permission updatePermissionPerm = createPermission("ADMIN_PERMISSION_UPDATE",
                                        "Update permission",
                                        "/api/admin/permissions/{id}", PermissionMethod.PUT, "ADMIN");
                        Permission deletePermissionPerm = createPermission("ADMIN_PERMISSION_DELETE",
                                        "Delete permission",
                                        "/api/admin/permissions/{id}", PermissionMethod.DELETE, "ADMIN");

                        // ==================== ADMIN - ROLE MANAGEMENT ====================
                        Permission listRolesPerm = createPermission("ADMIN_ROLE_LIST", "List all roles",
                                        "/api/admin/roles",
                                        PermissionMethod.GET, "ADMIN");
                        Permission getRolePerm = createPermission("ADMIN_ROLE_READ", "Get role by ID",
                                        "/api/admin/roles/{id}",
                                        PermissionMethod.GET, "ADMIN");
                        Permission createRolePerm = createPermission("ADMIN_ROLE_CREATE", "Create role",
                                        "/api/admin/roles",
                                        PermissionMethod.POST, "ADMIN");
                        Permission updateRolePerm = createPermission("ADMIN_ROLE_UPDATE", "Update role",
                                        "/api/admin/roles/{id}",
                                        PermissionMethod.PUT, "ADMIN");
                        Permission deleteRolePerm = createPermission("ADMIN_ROLE_DELETE", "Delete role",
                                        "/api/admin/roles/{id}",
                                        PermissionMethod.DELETE, "ADMIN");
                        Permission assignPermToRolePerm = createPermission("ADMIN_ROLE_ASSIGN_PERMISSION",
                                        "Assign permissions to role", "/api/admin/roles/{id}/permissions",
                                        PermissionMethod.POST, "ADMIN");
                        Permission removePermFromRolePerm = createPermission("ADMIN_ROLE_REMOVE_PERMISSION",
                                        "Remove permission from role",
                                        "/api/admin/roles/{roleId}/permissions/{permissionId}",
                                        PermissionMethod.DELETE, "ADMIN");

                        // ==================== ADMIN - USER ROLE MANAGEMENT ====================
                        Permission getUserRolesPerm = createPermission("ADMIN_USER_ROLES", "Get user roles",
                                        "/api/admin/users/{id}/roles", PermissionMethod.GET, "ADMIN");
                        Permission assignRoleToUserPerm = createPermission("ADMIN_USER_ASSIGN_ROLE",
                                        "Assign roles to user",
                                        "/api/admin/users/{id}/roles", PermissionMethod.POST, "ADMIN");
                        Permission removeRoleFromUserPerm = createPermission("ADMIN_USER_REMOVE_ROLE",
                                        "Remove role from user",
                                        "/api/admin/users/{userId}/roles/{roleId}", PermissionMethod.DELETE, "ADMIN");

                        // ==================== BLOG MANAGEMENT ====================
                        Permission createBlogPerm = createPermission("BLOG_CREATE", "Create blog", "/api/blogs",
                                        PermissionMethod.POST, "BLOGS");
                        Permission readBlogsPerm = createPermission("BLOG_READ_ALL", "Read all blogs", "/api/blogs",
                                        PermissionMethod.GET, "BLOGS");
                        Permission readBlogPerm = createPermission("BLOG_READ", "Read blog by ID", "/api/blogs/{id}",
                                        PermissionMethod.GET, "BLOGS");
                        Permission updateBlogPerm = createPermission("BLOG_UPDATE", "Update blog", "/api/blogs/{id}",
                                        PermissionMethod.PUT, "BLOGS");
                        Permission deleteBlogPerm = createPermission("BLOG_DELETE", "Delete blog", "/api/blogs/{id}",
                                        PermissionMethod.DELETE, "BLOGS");
                        Permission getBlogTagsPerm = createPermission("BLOG_TAGS", "Get blog tags", "/api/blogs/tags",
                                        PermissionMethod.GET, "BLOGS");

                        // ==================== BLOG COMMENTS ====================
                        Permission getBlogCommentsPerm = createPermission("BLOG_COMMENT_READ", "Get blog comments",
                                        "/api/blogs/{id}/comments", PermissionMethod.GET, "BLOGS");
                        Permission addBlogCommentPerm = createPermission("BLOG_COMMENT_CREATE", "Add blog comment",
                                        "/api/blogs/{id}/comments", PermissionMethod.POST, "BLOGS");
                        Permission deleteBlogCommentPerm = createPermission("BLOG_COMMENT_DELETE",
                                        "Delete blog comment",
                                        "/api/blogs/{blogId}/comments/{commentId}", PermissionMethod.DELETE, "BLOGS");

                        // ==================== COURSE MANAGEMENT ====================
                        Permission createCoursePerm = createPermission("COURSE_CREATE", "Create course", "/api/courses",
                                        PermissionMethod.POST, "COURSES");
                        Permission readCoursesPerm = createPermission("COURSE_READ_ALL", "Read all courses",
                                        "/api/courses",
                                        PermissionMethod.GET, "COURSES");
                        Permission readCoursePerm = createPermission("COURSE_READ", "Read course by ID",
                                        "/api/courses/{id}",
                                        PermissionMethod.GET, "COURSES");
                        Permission updateCoursePerm = createPermission("COURSE_UPDATE", "Update course",
                                        "/api/courses/{id}",
                                        PermissionMethod.PUT, "COURSES");
                        Permission deleteCoursePerm = createPermission("COURSE_DELETE", "Delete course",
                                        "/api/courses/{id}",
                                        PermissionMethod.DELETE, "COURSES");
                        Permission enrollCoursePerm = createPermission("COURSE_ENROLL", "Enroll in course",
                                        "/api/courses/{id}/enroll", PermissionMethod.POST, "COURSES");

                        // ==================== COURSE CHAPTERS ====================
                        Permission getCourseChaptersPerm = createPermission("COURSE_CHAPTER_READ",
                                        "Get course chapters",
                                        "/api/courses/{id}/chapters", PermissionMethod.GET, "COURSES");
                        Permission createChapterPerm = createPermission("COURSE_CHAPTER_CREATE", "Create chapter",
                                        "/api/courses/{id}/chapters", PermissionMethod.POST, "COURSES");
                        Permission updateChapterPerm = createPermission("COURSE_CHAPTER_UPDATE", "Update chapter",
                                        "/api/courses/{courseId}/chapters/{chapterId}", PermissionMethod.PUT,
                                        "COURSES");
                        Permission deleteChapterPerm = createPermission("COURSE_CHAPTER_DELETE", "Delete chapter",
                                        "/api/courses/{courseId}/chapters/{chapterId}", PermissionMethod.DELETE,
                                        "COURSES");

                        // ==================== COURSE LESSONS ====================
                        Permission getLessonPerm = createPermission("COURSE_LESSON_READ", "Get lesson detail",
                                        "/api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}/detail",
                                        PermissionMethod.GET,
                                        "COURSES");
                        Permission createLessonPerm = createPermission("COURSE_LESSON_CREATE", "Create lesson",
                                        "/api/courses/{courseId}/chapters/{chapterId}/lessons", PermissionMethod.POST,
                                        "COURSES");
                        Permission updateLessonPerm = createPermission("COURSE_LESSON_UPDATE", "Update lesson",
                                        "/api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}",
                                        PermissionMethod.PUT, "COURSES");
                        Permission deleteLessonPerm = createPermission("COURSE_LESSON_DELETE", "Delete lesson",
                                        "/api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}",
                                        PermissionMethod.DELETE,
                                        "COURSES");

                        // ==================== COURSE LESSON ASSETS ====================
                        Permission createLessonAssetPerm = createPermission("COURSE_LESSON_ASSET_CREATE",
                                        "Create lesson asset",
                                        "/api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}/assets",
                                        PermissionMethod.POST,
                                        "COURSES");
                        Permission updateLessonAssetPerm = createPermission("COURSE_LESSON_ASSET_UPDATE",
                                        "Update lesson asset",
                                        "/api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}/assets/{assetId}",
                                        PermissionMethod.PUT, "COURSES");
                        Permission deleteLessonAssetPerm = createPermission("COURSE_LESSON_ASSET_DELETE",
                                        "Delete lesson asset",
                                        "/api/courses/{courseId}/chapters/{chapterId}/lessons/{lessonId}/assets/{assetId}",
                                        PermissionMethod.DELETE, "COURSES");

                        // ==================== COURSE PROGRESS ====================
                        Permission getCourseProgressPerm = createPermission("COURSE_PROGRESS_READ",
                                        "Get course progress",
                                        "/api/courses/{id}/progress", PermissionMethod.GET, "COURSES");
                        Permission updateLessonProgressPerm = createPermission("COURSE_LESSON_PROGRESS_UPDATE",
                                        "Update lesson progress", "/api/courses/{courseId}/lessons/{lessonId}/progress",
                                        PermissionMethod.PUT, "COURSES");
                        Permission markLessonCompletePerm = createPermission("COURSE_LESSON_COMPLETE",
                                        "Mark lesson complete",
                                        "/api/courses/{courseId}/lessons/{lessonId}/progress/complete",
                                        PermissionMethod.POST, "COURSES");

                        // ==================== COURSE RATINGS ====================
                        Permission getCourseRatingPerm = createPermission("COURSE_RATING_READ", "Get course rating",
                                        "/api/courses/{id}/ratings", PermissionMethod.GET, "COURSES");
                        Permission submitCourseRatingPerm = createPermission("COURSE_RATING_CREATE",
                                        "Submit course rating",
                                        "/api/courses/{id}/ratings", PermissionMethod.POST, "COURSES");

                        // ==================== COURSE COMMENTS ====================
                        Permission getCourseCommentsPerm = createPermission("COURSE_COMMENT_READ",
                                        "Get course comments",
                                        "/api/courses/{id}/comments", PermissionMethod.GET, "COURSES");
                        Permission addCourseCommentPerm = createPermission("COURSE_COMMENT_CREATE",
                                        "Add course comment",
                                        "/api/courses/{id}/comments", PermissionMethod.POST, "COURSES");
                        Permission getLessonCommentsPerm = createPermission("COURSE_LESSON_COMMENT_READ",
                                        "Get lesson comments",
                                        "/api/courses/{courseId}/lessons/{lessonId}/comments", PermissionMethod.GET,
                                        "COURSES");
                        Permission addLessonCommentPerm = createPermission("COURSE_LESSON_COMMENT_CREATE",
                                        "Add lesson comment",
                                        "/api/courses/{courseId}/lessons/{lessonId}/comments", PermissionMethod.POST,
                                        "COURSES");
                        Permission getWorkspaceCommentsPerm = createPermission("COURSE_WORKSPACE_COMMENT_READ",
                                        "Get workspace comments",
                                        "/api/courses/{courseId}/lessons/{lessonId}/workspace/comments",
                                        PermissionMethod.GET, "COURSES");
                        Permission addWorkspaceCommentPerm = createPermission("COURSE_WORKSPACE_COMMENT_CREATE",
                                        "Add workspace comment",
                                        "/api/courses/{courseId}/lessons/{lessonId}/workspace/comments",
                                        PermissionMethod.POST, "COURSES");
                        Permission deleteCommentPerm = createPermission("COURSE_COMMENT_DELETE", "Delete comment",
                                        "/api/courses/{courseId}/comments/{commentId}", PermissionMethod.DELETE,
                                        "COURSES");

                        // ==================== COURSE EXERCISES ====================
                        Permission getExercisePerm = createPermission("COURSE_EXERCISE_READ", "Get exercise",
                                        "/api/courses/{courseId}/lessons/{lessonId}/exercise", PermissionMethod.GET,
                                        "COURSES");
                        Permission upsertExercisePerm = createPermission("COURSE_EXERCISE_UPSERT",
                                        "Create/Update exercise",
                                        "/api/courses/{courseId}/lessons/{lessonId}/exercise", PermissionMethod.PUT,
                                        "COURSES");
                        Permission submitExercisePerm = createPermission("COURSE_EXERCISE_SUBMIT", "Submit exercise",
                                        "/api/courses/{courseId}/lessons/{lessonId}/exercise/submissions",
                                        PermissionMethod.POST,
                                        "COURSES");

                        // ==================== COURSE WORKSPACE ====================
                        Permission getWorkspacePerm = createPermission("COURSE_WORKSPACE_READ", "Get workspace",
                                        "/api/courses/{courseId}/lessons/{lessonId}/workspace", PermissionMethod.GET,
                                        "COURSES");
                        Permission saveWorkspacePerm = createPermission("COURSE_WORKSPACE_SAVE", "Save workspace",
                                        "/api/courses/{courseId}/lessons/{lessonId}/workspace", PermissionMethod.PUT,
                                        "COURSES");

                        // ==================== COURSE SKILLS & TAGS ====================
                        Permission createSkillPerm = createPermission("COURSE_SKILL_CREATE", "Create skill",
                                        "/api/courses/skills",
                                        PermissionMethod.POST, "COURSES");
                        Permission getSkillPerm = createPermission("COURSE_SKILL_READ", "Get skill",
                                        "/api/courses/skills/{id}",
                                        PermissionMethod.GET, "COURSES");
                        Permission getAllSkillsPerm = createPermission("COURSE_SKILL_READ_ALL", "Get all skills",
                                        "/api/courses/skills", PermissionMethod.GET, "COURSES");
                        Permission updateSkillPerm = createPermission("COURSE_SKILL_UPDATE", "Update skill",
                                        "/api/courses/skills/{id}", PermissionMethod.PUT, "COURSES");
                        Permission deleteSkillPerm = createPermission("COURSE_SKILL_DELETE", "Delete skill",
                                        "/api/courses/skills/{id}", PermissionMethod.DELETE, "COURSES");
                        Permission createTagPerm = createPermission("COURSE_TAG_CREATE", "Create tag",
                                        "/api/courses/tags",
                                        PermissionMethod.POST, "COURSES");
                        Permission getTagPerm = createPermission("COURSE_TAG_READ", "Get tag", "/api/courses/tags/{id}",
                                        PermissionMethod.GET, "COURSES");
                        Permission getAllTagsPerm = createPermission("COURSE_TAG_READ_ALL", "Get all tags",
                                        "/api/courses/tags",
                                        PermissionMethod.GET, "COURSES");
                        Permission updateTagPerm = createPermission("COURSE_TAG_UPDATE", "Update tag",
                                        "/api/courses/tags/{id}",
                                        PermissionMethod.PUT, "COURSES");
                        Permission deleteTagPerm = createPermission("COURSE_TAG_DELETE", "Delete tag",
                                        "/api/courses/tags/{id}",
                                        PermissionMethod.DELETE, "COURSES");

                        // ==================== ENROLLMENT MANAGEMENT ====================
                        Permission createEnrollmentPerm = createPermission("ENROLLMENT_CREATE", "Create enrollment",
                                        "/api/enrollments", PermissionMethod.POST, "ENROLLMENTS");
                        Permission getEnrollmentPerm = createPermission("ENROLLMENT_READ", "Get enrollment by ID",
                                        "/api/enrollments/{enrollmentId}", PermissionMethod.GET, "ENROLLMENTS");
                        Permission getMyEnrollmentsPerm = createPermission("ENROLLMENT_MY_ENROLLMENTS",
                                        "Get my enrollments",
                                        "/api/enrollments/my-enrollments", PermissionMethod.GET, "ENROLLMENTS");

                        // ==================== LEARNING PATH MANAGEMENT ====================
                        Permission createLearningPathPerm = createPermission("LEARNING_PATH_CREATE",
                                        "Create learning path",
                                        "/api/learning-paths", PermissionMethod.POST, "LEARNING_PATHS");
                        Permission readLearningPathsPerm = createPermission("LEARNING_PATH_READ_ALL",
                                        "Read all learning paths",
                                        "/api/learning-paths", PermissionMethod.GET, "LEARNING_PATHS");
                        Permission readLearningPathPerm = createPermission("LEARNING_PATH_READ",
                                        "Read learning path by ID",
                                        "/api/learning-paths/{id}", PermissionMethod.GET, "LEARNING_PATHS");
                        Permission updateLearningPathPerm = createPermission("LEARNING_PATH_UPDATE",
                                        "Update learning path",
                                        "/api/learning-paths/{id}", PermissionMethod.PUT, "LEARNING_PATHS");
                        Permission deleteLearningPathPerm = createPermission("LEARNING_PATH_DELETE",
                                        "Delete learning path",
                                        "/api/learning-paths/{id}", PermissionMethod.DELETE, "LEARNING_PATHS");
                        Permission searchLearningPathsPerm = createPermission("LEARNING_PATH_SEARCH",
                                        "Search learning paths",
                                        "/api/learning-paths/search", PermissionMethod.GET, "LEARNING_PATHS");
                        Permission getLearningPathsByCreatorPerm = createPermission("LEARNING_PATH_BY_CREATOR",
                                        "Get learning paths by creator", "/api/learning-paths/creator/{userId}",
                                        PermissionMethod.GET,
                                        "LEARNING_PATHS");
                        Permission getLearningPathsByCoursePerm = createPermission("LEARNING_PATH_BY_COURSE",
                                        "Get learning paths by course", "/api/learning-paths/by-course/{courseId}",
                                        PermissionMethod.GET,
                                        "LEARNING_PATHS");

                        // ==================== LEARNING PATH COURSES ====================
                        Permission addCoursesToPathPerm = createPermission("LEARNING_PATH_ADD_COURSES",
                                        "Add courses to path",
                                        "/api/learning-paths/{id}/courses", PermissionMethod.POST, "LEARNING_PATHS");
                        Permission removeCourseFromPathPerm = createPermission("LEARNING_PATH_REMOVE_COURSE",
                                        "Remove course from path", "/api/learning-paths/{pathId}/courses/{courseId}",
                                        PermissionMethod.DELETE, "LEARNING_PATHS");
                        Permission reorderCoursesPerm = createPermission("LEARNING_PATH_REORDER_COURSES",
                                        "Reorder courses",
                                        "/api/learning-paths/{id}/courses/reorder", PermissionMethod.PUT,
                                        "LEARNING_PATHS");

                        // ==================== LEARNING PATH PROGRESS ====================
                        Permission createOrUpdateProgressPerm = createPermission("LEARNING_PATH_PROGRESS_UPSERT",
                                        "Create/Update progress", "/api/learning-paths/progress", PermissionMethod.POST,
                                        "LEARNING_PATHS");
                        Permission getProgressByUserAndPathPerm = createPermission("LEARNING_PATH_PROGRESS_READ",
                                        "Get progress by user and path",
                                        "/api/learning-paths/progress/user/{userId}/path/{pathId}",
                                        PermissionMethod.GET, "LEARNING_PATHS");
                        Permission getProgressByUserPerm = createPermission("LEARNING_PATH_PROGRESS_BY_USER",
                                        "Get progress by user", "/api/learning-paths/progress/user/{userId}",
                                        PermissionMethod.GET,
                                        "LEARNING_PATHS");
                        Permission getProgressByPathPerm = createPermission("LEARNING_PATH_PROGRESS_BY_PATH",
                                        "Get progress by path", "/api/learning-paths/progress/path/{pathId}",
                                        PermissionMethod.GET,
                                        "LEARNING_PATHS");
                        Permission deleteProgressPerm = createPermission("LEARNING_PATH_PROGRESS_DELETE",
                                        "Delete progress",
                                        "/api/learning-paths/progress/user/{userId}/path/{pathId}",
                                        PermissionMethod.DELETE,
                                        "LEARNING_PATHS");
                        Permission getPathStatisticsPerm = createPermission("LEARNING_PATH_STATISTICS",
                                        "Get path statistics",
                                        "/api/learning-paths/{id}/statistics", PermissionMethod.GET, "LEARNING_PATHS");

                        // ==================== FILE MANAGEMENT ====================
                        Permission uploadFilePerm = createPermission("FILE_UPLOAD", "Upload file", "/api/files/upload",
                                        PermissionMethod.POST, "FILES");
                        Permission uploadMultipleFilesPerm = createPermission("FILE_UPLOAD_MULTIPLE",
                                        "Upload multiple files",
                                        "/api/files/upload/multiple", PermissionMethod.POST, "FILES");
                        Permission getFilePerm = createPermission("FILE_READ", "Get file", "/api/files/{id}",
                                        PermissionMethod.GET,
                                        "FILES");
                        Permission listFilesPerm = createPermission("FILE_READ_ALL", "List files", "/api/files",
                                        PermissionMethod.GET, "FILES");
                        Permission getFilesByFolderPerm = createPermission("FILE_READ_BY_FOLDER", "Get files by folder",
                                        "/api/files/folder/{id}", PermissionMethod.GET, "FILES");
                        Permission deleteFilePerm = createPermission("FILE_DELETE", "Delete file", "/api/files/{id}",
                                        PermissionMethod.DELETE, "FILES");
                        Permission getFileStatisticsPerm = createPermission("FILE_STATISTICS", "Get file statistics",
                                        "/api/files/statistics", PermissionMethod.GET, "FILES");

                        // ==================== FOLDER MANAGEMENT ====================
                        Permission createFolderPerm = createPermission("FOLDER_CREATE", "Create folder",
                                        "/api/files/folders",
                                        PermissionMethod.POST, "FILES");
                        Permission getFoldersByUserPerm = createPermission("FOLDER_READ_BY_USER", "Get folders by user",
                                        "/api/files/folders/user/{id}", PermissionMethod.GET, "FILES");
                        Permission getFolderPerm = createPermission("FOLDER_READ", "Get folder",
                                        "/api/files/folders/{id}",
                                        PermissionMethod.GET, "FILES");
                        Permission getFolderTreePerm = createPermission("FOLDER_READ_TREE", "Get folder tree",
                                        "/api/files/folders/{id}/tree", PermissionMethod.GET, "FILES");
                        Permission updateFolderPerm = createPermission("FOLDER_UPDATE", "Update folder",
                                        "/api/files/folders/{id}",
                                        PermissionMethod.PUT, "FILES");
                        Permission deleteFolderPerm = createPermission("FOLDER_DELETE", "Delete folder",
                                        "/api/files/folders/{id}",
                                        PermissionMethod.DELETE, "FILES");

                        // ==================== FILE USAGE TRACKING ====================
                        Permission trackFileUsagePerm = createPermission("FILE_USAGE_TRACK", "Track file usage",
                                        "/api/files/usage/track", PermissionMethod.POST, "FILES");
                        Permission removeFileUsagePerm = createPermission("FILE_USAGE_REMOVE", "Remove file usage",
                                        "/api/files/usage/remove", PermissionMethod.DELETE, "FILES");
                        Permission removeAllFileUsagePerm = createPermission("FILE_USAGE_REMOVE_ALL",
                                        "Remove all file usage",
                                        "/api/files/usage/remove-all", PermissionMethod.DELETE, "FILES");
                        Permission listFileUsagesPerm = createPermission("FILE_USAGE_READ", "List file usages",
                                        "/api/files/usage/file/{id}", PermissionMethod.GET, "FILES");

                        // ==================== AI SERVICES ====================
                        Permission generateExercisesPerm = createPermission("AI_GENERATE_EXERCISES",
                                        "Generate exercises",
                                        "/api/ai/exercises/generate", PermissionMethod.POST, "AI");
                        Permission generateLearningPathPerm = createPermission("AI_GENERATE_LEARNING_PATH",
                                        "Generate learning path", "/api/ai/learning-paths/generate",
                                        PermissionMethod.POST, "AI");
                        Permission recommendRealtimePerm = createPermission("AI_RECOMMEND_REALTIME",
                                        "Recommend realtime",
                                        "/api/ai/recommendations/realtime", PermissionMethod.POST, "AI");
                        Permission recommendScheduledPerm = createPermission("AI_RECOMMEND_SCHEDULED",
                                        "Recommend scheduled",
                                        "/api/ai/recommendations/scheduled", PermissionMethod.POST, "AI");
                        Permission aiChatPerm = createPermission("AI_CHAT", "AI Chat", "/api/ai/chat/messages",
                                        PermissionMethod.POST, "AI");
                        Permission reindexCoursesPerm = createPermission("AI_REINDEX_COURSES", "Reindex courses",
                                        "/api/ai/admin/reindex-courses", PermissionMethod.POST, "AI");
                        Permission reindexLessonsPerm = createPermission("AI_REINDEX_LESSONS", "Reindex lessons",
                                        "/api/ai/admin/reindex-lessons", PermissionMethod.POST, "AI");
                        Permission reindexAllPerm = createPermission("AI_REINDEX_ALL", "Reindex all",
                                        "/api/ai/admin/reindex-all", PermissionMethod.POST, "AI");
                        Permission qdrantStatsPerm = createPermission("AI_QDRANT_STATS", "Qdrant stats",
                                        "/api/ai/admin/qdrant-stats", PermissionMethod.POST, "AI");

                        // ==================== PAYMENT SERVICES ====================
                        Permission createVnPayPaymentPerm = createPermission("PAYMENT_VNPAY_CREATE",
                                        "Create VNPay payment",
                                        "/api/v1/payment/vn-pay", PermissionMethod.GET, "PAYMENT");
                        Permission vnPayCallbackPerm = createPermission("PAYMENT_VNPAY_CALLBACK",
                                        "VNPay payment callback",
                                        "/api/v1/payment/vn-pay-callback", PermissionMethod.GET, "PAYMENT");
                        Permission createPayPalPaymentPerm = createPermission("PAYMENT_PAYPAL_CREATE",
                                        "Create PayPal payment",
                                        "/api/v1/payment/paypal/create", PermissionMethod.POST, "PAYMENT");
                        Permission payPalSuccessPerm = createPermission("PAYMENT_PAYPAL_SUCCESS",
                                        "PayPal payment success",
                                        "/api/v1/payment/paypal/success", PermissionMethod.GET, "PAYMENT");
                        Permission payPalCancelPerm = createPermission("PAYMENT_PAYPAL_CANCEL",
                                        "PayPal payment cancel",
                                        "/api/v1/payment/paypal/cancel", PermissionMethod.GET, "PAYMENT");

                        // ==================== TRANSACTION MANAGEMENT ====================
                        Permission getUserTransactionsPerm = createPermission("TRANSACTION_READ_BY_USER",
                                        "Get user transactions",
                                        "/api/v1/transactions/user/{userId}", PermissionMethod.GET, "PAYMENT");
                        Permission getTransactionByIdPerm = createPermission("TRANSACTION_READ",
                                        "Get transaction by ID",
                                        "/api/v1/transactions/{transactionId}", PermissionMethod.GET, "PAYMENT");
                        Permission getTransactionsByStatusPerm = createPermission("TRANSACTION_READ_BY_STATUS",
                                        "Get transactions by status",
                                        "/api/v1/transactions/status/{status}", PermissionMethod.GET, "PAYMENT");
                        Permission getTransactionPaymentsPerm = createPermission("TRANSACTION_PAYMENTS_READ",
                                        "Get transaction payments",
                                        "/api/v1/transactions/{transactionId}/payments", PermissionMethod.GET,
                                        "PAYMENT");

                        permissions = Arrays.asList(
                                        // User Management
                                        createUserPerm, getUsersPerm, getUserByIdPerm, getUserByEmailPerm,
                                        getUserByUsernamePerm,
                                        updateUserPerm, deleteUserPerm, getUserProfilePerm, activateUserPerm,
                                        deactivateUserPerm,
                                        changeUserStatusPerm, changePasswordPerm,
                                        // Admin - Permission Management
                                        listPermissionsPerm, getPermissionPerm, createPermissionPerm,
                                        updatePermissionPerm,
                                        deletePermissionPerm,
                                        // Admin - Role Management
                                        listRolesPerm, getRolePerm, createRolePerm, updateRolePerm, deleteRolePerm,
                                        assignPermToRolePerm,
                                        removePermFromRolePerm,
                                        // Admin - User Role Management
                                        getUserRolesPerm, assignRoleToUserPerm, removeRoleFromUserPerm,
                                        // Blog Management
                                        createBlogPerm, readBlogsPerm, readBlogPerm, updateBlogPerm, deleteBlogPerm,
                                        getBlogTagsPerm,
                                        getBlogCommentsPerm, addBlogCommentPerm, deleteBlogCommentPerm,
                                        // Course Management
                                        createCoursePerm, readCoursesPerm, readCoursePerm, updateCoursePerm,
                                        deleteCoursePerm,
                                        enrollCoursePerm,
                                        // Course Chapters
                                        getCourseChaptersPerm, createChapterPerm, updateChapterPerm, deleteChapterPerm,
                                        // Course Lessons
                                        getLessonPerm, createLessonPerm, updateLessonPerm, deleteLessonPerm,
                                        // Course Lesson Assets
                                        createLessonAssetPerm, updateLessonAssetPerm, deleteLessonAssetPerm,
                                        // Course Progress
                                        getCourseProgressPerm, updateLessonProgressPerm, markLessonCompletePerm,
                                        // Course Ratings
                                        getCourseRatingPerm, submitCourseRatingPerm,
                                        // Course Comments
                                        getCourseCommentsPerm, addCourseCommentPerm, getLessonCommentsPerm,
                                        addLessonCommentPerm,
                                        getWorkspaceCommentsPerm, addWorkspaceCommentPerm, deleteCommentPerm,
                                        // Course Exercises
                                        getExercisePerm, upsertExercisePerm, submitExercisePerm,
                                        // Course Workspace
                                        getWorkspacePerm, saveWorkspacePerm,
                                        // Course Skills & Tags
                                        createSkillPerm, getSkillPerm, getAllSkillsPerm, updateSkillPerm,
                                        deleteSkillPerm, createTagPerm,
                                        getTagPerm, getAllTagsPerm, updateTagPerm, deleteTagPerm,
                                        // Enrollment Management
                                        createEnrollmentPerm, getEnrollmentPerm, getMyEnrollmentsPerm,
                                        // Learning Path Management
                                        createLearningPathPerm, readLearningPathsPerm, readLearningPathPerm,
                                        updateLearningPathPerm,
                                        deleteLearningPathPerm, searchLearningPathsPerm, getLearningPathsByCreatorPerm,
                                        getLearningPathsByCoursePerm,
                                        // Learning Path Courses
                                        addCoursesToPathPerm, removeCourseFromPathPerm, reorderCoursesPerm,
                                        // Learning Path Progress
                                        createOrUpdateProgressPerm, getProgressByUserAndPathPerm, getProgressByUserPerm,
                                        getProgressByPathPerm, deleteProgressPerm, getPathStatisticsPerm,
                                        // File Management
                                        uploadFilePerm, uploadMultipleFilesPerm, getFilePerm, listFilesPerm,
                                        getFilesByFolderPerm,
                                        deleteFilePerm, getFileStatisticsPerm,
                                        // Folder Management
                                        createFolderPerm, getFoldersByUserPerm, getFolderPerm, getFolderTreePerm,
                                        updateFolderPerm,
                                        deleteFolderPerm,
                                        // File Usage Tracking
                                        trackFileUsagePerm, removeFileUsagePerm, removeAllFileUsagePerm,
                                        listFileUsagesPerm,
                                        // AI Services
                                        generateExercisesPerm, generateLearningPathPerm, recommendRealtimePerm,
                                        recommendScheduledPerm,
                                        aiChatPerm, reindexCoursesPerm, reindexLessonsPerm, reindexAllPerm,
                                        qdrantStatsPerm,
                                        // Payment Services
                                        createVnPayPaymentPerm, vnPayCallbackPerm, createPayPalPaymentPerm,
                                        payPalSuccessPerm, payPalCancelPerm,
                                        // Transaction Management
                                        getUserTransactionsPerm, getTransactionByIdPerm, getTransactionsByStatusPerm,
                                        getTransactionPaymentsPerm);

                        permissionRepository.saveAll(permissions);
                        System.out.println("=== " + permissions.size() + " PERMISSIONS CREATED ===");
                } else {
                        permissions = permissionRepository.findAll();
                }

                // 3. Assign Permissions to Roles với phân quyền chi tiết
                if (rolePermissionRepository.count() == 0 && permissions != null) {
                        // ===== ADMIN - Có tất cả permissions =====
                        for (Permission permission : permissions) {
                                createRolePermission(adminRole.getId(), permission.getId());
                        }
                        System.out.println("✅ ADMIN role assigned " + permissions.size() + " permissions");

                        // ===== INSTRUCTOR - Có permissions quản lý content =====
                        String[] instructorPermissions = {
                                        // User - Read only
                                        "USER_READ_ALL", "USER_READ", "USER_PROFILE", "USER_READ_EMAIL",
                                        "USER_READ_USERNAME",
                                        "USER_CHANGE_PASSWORD",
                                        // Blog - Full CRUD
                                        "BLOG_CREATE", "BLOG_READ_ALL", "BLOG_READ", "BLOG_UPDATE", "BLOG_DELETE",
                                        "BLOG_TAGS",
                                        "BLOG_COMMENT_READ", "BLOG_COMMENT_CREATE", "BLOG_COMMENT_DELETE",
                                        // Course - Full CRUD
                                        "COURSE_CREATE", "COURSE_READ_ALL", "COURSE_READ", "COURSE_UPDATE",
                                        "COURSE_DELETE",
                                        "COURSE_CHAPTER_READ", "COURSE_CHAPTER_CREATE", "COURSE_CHAPTER_UPDATE",
                                        "COURSE_CHAPTER_DELETE",
                                        "COURSE_LESSON_READ", "COURSE_LESSON_CREATE", "COURSE_LESSON_UPDATE",
                                        "COURSE_LESSON_DELETE",
                                        "COURSE_LESSON_ASSET_CREATE", "COURSE_LESSON_ASSET_UPDATE",
                                        "COURSE_LESSON_ASSET_DELETE",
                                        "COURSE_PROGRESS_READ", "COURSE_RATING_READ", "COURSE_RATING_CREATE",
                                        "COURSE_COMMENT_READ", "COURSE_COMMENT_CREATE", "COURSE_COMMENT_DELETE",
                                        "COURSE_LESSON_COMMENT_READ", "COURSE_LESSON_COMMENT_CREATE",
                                        "COURSE_WORKSPACE_COMMENT_READ", "COURSE_WORKSPACE_COMMENT_CREATE",
                                        "COURSE_EXERCISE_READ", "COURSE_EXERCISE_UPSERT", "COURSE_EXERCISE_SUBMIT",
                                        "COURSE_WORKSPACE_READ", "COURSE_WORKSPACE_SAVE",
                                        "COURSE_SKILL_CREATE", "COURSE_SKILL_READ", "COURSE_SKILL_READ_ALL",
                                        "COURSE_SKILL_UPDATE",
                                        "COURSE_SKILL_DELETE",
                                        "COURSE_TAG_CREATE", "COURSE_TAG_READ", "COURSE_TAG_READ_ALL",
                                        "COURSE_TAG_UPDATE",
                                        "COURSE_TAG_DELETE",
                                        // Learning Path - Full CRUD
                                        "LEARNING_PATH_CREATE", "LEARNING_PATH_READ_ALL", "LEARNING_PATH_READ",
                                        "LEARNING_PATH_UPDATE",
                                        "LEARNING_PATH_DELETE",
                                        "LEARNING_PATH_SEARCH", "LEARNING_PATH_BY_CREATOR", "LEARNING_PATH_BY_COURSE",
                                        "LEARNING_PATH_ADD_COURSES", "LEARNING_PATH_REMOVE_COURSE",
                                        "LEARNING_PATH_REORDER_COURSES",
                                        "LEARNING_PATH_PROGRESS_READ", "LEARNING_PATH_PROGRESS_BY_USER",
                                        "LEARNING_PATH_PROGRESS_BY_PATH",
                                        "LEARNING_PATH_STATISTICS",
                                        // File - Full access
                                        "FILE_UPLOAD", "FILE_UPLOAD_MULTIPLE", "FILE_READ", "FILE_READ_ALL",
                                        "FILE_READ_BY_FOLDER",
                                        "FILE_DELETE", "FILE_STATISTICS",
                                        "FOLDER_CREATE", "FOLDER_READ_BY_USER", "FOLDER_READ", "FOLDER_READ_TREE",
                                        "FOLDER_UPDATE",
                                        "FOLDER_DELETE",
                                        "FILE_USAGE_TRACK", "FILE_USAGE_REMOVE", "FILE_USAGE_REMOVE_ALL",
                                        "FILE_USAGE_READ",
                                        // AI - Use AI features
                                        "AI_GENERATE_EXERCISES", "AI_GENERATE_LEARNING_PATH", "AI_RECOMMEND_REALTIME",
                                        "AI_RECOMMEND_SCHEDULED", "AI_CHAT"
                        };
                        assignPermissionsByCode(instructorRole.getId(), permissions, instructorPermissions);
                        System.out.println("✅ INSTRUCTOR role assigned " + instructorPermissions.length
                                        + " permissions");

                        // ===== LEARNER - Chỉ có permissions xem và tham gia =====
                        String[] learnerPermissions = {
                                        // User - Own profile only
                                        "USER_PROFILE", "USER_CHANGE_PASSWORD",
                                        // Blog - Read and comment
                                        "BLOG_READ_ALL", "BLOG_READ", "BLOG_TAGS", "BLOG_COMMENT_READ",
                                        "BLOG_COMMENT_CREATE",
                                        // Course - Read and enroll
                                        "COURSE_READ_ALL", "COURSE_READ", "COURSE_ENROLL",
                                        "COURSE_CHAPTER_READ", "COURSE_LESSON_READ",
                                        "COURSE_PROGRESS_READ", "COURSE_LESSON_PROGRESS_UPDATE",
                                        "COURSE_LESSON_COMPLETE",
                                        "COURSE_RATING_READ", "COURSE_RATING_CREATE",
                                        "COURSE_COMMENT_READ", "COURSE_COMMENT_CREATE",
                                        "COURSE_LESSON_COMMENT_READ", "COURSE_LESSON_COMMENT_CREATE",
                                        "COURSE_WORKSPACE_COMMENT_READ", "COURSE_WORKSPACE_COMMENT_CREATE",
                                        "COURSE_EXERCISE_READ", "COURSE_EXERCISE_SUBMIT",
                                        "COURSE_WORKSPACE_READ", "COURSE_WORKSPACE_SAVE",
                                        "COURSE_SKILL_READ", "COURSE_SKILL_READ_ALL", "COURSE_TAG_READ",
                                        "COURSE_TAG_READ_ALL",
                                        // Learning Path - Read and track progress
                                        "LEARNING_PATH_READ_ALL", "LEARNING_PATH_READ", "LEARNING_PATH_SEARCH",
                                        "LEARNING_PATH_BY_CREATOR", "LEARNING_PATH_BY_COURSE",
                                        "LEARNING_PATH_PROGRESS_UPSERT", "LEARNING_PATH_PROGRESS_READ",
                                        "LEARNING_PATH_PROGRESS_BY_USER",
                                        "LEARNING_PATH_STATISTICS",
                                        // File - Own files only
                                        "FILE_UPLOAD", "FILE_UPLOAD_MULTIPLE", "FILE_READ", "FILE_READ_ALL",
                                        "FILE_READ_BY_FOLDER",
                                        "FILE_DELETE", "FILE_STATISTICS",
                                        "FOLDER_CREATE", "FOLDER_READ_BY_USER", "FOLDER_READ", "FOLDER_READ_TREE",
                                        "FOLDER_UPDATE",
                                        "FOLDER_DELETE",
                                        "FILE_USAGE_TRACK", "FILE_USAGE_REMOVE", "FILE_USAGE_READ",
                                        // AI - Basic AI features
                                        "AI_RECOMMEND_REALTIME", "AI_CHAT"
                        };
                        assignPermissionsByCode(learnerRole.getId(), permissions, learnerPermissions);
                        System.out.println(
                                        "✅ LEARNER role assigned " + learnerPermissions.length + " permissions");
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
                        User learnerUser = createUser("learner@techhub.com", "learner", "TechHub Learner",
                                        "learner123");
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
                        Permission permission = permissions.stream().filter(p -> p.getName().equals(code)).findFirst()
                                        .orElse(null);
                        if (permission != null) {
                                createRolePermission(roleId, permission.getId());
                        }
                }
        }
}
