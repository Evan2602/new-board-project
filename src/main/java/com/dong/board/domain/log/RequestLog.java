package com.dong.board.domain.log;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * API 요청 로그 도메인 엔티티
 * - 모든 HTTP 요청을 DB에 기록하기 위한 도메인 객체
 * - 에러 발생 시 예외 메시지, 스택 트레이스도 함께 저장
 * - adminMemo만 변경 가능, 나머지 필드는 불변
 */
@Getter
public class RequestLog {

    // DB AUTO_INCREMENT ID (생성 시 null, 저장 후 발급)
    private final Long id;

    // 요청 고유 식별자 (UUID 형식, 예: "550e8400-e29b-41d4-a716-446655440000")
    private final String requestId;

    // 요청 수신 시각
    private final LocalDateTime requestAt;

    // HTTP 메서드 (GET, POST, PUT, DELETE 등)
    private final String method;

    // 요청 URL (예: "/api/boards/1")
    private final String url;

    // 요청한 사용자 ID (비로그인 요청은 null)
    private final String userId;

    // 클라이언트 IP 주소 (X-Forwarded-For 헤더 우선)
    private final String ip;

    // 클라이언트 User-Agent 헤더 값
    private final String userAgent;

    // HTTP 응답 상태 코드 (200, 201, 400, 404, 500 등)
    private final Integer statusCode;

    // 요청 처리 시간 (밀리초)
    private final Long durationMs;

    // 에러 발생 시 예외 메시지 (2xx 요청은 null)
    private final String errorMessage;

    // 에러 발생 시 스택 트레이스 문자열 (4xx/5xx 요청, 최대 10000자)
    private final String stackTrace;

    // 관리자 메모 (장애 원인 기록용) — 유일하게 변경 가능한 필드
    private String adminMemo;

    private RequestLog(Long id, String requestId, LocalDateTime requestAt,
                       String method, String url, String userId, String ip,
                       String userAgent, Integer statusCode, Long durationMs,
                       String errorMessage, String stackTrace, String adminMemo) {
        this.id = id;
        this.requestId = requestId;
        this.requestAt = requestAt;
        this.method = method;
        this.url = url;
        this.userId = userId;
        this.ip = ip;
        this.userAgent = userAgent;
        this.statusCode = statusCode;
        this.durationMs = durationMs;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
        this.adminMemo = adminMemo;
    }

    /**
     * 새 요청 로그 생성 (LoggingFilter에서 호출)
     * id=null로 생성 → save() 후 DB가 AUTO_INCREMENT로 발급
     *
     * @param requestId   UUID 형식 요청 식별자
     * @param method      HTTP 메서드
     * @param url         요청 URL
     * @param userId      요청자 로그인 ID (비로그인 시 null)
     * @param ip          클라이언트 IP
     * @param userAgent   User-Agent 헤더
     * @param statusCode  응답 상태 코드
     * @param durationMs  처리 시간 (밀리초)
     * @param errorMessage 예외 메시지 (정상 요청은 null)
     * @param stackTrace  스택 트레이스 (정상 요청은 null)
     */
    public static RequestLog create(String requestId, String method, String url,
                                    String userId, String ip, String userAgent,
                                    Integer statusCode, Long durationMs,
                                    String errorMessage, String stackTrace) {
        return new RequestLog(null, requestId, LocalDateTime.now(),
                method, url, userId, ip, userAgent,
                statusCode, durationMs, errorMessage, stackTrace, null);
    }

    /**
     * DB 조회 결과로부터 도메인 객체 복원
     * JPA 인프라 레이어에서만 사용 — 비즈니스 로직에서 직접 호출 금지
     */
    public static RequestLog reconstruct(Long id, String requestId, LocalDateTime requestAt,
                                         String method, String url, String userId, String ip,
                                         String userAgent, Integer statusCode, Long durationMs,
                                         String errorMessage, String stackTrace, String adminMemo) {
        return new RequestLog(id, requestId, requestAt, method, url, userId, ip,
                userAgent, statusCode, durationMs, errorMessage, stackTrace, adminMemo);
    }

    /**
     * 관리자 메모 수정
     * 로그 내용은 불변이지만 메모는 관리자가 장애 원인 기록 등을 위해 수정 가능
     *
     * @param memo 관리자 메모 내용 (빈 문자열로 메모 삭제 가능)
     */
    public void updateAdminMemo(String memo) {
        this.adminMemo = memo;
    }
}
