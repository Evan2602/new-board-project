package com.dong.board.exception;

/**
 * 요청 로그를 찾을 수 없을 때 발생하는 예외 (HTTP 404)
 */
public class RequestLogNotFoundException extends RuntimeException {

    public RequestLogNotFoundException(Long id) {
        super("로그를 찾을 수 없습니다. id: " + id);
    }
}
