package com.omnibrain.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";

    @Test
    void roundTripReturnsTheSubject() {
        JwtUtil jwt = new JwtUtil(SECRET, 60);
        String token = jwt.generateToken("alice");
        assertTrue(jwt.validateToken(token));
        assertEquals("alice", jwt.extractUsername(token));
    }

    @Test
    void refusesToStartWithAWeakOrMissingSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("too-short", 60));
        assertThrows(IllegalStateException.class, () -> new JwtUtil("", 60));
        assertThrows(IllegalStateException.class, () -> new JwtUtil(null, 60));
    }

    @Test
    void rejectsATokenSignedWithADifferentSecret() {
        String forged = new JwtUtil("another-secret-another-secret-another-1", 60).generateToken("alice");
        assertFalse(new JwtUtil(SECRET, 60).validateToken(forged));
    }

    @Test
    void rejectsATamperedToken() {
        JwtUtil jwt = new JwtUtil(SECRET, 60);
        String token = jwt.generateToken("alice");
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("aa") ? "bb" : "aa");
        assertFalse(jwt.validateToken(tampered));
    }

    @Test
    void rejectsAnExpiredToken() {
        JwtUtil jwt = new JwtUtil(SECRET, -1);
        assertFalse(jwt.validateToken(jwt.generateToken("alice")));
    }

    @Test
    void rejectsAnUnsignedAlgNoneToken() {
        String none = Jwts.builder().setSubject("alice")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000)).compact();
        assertFalse(new JwtUtil(SECRET, 60).validateToken(none));
    }

    @Test
    void rejectsGarbage() {
        JwtUtil jwt = new JwtUtil(SECRET, 60);
        assertFalse(jwt.validateToken("not-a-jwt"));
        assertFalse(jwt.validateToken(""));
        assertTrue(jwt.parseUsername("a.b.c").isEmpty());
    }

    @Test
    void tokenSignedWithTheSameKeyButOtherUserCarriesThatUser() {
        var key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder().setSubject("bob")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key, SignatureAlgorithm.HS256).compact();
        assertEquals("bob", new JwtUtil(SECRET, 60).extractUsername(token));
    }
}
