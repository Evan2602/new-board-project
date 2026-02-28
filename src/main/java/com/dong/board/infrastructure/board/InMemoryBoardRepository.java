package com.dong.board.infrastructure.board;

import com.dong.board.domain.board.Board;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 인메모리 게시글 저장소 구현체 (비활성화)
 * JPA 연동 후 JpaBoardRepository로 대체됨
 * 빈 충돌 방지를 위해 @Repository 어노테이션 제거
 */
public class InMemoryBoardRepository implements BoardRepository {

    private final Map<Long, Board> store = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(0);

    @Override
    public Board save(Board board) {
        store.put(board.getId(), board);
        return board;
    }

    @Override
    public Optional<Board> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Board> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(Long id) {
        store.remove(id);
    }

    @Override
    public boolean existsById(Long id) {
        return store.containsKey(id);
    }

    /**
     * 작성자 ID로 게시글 목록 조회 (인메모리 미사용 구현체 — 빈 목록 반환)
     */
    @Override
    public List<Board> findByAuthorId(String authorId) {
        return store.values().stream()
                .filter(board -> board.getAuthorId().equals(authorId))
                .toList();
    }
}
