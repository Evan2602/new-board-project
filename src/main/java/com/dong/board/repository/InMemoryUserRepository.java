package com.dong.board.repository;

import com.dong.board.domain.User;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 인메모리 사용자 저장소 구현체
 * ConcurrentHashMap으로 스레드 안전성 보장, AtomicLong으로 ID 자동 증가
 */
@Repository
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> store = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(0);

    @Override
    public User save(User user) {
        store.put(user.getUsername(), user);
        return user;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(store.get(username));
    }

    @Override
    public boolean existsByUsername(String username) {
        return store.containsKey(username);
    }

    @Override
    public Long generateId() {
        return idSequence.incrementAndGet();
    }
}
