package com.dong.board.domain.user;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 도메인 엔티티
 * - userId: 로그인할 때 쓰는 ID (사용자가 직접 지정, 예: "hong123")
 * - username: 화면에 표시할 이름 (닉네임, 예: "홍길동")
 * - role: 사용자 권한 (ROLE_USER 일반, ROLE_ADMIN 관리자)
 * - status: 계정 상태 (ACTIVE 정상, SUSPENDED 정지, WITHDRAWN 탈퇴)
 */
@Getter
public class User {

    /**
     * 사용자 권한 역할
     * - ROLE_USER: 일반 사용자 (게시글 CRUD)
     * - ROLE_ADMIN: 관리자 (/admin/** 접근 가능)
     */
    public enum UserRole {
        ROLE_USER,
        ROLE_ADMIN
    }

    /**
     * 계정 상태
     * - ACTIVE: 정상 (로그인 및 모든 기능 이용 가능)
     * - SUSPENDED: 정지 (로그인 불가, 관리자에 의해 정지된 상태)
     * - WITHDRAWN: 탈퇴 (로그인 불가, 탈퇴 처리된 상태)
     */
    public enum UserStatus {
        ACTIVE,
        SUSPENDED,
        WITHDRAWN
    }

    // 시스템이 자동으로 부여하는 고유 식별 번호 (1, 2, 3 ...)
    private final Long id;

    // 로그인할 때 쓰는 ID (사용자가 직접 지정, 예: "hong123")
    // JWT 토큰의 subject(주인 식별자)로 사용됩니다
    private final String userId;

    // 화면에 표시할 이름 (닉네임, 예: "홍길동")
    // 이름이 바뀌어도 로그인 ID에는 영향이 없습니다
    private final String username;

    // BCrypt 알고리즘으로 해시된 비밀번호 (원문 복원 불가)
    // 비밀번호 초기화 시 변경 가능
    private String password;

    // 가입한 날짜/시간 (한 번 설정 후 변경 불가)
    private final LocalDateTime createdAt;

    // 사용자 권한 역할 (기본값 ROLE_USER)
    private final UserRole role;

    // 계정 상태 (기본값 ACTIVE, 관리자가 변경 가능)
    private UserStatus status;

    // 외부에서 new User(...)로 직접 생성하지 못하도록 private으로 막음
    private User(Long id, String userId, String username, String password,
                 LocalDateTime createdAt, UserRole role, UserStatus status) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.createdAt = createdAt;
        this.role = role;
        this.status = status;
    }

    /**
     * 새 사용자 생성 정적 팩토리 메서드
     *
     * @param id              시스템이 부여하는 고유 번호
     * @param userId          로그인할 때 쓸 아이디 (예: "hong123")
     * @param username        화면에 표시할 이름 (예: "홍길동")
     * @param encodedPassword BCrypt로 이미 해시된 비밀번호
     */
    public static User create(Long id, String userId, String username, String encodedPassword) {
        return new User(id, userId, username, encodedPassword, LocalDateTime.now(),
                UserRole.ROLE_USER, UserStatus.ACTIVE);
    }

    /**
     * 신규 일반 사용자 생성 (DB가 ID를 자동 발급할 때 사용)
     * 기본 권한: ROLE_USER, 기본 상태: ACTIVE
     *
     * @param userId          로그인할 때 쓸 아이디 (예: "hong123")
     * @param username        화면에 표시할 이름 (예: "홍길동")
     * @param encodedPassword BCrypt로 이미 해시된 비밀번호
     */
    public static User createNew(String userId, String username, String encodedPassword) {
        return new User(null, userId, username, encodedPassword, LocalDateTime.now(),
                UserRole.ROLE_USER, UserStatus.ACTIVE);
    }

    /**
     * 신규 관리자 계정 생성 (AdminInitializer에서만 사용)
     * 권한: ROLE_ADMIN — /admin/** 접근 가능, 기본 상태: ACTIVE
     *
     * @param userId          관리자 로그인 ID
     * @param username        관리자 표시 이름
     * @param encodedPassword BCrypt로 이미 해시된 비밀번호
     */
    public static User createAdmin(String userId, String username, String encodedPassword) {
        return new User(null, userId, username, encodedPassword, LocalDateTime.now(),
                UserRole.ROLE_ADMIN, UserStatus.ACTIVE);
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
     * @param role      사용자 권한 역할
     * @param status    계정 상태
     */
    public static User reconstruct(Long id, String userId, String username,
                                   String password, LocalDateTime createdAt,
                                   UserRole role, UserStatus status) {
        return new User(id, userId, username, password, createdAt, role, status);
    }

    /**
     * 계정 상태 변경 (관리자만 호출 가능)
     * ACTIVE ↔ SUSPENDED ↔ WITHDRAWN 전환
     *
     * @param newStatus 변경할 상태
     */
    public void changeStatus(UserStatus newStatus) {
        this.status = newStatus;
    }

    /**
     * 비밀번호 초기화 (관리자만 호출 가능)
     * 임시 비밀번호로 교체 — BCrypt 해시된 값을 저장
     *
     * @param encodedPassword BCrypt로 이미 해시된 임시 비밀번호
     */
    public void resetPassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
