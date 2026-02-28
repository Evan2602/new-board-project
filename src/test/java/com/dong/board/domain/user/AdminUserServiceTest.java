package com.dong.board.domain.user;

import com.dong.board.domain.board.Board;
import com.dong.board.domain.log.PageResult;
import com.dong.board.exception.UserNotFoundException;
import com.dong.board.infrastructure.board.BoardRepository;
import com.dong.board.infrastructure.log.RequestLogRepository;
import com.dong.board.infrastructure.log.RequestLogSearchCondition;
import com.dong.board.infrastructure.user.TokenBlacklistRepository;
import com.dong.board.infrastructure.user.UserRepository;
import com.dong.board.infrastructure.user.UserStatusHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserStatusHistoryRepository userStatusHistoryRepository;

    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private RequestLogRepository requestLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUserService adminUserService;

    // ---- 회원 목록 조회 ----

    @Test
    @DisplayName("회원 목록 조회 - 닉네임 검색 없이 전체 조회")
    void searchUsers_noFilter_returnsAll() {
        // given
        AdminUserSearchCommand command = new AdminUserSearchCommand(null, "DESC", 0, 20);
        User mockUser = createMockUser(1L, "hong123", "홍길동");
        Page<User> mockPage = new PageImpl<>(List.of(mockUser));
        given(userRepository.findAll(isNull(), any(Pageable.class))).willReturn(mockPage);
        given(requestLogRepository.findAll(any(RequestLogSearchCondition.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        PageResult<AdminUserResult> result = adminUserService.searchUsers(command);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).userId()).isEqualTo("hong123");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("회원 목록 조회 - 닉네임 검색")
    void searchUsers_withNicknameFilter() {
        // given
        AdminUserSearchCommand command = new AdminUserSearchCommand("홍", "DESC", 0, 20);
        User mockUser = createMockUser(1L, "hong123", "홍길동");
        Page<User> mockPage = new PageImpl<>(List.of(mockUser));
        given(userRepository.findAll(eq("홍"), any(Pageable.class))).willReturn(mockPage);
        given(requestLogRepository.findAll(any(RequestLogSearchCondition.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        PageResult<AdminUserResult> result = adminUserService.searchUsers(command);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).username()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("회원 목록 조회 - 가입일 오래된순 정렬")
    void searchUsers_ascSort() {
        // given: ASC 정렬 요청
        AdminUserSearchCommand command = new AdminUserSearchCommand(null, "ASC", 0, 20);
        Page<User> mockPage = new PageImpl<>(List.of());
        given(userRepository.findAll(isNull(), any(Pageable.class))).willReturn(mockPage);

        // when
        PageResult<AdminUserResult> result = adminUserService.searchUsers(command);

        // then: 정렬 방향이 ASC로 전달되었는지 확인
        verify(userRepository).findAll(isNull(), argThat(pageable ->
                pageable.getSort().getOrderFor("createdAt") != null &&
                pageable.getSort().getOrderFor("createdAt").isAscending()
        ));
    }

    // ---- 회원 상세 조회 ----

    @Test
    @DisplayName("회원 상세 조회 - 성공")
    void getUser_success() {
        // given
        User mockUser = createMockUser(1L, "hong123", "홍길동");
        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(boardRepository.findByAuthorId("hong123")).willReturn(List.of());
        given(userStatusHistoryRepository.findByUserId("hong123")).willReturn(List.of());
        given(requestLogRepository.findAll(any(RequestLogSearchCondition.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        AdminUserDetailResult result = adminUserService.getUser(1L);

        // then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.userId()).isEqualTo("hong123");
        assertThat(result.username()).isEqualTo("홍길동");
        assertThat(result.boards()).isEmpty();
        assertThat(result.statusHistories()).isEmpty();
    }

    @Test
    @DisplayName("회원 상세 조회 - 실패 (존재하지 않는 회원)")
    void getUser_notFound() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminUserService.getUser(999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("회원 상세 조회 - 작성 게시글 목록 포함")
    void getUser_withBoards() {
        // given
        User mockUser = createMockUser(1L, "hong123", "홍길동");
        Board mockBoard = Board.reconstruct(10L, "테스트 제목", "내용", "hong123",
                LocalDateTime.now(), LocalDateTime.now());
        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(boardRepository.findByAuthorId("hong123")).willReturn(List.of(mockBoard));
        given(userStatusHistoryRepository.findByUserId("hong123")).willReturn(List.of());
        given(requestLogRepository.findAll(any(RequestLogSearchCondition.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        AdminUserDetailResult result = adminUserService.getUser(1L);

        // then
        assertThat(result.boards()).hasSize(1);
        assertThat(result.boards().get(0).title()).isEqualTo("테스트 제목");
    }

    // ---- 회원 상태 변경 ----

    @Test
    @DisplayName("회원 상태 변경 - 성공 (정상 → 정지)")
    void changeUserStatus_success() {
        // given
        User mockUser = createMockUser(1L, "hong123", "홍길동");
        ChangeUserStatusCommand command = new ChangeUserStatusCommand(
                1L, User.UserStatus.SUSPENDED, "스팸 게시글 반복 등록", "admin"
        );
        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(userRepository.save(any(User.class))).willReturn(mockUser);
        given(userStatusHistoryRepository.save(any(UserStatusHistory.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        adminUserService.changeUserStatus(command);

        // then: 저장 및 이력 기록이 각각 호출되었는지 확인
        verify(userRepository).save(any(User.class));
        verify(userStatusHistoryRepository).save(any(UserStatusHistory.class));
    }

    @Test
    @DisplayName("회원 상태 변경 - 실패 (존재하지 않는 회원)")
    void changeUserStatus_notFound() {
        // given
        ChangeUserStatusCommand command = new ChangeUserStatusCommand(
                999L, User.UserStatus.SUSPENDED, "사유", "admin"
        );
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminUserService.changeUserStatus(command))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ---- 강제 로그아웃 ----

    @Test
    @DisplayName("강제 로그아웃 - 성공")
    void forceLogout_success() {
        // given
        User mockUser = createMockUser(1L, "hong123", "홍길동");
        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(tokenBlacklistRepository.save(any(TokenBlacklist.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        adminUserService.forceLogout(1L, "admin");

        // then: 블랙리스트 저장이 호출되었는지 확인
        verify(tokenBlacklistRepository).save(any(TokenBlacklist.class));
    }

    @Test
    @DisplayName("강제 로그아웃 - 실패 (존재하지 않는 회원)")
    void forceLogout_notFound() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminUserService.forceLogout(999L, "admin"))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ---- 비밀번호 초기화 ----

    @Test
    @DisplayName("비밀번호 초기화 - 성공 (임시 비밀번호 반환)")
    void resetPassword_success() {
        // given
        User mockUser = createMockUser(1L, "hong123", "홍길동");
        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(passwordEncoder.encode(anyString())).willReturn("encodedTempPassword");
        given(userRepository.save(any(User.class))).willReturn(mockUser);
        given(userStatusHistoryRepository.save(any(UserStatusHistory.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        String temporaryPassword = adminUserService.resetPassword(1L, "admin");

        // then: 임시 비밀번호가 반환되고, 저장 및 이력 기록이 호출되었는지 확인
        assertThat(temporaryPassword).isNotBlank();
        assertThat(temporaryPassword).hasSize(10);
        verify(userRepository).save(any(User.class));
        verify(userStatusHistoryRepository).save(any(UserStatusHistory.class));
    }

    @Test
    @DisplayName("비밀번호 초기화 - 실패 (존재하지 않는 회원)")
    void resetPassword_notFound() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminUserService.resetPassword(999L, "admin"))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ---- 헬퍼 메서드 ----

    private User createMockUser(Long id, String userId, String username) {
        return User.reconstruct(id, userId, username, "encodedPassword",
                LocalDateTime.now(), User.UserRole.ROLE_USER, User.UserStatus.ACTIVE);
    }
}

