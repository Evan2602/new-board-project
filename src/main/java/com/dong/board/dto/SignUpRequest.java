package com.dong.board.dto;

import com.dong.board.service.SignUpCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 HTTP 요청 DTO
 *
 * 클라이언트가 보내는 JSON 형식:
 * {
 *   "userId": "hong123",
 *   "username": "홍길동",
 *   "password": "password1!"
 * }
 */
public record SignUpRequest(
        @NotBlank(message = "아이디는 필수입니다.")
        String userId,      // 로그인할 때 쓸 아이디 (예: "hong123")

        @NotBlank(message = "이름은 필수입니다.")
        String username,    // 화면에 표시할 이름 (예: "홍길동")

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password     // 비밀번호 (8자 이상 필수)
) {
    /**
     * HTTP 요청 DTO를 Service 레이어 커맨드 객체로 변환
     * DTO에 있는 HTTP 어노테이션이 서비스 레이어까지 오염되지 않도록 분리
     */
    public SignUpCommand toCommand() {
        return new SignUpCommand(userId, username, password);
    }
}
