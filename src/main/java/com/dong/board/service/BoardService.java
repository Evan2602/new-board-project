package com.dong.board.service;

import com.dong.board.domain.Board;
import com.dong.board.exception.BoardAccessDeniedException;
import com.dong.board.exception.BoardNotFoundException;
import com.dong.board.repository.BoardRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 게시글 비즈니스 로직 서비스
 * HTTP 관련 DTO(Request/Response)에 의존하지 않고 Command/Result 객체를 사용합니다
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
    public BoardResult getBoard(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new BoardNotFoundException(id));
        return BoardResult.from(board);
    }

    /**
     * 게시글 목록 전체 조회
     */
    public List<BoardResult> getBoardList() {
        return boardRepository.findAll().stream()
                .map(BoardResult::from)
                .toList();
    }

    /**
     * 게시글 생성
     */
    public BoardResult createBoard(CreateBoardCommand command) {
        Long id = boardRepository.generateId();
        Board board = Board.create(id, command.title(), command.content(), command.authorId());
        Board saved = boardRepository.save(board);
        return BoardResult.from(saved);
    }

    /**
     * 게시글 수정 (작성자 본인만 가능)
     */
    public BoardResult updateBoard(Long id, UpdateBoardCommand command, String requestingUserId) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new BoardNotFoundException(id));
        if (!board.getAuthorId().equals(requestingUserId)) {
            throw new BoardAccessDeniedException();
        }
        board.update(command.title(), command.content());
        Board saved = boardRepository.save(board);
        return BoardResult.from(saved);
    }

    /**
     * 게시글 삭제 (작성자 본인만 가능)
     */
    public void deleteBoard(Long id, String requestingUserId) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new BoardNotFoundException(id));
        if (!board.getAuthorId().equals(requestingUserId)) {
            throw new BoardAccessDeniedException();
        }
        boardRepository.deleteById(id);
    }
}
