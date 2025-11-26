package syncqubits.ai.blog.pranuBlog.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogoutRequest {
    // Token will be extracted from Authorization header
    // No body needed, but keeping for future extensibility
    private String deviceInfo;
}