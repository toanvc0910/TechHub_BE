# Class Diagram Relationships - TechHub

Auto-generated from `techhub.sql`. Each row shows the child FK pointing to its parent table, with relationship type, multiplicity, and delete rule so the direction is explicit.

## Legend (6 UML relationship types)
- Association: required reference without ownership (or many-to-many junction tables).
- Inheritance: not present in this schema.
- Implementation: not present (no interfaces in SQL).
- Dependency: audit reference (`created_by`, `updated_by`).
- Aggregation: nullable reference; child can exist without the parent.
- Composition: strong ownership; FK is NOT NULL and `ON DELETE CASCADE`.

## Relationship Table (child -> parent)
| Child table | Parent table | FK column | Type | Multiplicity (child -> parent) | Delete rule | Note |
| --- | --- | --- | --- | --- | --- | --- |
| ai_generation_tasks | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| ai_generation_tasks | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| analytics | courses | course_id | Aggregation | 0..* -> 1 | No cascade |  |
| analytics | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| analytics | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| analytics | users | user_id | Aggregation | 0..* -> 1 | No cascade |  |
| audit_logs | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| audit_logs | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| audit_logs | users | user_id | Aggregation | 0..* -> 1 | No cascade |  |
| auth_providers | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| auth_providers | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| auth_providers | users | user_id | Composition | * -> 1 (required) | CASCADE |  |
| authentication_logs | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| authentication_logs | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| authentication_logs | users | user_id | Composition | * -> 1 (required) | CASCADE |  |
| badges | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| badges | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| blog_tags | blogs | blog_id | Association | * -> 1 (required) | CASCADE | Junction Blogs <-> Tags |
| blog_tags | tags | tag_id | Association | * -> 1 (required) | CASCADE | Junction Blogs <-> Tags |
| blogs | users | author_id | Association | * -> 1 (required) | No cascade |  |
| blogs | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| blogs | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| chapters | courses | course_id | Composition | * -> 1 (required) | CASCADE |  |
| chapters | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| chapters | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| chat_messages | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| chat_messages | chat_sessions | session_id | Composition | * -> 1 (required) | CASCADE |  |
| chat_messages | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| chat_sessions | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| chat_sessions | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| chat_sessions | users | user_id | Composition | * -> 1 (required) | CASCADE |  |
| comments | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| comments | comments | parent_id | Aggregation | 0..* -> 1 | No cascade |  |
| comments | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| comments | users | user_id | Association | * -> 1 (required) | No cascade |  |
| course_skills | courses | course_id | Association | * -> 1 (required) | CASCADE | Junction Courses <-> Skills |
| course_skills | skills | skill_id | Association | * -> 1 (required) | CASCADE | Junction Courses <-> Skills |
| course_tags | courses | course_id | Association | * -> 1 (required) | CASCADE | Junction Courses <-> Tags |
| course_tags | tags | tag_id | Association | * -> 1 (required) | CASCADE | Junction Courses <-> Tags |
| courses | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| courses | users | instructor_id | Association | * -> 1 (required) | No cascade |  |
| courses | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| enrollments | courses | course_id | Composition | * -> 1 (required) | CASCADE |  |
| enrollments | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| enrollments | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| enrollments | users | user_id | Composition | * -> 1 (required) | CASCADE |  |
| exercise_test_cases | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| exercise_test_cases | exercises | exercise_id | Composition | * -> 1 (required) | CASCADE |  |
| exercise_test_cases | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| exercises | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| exercises | lessons | lesson_id | Composition | * -> 1 (required) | CASCADE |  |
| exercises | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| file_folders | file_folders | parent_id | Aggregation | 0..* -> 1 | CASCADE |  |
| file_usage | files | file_id | Composition | * -> 1 (required) | CASCADE |  |
| files | file_folders | folder_id | Aggregation | 0..* -> 1 | CASCADE |  |
| forum_posts | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| forum_posts | forums | forum_id | Composition | * -> 1 (required) | CASCADE |  |
| forum_posts | forum_posts | parent_id | Aggregation | 0..* -> 1 | No cascade |  |
| forum_posts | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| forum_posts | users | user_id | Composition | * -> 1 (required) | CASCADE |  |
| forums | courses | course_id | Aggregation | 0..* -> 1 | No cascade |  |
| forums | users | created_by | Dependency | * -> 1 (required) | No cascade | Audit trail |
| forums | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| group_chats | courses | course_id | Aggregation | 0..* -> 1 | No cascade |  |
| group_chats | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| group_chats | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| leaderboards | courses | course_id | Aggregation | 0..* -> 1 | No cascade |  |
| leaderboards | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| leaderboards | learning_paths | path_id | Aggregation | 0..* -> 1 | No cascade |  |
| leaderboards | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| learning_path_courses | courses | course_id | Association | * -> 1 (required) | CASCADE | Junction Learning_Paths <-> Courses |
| learning_path_courses | learning_paths | path_id | Association | * -> 1 (required) | CASCADE | Junction Learning_Paths <-> Courses |
| learning_path_skills | learning_paths | path_id | Association | * -> 1 (required) | CASCADE | Junction Learning_Paths <-> Skills |
| learning_path_skills | skills | skill_id | Association | * -> 1 (required) | CASCADE | Junction Learning_Paths <-> Skills |
| learning_paths | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| learning_paths | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| lesson_assets | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| lesson_assets | lessons | lesson_id | Composition | * -> 1 (required) | CASCADE |  |
| lesson_assets | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| lessons | chapters | chapter_id | Composition | * -> 1 (required) | CASCADE |  |
| lessons | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| lessons | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| notifications | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| notifications | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| notifications | users | user_id | Composition | * -> 1 (required) | CASCADE |  |
| otps | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| otps | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| otps | users | user_id | Composition | * -> 1 (required) | CASCADE |  |
| path_progress | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| path_progress | learning_paths | path_id | Composition | * -> 1 (required) | CASCADE |  |
| path_progress | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| path_progress | users | user_id | Composition | * -> 1 (required) | CASCADE |  |
| payment_gateway_mappings | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| payment_gateway_mappings | transactions | transaction_id | Composition | * -> 1 (required) | CASCADE |  |
| payment_gateway_mappings | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| payment_gateway_mappings | users | user_id | Composition | * -> 1 (required) | CASCADE |  |
| payments | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| payments | transactions | transaction_id | Composition | * -> 1 (required) | CASCADE |  |
| payments | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| permissions | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| permissions | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| profiles | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| profiles | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| profiles | users | user_id | Composition | * -> 1 (required) | CASCADE |  |
| progress | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| progress | lessons | lesson_id | Composition | * -> 1 (required) | CASCADE |  |
| progress | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| progress | users | user_id | Composition | * -> 1 (required) | CASCADE |  |
| promotions | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| promotions | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| ratings | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| ratings | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| ratings | users | user_id | Composition | * -> 1 (required) | CASCADE |  |
| recommendations | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| recommendations | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| recommendations | users | user_id | Composition | * -> 1 (required) | CASCADE |  |
| rewards | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| rewards | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| role_permissions | users | created_by | Dependency | 0..* -> 1 | No cascade | Junction Roles <-> Permissions |
| role_permissions | permissions | permission_id | Association | * -> 1 (required) | CASCADE | Junction Roles <-> Permissions |
| role_permissions | roles | role_id | Association | * -> 1 (required) | CASCADE | Junction Roles <-> Permissions |
| role_permissions | users | updated_by | Dependency | 0..* -> 1 | No cascade | Junction Roles <-> Permissions |
| roles | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| roles | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| skills | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| skills | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| submissions | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| submissions | exercises | exercise_id | Composition | * -> 1 (required) | CASCADE |  |
| submissions | users | graded_by | Aggregation | 0..* -> 1 | No cascade |  |
| submissions | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| submissions | users | user_id | Composition | * -> 1 (required) | CASCADE |  |
| tags | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| tags | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| transaction_items | courses | course_id | Composition | * -> 1 (required) | CASCADE |  |
| transaction_items | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| transaction_items | transactions | transaction_id | Composition | * -> 1 (required) | CASCADE |  |
| transaction_items | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| transactions | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| transactions | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| transactions | users | user_id | Association | * -> 1 (required) | No cascade |  |
| translations | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| translations | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| user_badges | badges | badge_id | Association | * -> 1 (required) | CASCADE | Junction Users <-> Badges |
| user_badges | users | user_id | Association | * -> 1 (required) | CASCADE | Junction Users <-> Badges |
| user_codes | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| user_codes | lessons | lesson_id | Composition | * -> 1 (required) | CASCADE |  |
| user_codes | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| user_codes | users | user_id | Composition | * -> 1 (required) | CASCADE |  |
| user_permissions | users | created_by | Dependency | 0..* -> 1 | No cascade | Junction Users <-> Permissions |
| user_permissions | permissions | permission_id | Association | * -> 1 (required) | CASCADE | Junction Users <-> Permissions |
| user_permissions | users | updated_by | Dependency | 0..* -> 1 | No cascade | Junction Users <-> Permissions |
| user_permissions | users | user_id | Association | * -> 1 (required) | CASCADE | Junction Users <-> Permissions |
| user_points | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| user_points | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| user_points | users | user_id | Composition | 1 -> 1 (required) | CASCADE |  |
| user_roles | users | created_by | Dependency | 0..* -> 1 | No cascade | Junction Users <-> Roles |
| user_roles | roles | role_id | Association | * -> 1 (required) | CASCADE | Junction Users <-> Roles |
| user_roles | users | updated_by | Dependency | 0..* -> 1 | No cascade | Junction Users <-> Roles |
| user_roles | users | user_id | Association | * -> 1 (required) | CASCADE | Junction Users <-> Roles |
| user_twofa | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| user_twofa | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| user_twofa | users | user_id | Composition | 1 -> 1 (required) | CASCADE |  |
| users | users | created_by | Dependency | 0..* -> 1 | No cascade | Audit trail |
| users | users | updated_by | Dependency | 0..* -> 1 | No cascade | Audit trail |

