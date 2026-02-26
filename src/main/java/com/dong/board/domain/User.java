package com.dong.board.domain;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 도메인 엔티티
 */
@Getter
public class User {

    private final Long id;
    private final String username;
    private final String password;   // BCrypt 해시
    private final LocalDateTime createdAt;

    private User(Long id, String username, String password, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.createdAt = createdAt;
    }

    /**
     * 새 사용자 생성 정적 팩토리 메서드
     */
    public static User create(Long id, String username, String encodedPassword) {
        return new User(id, username, encodedPassword, LocalDateTime.now());
    }
}
