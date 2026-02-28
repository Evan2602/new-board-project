package com.dong.board.infrastructure.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 토큰 블랙리스트 Spring Data JPA 저장소
 * - JpaRepository가 기본 CRUD 메서드를 자동으로 구현
 * - JpaTokenBlacklistRepository (구현체)에서 내부적으로 사용
 */
public interface TokenBlacklistJpaRepository extends JpaRepository<JpaTokenBlacklistEntity, Long> {

    /**
     * 회원 로그인 ID로 블랙리스트 항목 조회
     * SELECT * FROM token_blacklist WHERE user_id = ?
     */
    Optional<JpaTokenBlacklistEntity> findByUserId(String userId);
}

