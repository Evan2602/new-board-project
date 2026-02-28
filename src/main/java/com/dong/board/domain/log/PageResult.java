package com.dong.board.domain.log;

import java.util.List;

/**
 * 페이징 결과 래퍼 객체 (제네릭)
 * Service → Controller 페이징 응답 전달
 *
 * @param <T>           응답 데이터 타입
 * @param content       현재 페이지 데이터 목록
 * @param page          현재 페이지 번호 (0부터 시작)
 * @param size          페이지 크기
 * @param totalElements 전체 데이터 건수
 * @param totalPages    전체 페이지 수
 */
public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
