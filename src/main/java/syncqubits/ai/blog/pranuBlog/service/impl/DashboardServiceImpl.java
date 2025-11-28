package syncqubits.ai.blog.pranuBlog.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import syncqubits.ai.blog.pranuBlog.dto.response.DashboardResponse;
import syncqubits.ai.blog.pranuBlog.dto.response.PostResponse;
import syncqubits.ai.blog.pranuBlog.dto.response.PagedPostResponse;
import syncqubits.ai.blog.pranuBlog.entity.Post;
import syncqubits.ai.blog.pranuBlog.repository.PostRepository;
import syncqubits.ai.blog.pranuBlog.service.DashboardService;
import syncqubits.ai.blog.pranuBlog.mapper.PostMapper;
import syncqubits.ai.blog.pranuBlog.dto.nested.MetricsDTO;

import java.time.LocalDateTime;
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
    public DashboardResponse getDashboard(
            Long authorId,
            String search,
            String status,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Boolean favoritesOnly,
            String sortBy,
            int page,
            int size
    ) {
        log.info("Fetching dashboard for author: {} with filters - search: {}, status: {}, sortBy: {}",
                authorId, search, status, sortBy);

        // Fetch all posts for aggregate metrics
        List<Post> allPosts = postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId);

        // Calculate aggregate metrics
        long totalPosts = allPosts.size();
        long publishedPosts = allPosts.stream()
                .filter(p -> p.getStatus() == Post.PostStatus.PUBLISHED)
                .count();
        long draftPosts = allPosts.stream()
                .filter(p -> p.getStatus() == Post.PostStatus.DRAFT)
                .count();

        int totalViews = allPosts.stream()
                .mapToInt(p -> getMetricValue(p.getMetrics(), "views"))
                .sum();

        int totalLikes = allPosts.stream()
                .mapToInt(p -> getMetricValue(p.getMetrics(), "likes"))
                .sum();

        int totalComments = allPosts.stream()
                .mapToInt(p -> getMetricValue(p.getMetrics(), "comments"))
                .sum();

        int totalFavorites = allPosts.stream()
                .mapToInt(p -> p.getFavorites() != null ? p.getFavorites().size() : 0)
                .sum();

        // Apply filters to get filtered posts
        List<Post> filteredPosts = applyFilters(allPosts, search, status, fromDate, toDate, favoritesOnly, authorId);

        // Apply sorting
        filteredPosts = applySorting(filteredPosts, sortBy);

        // Apply pagination manually
        PagedPostResponse pagedFilteredPosts = paginatePosts(filteredPosts, page, size);

        // Get recent posts (last 5, unfiltered)
        PagedPostResponse recentPosts = getRecentPosts(allPosts, 5);

        log.info("Dashboard fetched: {} total posts, {} filtered posts, {} views, {} likes, {} comments",
                totalPosts, filteredPosts.size(), totalViews, totalLikes, totalComments);

        return DashboardResponse.builder()
                .totalPosts(totalPosts)
                .publishedPosts(publishedPosts)
                .draftPosts(draftPosts)
                .totalViews(totalViews)
                .totalLikes(totalLikes)
                .totalComments(totalComments)
                .totalFavorites(totalFavorites)
                .recentPosts(recentPosts)
                .filteredPosts(pagedFilteredPosts)
                .build();
    }

    private List<Post> applyFilters(
            List<Post> posts,
            String search,
            String status,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Boolean favoritesOnly,
            Long authorId
    ) {
        return posts.stream()
                .filter(p -> {
                    // Search filter
                    if (search != null && !search.trim().isEmpty()) {
                        String searchLower = search.toLowerCase();
                        boolean matchesTitle = p.getTitle() != null &&
                                p.getTitle().toLowerCase().contains(searchLower);
                        boolean matchesContent = p.getContent() != null &&
                                p.getContent().toLowerCase().contains(searchLower);
                        if (!matchesTitle && !matchesContent) {
                            return false;
                        }
                    }

                    // Status filter
                    if (status != null && !status.trim().isEmpty()) {
                        try {
                            Post.PostStatus postStatus = Post.PostStatus.valueOf(status.toUpperCase());
                            if (p.getStatus() != postStatus) {
                                return false;
                            }
                        } catch (IllegalArgumentException e) {
                            log.warn("Invalid status filter: {}", status);
                        }
                    }

                    // Date range filter
                    if (fromDate != null && p.getCreatedAt().isBefore(fromDate)) {
                        return false;
                    }
                    if (toDate != null && p.getCreatedAt().isAfter(toDate)) {
                        return false;
                    }

                    // Favorites filter
                    if (favoritesOnly != null && favoritesOnly) {
                        if (p.getFavorites() == null ||
                                !p.getFavorites().containsKey(authorId.toString())) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    private List<Post> applySorting(List<Post> posts, String sortBy) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "RECENT";
        }

        switch (sortBy.toUpperCase()) {
            case "TOP_VIEWS":
                return posts.stream()
                        .sorted(Comparator.comparingInt(p ->
                                -getMetricValue(p.getMetrics(), "views")))
                        .collect(Collectors.toList());

            case "TOP_LIKES":
                return posts.stream()
                        .sorted(Comparator.comparingInt(p ->
                                -getMetricValue(p.getMetrics(), "likes")))
                        .collect(Collectors.toList());

            case "TOP_COMMENTS":
                return posts.stream()
                        .sorted(Comparator.comparingInt(p ->
                                -getMetricValue(p.getMetrics(), "comments")))
                        .collect(Collectors.toList());

            case "RECENT":
            default:
                return posts.stream()
                        .sorted(Comparator.comparing(Post::getCreatedAt).reversed())
                        .collect(Collectors.toList());
        }
    }

    private PagedPostResponse paginatePosts(List<Post> posts, int page, int size) {
        int totalElements = posts.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<PostResponse> content;
        if (fromIndex < totalElements) {
            content = posts.subList(fromIndex, toIndex).stream()
                    .map(this::toPostResponse)
                    .collect(Collectors.toList());
        } else {
            content = List.of();
        }

        return PagedPostResponse.builder()
                .content(content)
                .pageNumber(page)
                .pageSize(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .build();
    }

    private PagedPostResponse getRecentPosts(List<Post> posts, int limit) {
        List<PostResponse> recentPostResponses = posts.stream()
                .sorted(Comparator.comparing(Post::getCreatedAt).reversed())
                .limit(limit)
                .map(this::toPostResponse)
                .collect(Collectors.toList());

        return PagedPostResponse.builder()
                .content(recentPostResponses)
                .pageNumber(0)
                .pageSize(limit)
                .totalElements(recentPostResponses.size())
                .totalPages(1)
                .first(true)
                .last(true)
                .build();
    }

    private PostResponse toPostResponse(Post post) {
        PostResponse response = postMapper.toPostResponse(post);
        response.setMetrics(extractMetrics(post.getMetrics()));
        return response;
    }

    private MetricsDTO extractMetrics(Map<String, Object> metricsMap) {
        return MetricsDTO.builder()
                .views(getMetricValue(metricsMap, "views"))
                .likes(getMetricValue(metricsMap, "likes"))
                .comments(getMetricValue(metricsMap, "comments"))
                .build();
    }

    private int getMetricValue(Map<String, Object> metricsMap, String key) {
        if (metricsMap == null || !metricsMap.containsKey(key)) {
            return 0;
        }
        Object value = metricsMap.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
}