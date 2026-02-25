package com.dong.board.exception;

/**
 * 게시글을 찾을 수 없을 때 발생하는 예외 (HTTP 404)
 */
public class BoardNotFoundException extends RuntimeException {

    public BoardNotFoundException(Long id) {
        super("게시글을 찾을 수 없습니다. ID: " + id);
    }
}
