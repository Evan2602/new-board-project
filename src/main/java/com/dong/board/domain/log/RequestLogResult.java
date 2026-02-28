package com.dong.board.domain.log;

import java.time.LocalDateTime;

/**
 * 요청 로그 서비스 응답 객체
 * Service → Controller 데이터 전달 (HTTP 어노테이션 없음)
 */
public record RequestLogResult(
        Long id,
        String requestId,
        LocalDateTime requestAt,
        String method,
        String url,
        String userId,
        String ip,
        String userAgent,
        Integer statusCode,
        Long durationMs,
        String errorMessage,
        String stackTrace,
        String adminMemo
) {
    /**
     * 도메인 RequestLog → RequestLogResult 변환
     */
    public static RequestLogResult from(RequestLog log) {
        return new RequestLogResult(
                log.getId(),
                log.getRequestId(),
                log.getRequestAt(),
                log.getMethod(),
                log.getUrl(),
                log.getUserId(),
                log.getIp(),
                log.getUserAgent(),
                log.getStatusCode(),
                log.getDurationMs(),
                log.getErrorMessage(),
                log.getStackTrace(),
                log.getAdminMemo()
        );
    }
}
