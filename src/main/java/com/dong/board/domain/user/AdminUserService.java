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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 회원 관리 서비스
 * - 회원 목록/상세 조회, 상태 변경, 강제 로그아웃, 비밀번호 초기화 담당
 * - /admin/** 경로를 통해 ROLE_ADMIN 권한을 가진 관리자만 호출 가능
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserStatusHistoryRepository userStatusHistoryRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final BoardRepository boardRepository;
    private final RequestLogRepository requestLogRepository;
    private final PasswordEncoder passwordEncoder;

    // 임시 비밀번호 생성에 사용할 문자 집합 (영문 대소문자 + 숫자)
    private static final String PASSWORD_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TEMP_PASSWORD_LENGTH = 10;

    /**
     * 회원 목록 페이징 조회 (관리자용)
     * - 닉네임 부분 일치 검색 지원
     * - 가입일 기준 정렬 (ASC/DESC)
     * - 각 회원의 마지막 로그인 시각은 요청 로그에서 조회
     *
     * @param command 닉네임 검색어, 정렬 방향, 페이징 정보
     * @return 페이징된 회원 목록 결과
     */
    @Transactional(readOnly = true)
    public PageResult<AdminUserResult> searchUsers(AdminUserSearchCommand command) {
        // 정렬 방향 파싱 (기본값 DESC — 최신 가입자 우선)
        Sort.Direction direction = "ASC".equalsIgnoreCase(command.sortDirection())
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(
                command.page(),
                command.size(),
                Sort.by(direction, "createdAt")
        );

        Page<User> userPage = userRepository.findAll(command.nicknameKeyword(), pageable);

        // 각 회원의 마지막 로그인 시각 조회 (POST /api/auth/login 요청 로그에서 추출)
        List<AdminUserResult> results = userPage.getContent().stream()
                .map(user -> {
                    LocalDateTime lastLoginAt = findLastLoginAt(user.getUserId());
                    return AdminUserResult.from(user, lastLoginAt);
                })
                .toList();

        return new PageResult<>(
                results,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages()
        );
    }

    /**
     * 회원 상세 조회 (관리자용)
     * - 기본 회원 정보 + 작성 게시글 목록 + 최근 로그인 이력 + 상태 변경 이력
     *
     * @param id 회원 DB 고유 ID
     * @return 회원 상세 정보
     * @throws UserNotFoundException 해당 ID의 회원이 없을 때
     */
    @Transactional(readOnly = true)
    public AdminUserDetailResult getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        // 마지막 로그인 시각 조회
        LocalDateTime lastLoginAt = findLastLoginAt(user.getUserId());

        // 작성 게시글 목록 (최신순)
        List<AdminUserDetailResult.BoardSummary> boards = boardRepository
                .findByAuthorId(user.getUserId())
                .stream()
                .map(board -> new AdminUserDetailResult.BoardSummary(
                        board.getId(), board.getTitle(), board.getCreatedAt()))
                .toList();

        // 상태 변경 이력 (최신순)
        List<AdminUserDetailResult.StatusHistorySummary> statusHistories =
                userStatusHistoryRepository.findByUserId(user.getUserId())
                        .stream()
                        .map(AdminUserDetailResult.StatusHistorySummary::from)
                        .toList();

        return new AdminUserDetailResult(
                user.getId(),
                user.getUserId(),
                user.getUsername(),
                user.getCreatedAt(),
                user.getStatus(),
                lastLoginAt,
                boards,
                statusHistories
        );
    }

    /**
     * 회원 상태 변경 (정상 ↔ 정지 ↔ 탈퇴)
     * - 상태 변경 후 변경 이력을 user_status_histories 테이블에 기록
     *
     * @param command userId(DB ID), newStatus, reason, adminId
     * @throws UserNotFoundException 해당 ID의 회원이 없을 때
     */
    public void changeUserStatus(ChangeUserStatusCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        User.UserStatus previousStatus = user.getStatus();

        // 도메인 상태 변경
        user.changeStatus(command.newStatus());
        userRepository.save(user);

        // 상태 변경 이력 기록
        UserStatusHistory history = UserStatusHistory.create(
                user.getUserId(),
                previousStatus,
                command.newStatus(),
                command.reason(),
                command.adminId()
        );
        userStatusHistoryRepository.save(history);
    }

    /**
     * 강제 로그아웃 처리
     * - token_blacklist 테이블에 현재 시각을 invalidatedAt으로 기록
     * - 이 시각 이전에 발급된 모든 토큰이 JwtAuthenticationFilter에서 거부됨
     *
     * @param id      강제 로그아웃 대상 회원 DB ID
     * @param adminId 처리하는 관리자의 로그인 ID
     * @throws UserNotFoundException 해당 ID의 회원이 없을 때
     */
    public void forceLogout(Long id, String adminId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        // 블랙리스트에 현재 시각 기록 (이미 존재하면 갱신)
        TokenBlacklist blacklist = TokenBlacklist.create(user.getUserId());
        tokenBlacklistRepository.save(blacklist);
    }

    /**
     * 비밀번호 초기화
     * - 랜덤 임시 비밀번호(10자) 생성 → BCrypt 해시 → 저장
     * - 초기화 이력을 user_status_histories에 별도 기록 (상태 변경 없이 ACTIVE → ACTIVE + 사유로 구분)
     *
     * @param id      비밀번호를 초기화할 회원 DB ID
     * @param adminId 처리하는 관리자의 로그인 ID
     * @return 생성된 임시 비밀번호 (관리자에게 전달하여 사용자에게 안내)
     * @throws UserNotFoundException 해당 ID의 회원이 없을 때
     */
    public String resetPassword(Long id, String adminId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        // 임시 비밀번호 생성 (영문 대소문자 + 숫자 10자리)
        String temporaryPassword = generateTemporaryPassword();
        String encodedPassword = passwordEncoder.encode(temporaryPassword);

        // 비밀번호 변경
        user.resetPassword(encodedPassword);
        userRepository.save(user);

        // 비밀번호 초기화 이력 기록 (상태 이력 테이블 활용 — 상태는 동일하게 유지)
        UserStatusHistory history = UserStatusHistory.create(
                user.getUserId(),
                user.getStatus(),
                user.getStatus(),
                "[비밀번호 초기화] 관리자(" + adminId + ")에 의해 임시 비밀번호 발급",
                adminId
        );
        userStatusHistoryRepository.save(history);

        return temporaryPassword;
    }

    /**
     * 특정 회원의 마지막 로그인 시각 조회
     * 요청 로그에서 POST /api/auth/login + userId 조건으로 최신 성공 요청을 찾음
     *
     * @param userId 회원 로그인 ID
     * @return 마지막 로그인 시각 (로그인 이력 없으면 null)
     */
    private LocalDateTime findLastLoginAt(String userId) {
        // POST /api/auth/login + userId + 200 응답으로 마지막 로그인 시각 조회
        RequestLogSearchCondition condition = new RequestLogSearchCondition(
                null, null, "2xx", userId, "/api/auth/login"
        );
        Pageable pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "requestAt"));

        return requestLogRepository.findAll(condition, pageable)
                .getContent()
                .stream()
                .findFirst()
                .map(log -> log.getRequestAt())
                .orElse(null);
    }

    /**
     * 임시 비밀번호 생성 (영문 대소문자 + 숫자 10자리)
     * SecureRandom 사용으로 예측 불가능한 랜덤 비밀번호 생성
     */
    private String generateTemporaryPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}

