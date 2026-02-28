package com.dong.board.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 보안 설정 클래스
 *
 * 핵심 설정:
 * - CSRF 비활성화: REST API는 쿠키 대신 JWT를 사용하므로 CSRF 공격에 해당 없음
 * - Stateless 세션: 서버가 세션을 저장하지 않음 (JWT가 그 역할 대신)
 * - 공개 엔드포인트: 로그인/가입, 게시글 읽기는 토큰 없이 가능
 * - 인증 필요: 게시글 쓰기/수정/삭제는 유효한 JWT 필요
 */
@Configuration
@EnableWebSecurity  // Spring Security 웹 보안 기능을 활성화
public class SecurityConfig {

    /**
     * 보안 필터 체인 설정 빈
     * 모든 HTTP 요청은 이 체인을 거쳐 처리됩니다
     *
     * @param http      HttpSecurity 설정 빌더 (Spring이 주입)
     * @param jwtFilter JWT 인증 필터 (Spring이 주입)
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter) throws Exception {
        http
                // CSRF 보호 비활성화
                // REST API는 쿠키 기반 인증을 사용하지 않으므로 CSRF 불필요
                .csrf(csrf -> csrf.disable())

                // 세션 정책을 STATELESS로 설정
                // 서버가 HttpSession을 생성하지 않음 → 모든 인증은 JWT 토큰으로만 처리
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 엔드포인트별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // /api/auth/** (회원가입, 로그인): 누구나 접근 가능 (토큰 불필요)
                        .requestMatchers("/api/auth/**").permitAll()
                        // GET /api/boards/** (게시글 조회): 누구나 접근 가능 (토큰 불필요)
                        .requestMatchers(HttpMethod.GET, "/api/boards/**").permitAll()
                        // /admin/** (관리자 API): ROLE_ADMIN 권한 필수
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                        // 그 외 모든 요청 (POST/PUT/DELETE /api/boards 등): 유효한 JWT 필요
                        .anyRequest().authenticated()
                )

                // 인증/인가 실패 응답 커스터마이징
                .exceptionHandling(exc -> exc
                        // 401 Unauthorized: 인증 정보 없거나 유효하지 않은 경우
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\"}"
                            );
                        })
                        // 403 Forbidden: 인증은 됐지만 권한 부족 (예: 일반 사용자가 /admin/** 접근)
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"code\":\"ACCESS_DENIED\",\"message\":\"관리자 권한이 필요합니다.\"}"
                            );
                        })
                )

                // JWT 인증 필터를 기본 인증 필터(UsernamePasswordAuthenticationFilter) 앞에 삽입
                // 요청이 컨트롤러에 도달하기 전에 JWT를 먼저 검증
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt 비밀번호 인코더 빈
     * BCrypt는 단방향 해시 알고리즘: 원문 복원 불가, 같은 비밀번호도 매번 다른 해시 생성
     * AuthService에서 비밀번호 저장/검증 시 사용
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
