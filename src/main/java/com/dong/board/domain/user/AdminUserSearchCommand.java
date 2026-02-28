package com.dong.board.domain.user;

/**
 * 관리자 회원 목록 조회 명령 객체
 * Controller → Service 데이터 전달 (HTTP 어노테이션 없음)
 *
 * @param nicknameKeyword 닉네임 검색어 (부분 일치, null이면 전체 조회)
 * @param sortDirection   가입일 정렬 방향 ("ASC" 오래된순 / "DESC" 최신순, 기본값 DESC)
 * @param page            페이지 번호 (0부터 시작)
 * @param size            페이지 크기
 */
public record AdminUserSearchCommand(
        String nicknameKeyword,
        String sortDirection,
        int page,
        int size
) {
}

