package com.dong.board.domain.user;

import com.dong.board.exception.DuplicateUsernameException;
import com.dong.board.exception.InvalidCredentialsException;
import com.dong.board.infrastructure.user.UserRepository;
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
        // userId(로그인 ID), username(표시 이름), password(비밀번호)로 커맨드 생성
        SignUpCommand command = new SignUpCommand("hong123", "홍길동", "password123");
        // "hong123"이라는 ID는 아직 사용 중이지 않음
        given(userRepository.existsByUserId("hong123")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
        // 저장 시 그대로 반환 (인메모리 저장소 동작 모방)
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));
        // userId로 토큰 생성
        given(jwtProvider.generateToken("hong123")).willReturn("test.jwt.token");

        // when
        AuthResult result = authService.signUp(command);

        // then
        assertThat(result.accessToken()).isEqualTo("test.jwt.token");
        // userId와 username이 모두 올바르게 반환되는지 확인
        assertThat(result.userId()).isEqualTo("hong123");
        assertThat(result.username()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("회원가입 - 실패 (중복 아이디)")
    void signUp_duplicateUserId() {
        // given: 이미 "hong123"이 사용 중
        SignUpCommand command = new SignUpCommand("hong123", "홍길동", "password123");
        given(userRepository.existsByUserId("hong123")).willReturn(true);

        // when & then: 중복 예외 발생, 메시지에 "hong123" 포함
        assertThatThrownBy(() -> authService.signUp(command))
                .isInstanceOf(DuplicateUsernameException.class)
                .hasMessageContaining("hong123");
    }

    @Test
    @DisplayName("로그인 - 성공")
    void login_success() {
        // given
        LoginCommand command = new LoginCommand("hong123", "password123");
        // 저장소에는 userId="hong123", username="홍길동"인 사용자가 존재
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
