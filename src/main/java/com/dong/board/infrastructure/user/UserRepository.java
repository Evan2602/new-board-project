package com.dong.board.infrastructure.user;

import com.dong.board.domain.user.User;

import java.util.Optional;

/**
 * 사용자 저장소 인터페이스
 * 구체적인 저장 방식(인메모리, DB 등)에 의존하지 않는 추상화 계층
 */
public interface UserRepository {

    // 사용자 저장 (신규 생성 또는 수정)
    User save(User user);

    /**
     * 로그인 ID로 사용자 조회
     * 로그인 처리 시 사용됩니다
     *
     * @param userId 로그인 ID (예: "hong123")
     * @return 사용자가 존재하면 Optional.of(user), 없으면 Optional.empty()
     */
    Optional<User> findByUserId(String userId);

    /**
     * 로그인 ID가 이미 사용 중인지 확인
     * 회원가입 시 중복 검사에 사용됩니다
     *
     * @param userId 로그인 ID (예: "hong123")
     * @return 이미 존재하면 true, 사용 가능하면 false
     */
    boolean existsByUserId(String userId);
}
