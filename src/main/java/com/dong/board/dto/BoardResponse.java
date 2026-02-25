package com.dong.board.dto;

import com.dong.board.domain.Board;

import java.time.LocalDateTime;

/**
 * 게시글 응답 DTO
 */
public record BoardResponse(
        Long id,
        String title,
        String content,
        String author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Board 엔티티로부터 응답 DTO 생성 정적 팩토리 메서드
     */
    public static BoardResponse from(Board board) {
        return new BoardResponse(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getAuthor(),
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }
}
