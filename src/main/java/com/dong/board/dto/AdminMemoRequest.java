package com.dong.board.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 관리자 메모 수정 요청 DTO
 * - 빈 문자열("")로 요청 시 기존 메모 삭제
 */
public record AdminMemoRequest(
        @NotNull(message = "메모 내용은 null일 수 없습니다.")
        String memo
) {
}
