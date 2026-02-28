package com.dong.board.infrastructure.user;

import com.dong.board.domain.user.TokenBlacklist;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * TokenBlacklistRepository 인터페이스의 JPA 구현체
 * JpaTokenBlacklistEntity(JPA 레이어) ↔ TokenBlacklist(도메인 레이어) 변환 담당
 */
@Repository
@RequiredArgsConstructor
public class JpaTokenBlacklistRepository implements TokenBlacklistRepository {

    private final TokenBlacklistJpaRepository jpaRepository;

    /**
     * 블랙리스트 항목 저장 (신규 등록 또는 invalidatedAt 갱신)
     * userId가 이미 존재하면 덮어쓰기 — 가장 최근 강제 로그아웃 시각만 유효
     */
    @Override
    public TokenBlacklist save(TokenBlacklist tokenBlacklist) {
        // 기존 항목이 있으면 덮어쓰기 위해 id를 함께 넘김
        JpaTokenBlacklistEntity entity = jpaRepository.findByUserId(tokenBlacklist.getUserId())
                .map(existing -> {
                    // 기존 항목의 id를 재사용하여 UPDATE 처리
                    JpaTokenBlacklistEntity updated = JpaTokenBlacklistEntity.fromDomain(tokenBlacklist);
                    return updated;
                })
                .orElse(JpaTokenBlacklistEntity.fromDomain(tokenBlacklist));
        return jpaRepository.save(entity).toDomain();
    }

    /**
     * 회원 로그인 ID로 블랙리스트 조회
     */
    @Override
    public Optional<TokenBlacklist> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId).map(JpaTokenBlacklistEntity::toDomain);
    }
}

