package com.techhub.app.aiservice.enums;

public enum AiTaskStatus {
    DRAFT, // AI đã tạo xong, chờ admin review
    APPROVED, // Admin đã approve, đã lưu vào DB chính
    REJECTED, // Admin từ chối draft này
    PENDING, // Đang chờ xử lý (cho các task khác)
    RUNNING, // Đang chạy
    COMPLETED, // Hoàn thành (không phải draft)
    FAILED // Thất bại
}
