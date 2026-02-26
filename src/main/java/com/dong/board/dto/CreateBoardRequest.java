package com.dong.board.dto;

import com.dong.board.service.CreateBoardCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * 게시글 생성 HTTP 요청 DTO (Controller 레이어 전용)
 * 작성자 정보는 요청 본문이 아니라 JWT 토큰에서 자동 추출하므로 이 DTO에 없습니다
 */
public record CreateBoardRequest(
        @NotBlank(message = "제목은 필수입니다.")
        String title,    // 게시글 제목

        @NotBlank(message = "내용은 필수입니다.")
        String content   // 게시글 본문
) {
    /**
     * Service 레이어 요청 객체로 변환
     * JWT에서 추출한 authorId(로그인 ID)를 여기서 함께 전달합니다
     *
     * @param authorId JWT 토큰에서 추출한 작성자의 로그인 ID (예: "hong123")
     */
    public CreateBoardCommand toCommand(String authorId) {
        return new CreateBoardCommand(title, content, authorId);
    }
}
