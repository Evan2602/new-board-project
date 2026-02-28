package com.dong.board.infrastructure.user;

import com.dong.board.domain.user.TokenBlacklist;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 토큰 블랙리스트 JPA 엔티티
 * - 도메인 TokenBlacklist와 분리하여 JPA 제약을 인프라 레이어에 격리
 * - userId는 유니크 제약 — 회원당 하나의 블랙리스트 항목만 존재 (덮어쓰기 방식)
 */
@Entity
@Table(name = "token_blacklist",
        indexes = @Index(name = "idx_token_blacklist_user_id", columnList = "userId"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JpaTokenBlacklistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 강제 로그아웃 처리된 회원의 로그인 ID (유니크: 회원당 1개)
    @Column(nullable = false, unique = true, length = 50)
    private String userId;

    // 이 시각 이전에 발급된 토큰은 모두 무효 처리
    @Column(nullable = false)
    private LocalDateTime invalidatedAt;

    /**
     * 도메인 TokenBlacklist → JPA 엔티티 변환
     *
     * @param tokenBlacklist 도메인 블랙리스트 객체
     * @return JPA 엔티티
     */
    public static JpaTokenBlacklistEntity fromDomain(TokenBlacklist tokenBlacklist) {
        JpaTokenBlacklistEntity entity = new JpaTokenBlacklistEntity();
        entity.id = tokenBlacklist.getId();
        entity.userId = tokenBlacklist.getUserId();
        entity.invalidatedAt = tokenBlacklist.getInvalidatedAt();
        return entity;
    }

    /**
     * JPA 엔티티 → 도메인 TokenBlacklist 변환
     *
     * @return 도메인 블랙리스트 객체
     */
    public TokenBlacklist toDomain() {
        return TokenBlacklist.reconstruct(id, userId, invalidatedAt);
    }
}

