package syncqubits.ai.blog.pranuBlog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import syncqubits.ai.blog.pranuBlog.dto.request.CommentRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.CreatePostRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.ReplyRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.UpdatePostRequest;
import syncqubits.ai.blog.pranuBlog.dto.response.CommentResponse;
import syncqubits.ai.blog.pranuBlog.dto.response.PostDetailResponse;
import syncqubits.ai.blog.pranuBlog.dto.response.PostResponse;
import syncqubits.ai.blog.pranuBlog.service.PostService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Posts", description = "Blog post management endpoints")
@CrossOrigin(origins = {"*"})
public class PostController {

    private final PostService postService;

    @PostMapping
    @Operation(summary = "Create a new post", description = "Create a draft blog post")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Post created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<PostResponse> createPost(
            @Parameter(description = "Author ID", required = true)
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreatePostRequest request) {
        log.info("POST /api/posts - User: {}, Title: {}", userId, request.getTitle());
        PostResponse response = postService.createPost(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{postId}")
    @Operation(summary = "Update a post", description = "Update an existing post")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Post updated successfully"),
            @ApiResponse(responseCode = "404", description = "Post not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PostResponse> updatePost(
            @Parameter(description = "Author ID", required = true)
            @RequestHeader("X-User-Id") Long userId,
            @Parameter(description = "Post ID", required = true)
            @PathVariable Long postId,
            @Valid @RequestBody UpdatePostRequest request) {
        log.info("PUT /api/posts/{} - User: {}", postId, userId);
        PostResponse response = postService.updatePost(userId, postId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{postId}/publish")
    @Operation(summary = "Publish a post", description = "Publish a draft post and generate share token")
    @ApiResponse(responseCode = "200", description = "Post published successfully")
    public ResponseEntity<PostResponse> publishPost(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId) {
        log.info("POST /api/posts/{}/publish - User: {}", postId, userId);
        PostResponse response = postService.publishPost(userId, postId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{postId}/unpublish")
    @Operation(summary = "Unpublish a post", description = "Set post back to draft status")
    @ApiResponse(responseCode = "200", description = "Post unpublished successfully")
    public ResponseEntity<PostResponse> unpublishPost(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId) {
        log.info("POST /api/posts/{}/unpublish - User: {}", postId, userId);
        PostResponse response = postService.unpublishPost(userId, postId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "Delete a post", description = "Permanently delete a post")
    @ApiResponse(responseCode = "204", description = "Post deleted successfully")
    public ResponseEntity<Void> deletePost(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId) {
        log.info("DELETE /api/posts/{} - User: {}", postId, userId);
        postService.deletePost(userId, postId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get my posts", description = "Get all posts for authenticated user")
    @ApiResponse(responseCode = "200", description = "Posts retrieved successfully")
    public ResponseEntity<List<PostResponse>> getMyPosts(
            @RequestHeader("X-User-Id") Long userId) {
        log.info("GET /api/posts - User: {}", userId);
        List<PostResponse> response = postService.getMyPosts(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}")
    @Operation(summary = "Get post by ID", description = "Get post details by ID (owner only)")
    @ApiResponse(responseCode = "200", description = "Post retrieved successfully")
    public ResponseEntity<PostDetailResponse> getPostById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId) {
        log.info("GET /api/posts/{} - User: {}", postId, userId);
        PostDetailResponse response = postService.getPostById(userId, postId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/{shareToken}")
    @Operation(summary = "Get public post", description = "Get published post by share token (public access)")
    @ApiResponse(responseCode = "200", description = "Post retrieved successfully")
    public ResponseEntity<PostDetailResponse> getPublicPost(
            @PathVariable String shareToken,
            @RequestParam(required = false) String viewerGuestId,
            @RequestParam(required = false) String referrer,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        log.info("GET /api/posts/public/{} - Viewer: {}", shareToken, viewerGuestId);
        PostDetailResponse response = postService.getPublicPost(
                shareToken, viewerGuestId, referrer, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{postId}/favorite")
    @Operation(summary = "Toggle favorite", description = "Mark or unmark post as favorite")
    @ApiResponse(responseCode = "200", description = "Favorite toggled successfully")
    public ResponseEntity<PostResponse> toggleFavorite(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long postId) {
        log.info("POST /api/posts/{}/favorite - User: {}", postId, userId);
        PostResponse response = postService.toggleFavorite(userId, postId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/public/{shareToken}/like")
    @Operation(summary = "Like a post", description = "Like a public post (guest or user)")
    @ApiResponse(responseCode = "200", description = "Post liked successfully")
    public ResponseEntity<PostResponse> likePost(
            @PathVariable String shareToken,
            @RequestBody Map<String, Object> request) {
        log.info("POST /api/posts/public/{}/like", shareToken);

        Long userId = request.containsKey("userId") ?
                Long.valueOf(request.get("userId").toString()) : null;
        String guestName = (String) request.get("guestName");
        String guestIdentifier = (String) request.get("guestIdentifier");

        PostResponse response = postService.likePost(
                shareToken, userId, guestName, guestIdentifier);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/public/{shareToken}/comments")
    @Operation(summary = "Add a comment", description = "Add a comment to a public post")
    @ApiResponse(responseCode = "201", description = "Comment added successfully")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable String shareToken,
            @Valid @RequestBody CommentRequest request) {
        log.info("POST /api/posts/public/{}/comments", shareToken);
        CommentResponse response = postService.addComment(shareToken, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/public/{shareToken}/replies")
    @Operation(summary = "Add a reply", description = "Reply to an existing comment")
    @ApiResponse(responseCode = "201", description = "Reply added successfully")
    public ResponseEntity<CommentResponse> addReply(
            @PathVariable String shareToken,
            @Valid @RequestBody ReplyRequest request) {
        log.info("POST /api/posts/public/{}/replies", shareToken);
        CommentResponse response = postService.addReply(shareToken, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}