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

    // 시스템이 자동으로 부여하는 고유 식별 번호 (1, 2, 3 ...)
    private final Long id;

    // 로그인할 때 쓰는 ID (사용자가 직접 지정, 예: "hong123")
    // JWT 토큰의 subject(주인 식별자)로 사용됩니다
    private final String userId;

    // 화면에 표시할 이름 (닉네임, 예: "홍길동")
    // 이름이 바뀌어도 로그인 ID에는 영향이 없습니다
    private final String username;

    // BCrypt 알고리즘으로 해시된 비밀번호 (원문 복원 불가)
    private final String password;

    // 가입한 날짜/시간 (한 번 설정 후 변경 불가)
    private final LocalDateTime createdAt;

    // 외부에서 new User(...)로 직접 생성하지 못하도록 private으로 막음
    private User(Long id, String userId, String username, String password, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.createdAt = createdAt;
    }

    /**
     * 새 사용자 생성 정적 팩토리 메서드
     * new User(...) 대신 이 메서드를 통해 생성 → 생성 의도가 명확해짐
     *
     * @param id              시스템이 부여하는 고유 번호
     * @param userId          로그인할 때 쓸 아이디 (예: "hong123")
     * @param username        화면에 표시할 이름 (예: "홍길동")
     * @param encodedPassword BCrypt로 이미 해시된 비밀번호
     */
    public static User create(Long id, String userId, String username, String encodedPassword) {
        // 생성 시점의 현재 시각을 가입일로 기록
        return new User(id, userId, username, encodedPassword, LocalDateTime.now());
    }
}
