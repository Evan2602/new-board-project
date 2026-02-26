package com.dong.board.exception;

/**
 * 게시글 접근 권한이 없을 때 발생하는 예외 (HTTP 403)
 */
public class BoardAccessDeniedException extends RuntimeException {

    public BoardAccessDeniedException() {
        super("게시글에 대한 접근 권한이 없습니다.");
    }
}
