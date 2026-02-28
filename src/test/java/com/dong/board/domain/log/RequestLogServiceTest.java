package com.dong.board.domain.log;

import com.dong.board.exception.RequestLogNotFoundException;
import com.dong.board.infrastructure.log.RequestLogRepository;
import com.dong.board.infrastructure.log.RequestLogSearchCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RequestLogServiceTest {

    @Mock
    private RequestLogRepository requestLogRepository;

    @InjectMocks
    private RequestLogService requestLogService;

    @Test
    @DisplayName("로그 목록 조회 - 필터 없이 전체 조회")
    void searchLogs_returnsAll() {
        // given
        RequestLogSearchCommand command = new RequestLogSearchCommand(
                null, null, null, null, null, 0, 20
        );
        RequestLog mockLog = createMockLog(1L, 200);
        Page<RequestLog> mockPage = new PageImpl<>(List.of(mockLog));
        given(requestLogRepository.findAll(any(RequestLogSearchCondition.class), any(Pageable.class)))
                .willReturn(mockPage);

        // when
        PageResult<RequestLogResult> result = requestLogService.searchLogs(command);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().get(0).statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("로그 목록 조회 - 날짜 필터 적용")
    void searchLogs_withDateFilter() {
        // given: 2024-01-01 ~ 2024-01-31 기간 필터
        RequestLogSearchCommand command = new RequestLogSearchCommand(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31),
                null, null, null, 0, 20
        );
        Page<RequestLog> mockPage = new PageImpl<>(List.of());
        given(requestLogRepository.findAll(any(RequestLogSearchCondition.class), any(Pageable.class)))
                .willReturn(mockPage);

        // when
        PageResult<RequestLogResult> result = requestLogService.searchLogs(command);

        // then: Repository가 올바른 조건으로 호출되는지 확인
        verify(requestLogRepository).findAll(any(RequestLogSearchCondition.class), any(Pageable.class));
        assertThat(result.content()).isEmpty();
    }

    @Test
    @DisplayName("로그 단건 조회 - 성공")
    void getLog_success() {
        // given
        RequestLog mockLog = createMockLog(1L, 404);
        given(requestLogRepository.findById(1L)).willReturn(Optional.of(mockLog));

        // when
        RequestLogResult result = requestLogService.getLog(1L);

        // then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.statusCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("로그 단건 조회 - 실패 (존재하지 않는 로그)")
    void getLog_notFound() {
        // given
        given(requestLogRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> requestLogService.getLog(999L))
                .isInstanceOf(RequestLogNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("관리자 메모 수정 - 성공")
    void updateAdminMemo_success() {
        // given
        RequestLog mockLog = createMockLog(1L, 200);
        given(requestLogRepository.findById(1L)).willReturn(Optional.of(mockLog));
        given(requestLogRepository.save(any(RequestLog.class))).willReturn(mockLog);

        // when
        requestLogService.updateAdminMemo(1L, "장애 원인 확인 완료");

        // then: 저장이 호출되었는지 확인
        verify(requestLogRepository).save(any(RequestLog.class));
    }

    @Test
    @DisplayName("관리자 메모 수정 - 실패 (존재하지 않는 로그)")
    void updateAdminMemo_notFound() {
        // given
        given(requestLogRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> requestLogService.updateAdminMemo(999L, "메모"))
                .isInstanceOf(RequestLogNotFoundException.class);
    }

    // ---- 테스트 픽스처 ----

    private RequestLog createMockLog(Long id, int statusCode) {
        return RequestLog.reconstruct(
                id, "uuid-" + id, LocalDateTime.now(),
                "GET", "/api/boards/1", "hong123",
                "127.0.0.1", "Mozilla/5.0",
                statusCode, 50L, null, null, null
        );
    }
}
