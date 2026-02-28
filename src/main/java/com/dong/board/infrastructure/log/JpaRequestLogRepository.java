package com.dong.board.infrastructure.log;

import com.dong.board.domain.log.RequestLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 요청 로그 JPA 구현체
 * - RequestLogRepository 인터페이스를 구현
 * - JPA Specification 패턴으로 동적 필터 조건 처리
 */
@Repository
@RequiredArgsConstructor
public class JpaRequestLogRepository implements RequestLogRepository {

    private final RequestLogJpaRepository jpaRepository;

    @Override
    public RequestLog save(RequestLog log) {
        JpaRequestLogEntity entity = JpaRequestLogEntity.fromDomain(log);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<RequestLog> findById(Long id) {
        return jpaRepository.findById(id).map(JpaRequestLogEntity::toDomain);
    }

    @Override
    public Page<RequestLog> findAll(RequestLogSearchCondition condition, Pageable pageable) {
        // 각 필터 조건을 Specification으로 조합 (null 조건은 자동 무시)
        Specification<JpaRequestLogEntity> spec = buildSpecification(condition);
        return jpaRepository.findAll(spec, pageable).map(JpaRequestLogEntity::toDomain);
    }

    /**
     * 조회 조건으로부터 JPA Specification 생성
     * null 조건은 where(null)로 처리되어 자동으로 전체 조회
     */
    private Specification<JpaRequestLogEntity> buildSpecification(RequestLogSearchCondition cond) {
        return Specification
                .where(requestAtBetween(cond.startAt(), cond.endAt()))
                .and(statusGroupIn(cond.statusGroup()))
                .and(userIdEquals(cond.userId()))
                .and(urlContains(cond.urlKeyword()));
    }

    /**
     * 기간 필터: requestAt BETWEEN startAt AND endAt
     * 각 경계값이 null이면 해당 방향 제한 없음
     */
    private Specification<JpaRequestLogEntity> requestAtBetween(
            LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null) return null;
            if (start == null) return cb.lessThanOrEqualTo(root.get("requestAt"), end);
            if (end == null) return cb.greaterThanOrEqualTo(root.get("requestAt"), start);
            return cb.between(root.get("requestAt"), start, end);
        };
    }

    /**
     * 상태코드 그룹 필터
     * "2xx" → 200~299, "4xx" → 400~499, "5xx" → 500~599
     */
    private Specification<JpaRequestLogEntity> statusGroupIn(String group) {
        return (root, query, cb) -> {
            if (group == null || group.isBlank()) return null;
            return switch (group) {
                case "2xx" -> cb.between(root.get("statusCode"), 200, 299);
                case "4xx" -> cb.between(root.get("statusCode"), 400, 499);
                case "5xx" -> cb.between(root.get("statusCode"), 500, 599);
                default -> null; // 알 수 없는 그룹 → 전체 조회
            };
        };
    }

    /**
     * 사용자 ID 정확 일치 필터
     */
    private Specification<JpaRequestLogEntity> userIdEquals(String userId) {
        return (root, query, cb) -> {
            if (userId == null || userId.isBlank()) return null;
            return cb.equal(root.get("userId"), userId);
        };
    }

    /**
     * URL 키워드 부분 일치 필터 (LIKE %keyword%)
     */
    private Specification<JpaRequestLogEntity> urlContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            return cb.like(root.get("url"), "%" + keyword + "%");
        };
    }
}
