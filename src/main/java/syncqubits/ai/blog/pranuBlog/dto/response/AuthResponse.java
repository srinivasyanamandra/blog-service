package syncqubits.ai.blog.pranuBlog.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private Long userId;
    private String name;
    private String email;
    private String role;
    private LocalDateTime lastLoginAt;
    private String message;
}