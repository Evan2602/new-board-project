package com.dong.board.service;

/**
 * 회원가입 서비스 요청 커맨드
 * Controller → Service 레이어 간 데이터 전달 객체 (HTTP 어노테이션 없음)
 *
 * - userId: 로그인할 때 쓸 아이디 (예: "hong123")
 * - username: 화면에 표시할 이름 (예: "홍길동")
 * - password: 원문 비밀번호 (서비스 레이어에서 BCrypt로 해시)
 */
public record SignUpCommand(
        String userId,    // 로그인 ID (예: "hong123")
        String username,  // 표시 이름 (예: "홍길동")
        String password   // 원문 비밀번호 (해시 전)
) {}
