package syncqubits.ai.blog.pranuBlog.service;

import syncqubits.ai.blog.pranuBlog.dto.request.LoginRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.ResendOtpRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.SignupRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.VerifyOtpRequest;
import syncqubits.ai.blog.pranuBlog.dto.response.AuthResponse;
import syncqubits.ai.blog.pranuBlog.dto.response.LogoutResponse;
import syncqubits.ai.blog.pranuBlog.dto.response.TokenValidationResponse;

public interface AuthService {
    AuthResponse signup(SignupRequest request);
    AuthResponse verifyOtp(VerifyOtpRequest request);
    AuthResponse resendOtp(ResendOtpRequest request);
    AuthResponse login(LoginRequest request);
    LogoutResponse logout(String token); // NEW
    Long getUserIdFromToken(String token);
    TokenValidationResponse validateToken(String token);
}