package com.dong.board.repository;

import com.dong.board.domain.User;

import java.util.Optional;

/**
 * 사용자 저장소 인터페이스
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Long generateId();
}