## By Table (quick scan)
### users
- users.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- users.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### profiles
- profiles.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- profiles.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- profiles.user_id -> users.id (Composition; * -> 1 (required); delete: CASCADE)

### authentication_logs
- authentication_logs.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- authentication_logs.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- authentication_logs.user_id -> users.id (Composition; * -> 1 (required); delete: CASCADE)

### auth_providers
- auth_providers.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- auth_providers.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- auth_providers.user_id -> users.id (Composition; * -> 1 (required); delete: CASCADE)

### otps
- otps.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- otps.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- otps.user_id -> users.id (Composition; * -> 1 (required); delete: CASCADE)

### user_twofa
- user_twofa.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- user_twofa.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- user_twofa.user_id -> users.id (Composition; 1 -> 1 (required); delete: CASCADE)

### permissions
- permissions.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- permissions.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### roles
- roles.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- roles.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### role_permissions
- role_permissions.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Junction Roles <-> Permissions
- role_permissions.permission_id -> permissions.id (Association; * -> 1 (required); delete: CASCADE) - Junction Roles <-> Permissions
- role_permissions.role_id -> roles.id (Association; * -> 1 (required); delete: CASCADE) - Junction Roles <-> Permissions
- role_permissions.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Junction Roles <-> Permissions

### user_roles
- user_roles.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Junction Users <-> Roles
- user_roles.role_id -> roles.id (Association; * -> 1 (required); delete: CASCADE) - Junction Users <-> Roles
- user_roles.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Junction Users <-> Roles
- user_roles.user_id -> users.id (Association; * -> 1 (required); delete: CASCADE) - Junction Users <-> Roles

