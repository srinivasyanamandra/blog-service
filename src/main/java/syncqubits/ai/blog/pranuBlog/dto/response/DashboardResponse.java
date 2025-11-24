package syncqubits.ai.blog.pranuBlog.dto.response;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    private Long totalPosts;
    private Long publishedPosts;
    private Long draftPosts;
    private Integer totalViews;
    private Integer totalLikes;
    private Integer totalComments;
    private List<PostResponse> recentPosts;
    private List<PostResponse> popularPosts;
}