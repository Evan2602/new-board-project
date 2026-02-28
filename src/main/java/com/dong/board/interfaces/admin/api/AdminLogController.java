package com.dong.board.interfaces.admin.api;

import com.dong.board.domain.log.PageResult;
import com.dong.board.domain.log.RequestLogResult;
import com.dong.board.domain.log.RequestLogSearchCommand;
import com.dong.board.domain.log.RequestLogService;
import com.dong.board.dto.AdminLogDetailResponse;
import com.dong.board.dto.AdminLogListResponse;
import com.dong.board.dto.AdminMemoRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 관리자 로그 조회 API 컨트롤러
 * - /admin/** 경로는 SecurityConfig에서 ROLE_ADMIN 권한만 접근 가능
 * - 모든 메서드는 HTTP 변환만 담당 (비즈니스 로직은 RequestLogService에 위임)
 */
@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final RequestLogService requestLogService;

    /**
     * GET /admin/logs
     * 요청 로그 목록 페이징 조회
     * 모든 쿼리 파라미터는 선택적 (null이면 해당 필터 미적용)
     *
     * @param startDate   조회 시작일 (yyyy-MM-dd 형식)
     * @param endDate     조회 종료일 (yyyy-MM-dd 형식)
     * @param statusGroup 상태코드 그룹 ("2xx", "4xx", "5xx")
     * @param userId      특정 사용자 ID 필터
     * @param urlKeyword  URL 키워드 검색
     * @param page        페이지 번호 (기본값 0)
     * @param size        페이지 크기 (기본값 20)
     */
    @GetMapping
    public ResponseEntity<AdminLogListResponse> getLogs(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String statusGroup,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String urlKeyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        RequestLogSearchCommand command = new RequestLogSearchCommand(
                startDate, endDate, statusGroup, userId, urlKeyword, page, size
        );
        PageResult<RequestLogResult> result = requestLogService.searchLogs(command);
        return ResponseEntity.ok(AdminLogListResponse.from(result));
    }

    /**
     * GET /admin/logs/{id}
     * 요청 로그 단건 상세 조회 (에러 스택 트레이스, 관리자 메모 포함)
     *
     * @param id 로그 ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminLogDetailResponse> getLog(@PathVariable Long id) {
        RequestLogResult result = requestLogService.getLog(id);
        return ResponseEntity.ok(AdminLogDetailResponse.from(result));
    }

    /**
     * PATCH /admin/logs/{id}/memo
     * 관리자 메모 수정
     * 장애 원인 기록, 처리 현황 메모 등 자유 형식으로 작성
     *
     * @param id      로그 ID
     * @param request 메모 내용 (빈 문자열로 삭제 가능)
     */
    @PatchMapping("/{id}/memo")
    public ResponseEntity<AdminLogDetailResponse> updateMemo(
            @PathVariable Long id,
            @Valid @RequestBody AdminMemoRequest request) {
        RequestLogResult result = requestLogService.updateAdminMemo(id, request.memo());
        return ResponseEntity.ok(AdminLogDetailResponse.from(result));
    }
}
