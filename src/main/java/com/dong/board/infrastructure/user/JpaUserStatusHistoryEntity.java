package com.dong.board.infrastructure.user;

import com.dong.board.domain.user.User;
import com.dong.board.domain.user.UserStatusHistory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 회원 상태 변경 이력 JPA 엔티티
 * - 도메인 UserStatusHistory와 분리하여 JPA 제약을 인프라 레이어에 격리
 * - 도메인 UserStatusHistory ↔ JpaUserStatusHistoryEntity 변환을 담당
 */
@Entity
@Table(name = "user_status_histories",
        indexes = @Index(name = "idx_user_status_histories_user_id", columnList = "userId"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JpaUserStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 상태가 변경된 회원의 로그인 ID
    @Column(nullable = false, length = 50)
    private String userId;

    // 변경 전 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private User.UserStatus previousStatus;

    // 변경 후 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private User.UserStatus newStatus;

    // 상태 변경 사유
    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    // 처리한 관리자의 로그인 ID
    @Column(nullable = false, length = 50)
    private String adminId;

    // 상태 변경 처리 시각
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 도메인 UserStatusHistory → JPA 엔티티 변환
     *
     * @param history 도메인 상태 이력 객체
     * @return JPA 엔티티
     */
    public static JpaUserStatusHistoryEntity fromDomain(UserStatusHistory history) {
        JpaUserStatusHistoryEntity entity = new JpaUserStatusHistoryEntity();
        entity.id = history.getId();
        entity.userId = history.getUserId();
        entity.previousStatus = history.getPreviousStatus();
        entity.newStatus = history.getNewStatus();
        entity.reason = history.getReason();
        entity.adminId = history.getAdminId();
        entity.createdAt = history.getCreatedAt();
        return entity;
    }

    /**
     * JPA 엔티티 → 도메인 UserStatusHistory 변환
     *
     * @return 도메인 상태 이력 객체
     */
    public UserStatusHistory toDomain() {
        return UserStatusHistory.reconstruct(
                id, userId, previousStatus, newStatus, reason, adminId, createdAt
        );
    }
}

