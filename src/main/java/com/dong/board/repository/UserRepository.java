package com.dong.board.repository;

import com.dong.board.domain.User;

import java.util.Optional;

/**
 * 사용자 저장소 인터페이스
 * 구체적인 저장 방식(인메모리, DB 등)에 의존하지 않는 추상화 계층
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findByUserId(String userId);

    boolean existsByUserId(String userId);

    Long generateId();
}
