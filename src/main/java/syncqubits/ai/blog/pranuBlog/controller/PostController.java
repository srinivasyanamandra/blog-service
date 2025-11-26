package syncqubits.ai.blog.pranuBlog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import syncqubits.ai.blog.pranuBlog.dto.request.*;
import syncqubits.ai.blog.pranuBlog.dto.response.*;
import syncqubits.ai.blog.pranuBlog.service.PostService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Posts", description = "Blog post management")
@CrossOrigin(origins = "*")
public class PostController {

    private final PostService postService;

    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create post", description = "Create a draft blog post")
    @ApiResponse(responseCode = "201", description = "Post created")
    public ResponseEntity<PostResponse> createPost(
            Authentication authentication,
            @Valid @RequestBody CreatePostRequest request) {
        Long userId = getUserId(authentication);
        log.info("POST /api/posts - User: {}", userId);
        PostResponse response = postService.createPost(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{postId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update post")
    @ApiResponse(responseCode = "200", description = "Post updated")
    public ResponseEntity<PostResponse> updatePost(
            Authentication authentication,
            @PathVariable Long postId,
            @Valid @RequestBody UpdatePostRequest request) {
        Long userId = getUserId(authentication);
        log.info("PUT /api/posts/{} - User: {}", postId, userId);
        PostResponse response = postService.updatePost(userId, postId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{postId}/publish")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Publish post")
    @ApiResponse(responseCode = "200", description = "Post published")
    public ResponseEntity<PostResponse> publishPost(
            Authentication authentication,
            @PathVariable Long postId) {
        Long userId = getUserId(authentication);
        log.info("POST /api/posts/{}/publish - User: {}", postId, userId);
        PostResponse response = postService.publishPost(userId, postId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{postId}/unpublish")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Unpublish post")
    @ApiResponse(responseCode = "200", description = "Post unpublished")
    public ResponseEntity<PostResponse> unpublishPost(
            Authentication authentication,
            @PathVariable Long postId) {
        Long userId = getUserId(authentication);
        log.info("POST /api/posts/{}/unpublish - User: {}", postId, userId);
        PostResponse response = postService.unpublishPost(userId, postId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{postId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete post")
    @ApiResponse(responseCode = "204", description = "Post deleted")
    public ResponseEntity<Void> deletePost(
            Authentication authentication,
            @PathVariable Long postId) {
        Long userId = getUserId(authentication);
        log.info("DELETE /api/posts/{} - User: {}", postId, userId);
        postService.deletePost(userId, postId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get my posts")
    @ApiResponse(responseCode = "200", description = "Posts retrieved")
    public ResponseEntity<List<PostResponse>> getMyPosts(Authentication authentication) {
        Long userId = getUserId(authentication);
        log.info("GET /api/posts - User: {}", userId);
        List<PostResponse> response = postService.getMyPosts(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get post by ID")
    @ApiResponse(responseCode = "200", description = "Post retrieved")
    public ResponseEntity<PostDetailResponse> getPostById(
            Authentication authentication,
            @PathVariable Long postId) {
        Long userId = getUserId(authentication);
        log.info("GET /api/posts/{} - User: {}", postId, userId);
        PostDetailResponse response = postService.getPostById(userId, postId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/list")
    @Operation(summary = "List all public posts", description = "Get all published posts (no auth required)")
    @ApiResponse(responseCode = "200", description = "Public posts retrieved")
    public ResponseEntity<List<PostResponse>> listPublicPosts() {
        log.info("GET /api/posts/public/list");
        List<PostResponse> response = postService.listPublicPosts();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/{shareToken}")
    @Operation(summary = "Get public post", description = "View published post by token")
    @ApiResponse(responseCode = "200", description = "Post retrieved")
    public ResponseEntity<PostDetailResponse> getPublicPost(
            @PathVariable String shareToken,
            @RequestParam String guestName, // NOW MANDATORY
            @RequestParam(required = false) String viewerGuestId,
            @RequestParam(required = false) String referrer,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest request) {
        log.info("GET /api/posts/public/{}", shareToken);
        PostDetailResponse response = postService.getPublicPost(
                shareToken, guestName, viewerGuestId, referrer, userAgent, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{postId}/favorite")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Toggle favorite")
    @ApiResponse(responseCode = "200", description = "Favorite toggled")
    public ResponseEntity<PostResponse> toggleFavorite(
            Authentication authentication,
            @PathVariable Long postId) {
        Long userId = getUserId(authentication);
        log.info("POST /api/posts/{}/favorite - User: {}", postId, userId);
        PostResponse response = postService.toggleFavorite(userId, postId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/public/{shareToken}/like")
    @Operation(summary = "Toggle like", description = "Like or unlike a post (toggle behavior)")
    @ApiResponse(responseCode = "200", description = "Like toggled")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable String shareToken,
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {
        log.info("POST /api/posts/public/{}/like", shareToken);

        Long userId = requestBody.containsKey("userId") ?
                Long.valueOf(requestBody.get("userId").toString()) : null;
        String guestName = (String) requestBody.get("guestName");
        String guestIdentifier = (String) requestBody.get("guestIdentifier");

        if (guestName == null || guestName.trim().isEmpty()) {
            throw new IllegalArgumentException("Guest name is required");
        }

        PostResponse response = postService.toggleLike(
                shareToken, userId, guestName, guestIdentifier, request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "post", response
        ));
    }


    @PostMapping("/public/{shareToken}/comments")
    @Operation(summary = "Add comment")
    @ApiResponse(responseCode = "201", description = "Comment added")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable String shareToken,
            @Valid @RequestBody CommentRequest request,
            HttpServletRequest httpRequest) {
        log.info("POST /api/posts/public/{}/comments", shareToken);
        CommentResponse response = postService.addComment(shareToken, request, httpRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/public/{shareToken}/replies")
    @Operation(summary = "Add reply")
    @ApiResponse(responseCode = "201", description = "Reply added")
    public ResponseEntity<CommentResponse> addReply(
            @PathVariable String shareToken,
            @Valid @RequestBody ReplyRequest request) {
        log.info("POST /api/posts/public/{}/replies", shareToken);
        CommentResponse response = postService.addReply(shareToken, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}