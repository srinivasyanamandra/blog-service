package syncqubits.ai.blog.pranuBlog.dto.nested;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeEntryDTO {
    private Long userId;
    private String guestName;
    private String guestIdentifier;
    private String ipAddress;
    private LocalDateTime likedAt;
}