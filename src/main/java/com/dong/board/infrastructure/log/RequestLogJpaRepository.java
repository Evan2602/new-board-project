package com.dong.board.infrastructure.log;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 요청 로그 Spring Data JPA 인터페이스
 * - JpaRepository: 기본 CRUD 메서드 자동 구현
 * - JpaSpecificationExecutor: Specification 기반 동적 필터 조회 지원
 *   → findAll(Specification, Pageable) 메서드를 자동으로 제공
 */
public interface RequestLogJpaRepository
        extends JpaRepository<JpaRequestLogEntity, Long>,
                JpaSpecificationExecutor<JpaRequestLogEntity> {
}
