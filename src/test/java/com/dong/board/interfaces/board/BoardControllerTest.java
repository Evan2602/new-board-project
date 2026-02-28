package com.dong.board.interfaces.board;

import com.dong.board.dto.CreateBoardRequest;
import com.dong.board.dto.UpdateBoardRequest;
import com.dong.board.exception.BoardNotFoundException;
import com.dong.board.infrastructure.log.RequestLogRepository;
import com.dong.board.infrastructure.user.TokenBlacklistRepository;
import com.dong.board.interfaces.board.api.BoardController;
import com.dong.board.security.JwtProvider;
import com.dong.board.security.SecurityConfig;
import com.dong.board.domain.board.BoardResult;
import com.dong.board.domain.board.BoardService;
import com.dong.board.domain.board.CreateBoardCommand;
import com.dong.board.domain.board.UpdateBoardCommand;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BoardController.class)
@Import(SecurityConfig.class)
class BoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BoardService boardService;

    // JwtAuthenticationFilter가 JwtProvider에 의존하므로 모킹 필요
    @MockitoBean
    private JwtProvider jwtProvider;

    // LoggingFilter가 RequestLogRepository에 의존하므로 모킹 필요
    @MockitoBean
    private RequestLogRepository requestLogRepository;

    // JwtAuthenticationFilter → TokenBlacklistRepository 의존성 충족 필수
    @MockitoBean
    private TokenBlacklistRepository tokenBlacklistRepository;

    // 테스트용 샘플 BoardResult 생성 (authorId에 로그인 ID 사용)
    private BoardResult createSampleResult(Long id) {
        return new BoardResult(id, "제목", "내용", "hong123","홍길동", LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /api/boards → 200 목록 조회")
    void getBoardList_returns200() throws Exception {
        // given
        given(boardService.getBoardList()).willReturn(List.of(createSampleResult(1L)));

        // when & then
        mockMvc.perform(get("/api/boards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/boards/{id} → 200 단건 조회")
    void getBoard_returns200() throws Exception {
        // given
        given(boardService.getBoard(1L)).willReturn(createSampleResult(1L));

        // when & then
        mockMvc.perform(get("/api/boards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/boards/{id} → 404 (존재하지 않는 게시글)")
    void getBoard_returns404() throws Exception {
        // given
        given(boardService.getBoard(99L)).willThrow(new BoardNotFoundException(99L));

        // when & then
        mockMvc.perform(get("/api/boards/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOARD_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/boards → 201 게시글 생성")
    void createBoard_returns201() throws Exception {
        // given - authorId 필드 없음 (JWT에서 자동 추출)
        CreateBoardRequest request = new CreateBoardRequest("제목", "내용");
        given(boardService.createBoard(any(CreateBoardCommand.class))).willReturn(createSampleResult(1L));

        // when & then
        mockMvc.perform(post("/api/boards")
                        .with(user("hong123"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /api/boards → 400 (Validation 실패: 빈 제목)")
    void createBoard_returns400_whenTitleBlank() throws Exception {
        // given - title이 빈 문자열 → @NotBlank 검증 실패
        CreateBoardRequest request = new CreateBoardRequest("", "내용");

        // when & then
        mockMvc.perform(post("/api/boards")
                        .with(user("hong123"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PUT /api/boards/{id} → 200 게시글 수정")
    void updateBoard_returns200() throws Exception {
        // given: updateBoard(1L, command, "hong123") 호출 시 성공 반환
        UpdateBoardRequest request = new UpdateBoardRequest("새 제목", "새 내용");
        given(boardService.updateBoard(eq(1L), any(UpdateBoardCommand.class), eq("hong123")))
                .willReturn(createSampleResult(1L));

        // when & then
        mockMvc.perform(put("/api/boards/1")
                        .with(user("hong123"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/boards/{id} → 404 (존재하지 않는 게시글)")
    void updateBoard_returns404() throws Exception {
        // given
        UpdateBoardRequest request = new UpdateBoardRequest("새 제목", "새 내용");
        given(boardService.updateBoard(eq(99L), any(UpdateBoardCommand.class), anyString()))
                .willThrow(new BoardNotFoundException(99L));

        // when & then
        mockMvc.perform(put("/api/boards/99")
                        .with(user("hong123"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOARD_NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /api/boards/{id} → 204 게시글 삭제")
    void deleteBoard_returns204() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/boards/1")
                        .with(user("hong123"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/boards/{id} → 404 (존재하지 않는 게시글)")
    void deleteBoard_returns404() throws Exception {
        // given: userId "hong123"으로 99번 게시글 삭제 시도 → 404 예외
        doThrow(new BoardNotFoundException(99L)).when(boardService).deleteBoard(99L, "hong123");

        // when & then
        mockMvc.perform(delete("/api/boards/99")
                        .with(user("hong123"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOARD_NOT_FOUND"));
    }
}
