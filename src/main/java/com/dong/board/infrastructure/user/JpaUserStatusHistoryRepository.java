package com.dong.board.infrastructure.user;

import com.dong.board.domain.user.UserStatusHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * UserStatusHistoryRepository 인터페이스의 JPA 구현체
 * JpaUserStatusHistoryEntity(JPA 레이어) ↔ UserStatusHistory(도메인 레이어) 변환 담당
 */
@Repository
@RequiredArgsConstructor
public class JpaUserStatusHistoryRepository implements UserStatusHistoryRepository {

    private final UserStatusHistoryJpaRepository jpaRepository;

    /**
     * 상태 변경 이력 저장
     */
    @Override
    public UserStatusHistory save(UserStatusHistory history) {
        JpaUserStatusHistoryEntity entity = JpaUserStatusHistoryEntity.fromDomain(history);
        return jpaRepository.save(entity).toDomain();
    }

    /**
     * 특정 회원의 상태 변경 이력 전체 조회 (최신순)
     */
    @Override
    public List<UserStatusHistory> findByUserId(String userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(JpaUserStatusHistoryEntity::toDomain)
                .toList();
    }
}

