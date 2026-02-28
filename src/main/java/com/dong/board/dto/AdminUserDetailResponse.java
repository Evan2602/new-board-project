package com.dong.board.dto;

import com.dong.board.domain.user.AdminUserDetailResult;
import com.dong.board.domain.user.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 회원 상세 조회 응답 DTO
 * - 기본 정보 + 작성 게시글 목록 + 상태 변경 이력 포함
 */
public record AdminUserDetailResponse(
        Long id,
        String userId,
        String username,
        LocalDateTime createdAt,
        User.UserStatus status,
        LocalDateTime lastLoginAt,
        List<BoardItem> boards,
        List<HistoryItem> statusHistories
) {

    /**
     * 작성 게시글 아이템
     */
    public record BoardItem(
            Long id,
            String title,
            LocalDateTime createdAt
    ) {
    }

    /**
     * 상태 변경 이력 아이템
     */
    public record HistoryItem(
            User.UserStatus previousStatus,
            User.UserStatus newStatus,
            String reason,
            String adminId,
            LocalDateTime createdAt
    ) {
    }

    /**
     * AdminUserDetailResult → AdminUserDetailResponse 변환
     */
    public static AdminUserDetailResponse from(AdminUserDetailResult result) {
        List<BoardItem> boardItems = result.boards().stream()
                .map(b -> new BoardItem(b.id(), b.title(), b.createdAt()))
                .toList();

        List<HistoryItem> historyItems = result.statusHistories().stream()
                .map(h -> new HistoryItem(
                        h.previousStatus(), h.newStatus(),
                        h.reason(), h.adminId(), h.createdAt()))
                .toList();

        return new AdminUserDetailResponse(
                result.id(),
                result.userId(),
                result.username(),
                result.createdAt(),
                result.status(),
                result.lastLoginAt(),
                boardItems,
                historyItems
        );
    }
}

