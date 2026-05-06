/**
 * File: JwtUtil.java
 * Author: system
 * Date: 2026-05-03
 */
package app.xinqianmao.com.common.auth;

import app.xinqianmao.com.common.exception.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT token utility. Generate, parse, validate tokens.
 * Claims stored: userId (subject), tenantCode, isAdmin.
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret:mypet-jwt-secret-key-2026}") String secret,
                   @Value("${jwt.expiration:86400000}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * Generate JWT token with user info embedded in claims.
     */
    public String generateToken(String userId, String tenantCode, boolean isAdmin) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .subject(userId)
                .claim("tenantCode", tenantCode)
                .claim("isAdmin", isAdmin)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * Parse and validate token, return claims.
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.warn("JWT parse failed: {}", e.getMessage());
            throw new BizException("401", "Invalid or expired token");
        }
    }

    /**
     * Extract user auth info from token.
     */
    public UserAuthInfo getUserAuthInfo(String token) {
        Claims claims = parseToken(token);
        UserAuthInfo info = new UserAuthInfo();
        info.setUserId(claims.getSubject());
        info.setTenantCode(claims.get("tenantCode", String.class));
        info.setAdmin(Boolean.TRUE.equals(claims.get("isAdmin", Boolean.class)));
        return info;
    }
}
