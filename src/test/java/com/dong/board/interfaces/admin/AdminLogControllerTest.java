package com.dong.board.interfaces.admin;

import com.dong.board.domain.log.PageResult;
import com.dong.board.domain.log.RequestLogResult;
import com.dong.board.domain.log.RequestLogSearchCommand;
import com.dong.board.domain.log.RequestLogService;
import com.dong.board.dto.AdminMemoRequest;
import com.dong.board.exception.RequestLogNotFoundException;
import com.dong.board.infrastructure.log.RequestLogRepository;
import com.dong.board.infrastructure.user.TokenBlacklistRepository;
import com.dong.board.interfaces.admin.api.AdminLogController;
import com.dong.board.security.JwtProvider;
import com.dong.board.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminLogController.class)
@Import(SecurityConfig.class)
class AdminLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RequestLogService requestLogService;

    // JwtAuthenticationFilter 의존성 충족 필수
    @MockitoBean
    private JwtProvider jwtProvider;

    // LoggingFilter 의존성 충족 필수
    @MockitoBean
    private RequestLogRepository requestLogRepository;

    // JwtAuthenticationFilter → TokenBlacklistRepository 의존성 충족 필수
    @MockitoBean
    private TokenBlacklistRepository tokenBlacklistRepository;

    // ---- 로그 목록 조회 ----

    @Test
    @DisplayName("GET /api/admin/logs → 200 (ROLE_ADMIN)")
    void getLogs_returns200_whenAdmin() throws Exception {
        // given: 빈 페이지 결과 반환
        PageResult<RequestLogResult> mockResult = new PageResult<>(List.of(), 0, 20, 0L, 0);
        given(requestLogService.searchLogs(any(RequestLogSearchCommand.class))).willReturn(mockResult);

        mockMvc.perform(get("/api/admin/logs")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /api/admin/logs → 403 (ROLE_USER)")
    void getLogs_returns403_whenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/logs")
                        .with(user("hong123").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/logs → 401 (미인증)")
    void getLogs_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/admin/logs?statusGroup=4xx → 200 (필터 적용)")
    void getLogs_withStatusGroupFilter_returns200() throws Exception {
        RequestLogResult logResult = createMockLogResult(1L, 404, "BOARD_NOT_FOUND");
        PageResult<RequestLogResult> mockResult = new PageResult<>(List.of(logResult), 0, 20, 1L, 1);
        given(requestLogService.searchLogs(any(RequestLogSearchCommand.class))).willReturn(mockResult);

        mockMvc.perform(get("/api/admin/logs")
                        .with(user("admin").roles("ADMIN"))
                        .param("statusGroup", "4xx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].statusCode").value(404))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---- 로그 상세 조회 ----

    @Test
    @DisplayName("GET /api/admin/logs/{id} → 200 (ROLE_ADMIN)")
    void getLog_returns200() throws Exception {
        RequestLogResult logResult = createMockLogResult(1L, 200, null);
        given(requestLogService.getLog(1L)).willReturn(logResult);

        mockMvc.perform(get("/api/admin/logs/1")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.statusCode").value(200));
    }

    @Test
    @DisplayName("GET /api/admin/logs/{id} → 404 (존재하지 않는 로그)")
    void getLog_returns404_whenNotFound() throws Exception {
        given(requestLogService.getLog(999L))
                .willThrow(new RequestLogNotFoundException(999L));

        mockMvc.perform(get("/api/admin/logs/999")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOG_NOT_FOUND"));
    }

    // ---- 관리자 메모 수정 ----

    @Test
    @DisplayName("PATCH /api/admin/logs/{id}/memo → 200 (메모 수정 성공)")
    void updateMemo_returns200() throws Exception {
        AdminMemoRequest request = new AdminMemoRequest("장애 원인 조사 완료");
        RequestLogResult updated = createMockLogResultWithMemo(1L, "장애 원인 조사 완료");
        given(requestLogService.updateAdminMemo(eq(1L), anyString())).willReturn(updated);

        mockMvc.perform(patch("/api/admin/logs/1/memo")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminMemo").value("장애 원인 조사 완료"));
    }

    @Test
    @DisplayName("PATCH /api/admin/logs/{id}/memo → 400 (memo 필드 null)")
    void updateMemo_returns400_whenMemoIsNull() throws Exception {
        mockMvc.perform(patch("/api/admin/logs/1/memo")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memo\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/admin/logs/{id}/memo → 403 (ROLE_USER)")
    void updateMemo_returns403_whenNotAdmin() throws Exception {
        AdminMemoRequest request = new AdminMemoRequest("메모");

        mockMvc.perform(patch("/api/admin/logs/1/memo")
                        .with(user("hong123").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ---- 테스트 픽스처 ----

    private RequestLogResult createMockLogResult(Long id, int statusCode, String errorMessage) {
        return new RequestLogResult(
                id, "uuid-" + id, LocalDateTime.now(),
                "GET", "/api/boards/1", "hong123",
                "127.0.0.1", "Mozilla/5.0",
                statusCode, 50L, errorMessage, null, null
        );
    }

    private RequestLogResult createMockLogResultWithMemo(Long id, String adminMemo) {
        return new RequestLogResult(
                id, "uuid-" + id, LocalDateTime.now(),
                "GET", "/api/boards/1", "hong123",
                "127.0.0.1", "Mozilla/5.0",
                200, 50L, null, null, adminMemo
        );
    }
}
