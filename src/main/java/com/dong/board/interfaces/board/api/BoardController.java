package com.dong.board.interfaces.board.api;

import com.dong.board.dto.BoardResponse;
import com.dong.board.dto.CreateBoardRequest;
import com.dong.board.dto.UpdateBoardRequest;
import com.dong.board.domain.board.BoardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 게시글 REST API 컨트롤러
 * 역할: HTTP 요청/응답 처리만 담당
 * - 요청 본문(JSON) → Command 객체로 변환 → 서비스 호출
 * - 서비스 결과(Result) → 응답 DTO(Response)로 변환 → JSON 반환
 *
 * auth.getName()이 반환하는 값:
 * JWT 필터에서 UsernamePasswordAuthenticationToken의 principal(주체)로 userId를 저장했으므로
 * auth.getName()은 곧 userId(로그인 ID, 예: "hong123")를 반환합니다
 */
@RestController
@RequestMapping("/api/boards")
public class BoardController {

    // 게시글 비즈니스 로직을 처리하는 서비스
    private final BoardService boardService;

    // 생성자 주입
    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    /**
     * GET /api/boards → 게시글 목록 조회 (200 OK)
     * 누구나 접근 가능 (JWT 불필요)
     */
    @GetMapping
    public ResponseEntity<List<BoardResponse>> getBoardList() {
        // 서비스에서 전체 목록 조회 후 각 Result를 Response DTO로 변환
        List<BoardResponse> responses = boardService.getBoardList().stream()
                .map(BoardResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * GET /api/boards/{id} → 게시글 단건 조회 (200 OK / 404 Not Found)
     * 누구나 접근 가능 (JWT 불필요)
     */
    @GetMapping("/{id}")
    public ResponseEntity<BoardResponse> getBoard(@PathVariable Long id) {
        return ResponseEntity.ok(BoardResponse.from(boardService.getBoard(id)));
    }

    /**
     * POST /api/boards → 게시글 생성 (201 Created)
     * JWT 필요 — 작성자 ID는 요청 본문이 아니라 JWT 토큰에서 자동 추출
     *
     * @param auth JWT 필터가 SecurityContext에 저장한 인증 정보
     *             auth.getName() = userId (로그인 ID, 예: "hong123")
     */
    @PostMapping
    public ResponseEntity<BoardResponse> createBoard(
            @Valid @RequestBody CreateBoardRequest request,
            Authentication auth) {
        // JWT 토큰에서 추출한 로그인 ID를 authorId로 사용
        // auth.getName() = JwtAuthenticationFilter가 SecurityContext에 저장한 userId (예: "hong123")
        String userId = auth.getName();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BoardResponse.from(boardService.createBoard(request.toCommand(userId))));
    }

    /**
     * PUT /api/boards/{id} → 게시글 수정 (200 / 404 / 403)
     */
    @PutMapping("/{id}")
    public ResponseEntity<BoardResponse> updateBoard(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBoardRequest request,
            Authentication auth) {
        return ResponseEntity.ok(
                BoardResponse.from(boardService.updateBoard(id, request.toCommand(), auth.getName())));
    }

    /**
     * DELETE /api/boards/{id} → 게시글 삭제 (204 / 404 / 403)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable Long id,
            Authentication auth) {
        boardService.deleteBoard(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
