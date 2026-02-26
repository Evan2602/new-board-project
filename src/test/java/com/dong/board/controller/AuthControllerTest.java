package com.dong.board.controller;

import com.dong.board.dto.LoginRequest;
import com.dong.board.dto.SignUpRequest;
import com.dong.board.exception.DuplicateUsernameException;
import com.dong.board.exception.InvalidCredentialsException;
import com.dong.board.security.JwtProvider;
import com.dong.board.service.AuthResult;
import com.dong.board.service.AuthService;
import com.dong.board.service.LoginCommand;
import com.dong.board.service.SignUpCommand;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    // JwtAuthenticationFilter가 JwtProvider에 의존하므로 모킹 필요
    @MockitoBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("POST /api/auth/sign-up → 201 회원가입 성공")
    void signUp_returns201() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("hong123", "홍길동", "password123");
        AuthResult result = new AuthResult("test.jwt.token", "hong123", "홍길동");
        given(authService.signUp(any(SignUpCommand.class))).willReturn(result);

        // when & then
        mockMvc.perform(post("/api/auth/sign-up")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("test.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value("hong123"))
                .andExpect(jsonPath("$.username").value("홍길동"));
    }

    @Test
    @DisplayName("POST /api/auth/sign-up → 409 (중복 아이디)")
    void signUp_returns409_whenDuplicateUserId() throws Exception {
        // given
        SignUpRequest request = new SignUpRequest("hong123", "홍길동", "password123");
        given(authService.signUp(any(SignUpCommand.class)))
                .willThrow(new DuplicateUsernameException("hong123"));

        // when & then
        mockMvc.perform(post("/api/auth/sign-up")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_USERNAME"));
    }

    @Test
    @DisplayName("POST /api/auth/sign-up → 400 (Validation 실패: 짧은 비밀번호)")
    void signUp_returns400_whenPasswordTooShort() throws Exception {
        // given - 8자 미만 비밀번호
        SignUpRequest request = new SignUpRequest("hong123", "홍길동", "short");

        // when & then
        mockMvc.perform(post("/api/auth/sign-up")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/auth/login → 200 로그인 성공")
    void login_returns200() throws Exception {
        // given
        LoginRequest request = new LoginRequest("hong123", "password123");
        AuthResult result = new AuthResult("test.jwt.token", "hong123", "홍길동");
        given(authService.login(any(LoginCommand.class))).willReturn(result);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test.jwt.token"))
                .andExpect(jsonPath("$.userId").value("hong123"))
                .andExpect(jsonPath("$.username").value("홍길동"));
    }

    @Test
    @DisplayName("POST /api/auth/login → 401 (잘못된 비밀번호)")
    void login_returns401_whenWrongPassword() throws Exception {
        // given
        LoginRequest request = new LoginRequest("hong123", "wrongpassword");
        given(authService.login(any(LoginCommand.class)))
                .willThrow(new InvalidCredentialsException());

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }
}
