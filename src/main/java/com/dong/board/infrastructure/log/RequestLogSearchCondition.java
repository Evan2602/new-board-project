package com.dong.board.infrastructure.log;

import java.time.LocalDateTime;

/**
 * 요청 로그 조회 필터 조건
 * null인 필드는 해당 조건을 무시하고 전체 조회
 *
 * @param startAt     조회 시작 시각 (null이면 필터 없음)
 * @param endAt       조회 종료 시각 (null이면 필터 없음)
 * @param statusGroup 상태코드 그룹 ("2xx", "4xx", "5xx", null이면 전체)
 * @param userId      특정 사용자 ID 필터 (null이면 전체)
 * @param urlKeyword  URL 키워드 검색 (null이면 전체, LIKE 검색)
 */
public record RequestLogSearchCondition(
        LocalDateTime startAt,
        LocalDateTime endAt,
        String statusGroup,
        String userId,
        String urlKeyword
) {
}
