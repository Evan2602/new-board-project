package com.dong.board.repository;

import com.dong.board.domain.User;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 인메모리 사용자 저장소 구현체
 * - 실제 DB 없이 메모리(Map)에 사용자를 저장합니다
 * - ConcurrentHashMap: 여러 요청이 동시에 와도 안전하게 처리 (스레드 안전)
 * - AtomicLong: 여러 요청이 동시에 ID를 요청해도 번호가 겹치지 않음 (원자적 증가)
 */
@Repository
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

    @Override
    public Long generateId() {
        // 현재 카운터 값을 1 올리고 그 값을 반환 (원자적 연산: 동시 요청 시에도 안전)
        return idSequence.incrementAndGet();
    }
}
