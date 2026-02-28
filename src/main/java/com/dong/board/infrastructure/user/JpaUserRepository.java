package com.dong.board.infrastructure.user;

import com.dong.board.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository 인터페이스의 JPA 구현체
 * - InMemoryUserRepository를 대체
 * - JpaUserEntity(JPA 레이어) ↔ User(도메인 레이어) 변환을 담당
 */
@Repository
@RequiredArgsConstructor
public class JpaUserRepository implements UserRepository {

    // Spring Data JPA가 자동 구현하는 저장소
    private final UserJpaRepository userJpaRepository;

    /**
     * 사용자 저장 (신규 생성)
     * - user.getId()가 null이면 INSERT
     * - 저장 후 DB가 발급한 ID가 포함된 User 객체를 반환
     */
    @Override
    public User save(User user) {
        JpaUserEntity entity = JpaUserEntity.fromDomain(user);
        JpaUserEntity saved = userJpaRepository.save(entity);
        return saved.toDomain();
    }

    /**
     * 로그인 ID로 사용자 조회
     * 로그인 처리 및 닉네임 조회 시 사용
     */
    @Override
    public Optional<User> findByUserId(String userId) {
        return userJpaRepository.findByUserId(userId)
                .map(JpaUserEntity::toDomain);
    }

    /**
     * 로그인 ID 중복 여부 확인
     * 회원가입 시 중복 검사에 사용
     */
    @Override
    public boolean existsByUserId(String userId) {
        return userJpaRepository.existsByUserId(userId);
    }
}
