package com.dong.board.infrastructure.board;

import com.dong.board.domain.board.Board;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 게시글 JPA 엔티티
 * - 도메인 Board와 분리하여 JPA 제약(기본 생성자, 가변 필드)을 인프라 레이어에 격리
 * - 도메인 Board ↔ JpaBoardEntity 변환을 담당
 */
@Entity
@Table(name = "boards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 필수: protected 기본 생성자
public class JpaBoardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL AUTO_INCREMENT
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 작성자의 로그인 ID (예: "hong123") — FK 없이 로그인 ID 문자열로 저장
    @Column(nullable = false, length = 100)
    private String authorId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 도메인 Board → JPA 엔티티 변환
     * id가 null이면 신규 INSERT, id가 있으면 UPDATE 처리
     *
     * @param board 도메인 게시글 객체
     * @return JPA 엔티티
     */
    public static JpaBoardEntity fromDomain(Board board) {
        JpaBoardEntity entity = new JpaBoardEntity();
        entity.id = board.getId();
        entity.title = board.getTitle();
        entity.content = board.getContent();
        entity.authorId = board.getAuthorId();
        entity.createdAt = board.getCreatedAt();
        entity.updatedAt = board.getUpdatedAt();
        return entity;
    }

    /**
     * JPA 엔티티 → 도메인 Board 변환
     * DB 조회 후 도메인 객체로 복원할 때 사용
     *
     * @return 도메인 게시글 객체
     */
    public Board toDomain() {
        return Board.reconstruct(id, title, content, authorId, createdAt, updatedAt);
    }
}
