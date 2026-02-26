package com.dong.board.service;

/**
 * 게시글 생성 서비스 요청 커맨드
 * Controller → Service 레이어 간 데이터 전달 객체 (HTTP 관련 어노테이션 없음)
 */
public record CreateBoardCommand(
        String title,
        String content,
        String author
) {}
