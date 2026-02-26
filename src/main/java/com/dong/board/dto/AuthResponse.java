package com.dong.board.dto;

import com.dong.board.service.AuthResult;

/**
 * 인증 HTTP 응답 DTO (회원가입/로그인 공통)
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        String userId,
        String username
) {
    /**
     * Service 결과 객체로부터 HTTP 응답 DTO 생성
     */
    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(
                result.accessToken(),
                "Bearer",
                result.userId(),
                result.username()
        );
    }
}
