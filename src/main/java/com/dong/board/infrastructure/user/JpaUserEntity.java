package com.dong.board.infrastructure.user;

import com.dong.board.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 JPA 엔티티
 * - 도메인 User와 분리하여 JPA 제약을 인프라 레이어에 격리
 * - 도메인 User ↔ JpaUserEntity 변환을 담당
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 필수: protected 기본 생성자
public class JpaUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL AUTO_INCREMENT
    private Long id;

    // 로그인 ID (예: "hong123") — 유니크 제약
    @Column(nullable = false, unique = true, length = 50)
    private String userId;

    // 화면에 표시할 닉네임 (예: "홍길동")
    @Column(nullable = false, length = 50)
    private String username;

    // BCrypt 해시된 비밀번호 (원문 저장 절대 금지)
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 도메인 User → JPA 엔티티 변환
     *
     * @param user 도메인 사용자 객체
     * @return JPA 엔티티
     */
    public static JpaUserEntity fromDomain(User user) {
        JpaUserEntity entity = new JpaUserEntity();
        entity.id = user.getId();
        entity.userId = user.getUserId();
        entity.username = user.getUsername();
        entity.password = user.getPassword();
        entity.createdAt = user.getCreatedAt();
        return entity;
    }

    /**
     * JPA 엔티티 → 도메인 User 변환
     * DB 조회 후 도메인 객체로 복원할 때 사용
     *
     * @return 도메인 사용자 객체
     */
    public User toDomain() {
        return User.reconstruct(id, userId, username, password, createdAt);
    }
}
