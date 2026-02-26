package com.dong.board.service;

import com.dong.board.domain.User;
import com.dong.board.exception.DuplicateUsernameException;
import com.dong.board.exception.InvalidCredentialsException;
import com.dong.board.repository.UserRepository;
import com.dong.board.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입 - 성공")
    void signUp_success() {
        // given
        SignUpCommand command = new SignUpCommand("hong123", "홍길동", "password123");
        given(userRepository.existsByUserId("hong123")).willReturn(false);
        given(userRepository.generateId()).willReturn(1L);
        given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));
        given(jwtProvider.generateToken("hong123")).willReturn("test.jwt.token");

        // when
        AuthResult result = authService.signUp(command);

        // then
        assertThat(result.accessToken()).isEqualTo("test.jwt.token");
        assertThat(result.userId()).isEqualTo("hong123");
        assertThat(result.username()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("회원가입 - 실패 (중복 아이디)")
    void signUp_duplicateUserId() {
        // given
        SignUpCommand command = new SignUpCommand("hong123", "홍길동", "password123");
        given(userRepository.existsByUserId("hong123")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signUp(command))
                .isInstanceOf(DuplicateUsernameException.class)
                .hasMessageContaining("hong123");
    }

    @Test
    @DisplayName("로그인 - 성공")
    void login_success() {
        // given
        LoginCommand command = new LoginCommand("hong123", "password123");
        User user = User.create(1L, "hong123", "홍길동", "encodedPassword");
        given(userRepository.findByUserId("hong123")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
        given(jwtProvider.generateToken("hong123")).willReturn("test.jwt.token");

        // when
        AuthResult result = authService.login(command);

        // then
        assertThat(result.accessToken()).isEqualTo("test.jwt.token");
        assertThat(result.userId()).isEqualTo("hong123");
        assertThat(result.username()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("로그인 - 실패 (존재하지 않는 사용자)")
    void login_userNotFound() {
        // given
        LoginCommand command = new LoginCommand("unknown", "password123");
        given(userRepository.findByUserId("unknown")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(command))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("로그인 - 실패 (비밀번호 불일치)")
    void login_wrongPassword() {
        // given
        LoginCommand command = new LoginCommand("hong123", "wrongPassword");
        User user = User.create(1L, "hong123", "홍길동", "encodedPassword");
        given(userRepository.findByUserId("hong123")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(command))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
