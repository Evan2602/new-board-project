package com.dong.board.service;

/**
 * 회원가입 서비스 요청 커맨드
 * Controller → Service 레이어 간 데이터 전달 객체 (HTTP 어노테이션 없음)
 */
public record SignUpCommand(
        String userId,
        String username,
        String password
) {}
