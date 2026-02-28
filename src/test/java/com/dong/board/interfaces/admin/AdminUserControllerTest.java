package com.dong.board.interfaces.admin;

import com.dong.board.domain.log.PageResult;
import com.dong.board.domain.user.*;
import com.dong.board.dto.ChangeUserStatusRequest;
import com.dong.board.exception.UserNotFoundException;
import com.dong.board.infrastructure.log.RequestLogRepository;
import com.dong.board.infrastructure.user.TokenBlacklistRepository;
import com.dong.board.interfaces.admin.api.AdminUserController;
import com.dong.board.security.JwtProvider;
import com.dong.board.security.SecurityConfig;
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
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@Import(SecurityConfig.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminUserService adminUserService;

    // JwtAuthenticationFilter 의존성 충족 필수
    @MockitoBean
    private JwtProvider jwtProvider;

    // LoggingFilter 의존성 충족 필수
    @MockitoBean
    private RequestLogRepository requestLogRepository;

    // JwtAuthenticationFilter → TokenBlacklistRepository 의존성 충족 필수
    @MockitoBean
    private TokenBlacklistRepository tokenBlacklistRepository;

    // ---- 회원 목록 조회 ----

    @Test
    @DisplayName("GET /api/admin/users → 200 (ROLE_ADMIN)")
    void getUsers_returns200_whenAdmin() throws Exception {
        // given
        PageResult<AdminUserResult> mockResult = new PageResult<>(List.of(), 0, 20, 0L, 0);
        given(adminUserService.searchUsers(any(AdminUserSearchCommand.class))).willReturn(mockResult);

        mockMvc.perform(get("/api/admin/users")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /api/admin/users → 403 (ROLE_USER)")
    void getUsers_returns403_whenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .with(user("hong123").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/users → 401 (미인증)")
    void getUsers_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/admin/users?nickname=홍 → 200 (닉네임 검색)")
    void getUsers_withNicknameFilter_returns200() throws Exception {
        // given
        AdminUserResult mockResult = createMockUserResult(1L, "hong123", "홍길동");
        PageResult<AdminUserResult> pageResult = new PageResult<>(List.of(mockResult), 0, 20, 1L, 1);
        given(adminUserService.searchUsers(any(AdminUserSearchCommand.class))).willReturn(pageResult);

        mockMvc.perform(get("/api/admin/users")
                        .with(user("admin").roles("ADMIN"))
                        .param("nickname", "홍"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value("hong123"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ---- 회원 상세 조회 ----

    @Test
    @DisplayName("GET /api/admin/users/{id} → 200 (ROLE_ADMIN)")
    void getUser_returns200() throws Exception {
        // given
        AdminUserDetailResult mockDetail = createMockUserDetailResult(1L, "hong123", "홍길동");
        given(adminUserService.getUser(1L)).willReturn(mockDetail);

        mockMvc.perform(get("/api/admin/users/1")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value("hong123"))
                .andExpect(jsonPath("$.boards").isArray())
                .andExpect(jsonPath("$.statusHistories").isArray());
    }

    @Test
    @DisplayName("GET /api/admin/users/{id} → 404 (존재하지 않는 회원)")
    void getUser_returns404_whenNotFound() throws Exception {
        // given
        given(adminUserService.getUser(999L))
                .willThrow(new UserNotFoundException(999L));

        mockMvc.perform(get("/api/admin/users/999")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    // ---- 회원 상태 변경 ----

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/status → 200 (상태 변경 성공)")
    void changeStatus_returns200() throws Exception {
        // given
        ChangeUserStatusRequest request = new ChangeUserStatusRequest(
                User.UserStatus.SUSPENDED, "스팸 게시글 반복 등록"
        );

        mockMvc.perform(patch("/api/admin/users/1/status")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /api/admin/users/{id}/status → 400 (사유 누락)")
    void changeStatus_returns400_whenReasonBlank() throws Exception {
        // given: reason이 빈 문자열
        ChangeUserStatusRequest request = new ChangeUserStatusRequest(
                User.UserStatus.SUSPENDED, ""
        );

        mockMvc.perform(patch("/api/admin/users/1/status")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- 강제 로그아웃 ----

    @Test
    @DisplayName("POST /api/admin/users/{id}/force-logout → 200 (강제 로그아웃 성공)")
    void forceLogout_returns200() throws Exception {
        mockMvc.perform(post("/api/admin/users/1/force-logout")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/admin/users/{id}/force-logout → 404 (존재하지 않는 회원)")
    void forceLogout_returns404_whenNotFound() throws Exception {
        // given: void 메서드는 doThrow 방식으로 모킹
        org.mockito.Mockito.doThrow(new UserNotFoundException(999L))
                .when(adminUserService).forceLogout(eq(999L), anyString());

        mockMvc.perform(post("/api/admin/users/999/force-logout")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    // ---- 비밀번호 초기화 ----

    @Test
    @DisplayName("POST /api/admin/users/{id}/reset-password → 200 (비밀번호 초기화 성공)")
    void resetPassword_returns200() throws Exception {
        // given
        given(adminUserService.resetPassword(eq(1L), anyString())).willReturn("Temp123456");

        mockMvc.perform(post("/api/admin/users/1/reset-password")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").value("Temp123456"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // ---- 헬퍼 메서드 ----

    private AdminUserResult createMockUserResult(Long id, String userId, String username) {
        return new AdminUserResult(id, userId, username, LocalDateTime.now(),
                User.UserStatus.ACTIVE, null);
    }

    private AdminUserDetailResult createMockUserDetailResult(Long id, String userId, String username) {
        return new AdminUserDetailResult(
                id, userId, username, LocalDateTime.now(),
                User.UserStatus.ACTIVE, null,
                List.of(), List.of()
        );
    }
}
