package com.dong.board.dto;

import com.dong.board.service.SignUpCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 HTTP 요청 DTO
 */
public record SignUpRequest(
        @NotBlank(message = "사용자명은 필수입니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password
) {
    public SignUpCommand toCommand() {
        return new SignUpCommand(username, password);
    }
}
