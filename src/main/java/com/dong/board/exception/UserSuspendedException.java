package com.dong.board.exception;

/**
 * 정지된 계정으로 로그인 시도 시 발생하는 예외
 * → 403 Forbidden
 */
public class UserSuspendedException extends RuntimeException {
    public UserSuspendedException() {
        super("계정이 정지되었습니다. 관리자에게 문의하세요.");
    }
}

