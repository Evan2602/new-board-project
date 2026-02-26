package com.dong.board.service;

import com.dong.board.domain.Board;

import java.time.LocalDateTime;

/**
 * 게시글 서비스 응답 결과
 * Service → Controller 레이어 간 데이터 전달 객체 (도메인 엔티티를 외부에 노출하지 않음)
 *
 * - authorId: 작성자의 로그인 ID (예: "hong123")
 *   → 이름 대신 ID를 저장해서 이름 변경 시에도 "내 글" 판별 가능
 */
public record BoardResult(
        Long id,
        String title,
        String content,
        String authorId,       // 작성자의 로그인 ID (예: "hong123")
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Board 도메인 엔티티로부터 서비스 결과 생성 정적 팩토리 메서드
     * 도메인 객체를 서비스 결과 레코드로 변환합니다
     */
    public static BoardResult from(Board board) {
        return new BoardResult(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getAuthorId(),   // authorId로 변경
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }
}
