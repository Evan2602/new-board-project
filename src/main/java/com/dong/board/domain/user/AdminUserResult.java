package com.dong.board.domain.user;

import java.time.LocalDateTime;

/**
 * 관리자 회원 목록 조회 결과 객체 (목록용)
 * Service → Controller 레이어 간 데이터 전달
 *
 * @param id            DB 고유 ID
 * @param userId        로그인 ID (예: "hong123")
 * @param username      닉네임 (예: "홍길동")
 * @param createdAt     가입일
 * @param status        계정 상태 (ACTIVE, SUSPENDED, WITHDRAWN)
 * @param lastLoginAt   마지막 로그인 시각 (로그인 이력 없으면 null)
 */
public record AdminUserResult(
        Long id,
        String userId,
        String username,
        LocalDateTime createdAt,
        User.UserStatus status,
        LocalDateTime lastLoginAt
) {
    /**
     * 도메인 User + 마지막 로그인 시각으로 결과 객체 생성
     *
     * @param user          도메인 사용자 객체
     * @param lastLoginAt   마지막 로그인 시각 (없으면 null)
     */
    public static AdminUserResult from(User user, LocalDateTime lastLoginAt) {
        return new AdminUserResult(
                user.getId(),
                user.getUserId(),
                user.getUsername(),
                user.getCreatedAt(),
                user.getStatus(),
                lastLoginAt
        );
    }
}

