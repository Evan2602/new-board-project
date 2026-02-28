package com.dong.board.dto;

import com.dong.board.domain.log.PageResult;
import com.dong.board.domain.log.RequestLogResult;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 로그 목록 조회 응답 DTO
 * - 목록에서는 stackTrace를 제외 (트래픽 절감)
 * - 상세 조회 시 AdminLogDetailResponse 사용
 */
public record AdminLogListResponse(
        List<LogSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * 로그 목록 아이템 (스택 트레이스 제외)
     */
    public record LogSummary(
            Long id,
            String requestId,
            LocalDateTime requestAt,
            String method,
            String url,
            String userId,
            String ip,
            Integer statusCode,
            Long durationMs,
            String errorMessage  // 에러 요약 메시지 (정상 요청은 null)
    ) {
        public static LogSummary from(RequestLogResult result) {
            return new LogSummary(
                    result.id(),
                    result.requestId(),
                    result.requestAt(),
                    result.method(),
                    result.url(),
                    result.userId(),
                    result.ip(),
                    result.statusCode(),
                    result.durationMs(),
                    result.errorMessage()
            );
        }
    }

    /**
     * PageResult<RequestLogResult> → AdminLogListResponse 변환
     */
    public static AdminLogListResponse from(PageResult<RequestLogResult> pageResult) {
        List<LogSummary> summaries = pageResult.content().stream()
                .map(LogSummary::from)
                .toList();
        return new AdminLogListResponse(
                summaries,
                pageResult.page(),
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages()
        );
    }
}
