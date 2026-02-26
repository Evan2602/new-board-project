package com.dong.board.service;

import com.dong.board.domain.Board;

import java.time.LocalDateTime;

/**
 * 게시글 서비스 응답 결과
 * Service → Controller 레이어 간 데이터 전달 객체 (도메인 엔티티를 외부에 노출하지 않음)
 */
public record BoardResult(
        Long id,
        String title,
        String content,
        String author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Board 도메인 엔티티로부터 서비스 결과 생성 정적 팩토리 메서드
     */
    public static BoardResult from(Board board) {
        return new BoardResult(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getAuthor(),
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }
}
