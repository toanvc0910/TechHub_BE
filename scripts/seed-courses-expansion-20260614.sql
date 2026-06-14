-- Expand the course catalog from 13 to 26 courses, matching the existing course
-- format exactly: Vietnamese metadata, 5 chapters x 3 lessons (15) per course on the
-- shared lesson template, real topic-style Unsplash thumbnails, populated
-- intro_video_file, and price + discount in the same band as existing courses.
--
-- Idempotent: deterministic ids via md5(...)::uuid. The script DELETEs its own 13
-- courses first (cascades to chapters/lessons/course_skills/course_tags) and reinserts,
-- so re-running always converges to the same result and is safe to run repeatedly.
-- Owner (instructor_id/created_by) is resolved dynamically from existing data.
--
-- Apply:  docker run --rm -e PGPASSWORD=... -i postgres:16-alpine \
--           psql -h <host> -p 5432 -U techhub -d techhub -v ON_ERROR_STOP=1 < this.sql
-- After applying, open Manage > Dashboard > "Reindex Courses" for AI search.

BEGIN;

-- 0) Owner: existing course instructor -> any INSTRUCTOR -> any active user.
CREATE TEMPORARY TABLE tmp_owner ON COMMIT DROP AS
SELECT uid FROM (
    SELECT instructor_id AS uid, 1 AS pri, created AS ord
    FROM courses WHERE is_active = 'Y' AND instructor_id IS NOT NULL
    UNION ALL
    SELECT u.id, 2, u.created FROM users u
    JOIN user_roles ur ON ur.user_id = u.id AND ur.is_active = 'Y'
    JOIN roles r ON r.id = ur.role_id AND r.name = 'INSTRUCTOR'
    WHERE u.is_active = 'Y'
    UNION ALL
    SELECT id, 3, created FROM users WHERE is_active = 'Y'
) s ORDER BY pri, ord LIMIT 1;

