-- Links published blogs to real published courses and lessons.
-- Safe to rerun: it updates the same blog rows from stable titles/IDs.

BEGIN;

WITH blog_course_plan(blog_id, course_order, course_title) AS (
    VALUES
        ('d1000001-7a21-4fb1-9a01-000000000001'::uuid, 1, 'AI Chat prompting cho lập trình viên'),
        ('d1000001-7a21-4fb1-9a01-000000000001'::uuid, 2, 'Python nền tảng cho người mới bắt đầu'),
        ('d1000002-7a21-4fb1-9a01-000000000002'::uuid, 1, 'React hiện đại: component, state và form'),
        ('d1000002-7a21-4fb1-9a01-000000000002'::uuid, 2, 'Next.js App Router cho sản phẩm học trực tuyến'),
        ('d1000002-7a21-4fb1-9a01-000000000002'::uuid, 3, 'HTML & CSS dựng giao diện responsive'),
        ('d1000003-7a21-4fb1-9a01-000000000003'::uuid, 1, 'Spring Boot REST API thực chiến'),
        ('d1000003-7a21-4fb1-9a01-000000000003'::uuid, 2, 'Java 17 core cho backend developer'),
        ('d1000003-7a21-4fb1-9a01-000000000003'::uuid, 3, 'API testing với Postman và contract cơ bản'),
        ('d1000004-7a21-4fb1-9a01-000000000004'::uuid, 1, 'PostgreSQL cho ứng dụng web'),
        ('d1000004-7a21-4fb1-9a01-000000000004'::uuid, 2, 'AI Chat prompting cho lập trình viên'),
        ('d1000005-7a21-4fb1-9a01-000000000005'::uuid, 1, 'Web Security essentials cho lập trình viên'),
        ('d1000005-7a21-4fb1-9a01-000000000005'::uuid, 2, 'Spring Boot REST API thực chiến'),
        ('d1000005-7a21-4fb1-9a01-000000000005'::uuid, 3, 'Java 17 core cho backend developer'),
        ('d1000006-7a21-4fb1-9a01-000000000006'::uuid, 1, 'Docker căn bản cho môi trường dev'),
        ('d1000006-7a21-4fb1-9a01-000000000006'::uuid, 2, 'Git & GitHub workflow cho dự án nhóm'),
        ('d1000007-7a21-4fb1-9a01-000000000007'::uuid, 1, 'Python nền tảng cho người mới bắt đầu'),
        ('d1000007-7a21-4fb1-9a01-000000000007'::uuid, 2, 'JavaScript căn bản cho web tương tác'),
        ('d1000007-7a21-4fb1-9a01-000000000007'::uuid, 3, 'API testing với Postman và contract cơ bản'),
        ('d1000008-7a21-4fb1-9a01-000000000008'::uuid, 1, 'AI Chat prompting cho lập trình viên'),
        ('d1000008-7a21-4fb1-9a01-000000000008'::uuid, 2, 'Python nền tảng cho người mới bắt đầu'),
        ('d1000009-7a21-4fb1-9a01-000000000009'::uuid, 1, 'Next.js App Router cho sản phẩm học trực tuyến'),
        ('d1000009-7a21-4fb1-9a01-000000000009'::uuid, 2, 'JavaScript căn bản cho web tương tác'),
        ('d1000009-7a21-4fb1-9a01-000000000009'::uuid, 3, 'HTML & CSS dựng giao diện responsive'),
        ('d100000a-7a21-4fb1-9a01-000000000010'::uuid, 1, 'Git & GitHub workflow cho dự án nhóm'),
        ('d100000a-7a21-4fb1-9a01-000000000010'::uuid, 2, 'Next.js App Router cho sản phẩm học trực tuyến'),
        ('d100000a-7a21-4fb1-9a01-000000000010'::uuid, 3, 'Spring Boot REST API thực chiến'),
        ('d100000b-7a21-4fb1-9a01-000000000011'::uuid, 1, 'AI Chat prompting cho lập trình viên'),
        ('d100000b-7a21-4fb1-9a01-000000000011'::uuid, 2, 'PostgreSQL cho ứng dụng web'),
        ('d100000b-7a21-4fb1-9a01-000000000011'::uuid, 3, 'Next.js App Router cho sản phẩm học trực tuyến'),
        ('d100000c-7a21-4fb1-9a01-000000000012'::uuid, 1, 'Git & GitHub workflow cho dự án nhóm'),
        ('d100000c-7a21-4fb1-9a01-000000000012'::uuid, 2, 'Docker căn bản cho môi trường dev'),
        ('d100000c-7a21-4fb1-9a01-000000000012'::uuid, 3, 'Next.js App Router cho sản phẩm học trực tuyến')
),
selected_courses AS (
    SELECT
        plan.blog_id,
        plan.course_order,
        c.id AS course_id
    FROM blog_course_plan plan
    JOIN courses c
        ON c.title = plan.course_title
       AND c.is_active = 'Y'
       AND c.status = 'PUBLISHED'
),
course_rollup AS (
    SELECT
        blog_id,
        array_agg(course_id ORDER BY course_order)::uuid[] AS course_ids
    FROM selected_courses
    GROUP BY blog_id
),
ranked_lessons AS (
    SELECT
        sc.blog_id,
        l.id AS lesson_id,
        row_number() OVER (
            PARTITION BY sc.blog_id
            ORDER BY sc.course_order, ch."order" ASC NULLS LAST, l."order" ASC NULLS LAST, l.created ASC
        ) AS lesson_rank
    FROM selected_courses sc
    JOIN chapters ch
        ON ch.course_id = sc.course_id
       AND ch.is_active = 'Y'
    JOIN lessons l
        ON l.chapter_id = ch.id
       AND l.is_active = 'Y'
),
lesson_rollup AS (
    SELECT
        blog_id,
        array_agg(lesson_id ORDER BY lesson_rank)::uuid[] AS lesson_ids
    FROM ranked_lessons
    WHERE lesson_rank <= 6
    GROUP BY blog_id
)
UPDATE blogs b
SET related_course_ids = COALESCE(cr.course_ids, ARRAY[]::uuid[]),
    related_lesson_ids = COALESCE(lr.lesson_ids, ARRAY[]::uuid[]),
    updated = CURRENT_TIMESTAMP
FROM course_rollup cr
LEFT JOIN lesson_rollup lr ON lr.blog_id = cr.blog_id
WHERE b.id = cr.blog_id
  AND b.is_active = 'Y';

COMMIT;
