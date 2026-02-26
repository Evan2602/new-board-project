package com.dong.board.exception;

/**
 * 이미 사용 중인 사용자명으로 가입 시도 시 발생하는 예외 (HTTP 409)
 */
public class DuplicateUsernameException extends RuntimeException {

    public DuplicateUsernameException(String username) {
        super("이미 사용 중인 사용자명입니다: " + username);
    }
}
