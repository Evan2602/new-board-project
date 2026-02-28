package com.dong.board.domain.log;

import java.time.LocalDate;

/**
 * 요청 로그 조회 명령 객체
 * Controller → Service 데이터 전달 (HTTP 어노테이션 없음)
 * null 필드는 해당 조건을 무시하고 전체 조회
 *
 * @param startDate  조회 시작일 (null이면 필터 없음)
 * @param endDate    조회 종료일 (null이면 필터 없음)
 * @param statusGroup 상태코드 그룹 ("2xx", "4xx", "5xx", null이면 전체)
 * @param userId     특정 사용자 ID 필터 (null이면 전체)
 * @param urlKeyword URL 키워드 검색 (null이면 전체)
 * @param page       페이지 번호 (0부터 시작)
 * @param size       페이지 크기
 */
public record RequestLogSearchCommand(
        LocalDate startDate,
        LocalDate endDate,
        String statusGroup,
        String userId,
        String urlKeyword,
        int page,
        int size
) {
}
