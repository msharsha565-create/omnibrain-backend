package com.omnibrain.auth;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and validates HS256 JWTs. The signing secret comes from configuration
 * ({@code omnibrain.jwt.secret}, set from the JWT_SECRET environment variable) and the
 * application refuses to start without a strong one: there is no default.
 */
@Component
public class JwtUtil {

    static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final long ttlMillis;

    public JwtUtil(
            @Value("${omnibrain.jwt.secret}") String secret,
            @Value("${omnibrain.jwt.ttl-minutes:60}") long ttlMinutes
    ) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "omnibrain.jwt.secret (JWT_SECRET) must be at least "
                            + MIN_SECRET_BYTES + " bytes; generate one with: openssl rand -base64 48");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.ttlMillis = ttlMinutes * 60_000L;
    }

    public String generateToken(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** The token's subject if it is correctly signed and unexpired, otherwise empty. */
    public Optional<String> parseUsername(String token) {
        try {
            return Optional.ofNullable(Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public boolean validateToken(String token) {
        return parseUsername(token).isPresent();
    }

    public String extractUsername(String token) {
        return parseUsername(token).orElseThrow(() -> new JwtException("invalid token"));
    }
}
