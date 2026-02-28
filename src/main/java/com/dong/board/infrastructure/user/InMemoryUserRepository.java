package com.dong.board.infrastructure.user;

import com.dong.board.domain.user.User;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 인메모리 사용자 저장소 구현체 (비활성화)
 * JPA 연동 후 JpaUserRepository로 대체됨
 * 빈 충돌 방지를 위해 @Repository 어노테이션 제거
 */
public class InMemoryUserRepository implements UserRepository {

    // 저장소: key = 로그인 ID(userId), value = 사용자 객체
    // userId를 key로 쓰는 이유: userId로 빠르게 사용자를 찾기 위해 (Map의 O(1) 조회)
    private final Map<String, User> store = new ConcurrentHashMap<>();

    // ID 자동 증가 카운터 (0에서 시작, 저장 시마다 1씩 증가)
    private final AtomicLong idSequence = new AtomicLong(0);

    @Override
    public User save(User user) {
        // 로그인 ID를 key로 사용해서 저장 (같은 ID로 다시 저장하면 덮어씀)
        store.put(user.getUserId(), user);
        return user;
    }

    @Override
    public Optional<User> findByUserId(String userId) {
        // Map에서 로그인 ID로 조회, 없으면 null → Optional.empty() 반환
        return Optional.ofNullable(store.get(userId));
    }

    @Override
    public boolean existsByUserId(String userId) {
        // Map의 key에 해당 로그인 ID가 있는지 확인
        return store.containsKey(userId);
    }

    /**
     * DB 고유 ID로 사용자 조회 (인메모리 미사용 구현체)
     */
    @Override
    public java.util.Optional<User> findById(Long id) {
        return store.values().stream()
                .filter(user -> id.equals(user.getId()))
                .findFirst();
    }

    /**
     * 닉네임 검색 + 페이징 조회 (인메모리 미사용 구현체)
     */
    @Override
    public org.springframework.data.domain.Page<User> findAll(String nicknameKeyword, org.springframework.data.domain.Pageable pageable) {
        return org.springframework.data.domain.Page.empty(pageable);
    }
}
