package com.dong.board.infrastructure.board;

import com.dong.board.domain.board.Board;

import java.util.List;
import java.util.Optional;

/**
 * 게시글 저장소 인터페이스
 * 구현체(JpaBoardRepository 등)는 이 인터페이스에만 의존
 */
public interface BoardRepository {

    Board save(Board board);

    Optional<Board> findById(Long id);

    List<Board> findAll();

    void deleteById(Long id);

    boolean existsById(Long id);

    /**
     * 특정 작성자의 게시글 목록 조회 (최신순)
     * 관리자 회원 상세 조회 시 작성 게시글 목록 표시용
     *
     * @param authorId 작성자의 로그인 ID (예: "hong123")
     * @return 해당 작성자의 게시글 목록 (최신순 정렬)
     */
    List<Board> findByAuthorId(String authorId);
}
