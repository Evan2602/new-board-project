package com.dong.board.domain.log;

import com.dong.board.exception.RequestLogNotFoundException;
import com.dong.board.infrastructure.log.RequestLogRepository;
import com.dong.board.infrastructure.log.RequestLogSearchCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 요청 로그 관리 서비스
 * - 관리자 전용 로그 조회/상세/메모 수정 기능
 */
@Service
@Transactional
@RequiredArgsConstructor
public class RequestLogService {

    private final RequestLogRepository requestLogRepository;

    /**
     * 요청 로그 목록 페이징 조회 (관리자용)
     * 기본 정렬: requestAt DESC (최신순)
     *
     * @param command 조회 필터 및 페이징 조건
     * @return 페이징된 로그 결과
     */
    @Transactional(readOnly = true)
    public PageResult<RequestLogResult> searchLogs(RequestLogSearchCommand command) {
        // LocalDate → LocalDateTime 변환 (시작: 해당 날짜 00:00:00, 종료: 23:59:59)
        LocalDateTime startAt = (command.startDate() != null)
                ? command.startDate().atStartOfDay() : null;
        LocalDateTime endAt = (command.endDate() != null)
                ? command.endDate().atTime(23, 59, 59) : null;

        RequestLogSearchCondition condition = new RequestLogSearchCondition(
                startAt, endAt, command.statusGroup(), command.userId(), command.urlKeyword()
        );

        // 최신순(requestAt DESC) 기본 정렬
        Pageable pageable = PageRequest.of(
                command.page(),
                command.size(),
                Sort.by(Sort.Direction.DESC, "requestAt")
        );

        Page<RequestLog> page = requestLogRepository.findAll(condition, pageable);

        return new PageResult<>(
                page.getContent().stream().map(RequestLogResult::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    /**
     * 요청 로그 단건 상세 조회 (에러 로그 포함)
     *
     * @param id 로그 ID
     * @return 로그 상세 정보 (stackTrace, adminMemo 포함)
     * @throws RequestLogNotFoundException 해당 ID의 로그가 없을 때
     */
    @Transactional(readOnly = true)
    public RequestLogResult getLog(Long id) {
        RequestLog log = requestLogRepository.findById(id)
                .orElseThrow(() -> new RequestLogNotFoundException(id));
        return RequestLogResult.from(log);
    }

    /**
     * 관리자 메모 수정 (PATCH)
     * 장애 원인 기록, 처리 현황 메모 등 관리자가 자유롭게 작성
     *
     * @param id   로그 ID
     * @param memo 메모 내용 (빈 문자열로 메모 삭제 가능)
     * @return 수정된 로그 상세 정보
     * @throws RequestLogNotFoundException 해당 ID의 로그가 없을 때
     */
    public RequestLogResult updateAdminMemo(Long id, String memo) {
        RequestLog log = requestLogRepository.findById(id)
                .orElseThrow(() -> new RequestLogNotFoundException(id));
        log.updateAdminMemo(memo);
        RequestLog saved = requestLogRepository.save(log);
        return RequestLogResult.from(saved);
    }
}
