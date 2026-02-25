package com.dong.board.service;

import com.dong.board.domain.Board;
import com.dong.board.dto.BoardResponse;
import com.dong.board.dto.CreateBoardRequest;
import com.dong.board.dto.UpdateBoardRequest;
import com.dong.board.exception.BoardNotFoundException;
import com.dong.board.repository.BoardRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 게시글 비즈니스 로직 서비스
 * BoardRepository 인터페이스 타입으로 주입받아 구현체에 직접 의존하지 않음
 */
@Service
public class BoardService {

    private final BoardRepository boardRepository;

    public BoardService(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }

    /**
     * 게시글 단건 조회
     */
    public BoardResponse getBoard(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new BoardNotFoundException(id));
        return BoardResponse.from(board);
    }

    /**
     * 게시글 목록 조회
     */
    public List<BoardResponse> getBoardList() {
        return boardRepository.findAll().stream()
                .map(BoardResponse::from)
                .toList();
    }

    /**
     * 게시글 생성
     */
    public BoardResponse createBoard(CreateBoardRequest request) {
        Long id = boardRepository.generateId();
        Board board = Board.create(id, request.title(), request.content(), request.author());
        Board saved = boardRepository.save(board);
        return BoardResponse.from(saved);
    }

    /**
     * 게시글 수정
     */
    public BoardResponse updateBoard(Long id, UpdateBoardRequest request) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new BoardNotFoundException(id));
        board.update(request.title(), request.content());
        Board saved = boardRepository.save(board);
        return BoardResponse.from(saved);
    }

    /**
     * 게시글 삭제
     */
    public void deleteBoard(Long id) {
        if (!boardRepository.existsById(id)) {
            throw new BoardNotFoundException(id);
        }
        boardRepository.deleteById(id);
    }
}
