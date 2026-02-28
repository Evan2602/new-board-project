package com.dong.board.domain.board;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 게시글 도메인 엔티티
 * - authorId: 게시글을 작성한 사람의 로그인 ID (예: "hong123")
 *   → 이름 대신 로그인 ID를 저장해서, 이름이 바뀌어도 "내 글" 판별이 가능
 */
@Getter
public class Board {

    // 시스템이 자동으로 부여하는 게시글 고유 번호
    private final Long id;

    // 게시글 제목 (수정 가능하므로 final 아님)
    private String title;

    // 게시글 본문 내용 (수정 가능)
    private String content;

    // 작성자의 로그인 ID (예: "hong123") — 이름이 아니라 ID 저장
    // 권한 검사 시: board.getAuthorId().equals(요청자의 userId)
    private final String authorId;

    // 게시글이 처음 생성된 날짜/시간 (변경 불가)
    private final LocalDateTime createdAt;

    // 마지막으로 수정된 날짜/시간 (수정 시마다 갱신)
    private LocalDateTime updatedAt;

    // 외부에서 직접 new Board(...)로 생성하지 못하도록 private
    private Board(Long id, String title, String content, String authorId,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 새 게시글 생성 정적 팩토리 메서드
     *
     * @param id       시스템이 부여하는 게시글 고유 번호
     * @param title    게시글 제목
     * @param content  게시글 본문
     * @param authorId 작성자의 로그인 ID (JWT에서 추출한 값)
     */
    public static Board create(Long id, String title, String content, String authorId) {
        // 생성 시점의 현재 시각을 createdAt과 updatedAt 양쪽에 기록
        LocalDateTime now = LocalDateTime.now();
        return new Board(id, title, content, authorId, now, now);
    }

    /**
     * 신규 게시글 생성 (DB가 ID를 자동 발급할 때 사용)
     * JPA 연동 시 ID는 null로 생성하고, save() 후 DB가 AUTO_INCREMENT로 발급
     *
     * @param title    게시글 제목
     * @param content  게시글 본문
     * @param authorId 작성자의 로그인 ID (JWT에서 추출한 값)
     */
    public static Board createNew(String title, String content, String authorId) {
        LocalDateTime now = LocalDateTime.now();
        return new Board(null, title, content, authorId, now, now);
    }

    /**
     * DB 조회 결과로부터 도메인 객체 복원
     * JPA 인프라 레이어에서만 사용 — 비즈니스 로직에서 직접 호출 금지
     *
     * @param id        DB가 발급한 게시글 ID
     * @param title     게시글 제목
     * @param content   게시글 본문
     * @param authorId  작성자 로그인 ID
     * @param createdAt DB에 저장된 생성 시각
     * @param updatedAt DB에 저장된 최종 수정 시각
     */
    public static Board reconstruct(Long id, String title, String content, String authorId,
                                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Board(id, title, content, authorId, createdAt, updatedAt);
    }

    /**
     * 게시글 내용 수정 (Setter 대신 의미 있는 메서드 사용)
     * 수정 시각도 자동으로 업데이트됩니다
     */
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
        // 수정한 시각을 기록
        this.updatedAt = LocalDateTime.now();
    }
}
