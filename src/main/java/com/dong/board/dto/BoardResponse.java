package com.dong.board.dto;

import com.dong.board.service.BoardResult;

import java.time.LocalDateTime;

/**
 * 게시글 HTTP 응답 DTO (Controller 레이어 전용)
 * 도메인 엔티티 대신 서비스 결과 객체(BoardResult)로부터 생성합니다
 */
public record BoardResponse(
        Long id,
        String title,
        String content,
        String authorId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Service 레이어 결과 객체로부터 HTTP 응답 DTO 생성 정적 팩토리 메서드
     */
    public static BoardResponse from(BoardResult result) {
        return new BoardResponse(
                result.id(),
                result.title(),
                result.content(),
                result.authorId(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
