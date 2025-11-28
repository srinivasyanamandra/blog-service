package syncqubits.ai.blog.pranuBlog.dto.response;

import lombok.*;

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
    private Integer totalFavorites;

    // Paginated posts
    private PagedPostResponse recentPosts;
    private PagedPostResponse filteredPosts;
}