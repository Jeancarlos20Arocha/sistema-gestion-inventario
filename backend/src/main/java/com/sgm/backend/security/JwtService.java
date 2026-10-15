package com.sgm.backend.security;

import com.sgm.shared.constant.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;
    private final long resetExpirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs,
                      @Value("${app.jwt.reset-expiration-ms}") long resetExpirationMs) {
        this.key = from(secret);
        this.expirationMs = expirationMs;
        this.resetExpirationMs = resetExpirationMs;
    }

    private SecretKey from(String secret) {
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        } catch (DecodingException | IllegalArgumentException ex) {
            // El secreto no es base64 valido: se usa su representacion UTF-8 como clave
            return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }

    public String generateAccessToken(String username, Long userId, String role, String machineId) {
        return generate(username, userId, role, machineId, SecurityConstants.SCOPE_ACCESS, expirationMs);
    }

    public String generateResetToken(String username, Long userId) {
        return generate(username, userId, null, null, SecurityConstants.SCOPE_PASSWORD_RESET, resetExpirationMs);
    }

    private String generate(String username, Long userId, String role, String machineId,
                            String scope, long ttl) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim(SecurityConstants.CLAIM_USER_ID, userId)
                .claim(SecurityConstants.CLAIM_SCOPE, scope)
                .claim(SecurityConstants.CLAIM_ROLE, role)
                .claim(SecurityConstants.CLAIM_MACHINE_ID, machineId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Object claim(String token, String name) {
        return extractClaim(token, claims -> claims.get(name));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isValid(String token, String expectedScope) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            String scope = claims.get(SecurityConstants.CLAIM_SCOPE, String.class);
            return claims.getExpiration().after(new Date())
                    && (expectedScope == null || expectedScope.equals(scope));
        } catch (Exception ex) {
            return false;
        }
    }

    public Claims claims(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(claims(token));
    }
}