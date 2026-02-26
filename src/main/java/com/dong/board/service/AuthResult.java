package com.dong.board.service;

/**
 * 인증 서비스 응답 결과 (회원가입/로그인 공통)
 * Service → Controller 레이어로 결과를 전달하는 객체
 */
public record AuthResult(
        String accessToken,
        String userId,
        String username
) {}
