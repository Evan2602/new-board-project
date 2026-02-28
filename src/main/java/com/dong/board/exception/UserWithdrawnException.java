package com.dong.board.exception;

/**
 * 탈퇴한 계정으로 로그인 시도 시 발생하는 예외
 * → 403 Forbidden
 */
public class UserWithdrawnException extends RuntimeException {
    public UserWithdrawnException() {
        super("탈퇴한 계정입니다.");
    }
}

