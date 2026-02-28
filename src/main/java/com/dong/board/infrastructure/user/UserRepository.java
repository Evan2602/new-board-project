package com.dong.board.infrastructure.user;

import com.dong.board.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * 사용자 저장소 인터페이스
 * 구체적인 저장 방식(인메모리, DB 등)에 의존하지 않는 추상화 계층
 */
public interface UserRepository {

    // 사용자 저장 (신규 생성 또는 수정)
    User save(User user);

    /**
     * DB 고유 ID로 사용자 조회
     * 관리자 회원 상세 조회 시 사용
     *
     * @param id DB 고유 ID
     * @return 사용자가 존재하면 Optional.of(user), 없으면 Optional.empty()
     */
    Optional<User> findById(Long id);

    /**
     * 로그인 ID로 사용자 조회
     */
    Optional<User> findByUserId(String userId);

    /**
     * 로그인 ID가 이미 사용 중인지 확인
     */
    boolean existsByUserId(String userId);

    /**
     * 닉네임 검색 + 페이징 조회 (관리자 회원 목록 조회용)
     * nicknameKeyword가 null이면 전체 조회
     *
     * @param nicknameKeyword 닉네임 검색어 (부분 일치, null이면 전체)
     * @param pageable        페이징 및 정렬 정보
     * @return 페이징된 사용자 목록
     */
    Page<User> findAll(String nicknameKeyword, Pageable pageable);
}
