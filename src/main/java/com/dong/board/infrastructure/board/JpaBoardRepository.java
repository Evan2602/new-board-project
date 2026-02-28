package com.dong.board.infrastructure.board;

import com.dong.board.domain.board.Board;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * BoardRepository 인터페이스의 JPA 구현체
 * - InMemoryBoardRepository를 대체
 * - JpaBoardEntity(JPA 레이어) ↔ Board(도메인 레이어) 변환을 담당
 * - 도메인 레이어는 이 클래스의 존재를 모름 (의존 방향: 인프라 → 도메인)
 */
@Repository
@RequiredArgsConstructor
public class JpaBoardRepository implements BoardRepository {

    // Spring Data JPA가 자동 구현하는 저장소
    private final BoardJpaRepository boardJpaRepository;

    /**
     * 게시글 저장 (신규 생성 또는 수정)
     * - board.getId()가 null이면 INSERT, 있으면 UPDATE
     * - 저장 후 DB가 발급한 ID가 포함된 Board 객체를 반환
     */
    @Override
    public Board save(Board board) {
        JpaBoardEntity entity = JpaBoardEntity.fromDomain(board);
        JpaBoardEntity saved = boardJpaRepository.save(entity);
        return saved.toDomain();
    }

    /**
     * ID로 게시글 조회
     * JPA 엔티티를 도메인 객체로 변환해서 반환
     */
    @Override
    public Optional<Board> findById(Long id) {
        return boardJpaRepository.findById(id)
                .map(JpaBoardEntity::toDomain);
    }

    /**
     * 전체 게시글 목록 조회 (작성날짜 오름차순 - 오래된 글이 먼저, 최신 글이 마지막)
     */
    @Override
    public List<Board> findAll() {
        return boardJpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(JpaBoardEntity::toDomain)
                .toList();
    }

    /**
     * ID로 게시글 삭제
     */
    @Override
    public void deleteById(Long id) {
        boardJpaRepository.deleteById(id);
    }

    /**
     * ID로 게시글 존재 여부 확인
     */
    @Override
    public boolean existsById(Long id) {
        return boardJpaRepository.existsById(id);
    }

    /**
     * 특정 작성자의 게시글 목록 조회 (최신순)
     * 관리자 회원 상세 조회 시 작성 게시글 목록 표시용
     */
    @Override
    public List<Board> findByAuthorId(String authorId) {
        return boardJpaRepository.findByAuthorIdOrderByCreatedAtDesc(authorId).stream()
                .map(JpaBoardEntity::toDomain)
                .toList();
    }
}
