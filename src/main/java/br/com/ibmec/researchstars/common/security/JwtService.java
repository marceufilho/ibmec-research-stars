package br.com.ibmec.researchstars.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Geração e validação de tokens JWT (HS256). */
@Service
public class JwtService {

    private final Key key;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Gera um token com as claims esperadas pelo frontend (sub, role, name, email,
     * professorId).
     */
    public String generateToken(AuthUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.role());
        claims.put("email", user.email());
        claims.put("name", user.name());
        if (user.professorId() != null) {
            claims.put("professorId", user.professorId());
        }

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(user.userId()))
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Valida a assinatura/expiração e reconstrói o {@link AuthUser}. */
    public AuthUser parseToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();

        Long userId = Long.valueOf(claims.getSubject());
        Long professorId = claims.get("professorId") == null
                ? null
                : ((Number) claims.get("professorId")).longValue();
        String role = claims.get("role", String.class);
        String email = claims.get("email", String.class);
        String name = claims.get("name", String.class);

        return new AuthUser(userId, professorId, role, email, name);
    }
}
