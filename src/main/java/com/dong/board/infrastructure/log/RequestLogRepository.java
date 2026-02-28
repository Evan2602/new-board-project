package com.dong.board.infrastructure.log;

import com.dong.board.domain.log.RequestLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * 요청 로그 저장소 인터페이스
 * Service 계층이 의존하는 추상화 — 구현체(JPA)와 Service를 분리
 */
public interface RequestLogRepository {

    /**
     * 요청 로그 저장
     *
     * @param log 저장할 요청 로그 도메인 객체
     * @return 저장된 로그 (DB 발급 ID 포함)
     */
    RequestLog save(RequestLog log);

    /**
     * ID로 요청 로그 단건 조회
     *
     * @param id 로그 ID
     * @return 로그 (없으면 Optional.empty())
     */
    Optional<RequestLog> findById(Long id);

    /**
     * 동적 필터 조건으로 요청 로그 페이징 조회
     *
     * @param condition 조회 필터 조건 (null 필드는 무시)
     * @param pageable  페이징 및 정렬 정보
     * @return 페이징된 로그 목록
     */
    Page<RequestLog> findAll(RequestLogSearchCondition condition, Pageable pageable);
}
