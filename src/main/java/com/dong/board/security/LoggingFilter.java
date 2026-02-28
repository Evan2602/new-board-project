package com.dong.board.security;

import com.dong.board.domain.log.RequestLog;
import com.dong.board.infrastructure.log.RequestLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * API 요청 로깅 필터 — 모든 HTTP 요청을 DB에 기록
 *
 * 수집 정보: requestId(UUID), requestAt, method, url, userId, ip, userAgent, statusCode, durationMs
 * 에러 발생 시: errorMessage, stackTrace (GlobalExceptionHandler가 request attribute에 저장)
 *
 * 필터 실행 순서:
 * LoggingFilter (@Component) → Spring Security FilterChain → DispatcherServlet
 * 이 필터는 Security보다 먼저 실행되므로 userId를 JWT에서 직접 파싱
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingFilter extends OncePerRequestFilter {

    /** GlobalExceptionHandler가 에러 메시지를 저장하는 request attribute 키 */
    public static final String ATTR_ERROR_MESSAGE = "LOG_ERROR_MESSAGE";

    /** GlobalExceptionHandler가 스택 트레이스를 저장하는 request attribute 키 */
    public static final String ATTR_STACK_TRACE = "LOG_STACK_TRACE";

    // JWT 직접 파싱으로 userId 추출 (SecurityContextHolder 사용 불가 시점)
    private final JwtProvider jwtProvider;

    // 요청 로그 DB 저장소
    private final RequestLogRepository requestLogRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        // 요청마다 고유한 추적 ID 발급 (UUID)
        String requestId = UUID.randomUUID().toString();

        // 요청 시작 시점에 추출 가능한 정보
        String method = request.getMethod();
        String url = request.getRequestURI();
        String ip = extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        // Security 필터 실행 전이므로 JWT를 직접 파싱하여 userId 추출
        String userId = extractUserIdFromJwt(request);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            // filterChain.doFilter() 완료 후 응답 상태 코드 확인 가능
            int statusCode = response.getStatus();

            // GlobalExceptionHandler가 request attribute에 저장한 에러 정보 수집
            String errorMessage = (String) request.getAttribute(ATTR_ERROR_MESSAGE);
            String stackTrace = (String) request.getAttribute(ATTR_STACK_TRACE);

            // 로그 저장 실패가 원래 HTTP 응답에 영향을 주지 않도록 별도 try-catch
            try {
                RequestLog requestLog = RequestLog.create(
                        requestId, method, url, userId, ip, userAgent,
                        statusCode, durationMs, errorMessage, stackTrace
                );
                requestLogRepository.save(requestLog);
            } catch (Exception e) {
                // 로그 저장 실패는 경고 레벨로만 기록 (응답은 정상 반환)
                log.warn("[LoggingFilter] 요청 로그 저장 실패 - requestId={}, 원인: {}",
                        requestId, e.getMessage());
            }

            log.debug("[REQUEST] id={} | {}{} | status={} | {}ms | userId={}",
                    requestId, method, url, statusCode, durationMs, userId);
        }
    }

    /**
     * Authorization 헤더에서 JWT를 직접 파싱하여 userId 추출
     * Security FilterChain 실행 전 시점에서 사용하므로 SecurityContextHolder 사용 불가
     *
     * @return JWT가 유효하면 userId, 없거나 유효하지 않으면 null
     */
    private String extractUserIdFromJwt(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                if (jwtProvider.validateToken(token)) {
                    return jwtProvider.extractUserId(token);
                }
            } catch (Exception e) {
                // 유효하지 않은 토큰 → userId는 null (정상 처리)
            }
        }
        return null;
    }

    /**
     * 클라이언트 실제 IP 추출
     * 프록시/로드밸런서 환경에서는 X-Forwarded-For 헤더에 원본 IP가 담김
     */
    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For: client, proxy1, proxy2 — 맨 앞이 원본 클라이언트 IP
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
