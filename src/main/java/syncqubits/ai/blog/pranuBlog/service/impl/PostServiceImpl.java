package syncqubits.ai.blog.pranuBlog.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import syncqubits.ai.blog.pranuBlog.dto.nested.CommentEntryDTO;
import syncqubits.ai.blog.pranuBlog.dto.nested.LikeEntryDTO;
import syncqubits.ai.blog.pranuBlog.dto.nested.MetricsDTO;
import syncqubits.ai.blog.pranuBlog.dto.nested.ViewEntryDTO;
import syncqubits.ai.blog.pranuBlog.dto.request.CommentRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.CreatePostRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.ReplyRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.UpdatePostRequest;
import syncqubits.ai.blog.pranuBlog.dto.response.CommentResponse;
import syncqubits.ai.blog.pranuBlog.dto.response.PostDetailResponse;
import syncqubits.ai.blog.pranuBlog.dto.response.PostResponse;
import syncqubits.ai.blog.pranuBlog.entity.Post;
import syncqubits.ai.blog.pranuBlog.entity.User;
import syncqubits.ai.blog.pranuBlog.exception.ResourceNotFoundException;
import syncqubits.ai.blog.pranuBlog.exception.UnauthorizedException;
import syncqubits.ai.blog.pranuBlog.mapper.PostMapper;
import syncqubits.ai.blog.pranuBlog.repository.PostRepository;
import syncqubits.ai.blog.pranuBlog.repository.UserRepository;
import syncqubits.ai.blog.pranuBlog.service.PostService;
import syncqubits.ai.blog.pranuBlog.util.GeoLocationUtil;
import syncqubits.ai.blog.pranuBlog.util.IpAddressUtil;
import syncqubits.ai.blog.pranuBlog.util.TokenGenerator;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;
    private final TokenGenerator tokenGenerator;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final IpAddressUtil ipAddressUtil;
    private final GeoLocationUtil geoLocationUtil;


    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> listPublicPosts() {
        log.info("Fetching all public posts");

        List<Post> posts = postRepository.findByStatusAndIsPublicOrderByCreatedAtDesc(
                Post.PostStatus.PUBLISHED, true);

        return posts.stream()
                .map(this::toPostResponse)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public PostResponse createPost(Long authorId, CreatePostRequest request) {
        log.info("Creating post for author: {}", authorId);

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found"));

        if (request.getSlug() != null && postRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("Slug already exists");
        }

        Post post = postMapper.toEntity(request);
        post.setAuthor(author);

        // Initialize JSON fields
        post.setViews(initializeViews());
        post.setLikes(initializeLikes());
        post.setFavorites(initializeFavorites());
        post.setComments(initializeComments());
        post.setMetrics(initializeMetrics());

        post = postRepository.save(post);
        log.info("Post created with ID: {}", post.getId());

        return toPostResponse(post);
    }

    @Override
    @Transactional
    public PostResponse updatePost(Long authorId, Long postId, UpdatePostRequest request) {
        log.info("Updating post {} for author: {}", postId, authorId);

        Post post = getPostByIdAndAuthor(postId, authorId);

        if (request.getSlug() != null && !request.getSlug().equals(post.getSlug())) {
            if (postRepository.existsBySlug(request.getSlug())) {
                throw new IllegalArgumentException("Slug already exists");
            }
        }

        postMapper.updateEntityFromRequest(request, post);
        post = postRepository.save(post);

        log.info("Post updated successfully: {}", postId);
        return toPostResponse(post);
    }

    @Override
    @Transactional
    public PostResponse publishPost(Long authorId, Long postId) {
        log.info("Publishing post {} for author: {}", postId, authorId);

        Post post = getPostByIdAndAuthor(postId, authorId);

        if (post.getShareToken() == null) {
            post.setShareToken(tokenGenerator.generateShareToken());
        }

        post.setStatus(Post.PostStatus.PUBLISHED);
        post.setIsPublic(true);
        post = postRepository.save(post);

        log.info("Post published with share token: {}", post.getShareToken());
        return toPostResponse(post);
    }

    @Override
    @Transactional
    public PostResponse unpublishPost(Long authorId, Long postId) {
        log.info("Unpublishing post {} for author: {}", postId, authorId);

        Post post = getPostByIdAndAuthor(postId, authorId);
        post.setStatus(Post.PostStatus.DRAFT);
        post.setIsPublic(false);
        post = postRepository.save(post);

        log.info("Post unpublished: {}", postId);
        return toPostResponse(post);
    }

    @Override
    @Transactional
    public void deletePost(Long authorId, Long postId) {
        log.info("Deleting post {} for author: {}", postId, authorId);

        Post post = getPostByIdAndAuthor(postId, authorId);
        postRepository.delete(post);

        log.info("Post deleted: {}", postId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getMyPosts(Long authorId) {
        log.info("Fetching posts for author: {}", authorId);

        List<Post> posts = postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId);
        return posts.stream()
                .map(this::toPostResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PostDetailResponse getPostById(Long authorId, Long postId) {
        log.info("Fetching post {} for author: {}", postId, authorId);

        Post post = getPostByIdAndAuthor(postId, authorId);
        return toPostDetailResponse(post);
    }

    @Override
    @Transactional
    public PostDetailResponse getPublicPost(String shareToken, String guestName, String viewerGuestId,
                                            String referrer, String userAgent, HttpServletRequest request) {
        log.info("Fetching public post with token: {}", shareToken);

        Post post = postRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getIsPublic()) {
            throw new UnauthorizedException("Post is not public");
        }

        // Record view with IP and geolocation
        recordView(post, guestName, viewerGuestId, referrer, userAgent, request);

        return toPostDetailResponse(post);
    }

    @Override
    @Transactional
    public PostResponse toggleFavorite(Long authorId, Long postId) {
        log.info("Toggling favorite for post {} by author: {}", postId, authorId);

        Post post = getPostByIdAndAuthor(postId, authorId);

        Map<String, Object> favorites = post.getFavorites();
        Boolean isFavorite = (Boolean) favorites.getOrDefault("is_favorite", false);
        favorites.put("is_favorite", !isFavorite);

        post.setFavorites(favorites);
        post = postRepository.save(post);

        log.info("Favorite toggled to: {}", !isFavorite);
        return toPostResponse(post);
    }

    @Override
    @Transactional
    public PostResponse toggleLike(String shareToken, Long userId, String guestName,
                                   String guestIdentifier, HttpServletRequest request) {
        log.info("Toggling like for post with token: {}", shareToken);

        Post post = postRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        Map<String, Object> likes = post.getLikes();
        List<Map<String, Object>> entries = (List<Map<String, Object>>)
                likes.getOrDefault("entries", new ArrayList<>());

        // Find existing like
        Map<String, Object> existingLike = null;
        int existingIndex = -1;

        for (int i = 0; i < entries.size(); i++) {
            Map<String, Object> entry = entries.get(i);
            boolean isMatch = false;

            if (userId != null && entry.containsKey("user_id")) {
                isMatch = Objects.equals(entry.get("user_id"), userId.intValue());
            } else if (guestIdentifier != null && entry.containsKey("guest_identifier")) {
                isMatch = Objects.equals(entry.get("guest_identifier"), guestIdentifier);
            }

            if (isMatch) {
                existingLike = entry;
                existingIndex = i;
                break;
            }
        }

        String message;
        if (existingLike != null) {
            // Unlike - remove existing like
            entries.remove(existingIndex);
            message = "Post unliked successfully";
            log.info("Removed like from post");
        } else {
            // Like - add new like
            String ipAddress = ipAddressUtil.getClientIpAddress(request);

            Map<String, Object> likeEntry = new HashMap<>();
            if (userId != null) {
                likeEntry.put("user_id", userId);
            } else {
                likeEntry.put("guest_name", guestName);
                likeEntry.put("guest_identifier", guestIdentifier);
            }
            likeEntry.put("ip_address", ipAddress);
            likeEntry.put("liked_at", LocalDateTime.now().toString());

            entries.add(likeEntry);
            message = "Post liked successfully";
            log.info("Added like to post");
        }

        likes.put("entries", entries);
        likes.put("count", entries.size());

        post.setLikes(likes);
        updateMetrics(post);
        post = postRepository.save(post);

        PostResponse response = toPostResponse(post);
        // Add custom message field if needed
        log.info(message);
        return response;
    }

    @Override
    @Transactional
    public CommentResponse addComment(String shareToken, CommentRequest request, HttpServletRequest httpRequest) {
        log.info("Adding comment to post with token: {}", shareToken);

        Post post = postRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getAllowComments()) {
            throw new IllegalArgumentException("Comments are disabled for this post");
        }

        Map<String, Object> comments = post.getComments();
        List<Map<String, Object>> entries = (List<Map<String, Object>>)
                comments.getOrDefault("entries", new ArrayList<>());

        String commentId = "c" + UUID.randomUUID().toString().substring(0, 8);
        String ipAddress = ipAddressUtil.getClientIpAddress(httpRequest);

        Map<String, Object> commentEntry = new HashMap<>();
        commentEntry.put("comment_id", commentId);
        commentEntry.put("guest_name", request.getGuestName());

        if (request.getUserId() != null) {
            commentEntry.put("user_id", request.getUserId());
        }
        if (request.getGuestIdentifier() != null) {
            commentEntry.put("guest_identifier", request.getGuestIdentifier());
        }

        commentEntry.put("ip_address", ipAddress);
        commentEntry.put("content", request.getContent());
        commentEntry.put("created_at", LocalDateTime.now().toString());
        commentEntry.put("replies", new ArrayList<>());

        entries.add(commentEntry);
        comments.put("entries", entries);
        comments.put("count", calculateTotalComments(entries));

        post.setComments(comments);
        updateMetrics(post);
        post = postRepository.save(post);

        CommentEntryDTO commentDTO = mapToCommentEntryDTO(commentEntry);

        log.info("Comment added successfully with ID: {}", commentId);
        return CommentResponse.builder()
                .message("Comment added successfully")
                .comment(commentDTO)
                .build();
    }


    @Override
    @Transactional
    public CommentResponse addReply(String shareToken, ReplyRequest request) {
        log.info("Adding reply to comment {} in post with token: {}",
                request.getParentCommentId(), shareToken);

        Post post = postRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getAllowComments()) {
            throw new IllegalArgumentException("Comments are disabled for this post");
        }

        Map<String, Object> comments = post.getComments();
        List<Map<String, Object>> entries = (List<Map<String, Object>>)
                comments.getOrDefault("entries", new ArrayList<>());

        Map<String, Object> parentComment = findCommentById(entries, request.getParentCommentId());
        if (parentComment == null) {
            throw new ResourceNotFoundException("Parent comment not found");
        }

        String replyId = "r" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> replyEntry = new HashMap<>();
        replyEntry.put("comment_id", replyId);
        if (request.getUserId() != null) {
            replyEntry.put("user_id", request.getUserId());
        } else {
            replyEntry.put("guest_name", request.getGuestName());
        }
        replyEntry.put("content", request.getContent());
        replyEntry.put("created_at", LocalDateTime.now().toString());

        List<Map<String, Object>> replies = (List<Map<String, Object>>)
                parentComment.getOrDefault("replies", new ArrayList<>());
        replies.add(replyEntry);
        parentComment.put("replies", replies);

        comments.put("entries", entries);
        comments.put("count", calculateTotalComments(entries));

        post.setComments(comments);
        updateMetrics(post);
        post = postRepository.save(post);

        CommentEntryDTO replyDTO = mapToCommentEntryDTO(replyEntry);

        log.info("Reply added successfully with ID: {}", replyId);
        return CommentResponse.builder()
                .message("Reply added successfully")
                .comment(replyDTO)
                .build();
    }

    // Helper Methods

    private Post getPostByIdAndAuthor(Long postId, Long authorId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getAuthor().getId().equals(authorId)) {
            throw new UnauthorizedException("You don't have permission to access this post");
        }

        return post;
    }

    private void recordView(Post post, String guestName, String viewerGuestId, String referrer,
                            String userAgent, HttpServletRequest request) {
        Map<String, Object> views = post.getViews();
        List<Map<String, Object>> entries = (List<Map<String, Object>>)
                views.getOrDefault("entries", new ArrayList<>());

        String ipAddress = ipAddressUtil.getClientIpAddress(request);
        Map<String, String> location = geoLocationUtil.getLocationFromIp(ipAddress);

        Map<String, Object> viewEntry = new HashMap<>();
        viewEntry.put("viewer_guest_id", viewerGuestId);
        viewEntry.put("guest_name", guestName);
        viewEntry.put("ip_address", ipAddress);
        viewEntry.put("country", location.get("country"));
        viewEntry.put("city", location.get("city"));
        viewEntry.put("region", location.get("region"));
        viewEntry.put("viewed_at", LocalDateTime.now().toString());
        viewEntry.put("referrer", referrer);
        viewEntry.put("user_agent", userAgent);

        entries.add(viewEntry);

        // Calculate unique viewers by guestId
        Set<String> uniqueViewers = entries.stream()
                .map(e -> (String) e.get("viewer_guest_id"))
                .filter(Objects::nonNull)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toSet());

        views.put("entries", entries);
        views.put("count", entries.size());
        views.put("unique_viewers", uniqueViewers.size());

        post.setViews(views);
        updateMetrics(post);
        postRepository.save(post);
    }

    private void updateMetrics(Post post) {
        Map<String, Object> metrics = post.getMetrics();

        Map<String, Object> views = post.getViews();
        Map<String, Object> likes = post.getLikes();
        Map<String, Object> comments = post.getComments();

        metrics.put("views", views.getOrDefault("count", 0));
        metrics.put("likes", likes.getOrDefault("count", 0));
        metrics.put("comments", comments.getOrDefault("count", 0));

        post.setMetrics(metrics);
    }

    private Map<String, Object> findCommentById(List<Map<String, Object>> entries, String commentId) {
        for (Map<String, Object> entry : entries) {
            if (commentId.equals(entry.get("comment_id"))) {
                return entry;
            }
        }
        return null;
    }

    private int calculateTotalComments(List<Map<String, Object>> entries) {
        int count = entries.size();
        for (Map<String, Object> entry : entries) {
            List<Map<String, Object>> replies = (List<Map<String, Object>>)
                    entry.getOrDefault("replies", new ArrayList<>());
            count += replies.size();
        }
        return count;
    }

    private Map<String, Object> initializeViews() {
        Map<String, Object> views = new HashMap<>();
        views.put("count", 0);
        views.put("unique_viewers", 0);
        views.put("entries", new ArrayList<>());
        return views;
    }

    private Map<String, Object> initializeLikes() {
        Map<String, Object> likes = new HashMap<>();
        likes.put("count", 0);
        likes.put("entries", new ArrayList<>());
        return likes;
    }

    private Map<String, Object> initializeFavorites() {
        Map<String, Object> favorites = new HashMap<>();
        favorites.put("is_favorite", false);
        return favorites;
    }

    private Map<String, Object> initializeComments() {
        Map<String, Object> comments = new HashMap<>();
        comments.put("count", 0);
        comments.put("entries", new ArrayList<>());
        return comments;
    }

    private Map<String, Object> initializeMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("views", 0);
        metrics.put("likes", 0);
        metrics.put("comments", 0);
        metrics.put("recent_activity", new ArrayList<>());
        return metrics;
    }

    private PostResponse toPostResponse(Post post) {
        PostResponse response = postMapper.toPostResponse(post);
        response.setMetrics(extractMetrics(post.getMetrics()));
        return response;
    }

    private PostDetailResponse toPostDetailResponse(Post post) {
        PostDetailResponse response = postMapper.toPostDetailResponse(post);

        // Set favorite status
        Boolean isFavorite = (Boolean) post.getFavorites().getOrDefault("is_favorite", false);
        response.setIsFavorite(isFavorite);

        // Set views
        response.setViews(extractViewsData(post.getViews()));

        // Set likes
        response.setLikes(extractLikesData(post.getLikes()));

        // Set comments
        response.setComments(extractCommentsData(post.getComments()));

        // Set metrics
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

    private PostDetailResponse.ViewsData extractViewsData(Map<String, Object> viewsMap) {
        List<Map<String, Object>> entriesList = (List<Map<String, Object>>)
                viewsMap.getOrDefault("entries", new ArrayList<>());

        List<ViewEntryDTO> entries = entriesList.stream()
                .map(this::mapToViewEntryDTO)
                .collect(Collectors.toList());

        return PostDetailResponse.ViewsData.builder()
                .count((Integer) viewsMap.getOrDefault("count", 0))
                .uniqueViewers((Integer) viewsMap.getOrDefault("unique_viewers", 0))
                .entries(entries)
                .build();
    }

    private PostDetailResponse.LikesData extractLikesData(Map<String, Object> likesMap) {
        List<Map<String, Object>> entriesList = (List<Map<String, Object>>)
                likesMap.getOrDefault("entries", new ArrayList<>());

        List<LikeEntryDTO> entries = entriesList.stream()
                .map(this::mapToLikeEntryDTO)
                .collect(Collectors.toList());

        return PostDetailResponse.LikesData.builder()
                .count((Integer) likesMap.getOrDefault("count", 0))
                .entries(entries)
                .build();
    }

    private PostDetailResponse.CommentsData extractCommentsData(Map<String, Object> commentsMap) {
        List<Map<String, Object>> entriesList = (List<Map<String, Object>>)
                commentsMap.getOrDefault("entries", new ArrayList<>());

        List<CommentEntryDTO> entries = entriesList.stream()
                .map(this::mapToCommentEntryDTO)
                .collect(Collectors.toList());

        return PostDetailResponse.CommentsData.builder()
                .count((Integer) commentsMap.getOrDefault("count", 0))
                .entries(entries)
                .build();
    }

    // Update mapToViewEntryDTO
    private ViewEntryDTO mapToViewEntryDTO(Map<String, Object> map) {
        return ViewEntryDTO.builder()
                .viewerGuestId((String) map.get("viewer_guest_id"))
                .guestName((String) map.get("guest_name"))
                .ipAddress((String) map.get("ip_address"))
                .country((String) map.get("country"))
                .city((String) map.get("city"))
                .region((String) map.get("region"))
                .viewedAt(LocalDateTime.parse((String) map.get("viewed_at")))
                .referrer((String) map.get("referrer"))
                .userAgent((String) map.get("user_agent"))
                .build();
    }
    // Update mapToLikeEntryDTO
    private LikeEntryDTO mapToLikeEntryDTO(Map<String, Object> map) {
        Long userId = map.containsKey("user_id") ?
                ((Number) map.get("user_id")).longValue() : null;

        return LikeEntryDTO.builder()
                .userId(userId)
                .guestName((String) map.get("guest_name"))
                .guestIdentifier((String) map.get("guest_identifier"))
                .ipAddress((String) map.get("ip_address"))
                .likedAt(LocalDateTime.parse((String) map.get("liked_at")))
                .build();
    }


    private CommentEntryDTO mapToCommentEntryDTO(Map<String, Object> map) {
        Long userId = map.containsKey("user_id") ?
                ((Number) map.get("user_id")).longValue() : null;

        List<Map<String, Object>> repliesList = (List<Map<String, Object>>)
                map.getOrDefault("replies", new ArrayList<>());

        List<CommentEntryDTO> replies = repliesList.stream()
                .map(this::mapToCommentEntryDTO)
                .collect(Collectors.toList());

        return CommentEntryDTO.builder()
                .commentId((String) map.get("comment_id"))
                .userId(userId)
                .guestName((String) map.get("guest_name"))
                .content((String) map.get("content"))
                .createdAt(LocalDateTime.parse((String) map.get("created_at")))
                .replies(replies)
                .build();
    }
}