-- 1) Course catalog (Vietnamese). thumb = verified Unsplash photo id;
--    intro_idx/video_idx pick from the same media the existing courses use.
CREATE TEMPORARY TABLE tmp_courses ON COMMIT DROP AS
SELECT * FROM (VALUES
    ('linux-sysadmin', 'Quản trị hệ thống Linux cho người mới', 'BEGINNER'::course_level,
     'Khóa học giúp bạn tự tin làm chủ dòng lệnh Linux và vận hành server thật: hệ thống tập tin, người dùng và phân quyền, tiến trình, cài đặt phần mềm và tự động hóa bằng shell script và systemd.',
     449000::numeric, 329000::numeric,
     '["Thành thạo dòng lệnh và quản lý hệ thống tập tin Linux", "Quản lý người dùng, phân quyền và tiến trình", "Tự động hóa công việc bằng Bash script, cron và systemd"]'::jsonb,
     '["Máy tính chạy được Linux VM, container hoặc WSL2", "Biết dùng máy tính cơ bản, không cần kinh nghiệm Linux"]'::jsonb,
     '1629654297299-c8506221ca97', 1, 2),
    ('windows-server-ad', 'Quản trị Windows Server và Active Directory', 'INTERMEDIATE',
     'Triển khai và vận hành Windows Server trong môi trường domain: cài đặt role, thiết kế Active Directory, quản lý Group Policy, DNS/DHCP và tự động hóa quản trị bằng PowerShell.',
     649000, 489000,
     '["Cài đặt và cấu hình các role của Windows Server", "Thiết kế và quản lý domain, OU và Group Policy", "Tự động hóa quản trị bằng PowerShell"]'::jsonb,
     '["Windows 10/11 hoặc bản đánh giá Windows Server", "Nắm khái niệm mạng cơ bản (IP, DNS)"]'::jsonb,
     '1667372393119-3d4c48d07fc9', 2, 1),
    ('kubernetes-production', 'Kubernetes vận hành thực chiến', 'ADVANCED',
     'Chạy ứng dụng container ở quy mô lớn với Kubernetes: pod, deployment, service, ingress, config và secret, autoscaling và giám sát cho cụm production.',
     899000, 699000,
     '["Mô hình hóa workload bằng Pod, Deployment, StatefulSet", "Định tuyến lưu lượng với Service và Ingress", "Quản lý cấu hình, secret, lưu trữ và autoscaling"]'::jsonb,
     '["Nắm vững Docker và container", "Quen với YAML và dòng lệnh Linux"]'::jsonb,
     '1605745341112-85968b19335b', 3, 3),
    ('cicd-pipelines', 'CI/CD với GitHub Actions và Jenkins', 'INTERMEDIATE',
     'Tự động hóa build, test và triển khai. Thiết kế pipeline CI/CD với GitHub Actions và Jenkins, quản lý artifact, secret và phát hành an toàn với các cổng kiểm soát chất lượng.',
     599000, 449000,
     '["Thiết kế pipeline build và test cho mỗi commit", "Tự động triển khai bằng GitHub Actions và Jenkins", "Thêm cổng kiểm soát chất lượng và rollback"]'::jsonb,
     '["Biết Git và dòng lệnh cơ bản", "Quen với một hệ sinh thái (Node, Java hoặc Python)"]'::jsonb,
     '1667372335962-5fd503a8ae5b', 1, 1),
    ('terraform-iac', 'Infrastructure as Code với Terraform', 'INTERMEDIATE',
     'Quản lý hạ tầng đám mây theo cách khai báo với Terraform: HCL, quản lý state, module và quy trình an toàn để hạ tầng lặp lại được và quản lý bằng version.',
     699000, 529000,
     '["Viết hạ tầng bằng HCL với biến và output", "Quản lý state, workspace và remote backend an toàn", "Xây dựng module tái sử dụng cho nhiều môi trường"]'::jsonb,
     '["Nắm khái niệm cloud cơ bản (compute, network, storage)", "Quen với dòng lệnh và Git"]'::jsonb,
     '1518770660439-4636190af475', 2, 2),
    ('data-engineering-spark', 'Data Engineering với Apache Spark', 'ADVANCED',
     'Xây dựng pipeline dữ liệu quy mô lớn với Apache Spark: xử lý dữ liệu batch và streaming, tối ưu job và thiết kế ETL tin cậy trên cụm phân tán.',
     899000, 699000,
     '["Xử lý dữ liệu lớn với Spark RDD và DataFrame", "Xây dựng pipeline batch và structured streaming", "Tối ưu partition, join và bộ nhớ cho hiệu năng"]'::jsonb,
     '["Thành thạo SQL và Python hoặc Scala", "Hiểu cấu trúc dữ liệu cơ bản"]'::jsonb,
     '1460925895917-afdab827c52f', 3, 3),
    ('sql-data-analytics', 'SQL cho phân tích dữ liệu', 'INTERMEDIATE',
     'Biến dữ liệu thô thành thông tin hữu ích với SQL nâng cao: join, window function, CTE, tổng hợp và tối ưu truy vấn trên bộ dữ liệu thực tế.',
     449000, 329000,
     '["Viết truy vấn phức tạp với join, CTE và subquery", "Phân tích dữ liệu bằng window function và aggregation", "Đọc query plan và tối ưu hiệu năng"]'::jsonb,
     '["Biết SQL cơ bản (SELECT, WHERE, GROUP BY)", "Có sẵn một CSDL SQL (khuyến nghị PostgreSQL)"]'::jsonb,
     '1573164713714-d95e436ab8d6', 1, 1),
    ('data-warehousing-etl', 'Data Warehouse và pipeline ETL', 'INTERMEDIATE',
     'Thiết kế kho dữ liệu và xây dựng pipeline ETL/ELT: mô hình hóa chiều, star schema, điều phối bằng Airflow và data mart sẵn sàng cho phân tích.',
     699000, 529000,
     '["Thiết kế star/snowflake schema cho phân tích", "Xây dựng pipeline ETL/ELT và điều phối bằng Airflow", "Mô hình hóa fact, dimension và slowly changing dimension"]'::jsonb,
     '["Biết SQL ở mức vận dụng", "Biết Python cơ bản là một lợi thế"]'::jsonb,
     '1526374965328-7f61d4dc18c5', 2, 2),
    ('web-test-automation', 'Kiểm thử web tự động với Selenium và Playwright', 'INTERMEDIATE',
     'Xây dựng bộ kiểm thử giao diện end-to-end tin cậy: tự động hóa trình duyệt với Selenium và Playwright, áp dụng Page Object Model, xử lý chờ đợi và chạy trong CI.',
     499000, 369000,
     '["Tự động hóa luồng trình duyệt với Selenium và Playwright", "Thiết kế bộ test dễ bảo trì với Page Object Model", "Xử lý wait, selector và test chập chờn"]'::jsonb,
     '["Biết JavaScript hoặc Python cơ bản", "Hiểu cách trang web hoạt động (HTML/CSS)"]'::jsonb,
     '1517245386807-bb43f82c33c4', 3, 3),
    ('api-testing', 'Kiểm thử API tự động', 'BEGINNER',
     'Kiểm thử REST API một cách tự tin: test thủ công với Postman, tự động hóa với REST Assured và Newman, kiểm tra contract và tích hợp vào CI.',
     399000, 279000,
     '["Kiểm thử endpoint REST thủ công với Postman", "Tự động hóa test API với REST Assured và Newman", "Kiểm tra status, schema, contract và tích hợp CI"]'::jsonb,
     '["Hiểu cơ bản về HTTP và JSON", "Không cần kinh nghiệm kiểm thử trước đó"]'::jsonb,
     '1591453089816-0fbb971b454c', 1, 1),
    ('deep-learning-pytorch', 'Deep Learning với PyTorch', 'ADVANCED',
     'Xây dựng và huấn luyện mạng nơ-ron với PyTorch: tensor và autograd, CNN và RNN, vòng huấn luyện, regularization và triển khai mô hình.',
     899000, 699000,
     '["Làm việc với tensor, autograd và API PyTorch", "Xây dựng và huấn luyện CNN, RNN", "Áp dụng regularization, tối ưu và transfer learning"]'::jsonb,
     '["Thành thạo Python và NumPy", "Biết đại số tuyến tính và giải tích cơ bản"]'::jsonb,
     '1620712943543-bcc4688e7485', 2, 2),
    ('nlp-transformers', 'NLP và Transformers', 'ADVANCED',
     'Xử lý ngôn ngữ tự nhiên hiện đại với Transformers: từ embedding và attention đến fine-tune mô hình kiểu BERT/GPT bằng Hugging Face cho các bài toán NLP thực tế.',
     899000, 699000,
     '["Biểu diễn văn bản bằng tokenization và embedding", "Hiểu cơ chế attention và kiến trúc Transformer", "Fine-tune mô hình pretrained bằng Hugging Face"]'::jsonb,
     '["Biết Python và deep learning cơ bản", "Quen PyTorch hoặc TensorFlow là một lợi thế"]'::jsonb,
     '1487058792275-0ad4aaf24ca7', 3, 3),
    ('aws-solutions-architect', 'AWS: từ Cloud Practitioner đến Solutions Architect', 'ALL_LEVELS',
     'Đi từ người mới đến tư duy Solutions Architect trên AWS: nắm các dịch vụ lõi compute, storage, networking, database, security và thiết kế kiến trúc bền vững, tối ưu chi phí.',
     799000, 599000,
     '["Nắm các dịch vụ compute, storage, networking lõi của AWS", "Thiết kế kiến trúc an toàn và sẵn sàng cao", "Áp dụng IAM, VPC và Well-Architected Framework"]'::jsonb,
     '["Biết khái niệm IT và mạng cơ bản", "Không cần kinh nghiệm AWS trước đó"]'::jsonb,
     '1544197150-b99a580bb7a8', 1, 1)
) AS v(slug, title, level, description, price, discount, objectives, requirements, thumb, intro_idx, video_idx);

