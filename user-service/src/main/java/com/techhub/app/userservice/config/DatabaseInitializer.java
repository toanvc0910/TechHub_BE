package com.techhub.app.userservice.config;

import com.techhub.app.userservice.entity.Role;
import com.techhub.app.userservice.entity.User;
import com.techhub.app.userservice.entity.UserRole;
import com.techhub.app.userservice.enums.UserStatus;
import com.techhub.app.userservice.repository.RoleRepository;
import com.techhub.app.userservice.repository.UserRepository;
import com.techhub.app.userservice.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

        private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
        private static final String ROLE_ADMIN = "ADMIN";
        private static final String ROLE_INSTRUCTOR = "INSTRUCTOR";
        private static final String ROLE_LEARNER = "LEARNER";

        private final RoleRepository roleRepository;
        private final UserRepository userRepository;
        private final UserRoleRepository userRoleRepository;
        private final PasswordEncoder passwordEncoder;

        @Value("${app.security.bootstrap-admin.enabled:false}")
        private boolean bootstrapAdminEnabled;

        @Value("${app.security.bootstrap-admin.email:}")
        private String bootstrapAdminEmail;

        @Value("${app.security.bootstrap-admin.username:}")
        private String bootstrapAdminUsername;

        @Value("${app.security.bootstrap-admin.password:}")
        private String bootstrapAdminPassword;

        @Value("${app.security.bootstrap-instructor.email:}")
        private String bootstrapInstructorEmail;

        @Value("${app.security.bootstrap-instructor.username:}")
        private String bootstrapInstructorUsername;

        @Value("${app.security.bootstrap-instructor.password:}")
        private String bootstrapInstructorPassword;

        @Value("${app.security.bootstrap-learner.email:}")
        private String bootstrapLearnerEmail;

        @Value("${app.security.bootstrap-learner.username:}")
        private String bootstrapLearnerUsername;

        @Value("${app.security.bootstrap-learner.password:}")
        private String bootstrapLearnerPassword;

        @Override
        public void run(String... args) {
                Role superAdminRole = ensureRole(ROLE_SUPER_ADMIN, "Super administrator");
                Role adminRole = ensureRole(ROLE_ADMIN, "Administrator");
                Role instructorRole = ensureRole(ROLE_INSTRUCTOR, "Instructor");
                Role learnerRole = ensureRole(ROLE_LEARNER, "Learner");

                System.out.println("=== CORE ROLES INITIALIZED ===");
                System.out.println("SUPER_ADMIN role ID: " + superAdminRole.getId());
                System.out.println("ADMIN role ID: " + adminRole.getId());
                System.out.println("==============================");

                if (userRepository.count() == 0) {
                        bootstrapInitialUsers(superAdminRole, adminRole, instructorRole, learnerRole);
                } else {
                        System.out.println("Users already exist. Skipping bootstrap users to avoid overwriting accounts.");
                }
        }

        private Role ensureRole(String name, String description) {
                return roleRepository.findByName(name).orElseGet(() -> {
                        Role role = new Role();
                        role.setName(name);
                        role.setDescription(description);
                        role.setIsActive(true);
                        role.setCreated(LocalDateTime.now());
                        role.setUpdated(LocalDateTime.now());
                        return roleRepository.save(role);
                });
        }

        private void bootstrapInitialUsers(Role superAdminRole, Role adminRole, Role instructorRole, Role learnerRole) {
                if (!bootstrapAdminEnabled) {
                        System.out.println("No users found, but bootstrap users are disabled.");
                        System.out.println("Set app.security.bootstrap-admin.enabled=true to create initial users.");
                        return;
                }

                if (!hasBootstrapUserConfig(bootstrapAdminEmail, bootstrapAdminUsername, bootstrapAdminPassword)
                                || !hasBootstrapUserConfig(bootstrapInstructorEmail, bootstrapInstructorUsername,
                                                bootstrapInstructorPassword)
                                || !hasBootstrapUserConfig(bootstrapLearnerEmail, bootstrapLearnerUsername,
                                                bootstrapLearnerPassword)) {
                        System.out.println("No users found, but bootstrap user configuration is incomplete.");
                        System.out.println("Set admin, instructor, and learner bootstrap email/username/password.");
                        return;
                }

                createBootstrapUser(bootstrapAdminEmail, bootstrapAdminUsername, bootstrapAdminPassword,
                                superAdminRole, adminRole);
                createBootstrapUser(bootstrapInstructorEmail, bootstrapInstructorUsername, bootstrapInstructorPassword,
                                instructorRole);
                createBootstrapUser(bootstrapLearnerEmail, bootstrapLearnerUsername, bootstrapLearnerPassword,
                                learnerRole);

                System.out.println("=== BOOTSTRAP USERS CREATED ===");
                System.out.println("Admin email: " + bootstrapAdminEmail);
                System.out.println("Instructor email: " + bootstrapInstructorEmail);
                System.out.println("Learner email: " + bootstrapLearnerEmail);
                System.out.println("===============================");
        }

        private boolean hasBootstrapUserConfig(String email, String username, String password) {
                return StringUtils.hasText(email)
                                && StringUtils.hasText(username)
                                && StringUtils.hasText(password);
        }

        private void createBootstrapUser(String email, String username, String password, Role... roles) {
                if (userRepository.existsByEmail(email) || userRepository.existsByUsername(username)) {
                        System.out.println("Bootstrap user already exists, skipping: " + email);
                        return;
                }

                User user = createUser(email, username, password);
                user = userRepository.save(user);
                for (Role role : roles) {
                        createUserRole(user.getId(), role.getId());
                }
        }

        private User createUser(String email, String username, String password) {
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
                if (userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
                        return;
                }

                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRole.setIsActive(true);
                userRole.setAssignedAt(LocalDateTime.now());
                userRole.setCreated(LocalDateTime.now());
                userRole.setUpdated(LocalDateTime.now());
                userRoleRepository.save(userRole);
        }
}
