package com.dong.board.domain.board;

/**
 * 게시글 생성 서비스 요청 커맨드
 * Controller → Service 레이어 간 데이터 전달 객체 (HTTP 관련 어노테이션 없음)
 *
 * - authorId: 작성자의 로그인 ID (JWT 토큰에서 추출한 값, 예: "hong123")
 */
public record CreateBoardCommand(
        String title,     // 게시글 제목
        String content,   // 게시글 본문
        String authorId   // 작성자의 로그인 ID (예: "hong123")
) {}
