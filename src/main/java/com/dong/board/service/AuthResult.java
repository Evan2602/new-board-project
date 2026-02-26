package com.dong.board.service;

/**
 * 인증 서비스 응답 결과 (회원가입/로그인 공통)
 * Service → Controller 레이어로 결과를 전달하는 객체
 *
 * - accessToken: 발급된 JWT 토큰 (API 요청 시 Authorization 헤더에 첨부)
 * - userId: 로그인 ID (예: "hong123") — 이후 요청에서 사용자 식별에 사용
 * - username: 화면에 표시할 이름 (예: "홍길동")
 */
public record AuthResult(
        String accessToken,  // JWT 액세스 토큰 문자열
        String userId,       // 로그인 ID (예: "hong123")
        String username      // 표시 이름 (예: "홍길동")
) {}
