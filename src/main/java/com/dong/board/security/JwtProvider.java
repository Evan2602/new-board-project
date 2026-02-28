package com.dong.board.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT(JSON Web Token) 토큰 생성 및 검증 컴포넌트
 *
 * JWT란? 서버가 "이 사람은 인증된 사용자야"라고 증명하는 디지털 서명 토큰
 * 구조: header.payload.signature (점으로 구분된 3부분)
 * - header: 알고리즘 정보
 * - payload: 실제 데이터 (subject = 로그인 ID, 만료 시간 등)
 * - signature: 위변조 방지 서명 (시크릿 키로 생성)
 */
@Component
public class JwtProvider {

    // HMAC-SHA 알고리즘에 사용할 비밀 키 (서버만 알고 있어야 함)
    // 이 키가 노출되면 누구든 유효한 토큰을 만들 수 있으므로 매우 중요!
    private final SecretKey key;

    // 토큰 유효 기간 (밀리초 단위, 예: 86400000 = 24시간)
    private final long expirationMs;

    // 생성자: application.yaml의 jwt.secret, jwt.expiration-ms 값을 주입받아 키 생성
    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        // 문자열 시크릿을 HMAC-SHA 알고리즘용 키 객체로 변환
        // UTF-8 인코딩으로 바이트 배열 변환 후 키 생성
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * 로그인 ID와 권한 역할로 JWT 액세스 토큰 생성
     *
     * @param userId 로그인 ID (예: "hong123")
     * @param role   사용자 권한 (예: "ROLE_USER", "ROLE_ADMIN")
     * @return JWT 토큰 문자열
     */
    public String generateToken(String userId, String role) {
        return Jwts.builder()
                // subject: 토큰 주인을 식별하는 로그인 ID
                .subject(userId)
                // role 클레임: 권한 정보 포함 (필터에서 GrantedAuthority로 변환)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * 로그인 ID로 JWT 액세스 토큰 생성 (기본 권한 ROLE_USER)
     * 하위 호환성 유지용
     *
     * @param userId 로그인 ID (예: "hong123")
     * @return JWT 토큰 문자열 (예: "eyJhbGciOiJIUzI1NiJ9....")
     */
    public String generateToken(String userId) {
        return generateToken(userId, "ROLE_USER");
    }

    /**
     * JWT 토큰에서 로그인 ID(userId) 추출
     *
     * 추출 흐름: 토큰 파싱 → 서명 검증 → payload의 subject 반환
     *
     * @param token JWT 토큰 문자열
     * @return 토큰에 저장된 로그인 ID (예: "hong123")
     */
    public String extractUserId(String token) {
        return Jwts.parser()
                // 서명 검증에 사용할 키 설정 (생성 시와 동일한 키여야 함)
                .verifyWith(key)
                .build()
                // 토큰 파싱 + 서명 검증 (위변조 시 예외 발생)
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * JWT 토큰에서 권한 역할 추출
     *
     * @param token JWT 토큰 문자열
     * @return 권한 문자열 (예: "ROLE_ADMIN"), 클레임 없으면 null
     */
    public String extractRole(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    /**
     * JWT 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
