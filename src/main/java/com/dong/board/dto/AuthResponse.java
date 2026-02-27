package com.dong.board.dto;

import com.dong.board.domain.user.AuthResult;

/**
 * 인증 HTTP 응답 DTO (회원가입/로그인 공통)
 *
 * 클라이언트에게 반환되는 JSON 형식:
 * {
 *   "accessToken": "eyJhbGciOiJIUzI1NiJ9....",
 *   "tokenType": "Bearer",
 *   "userId": "hong123",
 *   "username": "홍길동"
 * }
 */
public record AuthResponse(
        String accessToken,  // JWT 토큰 (이후 API 요청 시 "Authorization: Bearer {토큰}" 헤더에 첨부)
        String tokenType,    // 토큰 타입 (항상 "Bearer")
        String userId,       // 로그인 ID (예: "hong123")
        String username      // 표시 이름 (예: "홍길동")
) {
    /**
     * Service 결과 객체로부터 HTTP 응답 DTO 생성
     * tokenType은 항상 "Bearer"로 고정
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
