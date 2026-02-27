package com.dong.board.domain.board;

import java.time.LocalDateTime;

/**
 * 게시글 서비스 응답 결과
 * Service → Controller 레이어 간 데이터 전달 객체 (도메인 엔티티를 외부에 노출하지 않음)
 */
public record BoardResult(
        Long id,
        String title,
        String content,
        String authorId,   // 작성자의 로그인 ID (예: "hong123")
        String userName,   // 작성자의 닉네임 (예: "홍길동")
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
