package syncqubits.ai.blog.pranuBlog.service;

import syncqubits.ai.blog.pranuBlog.dto.request.CommentRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.CreatePostRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.ReplyRequest;
import syncqubits.ai.blog.pranuBlog.dto.request.UpdatePostRequest;
import syncqubits.ai.blog.pranuBlog.dto.response.CommentResponse;
import syncqubits.ai.blog.pranuBlog.dto.response.PostDetailResponse;
import syncqubits.ai.blog.pranuBlog.dto.response.PostResponse;

import java.util.List;

public interface PostService {
    PostResponse createPost(Long authorId, CreatePostRequest request);
    PostResponse updatePost(Long authorId, Long postId, UpdatePostRequest request);
    PostResponse publishPost(Long authorId, Long postId);
    PostResponse unpublishPost(Long authorId, Long postId);
    void deletePost(Long authorId, Long postId);

    List<PostResponse> getMyPosts(Long authorId);
    PostDetailResponse getPostById(Long authorId, Long postId);
    List<PostResponse> listPublicPosts(); // NEW METHOD
    PostDetailResponse getPublicPost(String shareToken, String viewerGuestId, String referrer, String userAgent);

    PostResponse toggleFavorite(Long authorId, Long postId);
    PostResponse likePost(String shareToken, Long userId, String guestName, String guestIdentifier);
    CommentResponse addComment(String shareToken, CommentRequest request);
    CommentResponse addReply(String shareToken, ReplyRequest request);
}