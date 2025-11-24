package syncqubits.ai.blog.pranuBlog.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import syncqubits.ai.blog.pranuBlog.dto.response.DashboardResponse;
import syncqubits.ai.blog.pranuBlog.dto.response.PostResponse;
import syncqubits.ai.blog.pranuBlog.entity.Post;
import syncqubits.ai.blog.pranuBlog.repository.PostRepository;
import syncqubits.ai.blog.pranuBlog.service.DashboardService;
import syncqubits.ai.blog.pranuBlog.mapper.PostMapper;
import syncqubits.ai.blog.pranuBlog.dto.nested.MetricsDTO;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {
    private final PostRepository postRepository;
    private final PostMapper postMapper;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long authorId) {
        log.info("Fetching dashboard for author: {}", authorId);

        List<Post> allPosts = postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId);

        long totalPosts = allPosts.size();
        long publishedPosts = allPosts.stream()
                .filter(p -> p.getStatus() == Post.PostStatus.PUBLISHED)
                .count();
        long draftPosts = allPosts.stream()
                .filter(p -> p.getStatus() == Post.PostStatus.DRAFT)
                .count();

        // Calculate total metrics
        int totalViews = allPosts.stream()
                .mapToInt(p -> (Integer) p.getMetrics().getOrDefault("views", 0))
                .sum();

        int totalLikes = allPosts.stream()
                .mapToInt(p -> (Integer) p.getMetrics().getOrDefault("likes", 0))
                .sum();

        int totalComments = allPosts.stream()
                .mapToInt(p -> (Integer) p.getMetrics().getOrDefault("comments", 0))
                .sum();

        // Get recent posts (last 5)
        List<PostResponse> recentPosts = allPosts.stream()
                .limit(5)
                .map(this::toPostResponse)
                .collect(Collectors.toList());

        // Get popular posts (by views, top 5)
        List<PostResponse> popularPosts = allPosts.stream()
                .sorted(Comparator.comparingInt(p ->
                        -((Integer) p.getMetrics().getOrDefault("views", 0))))
                .limit(5)
                .map(this::toPostResponse)
                .collect(Collectors.toList());

        log.info("Dashboard fetched: {} posts, {} views, {} likes, {} comments",
                totalPosts, totalViews, totalLikes, totalComments);

        return DashboardResponse.builder()
                .totalPosts(totalPosts)
                .publishedPosts(publishedPosts)
                .draftPosts(draftPosts)
                .totalViews(totalViews)
                .totalLikes(totalLikes)
                .totalComments(totalComments)
                .recentPosts(recentPosts)
                .popularPosts(popularPosts)
                .build();
    }

    private PostResponse toPostResponse(Post post) {
        PostResponse response = postMapper.toPostResponse(post);
        response.setMetrics(extractMetrics(post.getMetrics()));
        return response;
    }

    private MetricsDTO extractMetrics(Map<String, Object> metricsMap) {
        return MetricsDTO.builder()
                .views((Integer) metricsMap.getOrDefault("views", 0))
                .likes((Integer) metricsMap.getOrDefault("likes", 0))
                .comments((Integer) metricsMap.getOrDefault("comments", 0))
                .build();
    }
}
