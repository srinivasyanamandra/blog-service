package syncqubits.ai.blog.pranuBlog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import syncqubits.ai.blog.pranuBlog.dto.request.LoginRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.ResendOtpRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.SignupRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.VerifyOtpRequest;
import syncqubits.ai.blog.pranuBlog.dto.response.AuthResponse;
import syncqubits.ai.blog.pranuBlog.dto.response.LogoutResponse;
import syncqubits.ai.blog.pranuBlog.dto.response.TokenValidationResponse;
import syncqubits.ai.blog.pranuBlog.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User authentication with JWT and OTP")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "Register a new user", description = "Create account and send OTP to email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created, OTP sent"),
            @ApiResponse(responseCode = "400", description = "Invalid input or email exists")
    })
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("POST /api/auth/signup - Email: {}", request.getEmail());
        AuthResponse response = authService.signup(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP", description = "Verify email with 6-digit OTP and get JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP verified, JWT token issued"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired OTP")
    })
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("POST /api/auth/verify-otp - Email: {}", request.getEmail());
        AuthResponse response = authService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend OTP", description = "Request new OTP if expired")
    @ApiResponse(responseCode = "200", description = "New OTP sent")
    public ResponseEntity<AuthResponse> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        log.info("POST /api/auth/resend-otp - Email: {}", request.getEmail());
        AuthResponse response = authService.resendOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticate and get JWT token OR OTP if logged out")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful OR OTP sent"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/auth/login - Email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout user", description = "Invalidate JWT token and require OTP on next login")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "Invalid or missing token")
    })
    public ResponseEntity<LogoutResponse> logout(
            @RequestHeader("Authorization") String authHeader) {
        log.info("POST /api/auth/logout");
        LogoutResponse response = authService.logout(authHeader);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate-token")
    @Operation(summary = "Validate JWT token", description = "Check if token is valid and get user info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token validation result"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<TokenValidationResponse> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        log.info("POST /api/auth/validate-token");
        TokenValidationResponse response = authService.validateToken(authHeader);
        return ResponseEntity.ok(response);
    }
}