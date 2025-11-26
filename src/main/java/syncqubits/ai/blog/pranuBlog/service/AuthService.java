package syncqubits.ai.blog.pranuBlog.service;

import syncqubits.ai.blog.pranuBlog.dto.request.LoginRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.ResendOtpRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.SignupRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.VerifyOtpRequest;
import syncqubits.ai.blog.pranuBlog.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse signup(SignupRequest request);
    AuthResponse verifyOtp(VerifyOtpRequest request);
    AuthResponse resendOtp(ResendOtpRequest request);
    AuthResponse login(LoginRequest request);
    Long getUserIdFromToken(String token);
}