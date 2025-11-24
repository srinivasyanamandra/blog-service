package syncqubits.ai.blog.pranuBlog.dto.response;

import lombok.*;
import syncqubits.ai.blog.pranuBlog.dto.nested.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDetailResponse {
    private Long id;
    private String title;
    private String slug;
    private String shareToken;
    private String content;
    private String excerpt;
    private String coverImageUrl;
    private String status;
    private Boolean isPublic;
    private Boolean allowComments;
    private Boolean isFavorite;

    private ViewsData views;
    private LikesData likes;
    private CommentsData comments;
    private MetricsDTO metrics;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String authorName;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ViewsData {
        private Integer count;
        private Integer uniqueViewers;
        private List<ViewEntryDTO> entries;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LikesData {
        private Integer count;
        private List<LikeEntryDTO> entries;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommentsData {
        private Integer count;
        private List<CommentEntryDTO> entries;
    }
}