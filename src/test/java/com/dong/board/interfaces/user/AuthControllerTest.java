package com.dong.board.interfaces.user;

import com.dong.board.dto.LoginRequest;
import com.dong.board.dto.SignUpRequest;
import com.dong.board.exception.DuplicateUsernameException;
import com.dong.board.exception.InvalidCredentialsException;
import com.dong.board.infrastructure.log.RequestLogRepository;
import com.dong.board.interfaces.user.api.AuthController;
import com.dong.board.security.JwtProvider;
import com.dong.board.security.SecurityConfig;
import com.dong.board.domain.user.AuthResult;
import com.dong.board.domain.user.AuthService;
import com.dong.board.domain.user.LoginCommand;
import com.dong.board.domain.user.SignUpCommand;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
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

    // LoggingFilter가 RequestLogRepository에 의존하므로 모킹 필요
    @MockitoBean
    private RequestLogRepository requestLogRepository;

    @Test
    @DisplayName("POST /api/auth/sign-up → 201 회원가입 성공")
    void signUp_returns201() throws Exception {
        // given
        // userId, username, password 세 필드 모두 전달
        SignUpRequest request = new SignUpRequest("hong123", "홍길동", "password123");
        // 서비스 응답에도 userId, username 둘 다 포함
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
                // userId와 username 둘 다 응답에 포함되는지 검증
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
        // given - 8자 미만 비밀번호, userId와 username은 정상
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
        // given: userId로 로그인 요청
        LoginRequest request = new LoginRequest("hong123", "password123");
        // 응답에 userId, username 둘 다 포함
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
