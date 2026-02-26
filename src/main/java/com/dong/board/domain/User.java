package com.dong.board.domain;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 도메인 엔티티
 * - userId: 로그인할 때 쓰는 ID (사용자가 직접 지정, 예: "hong123")
 * - username: 화면에 표시할 이름 (닉네임, 예: "홍길동")
 */
@Getter
public class User {

    private final Long id;
    private final String userId;
    private final String username;
    private final String password;   // BCrypt 해시
    private final LocalDateTime createdAt;

    private User(Long id, String userId, String username, String password, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.createdAt = createdAt;
    }

    /**
     * 새 사용자 생성 정적 팩토리 메서드
     */
    public static User create(Long id, String userId, String username, String encodedPassword) {
        return new User(id, userId, username, encodedPassword, LocalDateTime.now());
    }
}
