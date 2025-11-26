package syncqubits.ai.blog.pranuBlog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import syncqubits.ai.blog.pranuBlog.entity.InvalidatedToken;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, Long> {

    Optional<InvalidatedToken> findByToken(String token);

    boolean existsByToken(String token);

    @Modifying
    @Query("DELETE FROM InvalidatedToken it WHERE it.expiryTime < :now")
    void deleteExpiredTokens(LocalDateTime now);
}