package com.dong.board.controller;

import com.dong.board.dto.BoardResponse;
import com.dong.board.dto.CreateBoardRequest;
import com.dong.board.dto.UpdateBoardRequest;
import com.dong.board.service.BoardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 게시글 REST API 컨트롤러
 * HTTP 요청/응답 처리만 담당, 비즈니스 로직은 BoardService에 위임
 */
@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    /**
     * GET /api/boards → 게시글 목록 조회 (200)
     */
    @GetMapping
    public ResponseEntity<List<BoardResponse>> getBoardList() {
        return ResponseEntity.ok(boardService.getBoardList());
    }

    /**
     * GET /api/boards/{id} → 게시글 단건 조회 (200 / 404)
     */
    @GetMapping("/{id}")
    public ResponseEntity<BoardResponse> getBoard(@PathVariable Long id) {
        return ResponseEntity.ok(boardService.getBoard(id));
    }

    /**
     * POST /api/boards → 게시글 생성 (201)
     */
    @PostMapping
    public ResponseEntity<BoardResponse> createBoard(@Valid @RequestBody CreateBoardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(boardService.createBoard(request));
    }

    /**
     * PUT /api/boards/{id} → 게시글 수정 (200 / 404)
     */
    @PutMapping("/{id}")
    public ResponseEntity<BoardResponse> updateBoard(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBoardRequest request) {
        return ResponseEntity.ok(boardService.updateBoard(id, request));
    }

    /**
     * DELETE /api/boards/{id} → 게시글 삭제 (204 / 404)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long id) {
        boardService.deleteBoard(id);
        return ResponseEntity.noContent().build();
    }
}
