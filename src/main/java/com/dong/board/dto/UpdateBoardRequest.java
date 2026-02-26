package com.dong.board.dto;

import com.dong.board.service.UpdateBoardCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * 게시글 수정 HTTP 요청 DTO (Controller 레이어 전용)
 * 검증 어노테이션은 이 레이어에서만 사용
 */
public record UpdateBoardRequest(

        @NotBlank(message = "제목은 필수입니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        String content
) {
    /**
     * Service 레이어 요청 객체로 변환
     */
    public UpdateBoardCommand toCommand() {
        return new UpdateBoardCommand(title, content);
    }
}
