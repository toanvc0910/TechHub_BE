-- Adds one long playable video to the first lesson of every active chapter.
-- Idempotent for the same lesson/video URL pairs.

BEGIN;

WITH long_video_library AS (
    SELECT
        1 AS slot,
        'python-101-business' AS source_key,
        'Python 101++: Let''s Get Down to Business' AS source_title,
        'https://archive.org/details/pyvideo_3153___Python_101_Lets_Get_Down_to_Business' AS source_page,
        'https://archive.org/download/pyvideo_3153___Python_101_Lets_Get_Down_to_Business/3153_Python_101_Lets_Get_Down_to_Business.mp4' AS video_url,
        6081 AS duration_seconds
    UNION ALL
    SELECT
        2,
        'docker-101-portable-future',
        'Docker 101: Meet the portable future',
        'https://archive.org/details/pyvideo_2837___Docker_101_Meet_the_portable_future_tutorial',
        'https://archive.org/download/pyvideo_2837___Docker_101_Meet_the_portable_future_tutorial/2837_Docker_101_Meet_the_portable_future_tutorial.mp4',
        2607
    UNION ALL
    SELECT
        3,
        'internet-programming-python',
        'PyCon 2009: Internet Programming with Python',
        'https://archive.org/details/pyvideo_185___pycon-2009-internet-programming-with-python-part-1-of-3',
        'https://archive.org/download/pyvideo_185___pycon-2009-internet-programming-with-python-part-1-of-3/185_pycon-2009-internet-programming-with-python-part-1-of-3.mp4',
        3520
),
target_lessons AS (
    SELECT
        c.id AS course_id,
        c.title AS course_title,
        c.created_by AS course_created_by,
        c.updated_by AS course_updated_by,
        c.instructor_id,
        ch.id AS chapter_id,
        ch."order" AS chapter_order,
        ch.title AS chapter_title,
        l.id AS lesson_id,
        l.title AS lesson_title,
        l.created_by AS lesson_created_by,
        l.updated_by AS lesson_updated_by,
        CASE
            WHEN c.title ILIKE '%Docker%' THEN 2
            WHEN c.title ILIKE '%Python%' THEN 1
            WHEN c.title ILIKE '%API%' OR c.title ILIKE '%PostgreSQL%' OR c.title ILIKE '%Security%' THEN 3
            ELSE ((ch."order" - 1) % 3) + 1
        END AS video_slot
    FROM courses c
    JOIN chapters ch ON ch.course_id = c.id AND ch.is_active = 'Y'
    JOIN lessons l ON l.chapter_id = ch.id AND l.is_active = 'Y'
    WHERE c.is_active = 'Y'
      AND l."order" = 1
),
selected_videos AS (
    SELECT
        t.*,
        v.source_key,
        v.source_title,
        v.source_page,
        v.video_url,
        v.duration_seconds
    FROM target_lessons t
    JOIN long_video_library v ON v.slot = t.video_slot
)
UPDATE lessons l
SET
    content_type = 'VIDEO'::content_type,
    video_url = s.video_url,
    estimated_duration = GREATEST(COALESCE(l.estimated_duration, 0), s.duration_seconds),
    updated = NOW(),
    updated_by = COALESCE(l.updated_by, s.lesson_updated_by, s.lesson_created_by, s.course_updated_by, s.course_created_by, s.instructor_id)
FROM selected_videos s
WHERE l.id = s.lesson_id;