-- Replace any previous run's courses (cascades to chapters/lessons/course_skills/course_tags).
DELETE FROM courses WHERE id IN (SELECT md5('techhub-course-' || slug)::uuid FROM tmp_courses);

INSERT INTO courses (id, title, description, price, currency, instructor_id, status, level, language,
                     discount_price, thumbnail, intro_video_file, objectives, requirements,
                     created_by, updated_by, is_active)
SELECT md5('techhub-course-' || c.slug)::uuid, c.title, c.description, c.price, 'VND', o.uid,
       'PUBLISHED'::course_status, c.level, 'VI'::lang, c.discount,
       'https://images.unsplash.com/photo-' || c.thumb || '?auto=format&fit=crop&w=1200&q=80',
       CASE c.intro_idx
            WHEN 1 THEN 'https://cdn.coverr.co/videos/coverr-programming-on-a-macbook-6399/1080p.mp4'
            WHEN 2 THEN 'https://cdn.coverr.co/videos/coverr-coding-on-a-laptop-2116/1080p.mp4'
            ELSE 'https://cdn.coverr.co/videos/coverr-coding-developer-7198/1080p.mp4' END,
       c.objectives, c.requirements, o.uid, o.uid, 'Y'
FROM tmp_courses c CROSS JOIN tmp_owner o;

