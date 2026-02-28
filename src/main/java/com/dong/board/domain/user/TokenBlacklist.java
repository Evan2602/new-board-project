package com.dong.board.domain.user;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 토큰 블랙리스트 도메인 엔티티
 * - 강제 로그아웃 시 해당 userId의 invalidatedAt을 기록
 * - JwtAuthenticationFilter에서 토큰 발급 시각(iat)이 invalidatedAt보다 이전이면 인증 거부
 * - userId 단위로 관리하므로 해당 회원의 기존 토큰 전체를 일괄 무효화 가능
 */
@Getter
public class TokenBlacklist {

    // DB AUTO_INCREMENT ID
    private final Long id;

    // 강제 로그아웃 처리된 회원의 로그인 ID
    private final String userId;

    // 이 시각 이전에 발급된 토큰은 모두 무효화됨
    private final LocalDateTime invalidatedAt;

    // 외부에서 직접 생성하지 못하도록 private
    private TokenBlacklist(Long id, String userId, LocalDateTime invalidatedAt) {
        this.id = id;
        this.userId = userId;
        this.invalidatedAt = invalidatedAt;
    }

    /**
     * 새 토큰 블랙리스트 항목 생성 (강제 로그아웃 처리 시 호출)
     * invalidatedAt = 현재 시각 → 이 시각 이전에 발급된 토큰은 모두 무효화
     *
     * @param userId 강제 로그아웃 대상 회원의 로그인 ID
     */
    public static TokenBlacklist create(String userId) {
        return new TokenBlacklist(null, userId, LocalDateTime.now());
    }

    /**
     * DB 조회 결과로부터 도메인 객체 복원
     * JPA 인프라 레이어에서만 사용 — 비즈니스 로직에서 직접 호출 금지
     */
    public static TokenBlacklist reconstruct(Long id, String userId, LocalDateTime invalidatedAt) {
        return new TokenBlacklist(id, userId, invalidatedAt);
    }
}