WITH long_video_library AS (
    SELECT
        1 AS slot,
        'python-101-business' AS source_key,
        'Python 101++: Let''s Get Down to Business' AS source_title,
        'https://archive.org/details/pyvideo_3153___Python_101_Lets_Get_Down_to_Business' AS source_page,
        'https://archive.org/download/pyvideo_3153___Python_101_Lets_Get_Down_to_Business/3153_Python_101_Lets_Get_Down_to_Business.mp4' AS video_url,
        6081 AS duration_seconds
    UNION ALL
    SELECT
        2,
        'docker-101-portable-future',
        'Docker 101: Meet the portable future',
        'https://archive.org/details/pyvideo_2837___Docker_101_Meet_the_portable_future_tutorial',
        'https://archive.org/download/pyvideo_2837___Docker_101_Meet_the_portable_future_tutorial/2837_Docker_101_Meet_the_portable_future_tutorial.mp4',
        2607
    UNION ALL
    SELECT
        3,
        'internet-programming-python',
        'PyCon 2009: Internet Programming with Python',
        'https://archive.org/details/pyvideo_185___pycon-2009-internet-programming-with-python-part-1-of-3',
        'https://archive.org/download/pyvideo_185___pycon-2009-internet-programming-with-python-part-1-of-3/185_pycon-2009-internet-programming-with-python-part-1-of-3.mp4',
        3520
),
target_lessons AS (
    SELECT
        c.id AS course_id,
        c.title AS course_title,
        c.created_by AS course_created_by,
        c.updated_by AS course_updated_by,
        c.instructor_id,
        ch.id AS chapter_id,
        ch."order" AS chapter_order,
        ch.title AS chapter_title,
        l.id AS lesson_id,
        l.title AS lesson_title,
        l.created_by AS lesson_created_by,
        l.updated_by AS lesson_updated_by,
        CASE
            WHEN c.title ILIKE '%Docker%' THEN 2
            WHEN c.title ILIKE '%Python%' THEN 1
            WHEN c.title ILIKE '%API%' OR c.title ILIKE '%PostgreSQL%' OR c.title ILIKE '%Security%' THEN 3
            ELSE ((ch."order" - 1) % 3) + 1
        END AS video_slot
    FROM courses c
    JOIN chapters ch ON ch.course_id = c.id AND ch.is_active = 'Y'
    JOIN lessons l ON l.chapter_id = ch.id AND l.is_active = 'Y'
    WHERE c.is_active = 'Y'
      AND l."order" = 1
),
selected_videos AS (
    SELECT
        t.*,
        v.source_key,
        v.source_title,
        v.source_page,
        v.video_url,
        v.duration_seconds
    FROM target_lessons t
    JOIN long_video_library v ON v.slot = t.video_slot
)
UPDATE lesson_assets la
SET
    title = LEFT('Long video: ' || s.course_title || ' - chapter ' || s.chapter_order, 255),
    description = 'Long-form playable lesson video. Source: ' || s.source_title,
    metadata = jsonb_build_object(
        'seed_key', '2026-06-04-long-course-videos',
        'source_key', s.source_key,
        'source_title', s.source_title,
        'source_page', s.source_page,
        'source_provider', 'archive.org',
        'duration_seconds', s.duration_seconds,
        'duration_minutes', ROUND((s.duration_seconds::numeric / 60.0), 1),
        'course_id', s.course_id,
        'course_title', s.course_title,
        'chapter_id', s.chapter_id,
        'chapter_title', s.chapter_title,
        'chapter_order', s.chapter_order,
        'lesson_title', s.lesson_title
    ),
    updated = NOW(),
    updated_by = COALESCE(la.updated_by, s.lesson_updated_by, s.lesson_created_by, s.course_updated_by, s.course_created_by, s.instructor_id)
FROM selected_videos s
WHERE la.lesson_id = s.lesson_id
  AND la.asset_type = 'VIDEO'::lesson_asset_type
  AND la.external_url = s.video_url
  AND la.is_active = 'Y';

