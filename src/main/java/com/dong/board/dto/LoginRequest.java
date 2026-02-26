package com.dong.board.dto;

import com.dong.board.service.LoginCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 HTTP 요청 DTO
 */
public record LoginRequest(
        @NotBlank(message = "아이디는 필수입니다.")
        String userId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
    /**
     * HTTP 요청 DTO를 Service 레이어 커맨드 객체로 변환
     */
    public LoginCommand toCommand() {
        return new LoginCommand(userId, password);
    }
}
