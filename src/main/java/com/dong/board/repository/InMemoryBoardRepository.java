package com.dong.board.repository;

import com.dong.board.domain.Board;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 인메모리 게시글 저장소 구현체
 * ConcurrentHashMap으로 스레드 안전성 보장, AtomicLong으로 ID 자동 증가
 */
@Repository
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

    @Override
    public Long generateId() {
        return idSequence.incrementAndGet();
    }
}