WITH long_video_library AS (
    SELECT
        1 AS slot,
        'python-101-business' AS source_key,
        'Python 101++: Let''s Get Down to Business' AS source_title,
        'https://archive.org/details/pyvideo_3153___Python_101_Lets_Get_Down_to_Business' AS source_page,
        'https://archive.org/download/pyvideo_3153___Python_101_Lets_Get_Down_to_Business/3153_Python_101_Lets_Get_Down_to_Business.mp4' AS video_url,
        6081 AS duration_seconds
    UNION ALL
    SELECT
        2,
        'docker-101-portable-future',
        'Docker 101: Meet the portable future',
        'https://archive.org/details/pyvideo_2837___Docker_101_Meet_the_portable_future_tutorial',
        'https://archive.org/download/pyvideo_2837___Docker_101_Meet_the_portable_future_tutorial/2837_Docker_101_Meet_the_portable_future_tutorial.mp4',
        2607
    UNION ALL
    SELECT
        3,
        'internet-programming-python',
        'PyCon 2009: Internet Programming with Python',
        'https://archive.org/details/pyvideo_185___pycon-2009-internet-programming-with-python-part-1-of-3',
        'https://archive.org/download/pyvideo_185___pycon-2009-internet-programming-with-python-part-1-of-3/185_pycon-2009-internet-programming-with-python-part-1-of-3.mp4',
        3520
),
target_lessons AS (
    SELECT
        c.id AS course_id,
        c.title AS course_title,
        c.created_by AS course_created_by,
        c.updated_by AS course_updated_by,
        c.instructor_id,
        ch.id AS chapter_id,
        ch."order" AS chapter_order,
        ch.title AS chapter_title,
        l.id AS lesson_id,
        l.title AS lesson_title,
        l.created_by AS lesson_created_by,
        l.updated_by AS lesson_updated_by,
        CASE
            WHEN c.title ILIKE '%Docker%' THEN 2
            WHEN c.title ILIKE '%Python%' THEN 1
            WHEN c.title ILIKE '%API%' OR c.title ILIKE '%PostgreSQL%' OR c.title ILIKE '%Security%' THEN 3
            ELSE ((ch."order" - 1) % 3) + 1
        END AS video_slot
    FROM courses c
    JOIN chapters ch ON ch.course_id = c.id AND ch.is_active = 'Y'
    JOIN lessons l ON l.chapter_id = ch.id AND l.is_active = 'Y'
    WHERE c.is_active = 'Y'
      AND l."order" = 1
),
selected_videos AS (
    SELECT
        t.*,
        v.source_key,
        v.source_title,
        v.source_page,
        v.video_url,
        v.duration_seconds
    FROM target_lessons t
    JOIN long_video_library v ON v.slot = t.video_slot
)
INSERT INTO lesson_assets (
    id,
    lesson_id,
    asset_type,
    "order",
    title,
    description,
    file_id,
    external_url,
    metadata,
    created,
    updated,
    created_by,
    updated_by,
    is_active
)
SELECT
    gen_random_uuid(),
    s.lesson_id,
    'VIDEO'::lesson_asset_type,
    COALESCE((SELECT MAX(existing."order") FROM lesson_assets existing WHERE existing.lesson_id = s.lesson_id), 0) + 1,
    LEFT('Long video: ' || s.course_title || ' - chapter ' || s.chapter_order, 255),
    'Long-form playable lesson video. Source: ' || s.source_title,
    NULL,
    s.video_url,
    jsonb_build_object(
        'seed_key', '2026-06-04-long-course-videos',
        'source_key', s.source_key,
        'source_title', s.source_title,
        'source_page', s.source_page,
        'source_provider', 'archive.org',
        'duration_seconds', s.duration_seconds,
        'duration_minutes', ROUND((s.duration_seconds::numeric / 60.0), 1),
        'course_id', s.course_id,
        'course_title', s.course_title,
        'chapter_id', s.chapter_id,
        'chapter_title', s.chapter_title,
        'chapter_order', s.chapter_order,
        'lesson_title', s.lesson_title
    ),
    NOW(),
    NOW(),
    COALESCE(s.lesson_created_by, s.course_created_by, s.instructor_id),
    COALESCE(s.lesson_updated_by, s.lesson_created_by, s.course_updated_by, s.course_created_by, s.instructor_id),
    'Y'
FROM selected_videos s
WHERE NOT EXISTS (
    SELECT 1
    FROM lesson_assets existing
    WHERE existing.lesson_id = s.lesson_id
      AND existing.asset_type = 'VIDEO'::lesson_asset_type
      AND existing.external_url = s.video_url
      AND existing.is_active = 'Y'
);

COMMIT;
