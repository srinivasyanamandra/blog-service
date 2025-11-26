package syncqubits.ai.blog.pranuBlog.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import syncqubits.ai.blog.pranuBlog.repository.InvalidatedTokenRepository;

import java.time.LocalDateTime;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupScheduler {

    private final InvalidatedTokenRepository invalidatedTokenRepository;

    /**
     * Clean up expired tokens from blacklist every day at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired invalidated tokens");

        try {
            LocalDateTime now = LocalDateTime.now();
            invalidatedTokenRepository.deleteExpiredTokens(now);
            log.info("Expired tokens cleanup completed successfully");
        } catch (Exception e) {
            log.error("Error during token cleanup: {}", e.getMessage(), e);
        }
    }
}