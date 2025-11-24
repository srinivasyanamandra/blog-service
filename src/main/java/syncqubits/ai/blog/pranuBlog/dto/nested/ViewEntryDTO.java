package syncqubits.ai.blog.pranuBlog.dto.nested;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewEntryDTO {
    private String viewerGuestId;
    private LocalDateTime viewedAt;
    private String referrer;
    private String userAgent;
}