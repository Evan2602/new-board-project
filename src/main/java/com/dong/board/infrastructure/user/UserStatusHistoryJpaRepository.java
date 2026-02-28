package com.dong.board.infrastructure.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 회원 상태 변경 이력 Spring Data JPA 저장소
 * - JpaRepository가 기본 CRUD 메서드를 자동으로 구현
 * - JpaUserStatusHistoryRepository (구현체)에서 내부적으로 사용
 */
public interface UserStatusHistoryJpaRepository extends JpaRepository<JpaUserStatusHistoryEntity, Long> {

    /**
     * 특정 회원의 상태 변경 이력 조회 (최신순)
     * SELECT * FROM user_status_histories WHERE user_id = ? ORDER BY created_at DESC
     */
    List<JpaUserStatusHistoryEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}

