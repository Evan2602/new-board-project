package com.dong.board.dto;

import com.dong.board.domain.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 회원 상태 변경 요청 DTO
 * PATCH /admin/users/{id}/status 요청 본문
 */
public record ChangeUserStatusRequest(
        @NotNull(message = "변경할 상태는 필수입니다.")
        User.UserStatus newStatus,

        @NotBlank(message = "상태 변경 사유는 필수입니다.")
        String reason
) {
}

