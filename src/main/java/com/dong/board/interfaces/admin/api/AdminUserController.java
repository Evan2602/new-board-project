package com.dong.board.interfaces.admin.api;

import com.dong.board.domain.log.PageResult;
import com.dong.board.domain.user.*;
import com.dong.board.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 회원 관리 API 컨트롤러
 * - /admin/** 경로는 SecurityConfig에서 ROLE_ADMIN 권한만 접근 가능
 * - 모든 메서드는 HTTP 변환만 담당 (비즈니스 로직은 AdminUserService에 위임)
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * GET /admin/users
     * 회원 목록 페이징 조회
     * 모든 쿼리 파라미터는 선택적 (null이면 해당 필터/정렬 미적용)
     *
     * @param nickname 닉네임 검색어 (부분 일치, 없으면 전체 조회)
     * @param sort     가입일 정렬 방향 ("ASC" 오래된순 / "DESC" 최신순, 기본값 DESC)
     * @param page     페이지 번호 (기본값 0)
     * @param size     페이지 크기 (기본값 20)
     */
    @GetMapping
    public ResponseEntity<AdminUserListResponse> getUsers(
            @RequestParam(required = false) String nickname,
            @RequestParam(defaultValue = "DESC") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        AdminUserSearchCommand command = new AdminUserSearchCommand(nickname, sort, page, size);
        PageResult<AdminUserResult> result = adminUserService.searchUsers(command);
        return ResponseEntity.ok(AdminUserListResponse.from(result));
    }

    /**
     * GET /admin/users/{id}
     * 회원 상세 조회
     * 기본 정보 + 작성 게시글 목록 + 마지막 로그인 시각 + 상태 변경 이력 포함
     *
     * @param id 회원 DB 고유 ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDetailResponse> getUser(@PathVariable Long id) {
        AdminUserDetailResult result = adminUserService.getUser(id);
        return ResponseEntity.ok(AdminUserDetailResponse.from(result));
    }

    /**
     * PATCH /admin/users/{id}/status
     * 회원 상태 변경 (정상 / 정지 / 탈퇴)
     * 상태 변경 사유를 함께 저장하여 이력 관리
     *
     * @param id      회원 DB 고유 ID
     * @param request 변경할 상태(newStatus), 사유(reason)
     * @param auth    Spring Security 인증 객체 (관리자 로그인 ID 추출용)
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeUserStatusRequest request,
            Authentication auth) {

        ChangeUserStatusCommand command = new ChangeUserStatusCommand(
                id,
                request.newStatus(),
                request.reason(),
                auth.getName()  // JWT에서 추출된 관리자 로그인 ID
        );
        adminUserService.changeUserStatus(command);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /admin/users/{id}/force-logout
     * 강제 로그아웃 처리
     * 해당 회원의 모든 기존 JWT 토큰을 무효화 (블랙리스트 방식)
     *
     * @param id   회원 DB 고유 ID
     * @param auth Spring Security 인증 객체 (관리자 로그인 ID 추출용)
     */
    @PostMapping("/{id}/force-logout")
    public ResponseEntity<Void> forceLogout(
            @PathVariable Long id,
            Authentication auth) {

        adminUserService.forceLogout(id, auth.getName());
        return ResponseEntity.ok().build();
    }

    /**
     * POST /admin/users/{id}/reset-password
     * 비밀번호 초기화
     * 임시 비밀번호를 생성하여 저장하고, 응답으로 반환
     *
     * @param id   회원 DB 고유 ID
     * @param auth Spring Security 인증 객체 (관리자 로그인 ID 추출용)
     */
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(
            @PathVariable Long id,
            Authentication auth) {

        String temporaryPassword = adminUserService.resetPassword(id, auth.getName());
        return ResponseEntity.ok(ResetPasswordResponse.of(temporaryPassword));
    }
}
