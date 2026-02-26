package com.dong.board.dto;

import com.dong.board.service.AuthResult;

/**
 * 인증 HTTP 응답 DTO (회원가입/로그인 공통)
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        String username
) {
    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(result.accessToken(), "Bearer", result.username());
    }
}
