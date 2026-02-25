package com.dong.board.exception;

import java.time.LocalDateTime;

/**
 * 에러 응답 형식
 * 예시: {"code": "BOARD_NOT_FOUND", "message": "...", "timestamp": "..."}
 */
public record ErrorResponse(
        String code,
        String message,
        LocalDateTime timestamp
) {}
