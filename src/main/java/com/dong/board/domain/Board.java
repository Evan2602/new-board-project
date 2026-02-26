package com.dong.board.domain;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 게시글 도메인 엔티티
 * - authorId: 게시글을 작성한 사람의 로그인 ID (예: "hong123")
 */
@Getter
public class Board {

    private final Long id;
    private String title;
    private String content;
    private final String authorId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
     */
    public static Board create(Long id, String title, String content, String authorId) {
        LocalDateTime now = LocalDateTime.now();
        return new Board(id, title, content, authorId, now, now);
    }

    /**
     * 게시글 내용 수정 (Setter 대신 의미 있는 메서드 사용)
     */
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }
}
