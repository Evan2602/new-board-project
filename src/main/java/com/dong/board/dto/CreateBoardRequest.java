package com.dong.board.dto;

import com.dong.board.service.CreateBoardCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * 게시글 생성 HTTP 요청 DTO (Controller 레이어 전용)
 * 작성자(author)는 JWT 토큰에서 자동 추출하므로 요청에서 제거
 */
public record CreateBoardRequest(
        @NotBlank(message = "제목은 필수입니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        String content
) {
    /**
     * Service 레이어 요청 객체로 변환 (JWT에서 추출한 작성자명 전달)
     */
    public CreateBoardCommand toCommand(String author) {
        return new CreateBoardCommand(title, content, author);
    }
}
