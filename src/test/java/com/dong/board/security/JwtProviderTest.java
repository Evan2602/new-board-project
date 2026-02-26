package com.dong.board.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        // 테스트용 시크릿 키 (32바이트 이상 필요)
        jwtProvider = new JwtProvider("test-secret-key-must-be-at-least-32-bytes!!", 86400000L);
    }

    @Test
    @DisplayName("토큰 생성 - 성공")
    void generateToken_success() {
        // when
        String token = jwtProvider.generateToken("hong123");

        // then
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("토큰에서 로그인 ID 추출 - 성공")
    void extractUserId_success() {
        // given
        String token = jwtProvider.generateToken("hong123");

        // when
        String userId = jwtProvider.extractUserId(token);

        // then
        assertThat(userId).isEqualTo("hong123");
    }

    @Test
    @DisplayName("유효한 토큰 검증 - 성공")
    void validateToken_validToken() {
        // given
        String token = jwtProvider.generateToken("hong123");

        // when & then
        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("위변조된 토큰 검증 - 실패")
    void validateToken_tamperedToken() {
        // given
        String token = jwtProvider.generateToken("hong123") + "tampered";

        // when & then
        assertThat(jwtProvider.validateToken(token)).isFalse();
    }
}
