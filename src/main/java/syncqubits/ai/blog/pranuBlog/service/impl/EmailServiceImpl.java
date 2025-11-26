package syncqubits.ai.blog.pranuBlog.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import syncqubits.ai.blog.pranuBlog.service.EmailService;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    @Override
    @Async
    public void sendOtpEmail(String to, String otp, String userName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Your OTP for Pranu Blog Verification");

            String htmlContent = buildOtpEmailContent(userName, otp);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to: {}", to, e);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    @Override
    @Async
    public void sendWelcomeEmail(String to, String userName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Welcome to Pranu Blog!");

            String htmlContent = buildWelcomeEmailContent(userName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}", to, e);
        }
    }

    private String buildOtpEmailContent(String userName, String otp) {
        String template = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .otp-box { background: white; border: 2px dashed #667eea; padding: 20px; margin: 20px 0; text-align: center; border-radius: 8px; }
                    .otp-code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 8px; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                    .warning { color: #e74c3c; font-size: 14px; margin-top: 15px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 Email Verification</h1>
                    </div>
                    <div class="content">
                        <p>Hi <strong>${USER}</strong>,</p>
                        <p>Thank you for signing up with Pranu Blog! To complete your registration, please use the following OTP:</p>
                        
                        <div class="otp-box">
                            <div class="otp-code">${OTP}</div>
                        </div>
                        
                        <p>This OTP will expire in <strong>5 minutes</strong>.</p>
                        
                        <p class="warning">⚠️ If you didn't request this code, please ignore this email.</p>
                        
                        <p>Best regards,<br><strong>Pranu Blog Team</strong></p>
                    </div>
                    <div class="footer">
                        <p>© 2024 Pranu Blog. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """;

        return template.replace("${USER}", htmlEscape(userName))
                .replace("${OTP}", htmlEscape(otp));
    }

    private String buildWelcomeEmailContent(String userName) {
        String template = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Welcome to Pranu Blog!</h1>
                    </div>
                    <div class="content">
                        <p>Hi <strong>${USER}</strong>,</p>
                        <p>Your account has been successfully verified! You can now start creating and sharing your amazing blog posts.</p>
                        <p>Happy blogging!</p>
                        <p>Best regards,<br><strong>Pranu Blog Team</strong></p>
                    </div>
                    <div class="footer">
                        <p>© 2024 Pranu Blog. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """;

        return template.replace("${USER}", htmlEscape(userName));
    }

    /** Minimal HTML-escaping helper to avoid HTML injection in simple templates. */
    private static String htmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

}