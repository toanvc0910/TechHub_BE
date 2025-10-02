package com.techhub.app.userservice.service.impl;

import com.techhub.app.userservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    @Async
    public void sendOTPEmail(String email, String otpCode, String purpose) {
        if ("dev".equals(activeProfile)) {
            log.info("DEV MODE: Skipping OTP email to: {} (Purpose: {}, OTP: {})", email, purpose, otpCode);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String htmlContent = createOTPEmailHtml(otpCode, purpose);

            helper.setTo(email);
            helper.setSubject("🔐 TechHub - Mã xác thực OTP");
            helper.setText(htmlContent, true); // true = HTML content
            helper.setFrom(fromEmail);

            mailSender.send(mimeMessage);
            log.info("OTP HTML email sent successfully to: {} for purpose: {}", email, purpose);
        } catch (MessagingException e) {
            log.warn("Failed to send HTML OTP email to: {} - Error: {}", email, e.getMessage());
            // Fallback to simple text email
            sendSimpleOTPEmail(email, otpCode, purpose);
        }
    }

    private String createOTPEmailHtml(String otpCode, String purpose) {
        return "<html>" +
                "<body style='font-family:Arial, sans-serif;'>" +
                "<div style='max-width:600px;margin:0 auto;padding:20px;border:1px solid #ddd;border-radius:5px;'>" +
                "<h2 style='color:#007bff;'>Mã xác thực OTP cho " + purpose + "</h2>" +
                "<p style='font-size:18px;'>Mã OTP của bạn là: <strong style='color:#d9534f;'>" + otpCode + "</strong></p>" +
                "<p>📅 Mã này sẽ hết hạn trong 15 phút.</p>" +
                "<p>Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này hoặc liên hệ với bộ phận hỗ trợ nếu bạn có bất kỳ thắc mắc nào.</p>" +
                "<p>Trân trọng,<br>Đội ngũ TechHub</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private void sendSimpleOTPEmail(String email, String otpCode, String purpose) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("TechHub - Verification Code");
            message.setText(String.format(
                "Hello,\n\n" +
                "Your verification code for %s is: %s\n\n" +
                "This code will expire in 15 minutes.\n\n" +
                "If you didn't request this code, please ignore this email or contact support if you have concerns.\n\n" +
                "Best regards,\n" +
                "TechHub Team",
                purpose, otpCode));
            message.setFrom(fromEmail);

            mailSender.send(message);
            log.info("Fallback: OTP email sent successfully to: {} for purpose: {}", email, purpose);
        } catch (Exception e) {
            log.warn("Fallback: Failed to send OTP email to: {} for purpose: {} - Error: {}", email, purpose, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendWelcomeEmail(String email, String username) {
        if ("dev".equals(activeProfile)) {
            log.info("DEV MODE: Skipping email send to: {} (Welcome message for user: {})", email, username);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String htmlContent = createWelcomeEmailHtml(username);

            helper.setTo(email);
            helper.setSubject("🎉 Chào mừng bạn đến với TechHub!");
            helper.setText(htmlContent, true); // true = HTML content
            helper.setFrom(fromEmail);

            mailSender.send(mimeMessage);
            log.info("Welcome HTML email sent successfully to: {}", email);
        } catch (MessagingException e) {
            log.warn("Failed to send HTML welcome email to: {} - Error: {}", email, e.getMessage());
            // Fallback to simple text email
            sendSimpleWelcomeEmail(email, username);
        }
    }

    private String createWelcomeEmailHtml(String username) {
        return "<html>" +
                "<body style='font-family:Arial, sans-serif;'>" +
                "<div style='max-width:600px;margin:0 auto;padding:20px;border:1px solid #ddd;border-radius:5px;'>" +
                "<h2 style='color:#007bff;'>Chào mừng bạn đến với TechHub, " + username + "!</h2>" +
                "<p>🎉 Chúc mừng bạn đã gia nhập cộng đồng TechHub!</p>" +
                "<p>Tài khoản của bạn đã được tạo thành công. Bạn có thể bắt đầu khám phá các khóa học và tài nguyên học tập ngay bây giờ.</p>" +
                "<p>Nếu bạn có bất kỳ câu hỏi nào, đừng ngần ngại liên hệ với đội ngũ hỗ trợ của chúng tôi.</p>" +
                "<p>Chúc bạn học tập vui vẻ và hiệu quả!</p>" +
                "<p>Trân trọng,<br>Đội ngũ TechHub</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private void sendSimpleWelcomeEmail(String email, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Welcome to TechHub!");
            message.setText(String.format(
                "Hello %s,\n\n" +
                "Welcome to TechHub! Your account has been created successfully.\n\n" +
                "You can now start exploring our courses and learning resources.\n\n" +
                "If you have any questions, feel free to contact our support team.\n\n" +
                "Happy learning!\n\n" +
                "Best regards,\n" +
                "TechHub Team",
                username));
            message.setFrom(fromEmail);

            mailSender.send(message);
            log.info("Fallback: Welcome email sent successfully to: {}", email);
        } catch (Exception e) {
            log.warn("Fallback: Failed to send welcome email to: {} - Error: {}", email, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String email, String otpCode) {
        if ("dev".equals(activeProfile)) {
            log.info("DEV MODE: Skipping password reset email to: {} (Reset code: {})", email, otpCode);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String htmlContent = createPasswordResetEmailHtml(otpCode);

            helper.setTo(email);
            helper.setSubject("🔒 TechHub - Yêu cầu đặt lại mật khẩu");
            helper.setText(htmlContent, true);
            helper.setFrom(fromEmail);

            mailSender.send(mimeMessage);
            log.info("Password reset HTML email sent successfully to: {}", email);
        } catch (MessagingException e) {
            log.warn("Failed to send HTML password reset email to: {} - Error: {}", email, e.getMessage());
            sendSimplePasswordResetEmail(email, otpCode);
        }
    }

    private String createPasswordResetEmailHtml(String otpCode) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family:Arial, sans-serif; background-color:#f4f4f4; margin:0; padding:20px;'>" +
                "<div style='max-width:600px; margin:0 auto; background-color:#ffffff; border-radius:10px; box-shadow:0 0 10px rgba(0,0,0,0.1); overflow:hidden;'>" +

                "<!-- Header -->" +
                "<div style='background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color:white; padding:30px; text-align:center;'>" +
                "<h1 style='margin:0; font-size:28px;'>🔒 Đặt lại mật khẩu</h1>" +
                "<p style='margin:10px 0 0 0; font-size:16px; opacity:0.9;'>TechHub Security</p>" +
                "</div>" +

                "<!-- Content -->" +
                "<div style='padding:40px 30px;'>" +
                "<p style='font-size:16px; color:#333; margin-bottom:20px;'>Xin chào,</p>" +
                "<p style='font-size:16px; color:#333; line-height:1.6; margin-bottom:25px;'>" +
                "Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản TechHub của mình. " +
                "Vui lòng sử dụng mã OTP dưới đây để hoàn tất quá trình:</p>" +

                "<!-- OTP Code Box -->" +
                "<div style='background: linear-gradient(135deg, #ff6b6b 0%, #ff8e53 100%); color:white; padding:25px; border-radius:8px; text-align:center; margin:25px 0;'>" +
                "<p style='margin:0; font-size:14px; opacity:0.9;'>Mã OTP của bạn:</p>" +
                "<h2 style='margin:10px 0 0 0; font-size:32px; font-weight:bold; letter-spacing:3px;'>" + otpCode + "</h2>" +
                "</div>" +

                "<!-- Warning -->" +
                "<div style='background-color:#fff3cd; border:1px solid #ffeaa7; border-radius:5px; padding:15px; margin:20px 0;'>" +
                "<p style='margin:0; color:#856404; font-size:14px;'>" +
                "⚠️ <strong>Lưu ý quan trọng:</strong><br>" +
                "• Mã này sẽ hết hạn sau 15 phút<br>" +
                "• Không chia sẻ mã này với bất kỳ ai<br>" +
                "• Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này" +
                "</p>" +
                "</div>" +

                "<p style='font-size:16px; color:#333; margin-top:30px;'>Trân trọng,<br><strong>Đội ngũ TechHub</strong></p>" +
                "</div>" +

                "<!-- Footer -->" +
                "<div style='background-color:#f8f9fa; padding:20px; text-align:center; border-top:1px solid #e9ecef;'>" +
                "<p style='margin:0; font-size:12px; color:#6c757d;'>© 2025 TechHub. Bảo mật và an toàn.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private void sendSimplePasswordResetEmail(String email, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("TechHub - Password Reset Request");
            message.setText(String.format(
                "Hello,\n\n" +
                "You have requested to reset your password for your TechHub account.\n\n" +
                "Your password reset OTP code is: %s\n\n" +
                "This code will expire in 15 minutes.\n\n" +
                "If you didn't request a password reset, please ignore this email or contact support if you have concerns.\n\n" +
                "Best regards,\n" +
                "TechHub Team",
                otpCode));
            message.setFrom(fromEmail);

            mailSender.send(message);
            log.info("Fallback: Password reset email sent successfully to: {}", email);
        } catch (Exception e) {
            log.warn("Fallback: Failed to send password reset email to: {} - Error: {}", email, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendAccountActivationEmail(String email, String username) {
        if ("dev".equals(activeProfile)) {
            log.info("DEV MODE: Skipping activation email to: {} (User: {})", email, username);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String htmlContent = createActivationEmailHtml(username);

            helper.setTo(email);
            helper.setSubject("✅ TechHub - Tài khoản được kích hoạt!");
            helper.setText(htmlContent, true);
            helper.setFrom(fromEmail);

            mailSender.send(mimeMessage);
            log.info("Account activation HTML email sent successfully to: {}", email);
        } catch (MessagingException e) {
            log.warn("Failed to send HTML activation email to: {} - Error: {}", email, e.getMessage());
            sendSimpleActivationEmail(email, username);
        }
    }

    private String createActivationEmailHtml(String username) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family:Arial, sans-serif; background-color:#f0f8ff; margin:0; padding:20px;'>" +
                "<div style='max-width:600px; margin:0 auto; background-color:#ffffff; border-radius:15px; box-shadow:0 0 20px rgba(0,0,0,0.1); overflow:hidden;'>" +

                "<!-- Header -->" +
                "<div style='background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%); color:white; padding:40px 30px; text-align:center;'>" +
                "<h1 style='margin:0; font-size:32px;'>🎉 Chúc mừng!</h1>" +
                "<p style='margin:15px 0 0 0; font-size:18px; opacity:0.9;'>Tài khoản đã được kích hoạt</p>" +
                "</div>" +

                "<!-- Content -->" +
                "<div style='padding:40px 30px;'>" +
                "<h2 style='color:#4CAF50; margin-bottom:20px; font-size:24px;'>Xin chào " + username + "! ✨</h2>" +
                "<p style='font-size:16px; color:#333; line-height:1.6; margin-bottom:25px;'>" +
                "Tin tuyệt vời! Tài khoản TechHub của bạn đã được kích hoạt thành công. " +
                "Bây giờ bạn có thể truy cập tất cả các tính năng và bắt đầu hành trình học tập của mình.</p>" +

                "<!-- Features Box -->" +
                "<div style='background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%); border-radius:10px; padding:25px; margin:25px 0;'>" +
                "<h3 style='margin:0 0 15px 0; color:#1976d2; font-size:18px;'>🚀 Những gì bạn có thể làm ngay bây giờ:</h3>" +
                "<ul style='margin:0; padding-left:20px; color:#333;'>" +
                "<li style='margin-bottom:8px;'>📚 Khám phá hàng ngàn khóa học chất lượng cao</li>" +
                "<li style='margin-bottom:8px;'>🎯 Tạo lộ trình học tập cá nhân hóa</li>" +
                "<li style='margin-bottom:8px;'>👥 Kết nối với cộng đồng học viên</li>" +
                "<li style='margin-bottom:8px;'>📈 Theo dõi tiến độ học tập của bạn</li>" +
                "<li style='margin-bottom:0;'>🏆 Nhận chứng chỉ hoàn thành khóa học</li>" +
                "</ul>" +
                "</div>" +

                "<!-- Call to Action -->" +
                "<div style='text-align:center; margin:30px 0;'>" +
                "<a href='#' style='background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%); color:white; padding:15px 30px; text-decoration:none; border-radius:25px; font-size:16px; font-weight:bold; display:inline-block;'>" +
                "🎓 Bắt đầu học ngay</a>" +
                "</div>" +

                "<p style='font-size:16px; color:#333; margin-top:30px; text-align:center;'>" +
                "Cảm ơn bạn đã gia nhập TechHub!<br>" +
                "<strong style='color:#4CAF50;'>Đội ngũ TechHub</strong></p>" +
                "</div>" +

                "<!-- Footer -->" +
                "<div style='background-color:#f8f9fa; padding:25px; text-align:center; border-top:1px solid #e9ecef;'>" +
                "<p style='margin:0; font-size:12px; color:#6c757d;'>© 2025 TechHub. Chúc bạn học tập hiệu quả! 🌟</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private void sendSimpleActivationEmail(String email, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("TechHub - Account Activated");
            message.setText(String.format(
                "Hello %s,\n\n" +
                "Great news! Your TechHub account has been activated.\n\n" +
                "You can now access all features and start your learning journey.\n\n" +
                "Thank you for joining TechHub!\n\n" +
                "Best regards,\n" +
                "TechHub Team",
                username));
            message.setFrom(fromEmail);

            mailSender.send(message);
            log.info("Fallback: Account activation email sent successfully to: {}", email);
        } catch (Exception e) {
            log.warn("Fallback: Failed to send activation email to: {} - Error: {}", email, e.getMessage());
        }
    }
}
