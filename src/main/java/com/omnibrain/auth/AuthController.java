package com.omnibrain.auth;

import com.omnibrain.entity.User;
import com.omnibrain.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/auth")
public class AuthController {

    public record Credentials(String username, String password) {}

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_.-]{3,50}");
    private static final int MIN_PASSWORD = 8;
    private static final int MAX_PASSWORD = 72; // BCrypt ignores bytes beyond 72

    private final JwtUtil jwtUtil;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    // Compared against when the username is unknown, so a miss costs as much as a wrong password.
    private final String dummyHash;

    public AuthController(JwtUtil jwtUtil, UserRepository users, PasswordEncoder encoder) {
        this.jwtUtil = jwtUtil;
        this.users = users;
        this.encoder = encoder;
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Credentials body) {
        if (body == null || body.username() == null || !USERNAME.matcher(body.username()).matches()) {
            return error(HttpStatus.BAD_REQUEST, "username must be 3-50 characters: letters, digits, . _ -");
        }
        if (body.password() == null || body.password().length() < MIN_PASSWORD
                || body.password().length() > MAX_PASSWORD) {
            return error(HttpStatus.BAD_REQUEST,
                    "password must be " + MIN_PASSWORD + "-" + MAX_PASSWORD + " characters");
        }
        if (users.findByUsername(body.username()).isPresent()) {
            return error(HttpStatus.CONFLICT, "username already taken");
        }
        User user = new User();
        user.setUsername(body.username());
        user.setPassword(encoder.encode(body.password()));
        user.setPlan("FREE");
        users.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("username", user.getUsername()));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Credentials body) {
        if (body == null || body.username() == null || body.password() == null) {
            return error(HttpStatus.BAD_REQUEST, "username and password are required");
        }
        User user = users.findByUsername(body.username()).orElse(null);
        boolean ok = encoder.matches(body.password(), user != null ? user.getPassword() : dummyHash)
                && user != null;
        if (!ok) {
            return error(HttpStatus.UNAUTHORIZED, "invalid username or password");
        }
        return ResponseEntity.ok(Map.of("token", jwtUtil.generateToken(user.getUsername())));
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
