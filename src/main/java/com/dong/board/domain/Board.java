package com.dong.board.domain;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 게시글 도메인 엔티티
 */
@Getter
public class Board {

    private final Long id;
    private String title;
    private String content;
    private String author;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Board(Long id, String title, String content, String author,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 새 게시글 생성 정적 팩토리 메서드
     */
    public static Board create(Long id, String title, String content, String author) {
        LocalDateTime now = LocalDateTime.now();
        return new Board(id, title, content, author, now, now);
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
