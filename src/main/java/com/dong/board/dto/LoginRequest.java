package com.dong.board.dto;

import com.dong.board.service.LoginCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 HTTP 요청 DTO
 */
public record LoginRequest(
        @NotBlank(message = "사용자명은 필수입니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
    public LoginCommand toCommand() {
        return new LoginCommand(username, password);
    }
}
