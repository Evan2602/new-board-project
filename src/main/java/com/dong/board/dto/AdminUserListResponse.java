package com.dong.board.dto;

import com.dong.board.domain.log.PageResult;
import com.dong.board.domain.user.AdminUserResult;
import com.dong.board.domain.user.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 회원 목록 조회 응답 DTO
 * - 목록에서는 게시글/이력 상세 정보 제외
 */
public record AdminUserListResponse(
        List<UserSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * 회원 목록 아이템 요약 정보
     */
    public record UserSummary(
            Long id,
            String userId,
            String username,
            LocalDateTime createdAt,
            User.UserStatus status,
            LocalDateTime lastLoginAt
    ) {
        public static UserSummary from(AdminUserResult result) {
            return new UserSummary(
                    result.id(),
                    result.userId(),
                    result.username(),
                    result.createdAt(),
                    result.status(),
                    result.lastLoginAt()
            );
        }
    }

    /**
     * PageResult<AdminUserResult> → AdminUserListResponse 변환
     */
    public static AdminUserListResponse from(PageResult<AdminUserResult> pageResult) {
        List<UserSummary> summaries = pageResult.content().stream()
                .map(UserSummary::from)
                .toList();
        return new AdminUserListResponse(
                summaries,
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages()
        );
    }
}

