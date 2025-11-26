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
import syncqubits.ai.blog.pranuBlog.dto.response.LogoutResponse;
import syncqubits.ai.blog.pranuBlog.dto.response.TokenValidationResponse;
import syncqubits.ai.blog.pranuBlog.entity.InvalidatedToken;
import syncqubits.ai.blog.pranuBlog.entity.User;
import syncqubits.ai.blog.pranuBlog.exception.UnauthorizedException;
import syncqubits.ai.blog.pranuBlog.mapper.UserMapper;
import syncqubits.ai.blog.pranuBlog.repository.InvalidatedTokenRepository;
import syncqubits.ai.blog.pranuBlog.repository.UserRepository;
import syncqubits.ai.blog.pranuBlog.service.AuthService;
import syncqubits.ai.blog.pranuBlog.service.EmailService;
import syncqubits.ai.blog.pranuBlog.util.JwtUtil;
import syncqubits.ai.blog.pranuBlog.util.OtpGenerator;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final InvalidatedTokenRepository invalidatedTokenRepository;
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
        user.setRequiresReVerification(false);

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

        if (user.getOtp() == null || user.getOtpExpiryTime() == null) {
            throw new UnauthorizedException("OTP not found. Please request a new one.");
        }

        if (LocalDateTime.now().isAfter(user.getOtpExpiryTime())) {
            throw new UnauthorizedException("OTP has expired. Please request a new one.");
        }

        if (!user.getOtp().equals(request.getOtp())) {
            throw new UnauthorizedException("Invalid OTP");
        }

        // Mark user as verified and clear re-verification flag
        user.setIsVerified(true);
        user.setRequiresReVerification(false);
        user.setOtp(null);
        user.setOtpExpiryTime(null);
        user = userRepository.save(user);

        log.info("User verified successfully: {}", user.getEmail());

        // Send welcome email only if first time verification
        if (user.getLastLoginAt() == null) {
            emailService.sendWelcomeEmail(user.getEmail(), user.getName());
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        AuthResponse response = userMapper.toAuthResponse(user);
        response.setToken(token);
        response.setMessage("Email verified successfully. You are now logged in.");
        return response;
    }

    @Override
    @Transactional
    public AuthResponse resendOtp(ResendOtpRequest request) {
        log.info("Resending OTP for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

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

        // Check if user requires re-verification (logged out)
        if (user.getRequiresReVerification() || !user.getIsVerified()) {
            // Generate new OTP
            String otp = otpGenerator.generateOtp();
            user.setOtp(otp);
            user.setOtpExpiryTime(LocalDateTime.now().plusSeconds(otpExpiration / 1000));
            userRepository.save(user);

            // Send OTP email
            emailService.sendOtpEmail(user.getEmail(), otp, user.getName());

            log.info("User requires verification. OTP sent to: {}", user.getEmail());

            return AuthResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .isVerified(false)
                    .message("Please verify your email. OTP has been sent to your email address.")
                    .build();
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
    @Transactional
    public LogoutResponse logout(String token) {
        log.info("Processing logout request");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // Validate token first
        if (!jwtUtil.validateToken(token)) {
            throw new UnauthorizedException("Invalid or expired token");
        }

        // Extract user info
        Long userId = jwtUtil.extractUserId(token);
        String email = jwtUtil.extractEmail(token);
        Date expiration = jwtUtil.extractExpiration(token);

        // Find user and set re-verification flag
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        user.setRequiresReVerification(true);
        user.setLastLogoutAt(LocalDateTime.now());
        userRepository.save(user);

        // Add token to blacklist
        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .token(token)
                .userId(userId)
                .expiryTime(expiration.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime())
                .reason("LOGOUT")
                .build();

        invalidatedTokenRepository.save(invalidatedToken);

        log.info("User logged out successfully: {}, Token invalidated", email);

        return LogoutResponse.builder()
                .success(true)
                .message("Logout successful. You will need to verify OTP on next login.")
                .email(email)
                .logoutAt(LocalDateTime.now())
                .build();
    }

    @Override
    public Long getUserIdFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtUtil.extractUserId(token);
    }

    @Override
    public TokenValidationResponse validateToken(String token) {
        log.info("Validating JWT token");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            Boolean isValid = jwtUtil.validateToken(token);

            if (isValid) {
                Long userId = jwtUtil.extractUserId(token);
                String email = jwtUtil.extractEmail(token);
                String role = jwtUtil.extractRole(token);
                Date expiration = jwtUtil.extractExpiration(token);

                return TokenValidationResponse.builder()
                        .valid(true)
                        .message("Token is valid")
                        .userId(userId)
                        .email(email)
                        .role(role)
                        .issuedAt(new Date(expiration.getTime() - 86400000))
                        .expiresAt(expiration)
                        .build();
            } else {
                return TokenValidationResponse.builder()
                        .valid(false)
                        .message("Token is invalid or expired")
                        .build();
            }
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return TokenValidationResponse.builder()
                    .valid(false)
                    .message("Token validation failed: " + e.getMessage())
                    .build();
        }
    }
}