-- Enriches seeded blogs with longer article bodies, inline images, and real system authors.
-- Safe to rerun: updates the same seeded blog IDs.

BEGIN;

WITH authors(author_key, user_id) AS (
    VALUES
        ('admin', 'eb2159ec-5992-4302-af92-e6c9b8b9b1a1'::uuid),
        ('instructor', '9a54a992-5fe9-4a6b-af7e-4a9c92d68fe5'::uuid),
        ('huan', 'f52aa8f8-ef75-4dd4-9317-5fbb7f6faa05'::uuid)
),
blog_enrichment(
    blog_id,
    author_key,
    cover_image,
    inline_image_1,
    inline_image_2,
    lead,
    context_text,
    implementation_text,
    check_1,
    check_2,
    check_3,
    check_4,
    image_caption_1,
    image_caption_2
) AS (
    VALUES
        (
            'd1000001-7a21-4fb1-9a01-000000000001'::uuid,
            'huan',
            'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1551434678-e076c223a692?auto=format&fit=crop&w=1400&q=80',
            'Một lộ trình học AI ứng dụng tốt không bắt đầu bằng việc gom thật nhiều công cụ. Nó bắt đầu bằng việc chọn đúng năng lực cần có, gắn từng phần vào dự án nhỏ, rồi dùng AI như một trợ lý kiểm tra và mở rộng cách nghĩ.',
            'Sinh viên CNTT thường bị quá tải vì AI có quá nhiều nhánh: prompt, embeddings, RAG, chatbot, agent, dữ liệu và triển khai. Nếu không chia tầng, người học dễ nhảy từ khái niệm này sang khái niệm khác mà không tạo ra sản phẩm chạy được.',
            'Trong TechHub, lộ trình nên được chia thành ba lớp. Lớp đầu là nền tảng lập trình và dữ liệu. Lớp thứ hai là cách dùng AI để đọc tài liệu, giải thích lỗi và tạo bài tập. Lớp cuối là xây một sản phẩm có AI thật, có dữ liệu thật, có đánh giá kết quả thay vì chỉ gọi API mẫu.',
            'Chọn một mục tiêu nghề nghiệp cụ thể như frontend có AI assistant, backend RAG hoặc data automation.',
            'Mỗi tuần chỉ học một năng lực chính và kết thúc bằng một demo nhỏ có thể chạy được.',
            'Lưu lại prompt, dữ liệu đầu vào và kết quả để biết AI đúng ở đâu, sai ở đâu.',
            'Sau mỗi chặng, viết lại bằng lời của mình: AI đã giúp gì và phần nào vẫn cần con người kiểm chứng.',
            'Không gian học AI hiệu quả cần có bài tập, dữ liệu thật và phản hồi nhanh.',
            'Một nhóm học tốt nên biến từng khái niệm AI thành sản phẩm nhỏ có thể demo.'
        ),
        (
            'd1000002-7a21-4fb1-9a01-000000000002'::uuid,
            'instructor',
            'https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1559028006-448665bd7c7f?auto=format&fit=crop&w=1400&q=80',
            'Giao diện học tập tốt không phải là giao diện nhiều hiệu ứng. Nó là giao diện giúp người học biết mình đang ở đâu, cần làm gì tiếp theo và có thể quay lại nội dung cũ mà không bị lạc.',
            'React và Next.js cho phép xây UI nhanh, nhưng sản phẩm học trực tuyến cần nhiều trạng thái hơn một landing page: danh sách khóa học, chương, bài học, tiến độ, khóa nội dung, bình luận, bài tập, file và video.',
            'Khi thiết kế TechHub, nên bắt đầu từ workflow học bài thật. Người học mở khóa học, chọn bài, xem video, đọc nội dung, làm bài tập và kiểm tra tiến độ. Component nên phục vụ workflow này trước, sau đó mới tối ưu animation và bố cục trang public.',
            'Tách rõ component hiển thị dữ liệu và component xử lý hành động.',
            'Luôn có trạng thái loading, empty, error và locked cho các màn hình học tập.',
            'Giữ metadata như thời lượng, loại bài học, trạng thái hoàn thành ở vị trí dễ quét.',
            'Kiểm tra responsive bằng nội dung dài, tiêu đề dài và ảnh thật thay vì dữ liệu mẫu quá ngắn.',
            'Frontend học tập cần ưu tiên dòng chảy thao tác hơn trang trí.',
            'Một bài học tốt phải đọc được trên mobile, desktop và khi dữ liệu tải chậm.'
        ),
        (
            'd1000003-7a21-4fb1-9a01-000000000003'::uuid,
            'admin',
            'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=1400&q=80',
            'REST API giúp hệ thống bắt đầu đơn giản, còn microservices giúp tách trách nhiệm khi sản phẩm lớn dần. Điều quan trọng là phải biết mỗi service chịu trách nhiệm nghiệp vụ nào, không tách service chỉ vì muốn kiến trúc nhìn phức tạp.',
            'Một nền tảng học trực tuyến có nhiều miền nghiệp vụ: user, course, blog, file, payment, notification, analytics và AI. Nếu tất cả nằm trong một module, thay đổi nhỏ ở thanh toán có thể ảnh hưởng tới học bài hoặc quản trị nội dung.',
            'Trong TechHub, frontend đi qua proxy, proxy định tuyến tới các service, còn AI service chạy riêng để xử lý vector, chat và sinh nội dung. Cách tách này giúp course-service không cần biết chi tiết embedding, và blog-service không phải gánh logic thanh toán.',
            'Mỗi service phải có database contract rõ: bảng nào là nguồn sự thật, bảng nào chỉ là dữ liệu phục vụ hiển thị.',
            'API trả về response thống nhất để frontend dễ xử lý lỗi.',
            'Các event bất đồng bộ như notification hoặc analytics không nên chặn workflow chính.',
            'Khi demo, giải thích bằng luồng người dùng trước rồi mới nói tới gateway, service discovery và message broker.',
            'Microservices chỉ có ý nghĩa khi ranh giới nghiệp vụ được giải thích rõ.',
            'Kiến trúc backend nên giúp nhóm sửa lỗi nhanh hơn, không phải chỉ để vẽ sơ đồ đẹp hơn.'
        ),
        (
            'd1000004-7a21-4fb1-9a01-000000000004'::uuid,
            'admin',
            'https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1460925895917-afdab827c52f?auto=format&fit=crop&w=1400&q=80',
            'Cá nhân hóa khóa học không có nghĩa là hệ thống đoán ngẫu nhiên người học thích gì. Nó phải dựa trên dữ liệu hành vi, mục tiêu học tập, kỹ năng hiện có và kết quả tương tác với từng bài học.',
            'Nếu chỉ hiển thị cùng một danh sách khóa học cho mọi người, learner mới bắt đầu và learner đã có kinh nghiệm backend sẽ nhận cùng một gợi ý. Điều đó làm giảm giá trị của nền tảng học tập.',
            'TechHub có thể dùng lịch sử học, tiến độ, tag kỹ năng và điểm bài tập để gợi ý bước tiếp theo. AI chỉ nên tham gia sau khi dữ liệu nền đã sạch, vì embedding tốt vẫn có thể trả lời sai nếu dữ liệu nguồn thiếu hoặc không phản ánh trạng thái thật.',
            'Chuẩn hóa dữ liệu course, lesson, skill và enrollment trước khi dùng cho đề xuất.',
            'Phân biệt gợi ý theo mục tiêu học tập, không chỉ theo khóa học phổ biến.',
            'Theo dõi người học bỏ dở ở bài nào để cải thiện nội dung.',
            'Luôn cho người dùng biết vì sao hệ thống gợi ý khóa học đó.',
            'Dashboard dữ liệu giúp instructor biết bài nào cần cải thiện.',
            'Cá nhân hóa tốt bắt đầu từ dữ liệu sạch và có ngữ cảnh.'
        ),
        (
            'd1000005-7a21-4fb1-9a01-000000000005'::uuid,
            'admin',
            'https://images.unsplash.com/photo-1563986768494-4dee2763ff3f?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1563986768494-4dee2763ff3f?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1516321497487-e288fb19713f?auto=format&fit=crop&w=1400&q=80',
            'Bảo mật đăng nhập không chỉ là có form login. Một hệ thống thật cần kiểm soát token, refresh token, vai trò người dùng và quyền truy cập tới từng nhóm API.',
            'Trong nền tảng học trực tuyến, admin, instructor và learner nhìn thấy những màn hình khác nhau. Nếu phân quyền lỏng, learner có thể gọi nhầm API quản trị hoặc instructor có thể sửa nội dung không thuộc quyền của mình.',
            'TechHub cần tách rõ xác thực và phân quyền. Xác thực trả lời câu hỏi người dùng là ai. Phân quyền trả lời câu hỏi người đó được làm gì. Proxy nên chuyển identity đã kiểm chứng xuống service, còn service vẫn giữ một lớp kiểm tra nghiệp vụ quan trọng.',
            'Access token nên sống ngắn để giảm rủi ro khi bị lộ.',
            'Refresh token cần lưu và có thể thu hồi khi người dùng logout hoặc đổi mật khẩu.',
            'Role trên UI chỉ để ẩn nút, role ở backend mới quyết định quyền thật.',
            'Log lỗi 401 và 403 riêng để biết vấn đề là chưa đăng nhập hay thiếu quyền.',
            'Luồng đăng nhập cần được kiểm thử từ frontend tới từng service phía sau.',
            'Một mô hình RBAC rõ giúp demo sản phẩm thuyết phục hơn vì từng actor có phạm vi riêng.'
        ),
        (
            'd1000006-7a21-4fb1-9a01-000000000006'::uuid,
            'huan',
            'https://images.unsplash.com/photo-1605745341112-85968b19335b?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1605745341112-85968b19335b?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=1400&q=80',
            'DevOps cho dự án capstone không cần bắt đầu bằng một pipeline quá phức tạp. Mục tiêu đầu tiên là mọi thành viên chạy được cùng một môi trường và bản demo có thể khởi động lại được khi có lỗi.',
            'Docker giúp đóng gói service, Docker Compose giúp chạy nhiều thành phần cùng nhau, còn CI giúp kiểm tra trước khi deploy. Nếu thiếu những phần này, nhóm thường gặp lỗi máy tôi chạy được nhưng máy server thì không.',
            'Với TechHub, mỗi service có cấu hình riêng nhưng phải cùng đi qua một topology rõ: database, Redis, Kafka, Qdrant, MinIO và proxy. Khi deploy, biến môi trường phải được quản lý ở stack, không hardcode trong code.',
            'Viết healthcheck cho service quan trọng để biết lỗi nằm ở app hay ở network.',
            'Tách build image và deploy stack để có thể rollback nhanh.',
            'Không dùng dữ liệu demo quá trống vì UI sẽ nhìn như chưa hoàn thiện.',
            'Ghi lại command kiểm tra sau deploy: login, gọi API, mở course, mở blog và kiểm tra AI.',
            'Môi trường demo ổn định làm giảm rủi ro trong buổi bảo vệ.',
            'Docker chỉ có giá trị khi cả nhóm dùng cùng một cách chạy và cùng một bộ biến môi trường.'
        ),
        (
            'd1000007-7a21-4fb1-9a01-000000000007'::uuid,
            'instructor',
            'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=1400&q=80',
            'Một bài học lập trình tốt phải đưa người học từ hiểu khái niệm tới làm được việc. Nếu chỉ có đoạn văn ngắn và vài gạch đầu dòng, learner khó biết phải luyện gì sau khi đọc xong.',
            'Bài học tương tác nên có mục tiêu rõ, ví dụ thực hành, lỗi thường gặp và tiêu chí hoàn thành. Video, tài liệu và bài tập phải bổ sung cho nhau thay vì lặp lại cùng một nội dung.',
            'Trong TechHub, instructor có thể thiết kế lesson theo nhịp: xem video, đọc giải thích, làm bài tập nhỏ, xem phản hồi và lưu tiến độ. Cấu trúc này giúp hệ thống đo được người học đang tiến bộ ở đâu.',
            'Mỗi lesson nên có một kết quả cụ thể như viết được component hoặc gọi được API.',
            'Bài tập phải gắn với nội dung vừa học, không nhảy sang kỹ năng khác.',
            'Feedback nên nói rõ sai ở bước nào và gợi ý sửa từng phần.',
            'Nếu có AI hỗ trợ, AI phải dựa trên lesson context thay vì trả lời chung chung.',
            'Hình ảnh trong bài giúp người học hình dung workflow trước khi code.',
            'Tương tác tốt là khi người học biết phải làm gì tiếp theo ngay sau khi đọc xong.'
        ),
        (
            'd1000008-7a21-4fb1-9a01-000000000008'::uuid,
            'huan',
            'https://images.unsplash.com/photo-1677442136019-21780ecad995?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1677442136019-21780ecad995?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1535223289827-42f1e9919769?auto=format&fit=crop&w=1400&q=80',
            'AI Chat có thể giúp học lập trình nhanh hơn, nhưng chỉ khi người học biết đặt câu hỏi đúng và biết kiểm chứng câu trả lời bằng code, tài liệu hoặc dữ liệu hệ thống.',
            'Vấn đề thường gặp là learner hỏi quá rộng như hãy dạy em React. Câu hỏi như vậy làm AI trả lời dài nhưng khó áp dụng. Câu hỏi tốt nên có mục tiêu, ngữ cảnh, lỗi cụ thể và kết quả mong muốn.',
            'Trong TechHub, AI Chat nên nhận thêm context từ khóa học, lesson và file người dùng. Khi có dữ liệu nền, AI có thể giải thích đúng bài đang học, nhắc lại bước còn thiếu và tránh suy đoán ngoài nội dung hệ thống.',
            'Đưa code lỗi, thông báo lỗi và mục tiêu mong muốn vào cùng một câu hỏi.',
            'Yêu cầu AI giải thích theo từng bước, sau đó tự chạy lại để kiểm chứng.',
            'Không copy kết quả nếu chưa hiểu lý do sửa.',
            'Lưu các prompt hiệu quả để tái sử dụng cho bài học sau.',
            'AI Chat hữu ích nhất khi có context thật từ lesson và file học tập.',
            'Người học vẫn cần kiểm chứng kết quả bằng test, log và hành vi của ứng dụng.'
        ),
        (
            'd1000009-7a21-4fb1-9a01-000000000009'::uuid,
            'instructor',
            'https://images.unsplash.com/photo-1460925895917-afdab827c52f?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1460925895917-afdab827c52f?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=1400&q=80',
            'Tốc độ tải trang ảnh hưởng trực tiếp tới cảm giác sản phẩm đã hoàn thiện hay chưa. Nền tảng học trực tuyến càng nhiều ảnh, video và dữ liệu thì càng cần tối ưu từ đầu.',
            'Người học không quan tâm service nào chậm. Họ chỉ thấy trang khóa học mở lâu, ảnh không hiện hoặc video bị giật. Vì vậy tối ưu performance phải đi từ trải nghiệm người dùng rồi truy ngược xuống API và asset.',
            'TechHub có nhiều nơi cần chú ý: SSR cho trang public, cache dữ liệu blog, kích thước ảnh khóa học, preload video hợp lý và tránh gọi lại API khi không cần. Nếu chỉ tối ưu một component, toàn trang vẫn có thể chậm.',
            'Ảnh nên dùng kích thước phù hợp với vị trí hiển thị, không dùng ảnh quá lớn cho card nhỏ.',
            'Danh sách blog và course nên có cache ngắn để giảm tải API.',
            'Skeleton phải phản ánh layout thật để không gây nhảy bố cục.',
            'Đo tốc độ bằng dữ liệu thật, không chỉ đo khi database gần như rỗng.',
            'Ảnh minh họa giúp card blog giàu thông tin hơn nhưng cần được tối ưu.',
            'Performance tốt là khi người dùng thao tác mượt dù hệ thống có dữ liệu thật.'
        ),
        (
            'd100000a-7a21-4fb1-9a01-000000000010'::uuid,
            'instructor',
            'https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1521737604893-d14cc237f11d?auto=format&fit=crop&w=1400&q=80',
            'Quản trị nội dung khóa học cần giống một quy trình xuất bản thật: có bản nháp, có kiểm tra, có tài nguyên đi kèm và có người chịu trách nhiệm rõ ràng.',
            'Nếu instructor sửa trực tiếp nội dung đang public mà không có quy trình, learner có thể gặp bài học thiếu video, thiếu file hoặc bài tập không khớp với phần giải thích. Điều này làm trải nghiệm học bị đứt đoạn.',
            'Trong TechHub, workflow nên bắt đầu từ draft, sau đó thêm chapter, lesson, video, asset, bài tập và cuối cùng mới publish. Admin có thể giám sát chất lượng, còn instructor tập trung vào nội dung của mình.',
            'Không publish bài học nếu chưa có mục tiêu, nội dung chính và tài nguyên tối thiểu.',
            'Mỗi thay đổi lớn nên kiểm tra ở màn public trước khi gửi learner.',
            'Asset như video, hình ảnh và tài liệu phải có tiêu đề rõ để quản trị dễ tìm lại.',
            'Author của blog hoặc course phải là user thật để audit và hiển thị đáng tin.',
            'Quy trình nội dung tốt làm sản phẩm giống nền tảng thật hơn là demo tĩnh.',
            'Bản nháp không phải phụ kiện, nó là lớp an toàn cho nội dung đang được biên tập.'
        ),
        (
            'd100000b-7a21-4fb1-9a01-000000000011'::uuid,
            'admin',
            'https://images.unsplash.com/photo-1553877522-43269d4ea984?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1553877522-43269d4ea984?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1552664730-d307ca884978?auto=format&fit=crop&w=1400&q=80',
            'Theo dõi tiến độ học tập không nên chỉ là phần trăm hoàn thành. Nó phải giúp learner hiểu mình đã học gì, còn thiếu gì và nên làm gì tiếp theo.',
            'Một course có nhiều bài học, video, bài tập và tài nguyên. Nếu không có dashboard tiến độ, người học dễ quên mình đang ở chương nào hoặc vì sao bài tiếp theo còn bị khóa.',
            'TechHub có thể dùng progress từng lesson, completion weight, trạng thái chương và lịch sử hoạt động để tạo ra gợi ý tiếp theo. Phần AI chỉ nên dựa vào những dữ liệu này sau khi chúng đã được đồng bộ đúng.',
            'Hiển thị tiến độ theo chương để người học biết điểm nghẽn cụ thể.',
            'Phân biệt đã xem video, đã đọc nội dung và đã nộp bài tập.',
            'Gợi ý bước tiếp theo phải dẫn tới lesson hoặc course cụ thể.',
            'Instructor nên thấy thống kê bài nào nhiều learner bỏ dở để cải thiện nội dung.',
            'Tiến độ học tập giúp biến một danh sách bài học thành hành trình có định hướng.',
            'Dữ liệu tốt làm đề xuất học tiếp đáng tin hơn và giảm cảm giác học mò.'
        ),
        (
            'd100000c-7a21-4fb1-9a01-000000000012'::uuid,
            'admin',
            'https://images.unsplash.com/photo-1556761175-b413da4baf72?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1556761175-b413da4baf72?auto=format&fit=crop&w=1400&q=80',
            'https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=1400&q=80',
            'Demo sản phẩm trước hội đồng cần kể được câu chuyện sản phẩm, không chỉ mở từng màn hình. Người xem phải hiểu ai dùng hệ thống, dùng để làm gì và dữ liệu thật đang chạy qua đâu.',
            'Một demo yếu thường có ba dấu hiệu: dữ liệu quá ít, nội dung không có hình ảnh, và tài khoản hiển thị không khớp với user thật trong hệ thống. Những lỗi nhỏ này làm sản phẩm giống bản mẫu hơn là nền tảng có thể sử dụng.',
            'Với TechHub, kịch bản nên đi từ learner xem blog và khóa học, đăng nhập, học bài, dùng AI, sau đó chuyển sang instructor quản trị nội dung và admin kiểm tra hệ thống. Mỗi bước cần có dữ liệu đủ dày để UI thể hiện đúng trạng thái.',
            'Chuẩn bị sẵn tài khoản admin, instructor và learner để demo role rõ ràng.',
            'Kiểm tra course, lesson, blog, file, payment và AI trước ngày trình bày.',
            'Dữ liệu demo phải có ảnh, video, tác giả thật và nội dung đủ dài.',
            'Có phương án dự phòng như ảnh chụp màn hình hoặc checklist nếu mạng chậm.',
            'Demo tốt là demo có câu chuyện nghiệp vụ rõ và dữ liệu không bị rỗng.',
            'Trước khi trình bày, hãy xem sản phẩm bằng mắt của người dùng cuối chứ không chỉ bằng mắt developer.'
        )
),
enriched AS (
    SELECT e.*, a.user_id AS author_id
    FROM blog_enrichment e
    JOIN authors a ON a.author_key = e.author_key
)
UPDATE blogs b
SET
    author_id = e.author_id,
    thumbnail = e.cover_image,
    content = concat(
        '<p><strong>', e.lead, '</strong></p>',
        '<p>', e.context_text, '</p>',
        '<figure><img src="', e.inline_image_1, '" alt="', e.image_caption_1, '" /><figcaption>', e.image_caption_1, '</figcaption></figure>',
        '<h2>Bối cảnh thực tế</h2>',
        '<p>', e.implementation_text, '</p>',
        '<p>Điểm quan trọng là nội dung blog không nên dừng ở mức mô tả ngắn. Một bài viết trên nền tảng học tập cần có tình huống, lý do, cách triển khai, lỗi thường gặp và checklist hành động để người đọc có thể áp dụng ngay sau khi đọc.</p>',
        '<h2>Cách áp dụng trong TechHub</h2>',
        '<p>Trong sản phẩm thật, bài viết phải gắn với dữ liệu đang có: user thật, khóa học thật, lesson thật, file thật và ảnh minh họa thật. Khi các phần này đồng bộ với nhau, trang blog không còn giống nội dung placeholder mà trở thành một phần của hệ sinh thái học tập.</p>',
        '<ul>',
        '<li>', e.check_1, '</li>',
        '<li>', e.check_2, '</li>',
        '<li>', e.check_3, '</li>',
        '<li>', e.check_4, '</li>',
        '</ul>',
        '<figure><img src="', e.inline_image_2, '" alt="', e.image_caption_2, '" /><figcaption>', e.image_caption_2, '</figcaption></figure>',
        '<h2>Lỗi thường gặp khi triển khai</h2>',
        '<p>Lỗi đầu tiên là dùng dữ liệu quá ngắn nên card blog, trang chi tiết và phần đọc thêm không đủ nội dung để kiểm tra layout. Lỗi thứ hai là chỉ có thumbnail bên ngoài nhưng body không có hình ảnh, khiến bài viết nhìn khô và khó đọc. Lỗi thứ ba là author không khớp với tài khoản thật, làm người dùng không biết bài viết thuộc về ai.</p>',
        '<h2>Checklist trước khi xuất bản</h2>',
        '<p>Trước khi publish, hãy mở lại bài viết ở trang public, kiểm tra ảnh trong nội dung, ảnh cover, tên tác giả, thời gian đọc, tag và phần attachment. Nếu một trong các phần này trống, bài viết vẫn còn cảm giác là dữ liệu seed chứ chưa phải nội dung thật.</p>',
        '<h2>Kết luận</h2>',
        '<p>Bài viết tốt cần làm ba việc cùng lúc: giải thích vấn đề rõ ràng, đưa ra cách làm có thể hành động và thể hiện được độ hoàn thiện của sản phẩm. Với TechHub, blog không chỉ là mục tin tức mà còn là nơi chứng minh hệ thống có dữ liệu, tác giả, hình ảnh và workflow xuất bản thật.</p>'
    ),
    attachments = jsonb_build_array(
        jsonb_build_object(
            'type', 'image',
            'url', e.inline_image_1,
            'caption', e.image_caption_1,
            'altText', e.image_caption_1
        ),
        jsonb_build_object(
            'type', 'image',
            'url', e.inline_image_2,
            'caption', e.image_caption_2,
            'altText', e.image_caption_2
        )
    ),
    created_by = e.author_id,
    updated_by = e.author_id,
    updated = NOW()
FROM enriched e
WHERE b.id = e.blog_id
  AND b.is_active = 'Y';

COMMIT;
