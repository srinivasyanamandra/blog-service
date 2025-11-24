package syncqubits.ai.blog.pranuBlog.dto.nested;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricsDTO {
    private Integer views;
    private Integer likes;
    private Integer comments;

    @Builder.Default
    private List<ActivityEntry> recentActivity = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActivityEntry {
        private String type;
        private LocalDateTime timestamp;
    }
}
