package com.dong.board.domain.user;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 회원 상태 변경 이력 도메인 엔티티
 * - 관리자가 회원 상태를 변경할 때마다 이력을 기록
 * - 모든 필드는 불변 (이력은 수정 불가)
 */
@Getter
public class UserStatusHistory {

    // DB AUTO_INCREMENT ID (생성 시 null, 저장 후 발급)
    private final Long id;

    // 상태가 변경된 회원의 로그인 ID
    private final String userId;

    // 변경 전 상태
    private final User.UserStatus previousStatus;

    // 변경 후 상태
    private final User.UserStatus newStatus;

    // 상태 변경 사유 (예: "스팸 게시글 반복 등록", "본인 탈퇴 요청")
    private final String reason;

    // 상태를 변경한 관리자의 로그인 ID
    private final String adminId;

    // 상태 변경이 처리된 시각
    private final LocalDateTime createdAt;

    // 외부에서 직접 생성하지 못하도록 private
    private UserStatusHistory(Long id, String userId,
                               User.UserStatus previousStatus, User.UserStatus newStatus,
                               String reason, String adminId, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.adminId = adminId;
        this.createdAt = createdAt;
    }

    /**
     * 새 상태 변경 이력 생성 정적 팩토리 메서드
     * id=null로 생성 → save() 후 DB가 AUTO_INCREMENT로 발급
     *
     * @param userId         상태가 변경된 회원의 로그인 ID
     * @param previousStatus 변경 전 상태
     * @param newStatus      변경 후 상태
     * @param reason         상태 변경 사유
     * @param adminId        처리한 관리자의 로그인 ID
     */
    public static UserStatusHistory create(String userId,
                                           User.UserStatus previousStatus,
                                           User.UserStatus newStatus,
                                           String reason,
                                           String adminId) {
        return new UserStatusHistory(null, userId, previousStatus, newStatus,
                reason, adminId, LocalDateTime.now());
    }

    /**
     * DB 조회 결과로부터 도메인 객체 복원
     * JPA 인프라 레이어에서만 사용 — 비즈니스 로직에서 직접 호출 금지
     */
    public static UserStatusHistory reconstruct(Long id, String userId,
                                                User.UserStatus previousStatus,
                                                User.UserStatus newStatus,
                                                String reason, String adminId,
                                                LocalDateTime createdAt) {
        return new UserStatusHistory(id, userId, previousStatus, newStatus,
                reason, adminId, createdAt);
    }
}

