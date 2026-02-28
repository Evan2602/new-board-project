package com.dong.board.infrastructure.user;

import com.dong.board.domain.user.TokenBlacklist;

import java.util.Optional;

/**
 * 토큰 블랙리스트 저장소 인터페이스
 * 구체적인 저장 방식에 의존하지 않는 추상화 계층
 */
public interface TokenBlacklistRepository {

    /**
     * 블랙리스트 항목 저장 (신규 등록 또는 갱신)
     *
     * @param tokenBlacklist 저장할 블랙리스트 도메인 객체
     * @return 저장된 블랙리스트 (DB 발급 ID 포함)
     */
    TokenBlacklist save(TokenBlacklist tokenBlacklist);

    /**
     * 회원 로그인 ID로 블랙리스트 조회
     *
     * @param userId 회원의 로그인 ID
     * @return 블랙리스트 항목 (없으면 Optional.empty())
     */
    Optional<TokenBlacklist> findByUserId(String userId);
}

