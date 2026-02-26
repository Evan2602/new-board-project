package com.dong.board.service;

/**
 * 로그인 서비스 요청 커맨드
 * Controller → Service 레이어 간 데이터 전달 객체 (HTTP 어노테이션 없음)
 *
 * - userId: 로그인할 때 쓰는 ID (예: "hong123")
 * - password: 원문 비밀번호 (서비스 레이어에서 BCrypt 검증)
 */
public record LoginCommand(
        String userId,   // 로그인 ID (예: "hong123")
        String password  // 원문 비밀번호
) {}
