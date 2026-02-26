package com.dong.board.dto;

import com.dong.board.service.LoginCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 HTTP 요청 DTO
 *
 * 클라이언트가 보내는 JSON 형식:
 * {
 *   "userId": "hong123",
 *   "password": "password1!"
 * }
 */
public record LoginRequest(
        @NotBlank(message = "아이디는 필수입니다.")
        String userId,    // 로그인 ID (예: "hong123")

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password   // 비밀번호
) {
    /**
     * HTTP 요청 DTO를 Service 레이어 커맨드 객체로 변환
     */
    public LoginCommand toCommand() {
        return new LoginCommand(userId, password);
    }
}
