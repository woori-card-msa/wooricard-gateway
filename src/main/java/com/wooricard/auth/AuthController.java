package com.wooricard.auth;

import com.wooricard.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;

    @Value("${jwt.client.id}")
    private String validClientId;

    @Value("${jwt.client.secret}")
    private String validClientSecret;

    @PostMapping("/token")
    public ResponseEntity<?> issueToken(@RequestBody TokenRequest request) {
        if (!validClientId.equals(request.getClientId()) ||
                !validClientSecret.equals(request.getClientSecret())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid client credentials"));
        }

        String token = jwtUtil.generateToken(request.getClientId());
        return ResponseEntity.ok(Map.of("access_token", token, "token_type", "Bearer"));
    }
}
