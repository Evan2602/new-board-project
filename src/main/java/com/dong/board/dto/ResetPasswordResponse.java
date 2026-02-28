package com.dong.board.dto;

/**
 * 비밀번호 초기화 응답 DTO
 * POST /admin/users/{id}/reset-password 응답 본문
 */
public record ResetPasswordResponse(
        String temporaryPassword,
        String message
) {
    public static ResetPasswordResponse of(String temporaryPassword) {
        return new ResetPasswordResponse(
                temporaryPassword,
                "비밀번호가 초기화되었습니다. 임시 비밀번호를 회원에게 안내해주세요."
        );
    }
}

