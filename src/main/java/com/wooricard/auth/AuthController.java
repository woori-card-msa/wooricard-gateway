package com.wooricard.auth;

import com.wooricard.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Authentication", description = "JWT 토큰 발급 API. 모든 API 호출 전 토큰을 먼저 발급받아야 합니다.")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;

    @Value("${jwt.client.id}")
    private String validClientId;

    @Value("${jwt.client.secret}")
    private String validClientSecret;

    @Operation(
            summary = "JWT 액세스 토큰 발급",
            description = "클라이언트 ID와 Secret으로 JWT 토큰을 발급합니다. 발급된 토큰은 1시간 동안 유효하며, 이후 모든 API 요청의 Authorization 헤더에 포함해야 합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 발급 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "access_token": "eyJhbGciOiJIUzI1NiJ9...",
                                      "token_type": "Bearer"
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "클라이언트 인증 실패 - ID 또는 Secret이 올바르지 않음",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "error": "Invalid client credentials"
                                    }
                                    """)))
    })
    @SecurityRequirements  // 이 엔드포인트는 토큰 없이 호출 가능
    @PostMapping("/token")
    public ResponseEntity<?> issueToken(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "클라이언트 인증 정보",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = TokenRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "clientId": "vensa",
                                      "clientSecret": "vensa-secret-2026"
                                    }
                                    """)))
            @RequestBody TokenRequest request) {
        if (!validClientId.equals(request.getClientId()) ||
                !validClientSecret.equals(request.getClientSecret())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid client credentials"));
        }

        String token = jwtUtil.generateToken(request.getClientId());
        return ResponseEntity.ok(Map.of("access_token", token, "token_type", "Bearer"));
    }
}
