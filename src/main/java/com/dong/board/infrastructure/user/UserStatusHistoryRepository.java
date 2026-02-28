package com.dong.board.infrastructure.user;

import com.dong.board.domain.user.UserStatusHistory;

import java.util.List;

/**
 * 회원 상태 변경 이력 저장소 인터페이스
 * 구체적인 저장 방식에 의존하지 않는 추상화 계층
 */
public interface UserStatusHistoryRepository {

    /**
     * 상태 변경 이력 저장
     *
     * @param history 저장할 이력 도메인 객체
     * @return 저장된 이력 (DB 발급 ID 포함)
     */
    UserStatusHistory save(UserStatusHistory history);

    /**
     * 특정 회원의 상태 변경 이력 전체 조회 (최신순)
     *
     * @param userId 회원의 로그인 ID
     * @return 해당 회원의 상태 변경 이력 목록 (최신순 정렬)
     */
    List<UserStatusHistory> findByUserId(String userId);
}

