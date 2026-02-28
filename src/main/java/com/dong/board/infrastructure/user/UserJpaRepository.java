package com.dong.board.infrastructure.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 사용자 Spring Data JPA 저장소
 * - JpaRepository가 기본 CRUD 메서드를 자동으로 구현
 * - JpaUserRepository (구현체)에서 내부적으로 사용
 */
public interface UserJpaRepository extends JpaRepository<JpaUserEntity, Long> {

    /**
     * 로그인 ID로 사용자 조회
     * Spring Data JPA가 메서드명 기반으로 쿼리를 자동 생성:
     * SELECT * FROM users WHERE user_id = ?
     */
    Optional<JpaUserEntity> findByUserId(String userId);

    /**
     * 로그인 ID 존재 여부 확인
     * Spring Data JPA가 메서드명 기반으로 쿼리를 자동 생성:
     * SELECT COUNT(*) > 0 FROM users WHERE user_id = ?
     */
    boolean existsByUserId(String userId);
}