### user_permissions
- user_permissions.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Junction Users <-> Permissions
- user_permissions.permission_id -> permissions.id (Association; * -> 1 (required); delete: CASCADE) - Junction Users <-> Permissions
- user_permissions.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Junction Users <-> Permissions
- user_permissions.user_id -> users.id (Association; * -> 1 (required); delete: CASCADE) - Junction Users <-> Permissions

### courses
- courses.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- courses.instructor_id -> users.id (Association; * -> 1 (required); delete: No cascade)
- courses.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### chapters
- chapters.course_id -> courses.id (Composition; * -> 1 (required); delete: CASCADE)
- chapters.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- chapters.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### lessons
- lessons.chapter_id -> chapters.id (Composition; * -> 1 (required); delete: CASCADE)
- lessons.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- lessons.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### lesson_assets
- lesson_assets.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- lesson_assets.lesson_id -> lessons.id (Composition; * -> 1 (required); delete: CASCADE)
- lesson_assets.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### exercises
- exercises.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- exercises.lesson_id -> lessons.id (Composition; * -> 1 (required); delete: CASCADE)
- exercises.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### exercise_test_cases
- exercise_test_cases.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- exercise_test_cases.exercise_id -> exercises.id (Composition; * -> 1 (required); delete: CASCADE)
- exercise_test_cases.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### progress
- progress.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- progress.lesson_id -> lessons.id (Composition; * -> 1 (required); delete: CASCADE)
- progress.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- progress.user_id -> users.id (Composition; * -> 1 (required); delete: CASCADE)

