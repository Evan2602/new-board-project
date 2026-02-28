package com.dong.board.exception;

/**
 * 회원을 찾을 수 없을 때 발생하는 예외
 * → 404 Not Found
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("회원을 찾을 수 없습니다. id: " + id);
    }
}

