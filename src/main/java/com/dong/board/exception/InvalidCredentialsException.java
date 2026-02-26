package com.dong.board.exception;

/**
 * 사용자명 또는 비밀번호가 올바르지 않을 때 발생하는 예외 (HTTP 401)
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("사용자명 또는 비밀번호가 올바르지 않습니다.");
    }
}