-- 2) Chapters: shared 5-chapter template, applied to every new course.
CREATE TEMPORARY TABLE tmp_chapter_tpl ON COMMIT DROP AS
SELECT * FROM (VALUES
    (1, 'Định hướng khóa học và bối cảnh thực tế'),
    (2, 'Nền tảng kỹ thuật cốt lõi'),
    (3, 'Thực hành có hướng dẫn'),
    (4, 'Dự án mini cuối khóa'),
    (5, 'Ôn tập, đánh giá và lộ trình tiếp theo')
) AS v(ord, title);

INSERT INTO chapters (id, title, "order", course_id, min_completion_threshold, auto_unlock, locked,
                      created_by, updated_by, is_active)
SELECT md5('techhub-ch-' || c.slug || '-' || t.ord)::uuid, t.title, t.ord,
       md5('techhub-course-' || c.slug)::uuid, 0.7, TRUE, (t.ord > 1), o.uid, o.uid, 'Y'
FROM tmp_courses c CROSS JOIN tmp_chapter_tpl t CROSS JOIN tmp_owner o;

-- 3) Lessons: shared 15-lesson template (3 per chapter). Lesson 1 of each chapter is a
--    VIDEO using the course's assigned video; the rest are TEXT/EXERCISE. First two
--    lessons of chapter 1 are free previews (matches existing courses).
CREATE TEMPORARY TABLE tmp_lesson_tpl ON COMMIT DROP AS
SELECT * FROM (VALUES
    (1, 1, 'Tổng quan khóa học và sản phẩm sẽ xây dựng', 'VIDEO'::content_type, TRUE, 0),
    (1, 2, 'Bản đồ kiến thức: học gì trước, học gì sau', 'TEXT', TRUE, 12),
    (1, 3, 'Chuẩn bị môi trường và checklist học tập', 'TEXT', FALSE, 15),
    (2, 1, 'Khái niệm cốt lõi cần nắm thật chắc', 'VIDEO', FALSE, 0),
    (2, 2, 'Mẫu code và mô hình tư duy thường dùng', 'TEXT', FALSE, 20),
    (2, 3, 'Lỗi phổ biến và cách tự debug', 'TEXT', FALSE, 18),
    (3, 1, 'Bài thực hành 1: đi từ yêu cầu đến bước nhỏ', 'VIDEO', FALSE, 0),
    (3, 2, 'Bài thực hành 2: hoàn thiện luồng chính', 'TEXT', FALSE, 28),
    (3, 3, 'Checkpoint thực hành: sửa lỗi và giải thích', 'EXERCISE', FALSE, 18),
    (4, 1, 'Phân tích yêu cầu dự án mini', 'VIDEO', FALSE, 0),
    (4, 2, 'Xây dựng từng phần và kiểm tra bằng checklist', 'TEXT', FALSE, 34),
    (4, 3, 'Nộp dự án mini và tự chấm theo rubric', 'EXERCISE', FALSE, 22),
    (5, 1, 'Tổng kết những kiến thức quan trọng nhất', 'VIDEO', FALSE, 0),
    (5, 2, 'Bộ câu hỏi tự kiểm tra cuối khóa', 'EXERCISE', FALSE, 20),
    (5, 3, 'Lộ trình học tiếp và đưa vào portfolio', 'TEXT', FALSE, 15)
) AS v(ch_ord, l_ord, title, ct, is_free, base_dur);

INSERT INTO lessons (id, title, description, "order", chapter_id, content_type, content, mandatory,
                     completion_weight, estimated_duration, is_free, video_url, created_by, updated_by, is_active)
