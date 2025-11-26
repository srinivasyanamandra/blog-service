package syncqubits.ai.blog.pranuBlog.service;

public interface EmailService {
    void sendOtpEmail(String to, String otp, String userName);
    void sendWelcomeEmail(String to, String userName);
}