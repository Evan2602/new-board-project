package com.dong.board.domain.user;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 회원 상세 조회 결과 객체
 * Service → Controller 레이어 간 데이터 전달
 *
 * @param id              DB 고유 ID
 * @param userId          로그인 ID
 * @param username        닉네임
 * @param createdAt       가입일
 * @param status          계정 상태
 * @param lastLoginAt     마지막 로그인 시각 (없으면 null)
 * @param boards          작성 게시글 목록 (최신순)
 * @param statusHistories 상태 변경 이력 (최신순)
 */
public record AdminUserDetailResult(
        Long id,
        String userId,
        String username,
        LocalDateTime createdAt,
        User.UserStatus status,
        LocalDateTime lastLoginAt,
        List<BoardSummary> boards,
        List<StatusHistorySummary> statusHistories
) {

    /**
     * 작성 게시글 요약 정보
     *
     * @param id        게시글 DB ID
     * @param title     게시글 제목
     * @param createdAt 작성일
     */
    public record BoardSummary(
            Long id,
            String title,
            LocalDateTime createdAt
    ) {
    }

    /**
     * 상태 변경 이력 요약 정보
     *
     * @param previousStatus 변경 전 상태
     * @param newStatus      변경 후 상태
     * @param reason         변경 사유
     * @param adminId        처리 관리자 로그인 ID
     * @param createdAt      처리 시각
     */
    public record StatusHistorySummary(
            User.UserStatus previousStatus,
            User.UserStatus newStatus,
            String reason,
            String adminId,
            LocalDateTime createdAt
    ) {
        /**
         * 도메인 UserStatusHistory → StatusHistorySummary 변환
         */
        public static StatusHistorySummary from(UserStatusHistory history) {
            return new StatusHistorySummary(
                    history.getPreviousStatus(),
                    history.getNewStatus(),
                    history.getReason(),
                    history.getAdminId(),
                    history.getCreatedAt()
            );
        }
    }
}

