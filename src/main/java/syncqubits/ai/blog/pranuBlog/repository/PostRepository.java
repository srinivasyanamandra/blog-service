package syncqubits.ai.blog.pranuBlog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import syncqubits.ai.blog.pranuBlog.entity.Post;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByShareToken(String shareToken);

    List<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    List<Post> findByAuthorIdAndStatusOrderByCreatedAtDesc(Long authorId, Post.PostStatus status);

    @Query("SELECT p FROM Post p WHERE p.author.id = :authorId ORDER BY p.updatedAt DESC")
    List<Post> findRecentPostsByAuthor(Long authorId);

    boolean existsBySlug(String slug);

    List<Post> findByStatusAndIsPublicOrderByCreatedAtDesc(Post.PostStatus status, Boolean isPublic);

}