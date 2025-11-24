package syncqubits.ai.blog.pranuBlog.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import syncqubits.ai.blog.pranuBlog.dto.request.LoginRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.SignupRequest;
import syncqubits.ai.blog.pranuBlog.dto.response.AuthResponse;
import syncqubits.ai.blog.pranuBlog.entity.User;
import syncqubits.ai.blog.pranuBlog.exception.UnauthorizedException;
import syncqubits.ai.blog.pranuBlog.mapper.UserMapper;
import syncqubits.ai.blog.pranuBlog.repository.UserRepository;
import syncqubits.ai.blog.pranuBlog.service.AuthService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        log.info("Attempting to signup user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        user = userRepository.save(user);
        log.info("User created successfully with ID: {}", user.getId());

        AuthResponse response = userMapper.toAuthResponse(user);
        response.setMessage("Signup successful");
        return response;
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

        user.setLastLoginAt(LocalDateTime.now());
        user = userRepository.save(user);

        log.info("User logged in successfully: {}", user.getEmail());

        AuthResponse response = userMapper.toAuthResponse(user);
        response.setMessage("Login successful");
        return response;
    }
}
