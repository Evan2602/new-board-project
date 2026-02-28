package com.dong.board.domain.user;

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

    /**
     * 신규 사용자 생성 (DB가 ID를 자동 발급할 때 사용)
     * JPA 연동 시 ID는 null로 생성하고, save() 후 DB가 AUTO_INCREMENT로 발급
     *
     * @param userId          로그인할 때 쓸 아이디 (예: "hong123")
     * @param username        화면에 표시할 이름 (예: "홍길동")
     * @param encodedPassword BCrypt로 이미 해시된 비밀번호
     */
    public static User createNew(String userId, String username, String encodedPassword) {
        return new User(null, userId, username, encodedPassword, LocalDateTime.now());
    }

    /**
     * DB 조회 결과로부터 도메인 객체 복원
     * JPA 인프라 레이어에서만 사용 — 비즈니스 로직에서 직접 호출 금지
     *
     * @param id        DB가 발급한 사용자 ID
     * @param userId    로그인 ID
     * @param username  닉네임
     * @param password  BCrypt 해시된 비밀번호
     * @param createdAt DB에 저장된 가입 시각
     */
    public static User reconstruct(Long id, String userId, String username,
                                   String password, LocalDateTime createdAt) {
        return new User(id, userId, username, password, createdAt);
    }
}
