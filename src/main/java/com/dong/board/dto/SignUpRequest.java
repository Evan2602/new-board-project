package com.dong.board.dto;

import com.dong.board.service.SignUpCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 HTTP 요청 DTO
 */
public record SignUpRequest(
        @NotBlank(message = "아이디는 필수입니다.")
        String userId,

        @NotBlank(message = "이름은 필수입니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password
) {
    /**
     * HTTP 요청 DTO를 Service 레이어 커맨드 객체로 변환
     */
    public SignUpCommand toCommand() {
        return new SignUpCommand(userId, username, password);
    }
}
