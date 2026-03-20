package com.wooricard.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "토큰 발급 요청")
public class TokenRequest {

    @Schema(description = "클라이언트 ID", example = "vensa")
    private String clientId;

    @Schema(description = "클라이언트 Secret", example = "vensa-secret-2026")
    private String clientSecret;
}
