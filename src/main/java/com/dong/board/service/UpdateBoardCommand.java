package com.dong.board.service;

/**
 * 게시글 수정 서비스 요청 커맨드
 * Controller → Service 레이어 간 데이터 전달 객체 (HTTP 관련 어노테이션 없음)
 */
public record UpdateBoardCommand(
        String title,
        String content
) {}