### comments
- comments.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- comments.parent_id -> comments.id (Aggregation; 0..* -> 1; delete: No cascade)
- comments.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- comments.user_id -> users.id (Association; * -> 1 (required); delete: No cascade)

### enrollments
- enrollments.course_id -> courses.id (Composition; * -> 1 (required); delete: CASCADE)
- enrollments.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- enrollments.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- enrollments.user_id -> users.id (Composition; * -> 1 (required); delete: CASCADE)

### ratings
- ratings.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- ratings.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- ratings.user_id -> users.id (Composition; * -> 1 (required); delete: CASCADE)

### submissions
- submissions.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- submissions.exercise_id -> exercises.id (Composition; * -> 1 (required); delete: CASCADE)
- submissions.graded_by -> users.id (Aggregation; 0..* -> 1; delete: No cascade)
- submissions.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- submissions.user_id -> users.id (Composition; * -> 1 (required); delete: CASCADE)

### user_codes
- user_codes.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- user_codes.lesson_id -> lessons.id (Composition; * -> 1 (required); delete: CASCADE)
- user_codes.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- user_codes.user_id -> users.id (Composition; * -> 1 (required); delete: CASCADE)

### promotions
- promotions.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- promotions.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### transactions
- transactions.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- transactions.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- transactions.user_id -> users.id (Association; * -> 1 (required); delete: No cascade)

### transaction_items
- transaction_items.course_id -> courses.id (Composition; * -> 1 (required); delete: CASCADE)
- transaction_items.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- transaction_items.transaction_id -> transactions.id (Composition; * -> 1 (required); delete: CASCADE)
- transaction_items.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### payments
- payments.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- payments.transaction_id -> transactions.id (Composition; * -> 1 (required); delete: CASCADE)
- payments.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### payment_gateway_mappings
- payment_gateway_mappings.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- payment_gateway_mappings.transaction_id -> transactions.id (Composition; * -> 1 (required); delete: CASCADE)
- payment_gateway_mappings.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- payment_gateway_mappings.user_id -> users.id (Composition; * -> 1 (required); delete: CASCADE)

### blogs
- blogs.author_id -> users.id (Association; * -> 1 (required); delete: No cascade)
- blogs.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- blogs.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### forums
- forums.course_id -> courses.id (Aggregation; 0..* -> 1; delete: No cascade)
- forums.created_by -> users.id (Dependency; * -> 1 (required); delete: No cascade) - Audit trail
- forums.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### forum_posts
- forum_posts.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- forum_posts.forum_id -> forums.id (Composition; * -> 1 (required); delete: CASCADE)
- forum_posts.parent_id -> forum_posts.id (Aggregation; 0..* -> 1; delete: No cascade)
- forum_posts.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- forum_posts.user_id -> users.id (Composition; * -> 1 (required); delete: CASCADE)

