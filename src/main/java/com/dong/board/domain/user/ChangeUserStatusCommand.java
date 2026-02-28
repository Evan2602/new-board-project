package com.dong.board.domain.user;

/**
 * 회원 상태 변경 명령 객체
 * Controller → Service 데이터 전달 (HTTP 어노테이션 없음)
 *
 * @param userId    상태를 변경할 회원의 DB 고유 ID
 * @param newStatus 변경할 상태 (ACTIVE, SUSPENDED, WITHDRAWN)
 * @param reason    상태 변경 사유 (이력에 기록)
 * @param adminId   처리하는 관리자의 로그인 ID (JWT에서 추출)
 */
public record ChangeUserStatusCommand(
        Long userId,
        User.UserStatus newStatus,
        String reason,
        String adminId
) {
}

