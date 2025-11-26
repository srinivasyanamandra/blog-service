package syncqubits.ai.blog.pranuBlog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "invalidated_tokens", indexes = {
        @Index(name = "idx_token", columnList = "token"),
        @Index(name = "idx_expiry", columnList = "expiryTime")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvalidatedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500, unique = true)
    private String token;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime expiryTime;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime invalidatedAt;

    @Column(nullable = false)
    private String reason; // LOGOUT, FORCE_LOGOUT, etc.
}