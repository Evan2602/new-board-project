package com.dong.board.dto;

import com.dong.board.domain.log.RequestLogResult;

import java.time.LocalDateTime;

/**
 * 관리자 로그 상세 조회 응답 DTO
 * - 목록과 달리 stackTrace, adminMemo 포함
 * - 에러 원인 분석 및 관리자 메모 확인 용도
 */
public record AdminLogDetailResponse(
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
        String stackTrace,  // 상세 조회에서만 포함
        String adminMemo    // 관리자 메모 (없으면 null)
) {

    /**
     * RequestLogResult → AdminLogDetailResponse 변환
     */
    public static AdminLogDetailResponse from(RequestLogResult result) {
        return new AdminLogDetailResponse(
                result.id(),
                result.requestId(),
                result.requestAt(),
                result.method(),
                result.url(),
                result.userId(),
                result.ip(),
                result.userAgent(),
                result.statusCode(),
                result.durationMs(),
                result.errorMessage(),
                result.stackTrace(),
                result.adminMemo()
        );
    }
}
