package com.dong.board.security;

import com.dong.board.infrastructure.user.TokenBlacklistRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * JWT 인증 필터 — 모든 HTTP 요청마다 한 번씩 실행됩니다 (OncePerRequestFilter)
 *
 * 동작 흐름:
 * 1. 요청 헤더에서 "Authorization: Bearer {토큰}" 추출
 * 2. 토큰 서명 유효성 검증
 * 3. 토큰 블랙리스트 확인 — 강제 로그아웃된 토큰이면 인증 거부
 * 4. 유효하면 토큰에서 로그인 ID(userId) 꺼내서 Spring Security 인증 컨텍스트에 저장
 * 5. 이후 컨트롤러에서 auth.getName()으로 userId 조회 가능
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // JWT 생성/검증/파싱을 담당하는 컴포넌트
    private final JwtProvider jwtProvider;

    // 강제 로그아웃 처리된 토큰 검증을 위한 블랙리스트 저장소
    private final TokenBlacklistRepository tokenBlacklistRepository;

    public JwtAuthenticationFilter(JwtProvider jwtProvider,
                                   TokenBlacklistRepository tokenBlacklistRepository) {
        this.jwtProvider = jwtProvider;
        this.tokenBlacklistRepository = tokenBlacklistRepository;
    }

    /**
     * 실제 필터 로직: 요청마다 실행
     *
     * @param request     HTTP 요청 객체 (헤더, 파라미터 등 포함)
     * @param response    HTTP 응답 객체
     * @param filterChain 다음 필터 또는 컨트롤러로 요청을 넘기는 체인
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 요청 헤더에서 JWT 토큰 추출 시도
        String token = extractToken(request);

        // 2. 토큰이 존재하고 서명이 유효한 경우에만 인증 처리
        if (token != null && jwtProvider.validateToken(token)) {
            // 3. 토큰에서 로그인 ID(userId)와 권한 역할(role) 추출
            String userId = jwtProvider.extractUserId(token);
            String role = jwtProvider.extractRole(token);

            // 4. 블랙리스트 검증 — 강제 로그아웃된 토큰인지 확인
            // 토큰 발급 시각(iat)이 invalidatedAt보다 이전이면 → 강제 로그아웃 처리된 토큰
            boolean isBlacklisted = tokenBlacklistRepository.findByUserId(userId)
                    .map(blacklist -> {
                        LocalDateTime issuedAt = jwtProvider.extractIssuedAt(token);
                        // 토큰 발급 시각이 무효화 시각보다 이전이면 블랙리스트 처리
                        return issuedAt.isBefore(blacklist.getInvalidatedAt());
                    })
                    .orElse(false);

            if (!isBlacklisted) {
                // role 클레임이 없는 구형 토큰은 기본값 ROLE_USER 적용
                String effectiveRole = (role != null) ? role : "ROLE_USER";

                // 5. Spring Security 인증 객체 생성
                // - principal(주체): userId 문자열 — auth.getName()으로 꺼낼 수 있음
                // - authorities(권한 목록): JWT에서 추출한 role → GrantedAuthority로 변환
                //   예) "ROLE_ADMIN" → hasAuthority("ROLE_ADMIN") 매칭
                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(effectiveRole));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);

                // 6. SecurityContextHolder에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 7. 다음 필터로 요청 전달 (인증 여부와 관계없이 항상 실행)
        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청 헤더에서 Bearer 토큰 추출
     *
     * Authorization 헤더 형식: "Bearer eyJhbGciOiJIUzI1NiJ9...."
     * "Bearer " (7글자) 이후의 문자열이 실제 토큰
     *
     * @param request HTTP 요청
     * @return JWT 토큰 문자열, 헤더가 없거나 형식이 맞지 않으면 null
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        // 헤더가 존재하고 "Bearer "로 시작하는 경우에만 토큰 추출
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7); // "Bearer " 7글자를 제외한 나머지가 토큰
        }
        return null;
    }
}
