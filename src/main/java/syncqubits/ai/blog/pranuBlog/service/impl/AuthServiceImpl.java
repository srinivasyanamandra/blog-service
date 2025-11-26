package syncqubits.ai.blog.pranuBlog.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import syncqubits.ai.blog.pranuBlog.dto.request.LoginRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.ResendOtpRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.SignupRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.VerifyOtpRequest;
import syncqubits.ai.blog.pranuBlog.dto.response.AuthResponse;
import syncqubits.ai.blog.pranuBlog.entity.User;
import syncqubits.ai.blog.pranuBlog.exception.UnauthorizedException;
import syncqubits.ai.blog.pranuBlog.mapper.UserMapper;
import syncqubits.ai.blog.pranuBlog.repository.UserRepository;
import syncqubits.ai.blog.pranuBlog.service.AuthService;
import syncqubits.ai.blog.pranuBlog.service.EmailService;
import syncqubits.ai.blog.pranuBlog.util.JwtUtil;
import syncqubits.ai.blog.pranuBlog.util.OtpGenerator;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final OtpGenerator otpGenerator;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.otp.expiration}")
    private Long otpExpiration;

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        log.info("Attempting to signup user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setIsVerified(false);

        // Generate and set OTP
        String otp = otpGenerator.generateOtp();
        user.setOtp(otp);
        user.setOtpExpiryTime(LocalDateTime.now().plusSeconds(otpExpiration / 1000));

        user = userRepository.save(user);
        log.info("User created successfully with ID: {}", user.getId());

        // Send OTP email
        emailService.sendOtpEmail(user.getEmail(), otp, user.getName());

        return AuthResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .isVerified(false)
                .message("Signup successful. Please check your email for OTP verification.")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        log.info("Verifying OTP for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (user.getIsVerified()) {
            throw new IllegalArgumentException("User already verified");
        }

        if (user.getOtp() == null || user.getOtpExpiryTime() == null) {
            throw new UnauthorizedException("OTP not found. Please request a new one.");
        }

        if (LocalDateTime.now().isAfter(user.getOtpExpiryTime())) {
            throw new UnauthorizedException("OTP has expired. Please request a new one.");
        }

        if (!user.getOtp().equals(request.getOtp())) {
            throw new UnauthorizedException("Invalid OTP");
        }

        // Mark user as verified
        user.setIsVerified(true);
        user.setOtp(null);
        user.setOtpExpiryTime(null);
        user = userRepository.save(user);

        log.info("User verified successfully: {}", user.getEmail());

        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getName());

        // Generate JWT token
        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        AuthResponse response = userMapper.toAuthResponse(user);
        response.setToken(token);
        response.setMessage("Email verified successfully. You can now login.");
        return response;
    }

    @Override
    @Transactional
    public AuthResponse resendOtp(ResendOtpRequest request) {
        log.info("Resending OTP for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (user.getIsVerified()) {
            throw new IllegalArgumentException("User already verified");
        }

        // Generate new OTP
        String otp = otpGenerator.generateOtp();
        user.setOtp(otp);
        user.setOtpExpiryTime(LocalDateTime.now().plusSeconds(otpExpiration / 1000));

        userRepository.save(user);
        log.info("New OTP generated for user: {}", user.getEmail());

        // Send OTP email
        emailService.sendOtpEmail(user.getEmail(), otp, user.getName());

        return AuthResponse.builder()
                .email(user.getEmail())
                .message("New OTP sent successfully. Please check your email.")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Attempting to login user with email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.getIsVerified()) {
            throw new UnauthorizedException("Please verify your email first");
        }

        user.setLastLoginAt(LocalDateTime.now());
        user = userRepository.save(user);

        log.info("User logged in successfully: {}", user.getEmail());

        // Generate JWT token
        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        AuthResponse response = userMapper.toAuthResponse(user);
        response.setToken(token);
        response.setMessage("Login successful");
        return response;
    }

    @Override
    public Long getUserIdFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.extractUserId(token);
    }
}