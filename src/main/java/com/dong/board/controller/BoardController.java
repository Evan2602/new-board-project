package com.dong.board.controller;

import com.dong.board.dto.BoardResponse;
import com.dong.board.dto.CreateBoardRequest;
import com.dong.board.dto.UpdateBoardRequest;
import com.dong.board.service.BoardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 게시글 REST API 컨트롤러
 * HTTP 요청/응답 처리만 담당: Request → Command 변환 후 서비스 호출, Result → Response 변환 후 반환
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
        List<BoardResponse> responses = boardService.getBoardList().stream()
                .map(BoardResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * GET /api/boards/{id} → 게시글 단건 조회 (200 / 404)
     */
    @GetMapping("/{id}")
    public ResponseEntity<BoardResponse> getBoard(@PathVariable Long id) {
        return ResponseEntity.ok(BoardResponse.from(boardService.getBoard(id)));
    }

    /**
     * POST /api/boards → 게시글 생성 (201) - 작성자를 JWT에서 자동 추출
     */
    @PostMapping
    public ResponseEntity<BoardResponse> createBoard(
            @Valid @RequestBody CreateBoardRequest request,
            @CurrentSecurityContext(expression = "authentication") Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BoardResponse.from(boardService.createBoard(request.toCommand(auth.getName()))));
    }

    /**
     * PUT /api/boards/{id} → 게시글 수정 (200 / 404 / 403)
     */
    @PutMapping("/{id}")
    public ResponseEntity<BoardResponse> updateBoard(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBoardRequest request,
            @CurrentSecurityContext(expression = "authentication") Authentication auth) {
        return ResponseEntity.ok(
                BoardResponse.from(boardService.updateBoard(id, request.toCommand(), auth.getName())));
    }

    /**
     * DELETE /api/boards/{id} → 게시글 삭제 (204 / 404 / 403)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable Long id,
            @CurrentSecurityContext(expression = "authentication") Authentication auth) {
        boardService.deleteBoard(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
