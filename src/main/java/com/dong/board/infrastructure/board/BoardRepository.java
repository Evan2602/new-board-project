package com.dong.board.infrastructure.board;

import com.dong.board.domain.board.Board;

import java.util.List;
import java.util.Optional;

/**
 * 게시글 저장소 인터페이스
 * 구현체(InMemoryBoardRepository 등)는 이 인터페이스에만 의존
 */
public interface BoardRepository {

    Board save(Board board);

    Optional<Board> findById(Long id);

    List<Board> findAll();

    void deleteById(Long id);

    boolean existsById(Long id);

    Long generateId();
}
