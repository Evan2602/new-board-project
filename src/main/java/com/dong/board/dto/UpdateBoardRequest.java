package com.dong.board.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 게시글 수정 요청 DTO
 */
public record UpdateBoardRequest(

        @NotBlank(message = "제목은 필수입니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        String content
) {}
