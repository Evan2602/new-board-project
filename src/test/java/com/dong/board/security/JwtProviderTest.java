package com.dong.board.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        // 테스트용 시크릿 키 (HMAC-SHA 알고리즘 요구사항: 32바이트 이상)
        jwtProvider = new JwtProvider("test-secret-key-must-be-at-least-32-bytes!!", 86400000L);
    }

    @Test
    @DisplayName("토큰 생성 - 성공")
    void generateToken_success() {
        // when: 로그인 ID로 JWT 토큰 생성
        String token = jwtProvider.generateToken("hong123");

        // then: 빈 문자열이 아닌 토큰이 생성되어야 함
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("토큰에서 로그인 ID 추출 - 성공")
    void extractUserId_success() {
        // given: 로그인 ID "hong123"으로 토큰 생성
        String token = jwtProvider.generateToken("hong123");

        // when: 토큰에서 로그인 ID 추출
        String userId = jwtProvider.extractUserId(token);

        // then: 원래 저장한 로그인 ID가 그대로 나와야 함
        assertThat(userId).isEqualTo("hong123");
    }

    @Test
    @DisplayName("유효한 토큰 검증 - 성공")
    void validateToken_validToken() {
        // given: 정상 토큰 생성
        String token = jwtProvider.generateToken("hong123");

        // when & then: 유효한 토큰이므로 true 반환
        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("위변조된 토큰 검증 - 실패")
    void validateToken_tamperedToken() {
        // given: 정상 토큰에 임의 문자열을 추가해서 위변조
        String token = jwtProvider.generateToken("hong123") + "tampered";

        // when & then: 서명이 맞지 않으므로 false 반환
        assertThat(jwtProvider.validateToken(token)).isFalse();
    }
}