SELECT md5('techhub-lsn-' || c.slug || '-' || t.ch_ord || '-' || t.l_ord)::uuid,
       t.title, 'Bài học thuộc phần "' || ct.title || '".', t.l_ord,
       md5('techhub-ch-' || c.slug || '-' || t.ch_ord)::uuid, t.ct,
       '<p>Nội dung bài học: ' || t.title || '.</p>', TRUE, 1,
       CASE WHEN t.ct = 'VIDEO'
            THEN (CASE c.video_idx WHEN 1 THEN 6081 WHEN 2 THEN 2607 ELSE 3520 END)
            ELSE t.base_dur END,
       t.is_free,
       CASE WHEN t.ct = 'VIDEO' THEN (CASE c.video_idx
            WHEN 1 THEN 'https://archive.org/download/pyvideo_3153___Python_101_Lets_Get_Down_to_Business/3153_Python_101_Lets_Get_Down_to_Business.mp4'
            WHEN 2 THEN 'https://archive.org/download/pyvideo_2837___Docker_101_Meet_the_portable_future_tutorial/2837_Docker_101_Meet_the_portable_future_tutorial.mp4'
            ELSE 'https://archive.org/download/pyvideo_185___pycon-2009-internet-programming-with-python-part-1-of-3/185_pycon-2009-internet-programming-with-python-part-1-of-3.mp4' END)
            ELSE NULL END,
       o.uid, o.uid, 'Y'
FROM tmp_courses c
CROSS JOIN tmp_lesson_tpl t
JOIN tmp_chapter_tpl ct ON ct.ord = t.ch_ord
CROSS JOIN tmp_owner o;

-- 4) Skills + course_skills.
CREATE TEMPORARY TABLE tmp_course_skills ON COMMIT DROP AS
SELECT * FROM (VALUES
    ('linux-sysadmin', 'Linux', 'TOOL'::skill_category),
    ('linux-sysadmin', 'Bash', 'LANGUAGE'),
    ('linux-sysadmin', 'System Administration', 'CONCEPT'),
    ('windows-server-ad', 'Windows Server', 'TOOL'),
    ('windows-server-ad', 'Active Directory', 'TOOL'),
    ('windows-server-ad', 'PowerShell', 'LANGUAGE'),
    ('kubernetes-production', 'Kubernetes', 'TOOL'),
    ('kubernetes-production', 'Docker', 'TOOL'),
    ('kubernetes-production', 'DevOps', 'CONCEPT'),
    ('cicd-pipelines', 'CI/CD', 'CONCEPT'),
    ('cicd-pipelines', 'GitHub Actions', 'TOOL'),
    ('cicd-pipelines', 'Jenkins', 'TOOL'),
    ('terraform-iac', 'Terraform', 'TOOL'),
    ('terraform-iac', 'Infrastructure as Code', 'CONCEPT'),
    ('terraform-iac', 'Cloud', 'CONCEPT'),
    ('data-engineering-spark', 'Apache Spark', 'FRAMEWORK'),
    ('data-engineering-spark', 'Python', 'LANGUAGE'),
    ('data-engineering-spark', 'Data Engineering', 'CONCEPT'),
    ('sql-data-analytics', 'SQL', 'LANGUAGE'),
    ('sql-data-analytics', 'PostgreSQL', 'TOOL'),
    ('sql-data-analytics', 'Data Analytics', 'CONCEPT'),
    ('data-warehousing-etl', 'Data Warehousing', 'CONCEPT'),
    ('data-warehousing-etl', 'ETL', 'CONCEPT'),
    ('data-warehousing-etl', 'Airflow', 'TOOL'),
    ('web-test-automation', 'Selenium', 'TOOL'),
    ('web-test-automation', 'Playwright', 'TOOL'),
    ('web-test-automation', 'Test Automation', 'CONCEPT'),
    ('api-testing', 'API Testing', 'CONCEPT'),
    ('api-testing', 'Postman', 'TOOL'),
    ('api-testing', 'REST Assured', 'TOOL'),
    ('deep-learning-pytorch', 'PyTorch', 'FRAMEWORK'),
    ('deep-learning-pytorch', 'Deep Learning', 'CONCEPT'),
    ('deep-learning-pytorch', 'Python', 'LANGUAGE'),
    ('nlp-transformers', 'NLP', 'CONCEPT'),
    ('nlp-transformers', 'Transformers', 'CONCEPT'),
    ('nlp-transformers', 'Hugging Face', 'TOOL'),
    ('aws-solutions-architect', 'AWS', 'TOOL'),
    ('aws-solutions-architect', 'Cloud Computing', 'CONCEPT'),
    ('aws-solutions-architect', 'Solutions Architecture', 'CONCEPT')
) AS v(slug, skill_name, category);

