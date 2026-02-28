package com.dong.board.dto;

import com.dong.board.domain.board.BoardResult;

import java.time.LocalDateTime;

/**
 * 게시글 HTTP 응답 DTO (Controller 레이어 전용)
 * 도메인 엔티티 대신 서비스 결과 객체(BoardResult)로부터 생성합니다
 *
 * 클라이언트에게 반환되는 JSON 형식:
 * {
 *   "id": 1,
 *   "title": "게시글 제목",
 *   "content": "게시글 내용",
 *   "authorId": "hong123",
 *   "userName": "홍길동",
 *   "isAdminAuthor": false,
 *   "createdAt": "2024-01-01T12:00:00",
 *   "updatedAt": "2024-01-01T12:00:00"
 * }
 */
public record BoardResponse(
        Long id,
        String title,
        String content,
        String authorId,
        String userName,
        boolean isAdminAuthor,   // 작성자가 관리자 권한(ROLE_ADMIN)을 가지고 있는지 여부
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
                result.userName(),
                result.isAdminAuthor(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
