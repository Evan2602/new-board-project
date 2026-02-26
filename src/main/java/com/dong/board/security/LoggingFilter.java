package com.dong.board.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 요청 로깅 필터 — 모든 HTTP 요청마다 IP, HTTP 메서드, URI, 처리 시간을 로그로 출력
 */
@Component
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        String ip = getClientIp(request);
        String method = request.getMethod();
        String uri = request.getRequestURI();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("[REQUEST] IP={} | Method={} | URI={} | Duration={}ms", ip, method, uri, duration);
        }
    }

    /**
     * 클라이언트 실제 IP 추출
     * 프록시/로드밸런서 환경에서는 X-Forwarded-For 헤더에 원본 IP가 담김
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For: client, proxy1, proxy2 — 맨 앞이 원본 클라이언트 IP
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