INSERT INTO skills (id, name, category, is_active)
SELECT md5('techhub-skill-' || lower(s.skill_name))::uuid, s.skill_name, s.category, 'Y'
FROM (SELECT DISTINCT skill_name, category FROM tmp_course_skills) s
ON CONFLICT (name) DO NOTHING;

INSERT INTO course_skills (id, course_id, skill_id)
SELECT md5('techhub-cs-' || cs.slug || '-' || lower(cs.skill_name))::uuid,
       md5('techhub-course-' || cs.slug)::uuid, sk.id
FROM tmp_course_skills cs
JOIN skills sk ON sk.name = cs.skill_name
ON CONFLICT (course_id, skill_id) DO NOTHING;

-- 5) Tags + course_tags. NB: live course_tags has a NOT NULL id column (drift vs
--    techhub.sql) so we supply a deterministic id.
CREATE TEMPORARY TABLE tmp_course_tags ON COMMIT DROP AS
SELECT * FROM (VALUES
    ('linux-sysadmin', 'Linux'), ('linux-sysadmin', 'SysAdmin'), ('linux-sysadmin', 'CLI'),
    ('windows-server-ad', 'Windows'), ('windows-server-ad', 'ActiveDirectory'), ('windows-server-ad', 'SysAdmin'),
    ('kubernetes-production', 'Kubernetes'), ('kubernetes-production', 'DevOps'), ('kubernetes-production', 'Containers'),
    ('cicd-pipelines', 'DevOps'), ('cicd-pipelines', 'CICD'), ('cicd-pipelines', 'Automation'),
    ('terraform-iac', 'DevOps'), ('terraform-iac', 'Terraform'), ('terraform-iac', 'IaC'),
    ('data-engineering-spark', 'Data'), ('data-engineering-spark', 'Spark'), ('data-engineering-spark', 'BigData'),
    ('sql-data-analytics', 'Data'), ('sql-data-analytics', 'SQL'), ('sql-data-analytics', 'Analytics'),
    ('data-warehousing-etl', 'Data'), ('data-warehousing-etl', 'ETL'), ('data-warehousing-etl', 'Warehouse'),
    ('web-test-automation', 'Testing'), ('web-test-automation', 'Automation'), ('web-test-automation', 'QA'),
    ('api-testing', 'Testing'), ('api-testing', 'API'), ('api-testing', 'QA'),
    ('deep-learning-pytorch', 'AI'), ('deep-learning-pytorch', 'DeepLearning'), ('deep-learning-pytorch', 'PyTorch'),
    ('nlp-transformers', 'AI'), ('nlp-transformers', 'NLP'), ('nlp-transformers', 'DeepLearning'),
    ('aws-solutions-architect', 'Cloud'), ('aws-solutions-architect', 'AWS'), ('aws-solutions-architect', 'Architecture')
) AS v(slug, tag_name);

INSERT INTO tags (id, name, is_active)
SELECT md5('techhub-tag-' || lower(t.tag_name))::uuid, t.tag_name, 'Y'
FROM (SELECT DISTINCT tag_name FROM tmp_course_tags) t
ON CONFLICT (name) DO NOTHING;

INSERT INTO course_tags (id, course_id, tag_id)
SELECT md5('techhub-ct-' || ct.slug || '-' || lower(ct.tag_name))::uuid,
       md5('techhub-course-' || ct.slug)::uuid, tg.id
FROM tmp_course_tags ct
JOIN tags tg ON tg.name = ct.tag_name
ON CONFLICT (course_id, tag_id) DO NOTHING;

COMMIT;

SELECT count(*) AS total_active_courses FROM courses WHERE is_active = 'Y';