### group_chats
- group_chats.course_id -> courses.id (Aggregation; 0..* -> 1; delete: No cascade)
- group_chats.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- group_chats.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### learning_paths
- learning_paths.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- learning_paths.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### learning_path_courses
- learning_path_courses.course_id -> courses.id (Association; * -> 1 (required); delete: CASCADE) - Junction Learning_Paths <-> Courses
- learning_path_courses.path_id -> learning_paths.id (Association; * -> 1 (required); delete: CASCADE) - Junction Learning_Paths <-> Courses

### skills
- skills.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- skills.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### tags
- tags.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- tags.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### course_skills
- course_skills.course_id -> courses.id (Association; * -> 1 (required); delete: CASCADE) - Junction Courses <-> Skills
- course_skills.skill_id -> skills.id (Association; * -> 1 (required); delete: CASCADE) - Junction Courses <-> Skills

### course_tags
- course_tags.course_id -> courses.id (Association; * -> 1 (required); delete: CASCADE) - Junction Courses <-> Tags
- course_tags.tag_id -> tags.id (Association; * -> 1 (required); delete: CASCADE) - Junction Courses <-> Tags

### learning_path_skills
- learning_path_skills.path_id -> learning_paths.id (Association; * -> 1 (required); delete: CASCADE) - Junction Learning_Paths <-> Skills
- learning_path_skills.skill_id -> skills.id (Association; * -> 1 (required); delete: CASCADE) - Junction Learning_Paths <-> Skills

### blog_tags
- blog_tags.blog_id -> blogs.id (Association; * -> 1 (required); delete: CASCADE) - Junction Blogs <-> Tags
- blog_tags.tag_id -> tags.id (Association; * -> 1 (required); delete: CASCADE) - Junction Blogs <-> Tags

### path_progress
- path_progress.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- path_progress.path_id -> learning_paths.id (Composition; * -> 1 (required); delete: CASCADE)
- path_progress.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- path_progress.user_id -> users.id (Composition; * -> 1 (required); delete: CASCADE)

### badges
- badges.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- badges.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### user_badges
- user_badges.badge_id -> badges.id (Association; * -> 1 (required); delete: CASCADE) - Junction Users <-> Badges
- user_badges.user_id -> users.id (Association; * -> 1 (required); delete: CASCADE) - Junction Users <-> Badges

### user_points
- user_points.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- user_points.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- user_points.user_id -> users.id (Composition; 1 -> 1 (required); delete: CASCADE)

### leaderboards
- leaderboards.course_id -> courses.id (Aggregation; 0..* -> 1; delete: No cascade)
- leaderboards.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- leaderboards.path_id -> learning_paths.id (Aggregation; 0..* -> 1; delete: No cascade)
- leaderboards.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### rewards
- rewards.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- rewards.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### notifications
- notifications.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- notifications.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- notifications.user_id -> users.id (Composition; * -> 1 (required); delete: CASCADE)

### analytics
- analytics.course_id -> courses.id (Aggregation; 0..* -> 1; delete: No cascade)
- analytics.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- analytics.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- analytics.user_id -> users.id (Aggregation; 0..* -> 1; delete: No cascade)

### recommendations
- recommendations.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- recommendations.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- recommendations.user_id -> users.id (Composition; * -> 1 (required); delete: CASCADE)

### translations
- translations.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- translations.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### chat_sessions
- chat_sessions.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- chat_sessions.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- chat_sessions.user_id -> users.id (Composition; * -> 1 (required); delete: CASCADE)

### chat_messages
- chat_messages.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- chat_messages.session_id -> chat_sessions.id (Composition; * -> 1 (required); delete: CASCADE)
- chat_messages.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail

### audit_logs
- audit_logs.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- audit_logs.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- audit_logs.user_id -> users.id (Aggregation; 0..* -> 1; delete: No cascade)

### file_folders
- file_folders.parent_id -> file_folders.id (Aggregation; 0..* -> 1; delete: CASCADE)

### files
- files.folder_id -> file_folders.id (Aggregation; 0..* -> 1; delete: CASCADE)

### file_usage
- file_usage.file_id -> files.id (Composition; * -> 1 (required); delete: CASCADE)

### ai_generation_tasks
- ai_generation_tasks.created_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
- ai_generation_tasks.updated_by -> users.id (Dependency; 0..* -> 1; delete: No cascade) - Audit trail
