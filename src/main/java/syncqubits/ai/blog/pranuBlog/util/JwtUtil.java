package syncqubits.ai.blog.pranuBlog.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import syncqubits.ai.blog.pranuBlog.repository.InvalidatedTokenRepository;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {

    private final InvalidatedTokenRepository invalidatedTokenRepository;

    @Value("${app.jwt.secret}")
    private String secret; // Recommended: Base64-encoded 256+ bit key

    @Value("${app.jwt.expiration}")
    private Long expiration;

    /**
     * Returns a SecretKey suitable for signing/verification.
     *
     * If you store a Base64-encoded secret (recommended), this will decode it.
     * If you store a raw passphrase, you can instead use:
     *   return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
     *
     * Prefer base64-encoded 32+ byte secrets (openssl rand -base64 32).
     */
    private SecretKey getSigningKey() {
        try {
            // Try to decode as Base64 first (recommended storage)
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException ex) {
            // If it's not valid Base64, fall back to raw bytes (less secure)
            log.warn("JWT secret is not valid Base64 — falling back to UTF-8 bytes (consider storing a Base64-encoded secret)");
            return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Generate a new JWT token for the user
     */
    public String generateToken(Long userId, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("role", role);
        return createToken(claims, email);
    }

    /**
     * Create JWT token with claims
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)            // use setClaims(...) with jjwt 0.11.x
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())    // Key-only overload works for HS algorithms
                .compact();
    }

    /**
     * Validate JWT token
     * Checks:
     * 1. Token signature validity
     * 2. Token expiration
     * 3. Token blacklist (logged out tokens)
     */
    public boolean validateToken(String token) {
        try {
            // First check if token is blacklisted (logged out)
            if (isTokenBlacklisted(token)) {
                log.warn("Token validation failed: Token is blacklisted (user logged out)");
                return false;
            }

            // parseClaimsJws will throw if token invalid/expired/signature incorrect
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);

            // Additional check for expiration
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if token is in the blacklist (invalidated)
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            return invalidatedTokenRepository.existsByToken(token);
        } catch (Exception e) {
            log.error("Error checking token blacklist: {}", e.getMessage());
            // In case of DB error, fail closed (deny access)
            return true;
        }
    }

    /**
     * Extract email from token
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract user ID from token
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    /**
     * Extract role from token
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Extract specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Check if token is expired
     */
    private boolean isTokenExpired(String token) {
        Date exp = extractExpiration(token);
        return exp != null && exp.before(new Date());
    }

    /**
     * Extract expiration date from token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}