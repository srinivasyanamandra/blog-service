package syncqubits.ai.blog.pranuBlog.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogoutResponse {
    private Boolean success;
    private String message;
    private String email;
    private LocalDateTime logoutAt;
}