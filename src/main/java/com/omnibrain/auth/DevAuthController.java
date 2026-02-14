package com.omnibrain.backend.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
public class DevAuthController {

    private static final String SECRET = "omnibrain-dev-secret-key-1234567890";

    @GetMapping("/api/dev/token")
    public String token() {
        return Jwts.builder()
                .setSubject("dev-user")
                .claim("email", "test@omnibrain.ai")
                .claim("role", "USER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
                .signWith(SignatureAlgorithm.HS256, SECRET.getBytes())
                .compact();
    }
}
