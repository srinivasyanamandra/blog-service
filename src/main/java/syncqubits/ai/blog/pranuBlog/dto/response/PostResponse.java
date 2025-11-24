package syncqubits.ai.blog.pranuBlog.dto.response;

import lombok.*;
import syncqubits.ai.blog.pranuBlog.dto.nested.MetricsDTO;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {
    private Long id;
    private String title;
    private String slug;
    private String shareToken;
    private String excerpt;
    private String coverImageUrl;
    private String status;
    private Boolean isPublic;
    private Boolean allowComments;
    private MetricsDTO metrics;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String authorName;
}