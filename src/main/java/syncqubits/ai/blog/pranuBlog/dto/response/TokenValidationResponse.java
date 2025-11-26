package syncqubits.ai.blog.pranuBlog.dto.response;

import lombok.*;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenValidationResponse {
    private Boolean valid;
    private String message;
    private Long userId;
    private String email;
    private String role;
    private Date issuedAt;
    private Date expiresAt;
}