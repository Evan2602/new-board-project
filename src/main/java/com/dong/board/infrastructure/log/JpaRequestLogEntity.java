package com.dong.board.infrastructure.log;

import com.dong.board.domain.log.RequestLog;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 요청 로그 JPA 엔티티 (request_logs 테이블)
 * - 도메인 RequestLog와 분리하여 JPA 제약을 인프라 레이어에 격리
 * - 대용량 데이터 처리를 위해 주요 컬럼에 인덱스 적용
 */
@Entity
@Table(
        name = "request_logs",
        indexes = {
                // 기간별 조회 성능 향상
                @Index(name = "idx_request_logs_request_at", columnList = "requestAt"),
                // 상태코드 그룹 필터 성능 향상
                @Index(name = "idx_request_logs_status_code", columnList = "statusCode"),
                // URL 검색 성능 향상
                @Index(name = "idx_request_logs_url", columnList = "url"),
                // 사용자 ID 필터 성능 향상
                @Index(name = "idx_request_logs_user_id", columnList = "userId")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JpaRequestLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 요청 고유 식별자 (UUID 형식) — 중복 저장 방지
    @Column(nullable = false, length = 36, unique = true)
    private String requestId;

    // 요청 수신 시각 (인덱스 대상)
    @Column(nullable = false)
    private LocalDateTime requestAt;

    // HTTP 메서드 (GET, POST 등)
    @Column(nullable = false, length = 10)
    private String method;

    // 요청 URL (인덱스 대상)
    @Column(nullable = false, length = 500)
    private String url;

    // 요청자 로그인 ID (비로그인 시 null, 인덱스 대상)
    @Column(length = 100)
    private String userId;

    // 클라이언트 IP 주소
    @Column(length = 50)
    private String ip;

    // User-Agent 헤더 값
    @Column(length = 500)
    private String userAgent;

    // HTTP 응답 상태 코드 (인덱스 대상)
    @Column(nullable = false)
    private Integer statusCode;

    // 요청 처리 시간 (밀리초)
    @Column(nullable = false)
    private Long durationMs;

    // 예외 메시지 (4xx/5xx 요청에만 저장, TEXT 타입)
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    // 스택 트레이스 전문 (TEXT 타입, 최대 10000자 제한 후 저장)
    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    // 관리자 메모 (장애 원인 기록용, PATCH로 수정 가능)
    @Column(columnDefinition = "TEXT")
    private String adminMemo;

    /**
     * 도메인 RequestLog → JPA 엔티티 변환
     */
    public static JpaRequestLogEntity fromDomain(RequestLog log) {
        JpaRequestLogEntity entity = new JpaRequestLogEntity();
        entity.id = log.getId();
        entity.requestId = log.getRequestId();
        entity.requestAt = log.getRequestAt();
        entity.method = log.getMethod();
        entity.url = log.getUrl();
        entity.userId = log.getUserId();
        entity.ip = log.getIp();
        entity.userAgent = log.getUserAgent();
        entity.statusCode = log.getStatusCode();
        entity.durationMs = log.getDurationMs();
        entity.errorMessage = log.getErrorMessage();
        entity.stackTrace = log.getStackTrace();
        entity.adminMemo = log.getAdminMemo();
        return entity;
    }

    /**
     * JPA 엔티티 → 도메인 RequestLog 변환
     */
    public RequestLog toDomain() {
        return RequestLog.reconstruct(id, requestId, requestAt, method, url,
                userId, ip, userAgent, statusCode, durationMs,
                errorMessage, stackTrace, adminMemo);
    }
}
