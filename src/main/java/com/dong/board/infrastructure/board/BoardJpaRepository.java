package com.dong.board.infrastructure.board;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 게시글 Spring Data JPA 저장소
 * - JpaRepository가 기본 CRUD 메서드를 자동으로 구현
 *   (findById, findAll, save, deleteById, existsById 등)
 * - JpaBoardRepository (구현체)에서 내부적으로 사용
 */
public interface BoardJpaRepository extends JpaRepository<JpaBoardEntity, Long> {
}
