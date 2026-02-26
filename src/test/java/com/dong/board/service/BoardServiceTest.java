package com.dong.board.service;

import com.dong.board.domain.Board;
import com.dong.board.exception.BoardAccessDeniedException;
import com.dong.board.exception.BoardNotFoundException;
import com.dong.board.repository.BoardRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @InjectMocks
    private BoardService boardService;

    @Test
    @DisplayName("게시글 단건 조회 - 성공")
    void getBoard_success() {
        // given: authorId로 로그인 ID("hong123")를 저장
        Board board = Board.create(1L, "제목", "내용", "hong123");
        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        // when
        BoardResult result = boardService.getBoard(1L);

        // then: authorId가 "hong123"으로 반환되는지 확인
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("제목");
        assertThat(result.content()).isEqualTo("내용");
        assertThat(result.authorId()).isEqualTo("hong123");
    }

    @Test
    @DisplayName("게시글 단건 조회 - 실패 (존재하지 않는 게시글)")
    void getBoard_notFound() {
        // given
        given(boardRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> boardService.getBoard(99L))
                .isInstanceOf(BoardNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("게시글 목록 조회 - 성공")
    void getBoardList_success() {
        // given: 두 사용자의 게시글
        Board board1 = Board.create(1L, "제목1", "내용1", "hong123");
        Board board2 = Board.create(2L, "제목2", "내용2", "kim456");
        given(boardRepository.findAll()).willReturn(List.of(board1, board2));

        // when
        List<BoardResult> results = boardService.getBoardList();

        // then
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("게시글 생성 - 성공")
    void createBoard_success() {
        // given: authorId에 로그인 ID 전달
        CreateBoardCommand command = new CreateBoardCommand("제목", "내용", "hong123");
        Board board = Board.create(1L, "제목", "내용", "hong123");
        given(boardRepository.generateId()).willReturn(1L);
        given(boardRepository.save(any(Board.class))).willReturn(board);

        // when
        BoardResult result = boardService.createBoard(command);

        // then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("제목");
    }

    @Test
    @DisplayName("게시글 수정 - 성공")
    void updateBoard_success() {
        // given: 작성자 로그인 ID "hong123"으로 게시글 생성
        Board board = Board.create(1L, "원래 제목", "원래 내용", "hong123");
        UpdateBoardCommand command = new UpdateBoardCommand("새 제목", "새 내용");
        given(boardRepository.findById(1L)).willReturn(Optional.of(board));
        given(boardRepository.save(any(Board.class))).willReturn(board);

        // when: 동일한 userId로 수정 요청 → 성공
        BoardResult result = boardService.updateBoard(1L, command, "hong123");

        // then
        assertThat(result.title()).isEqualTo("새 제목");
        assertThat(result.content()).isEqualTo("새 내용");
    }

    @Test
    @DisplayName("게시글 수정 - 실패 (존재하지 않는 게시글)")
    void updateBoard_notFound() {
        // given
        UpdateBoardCommand command = new UpdateBoardCommand("새 제목", "새 내용");
        given(boardRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> boardService.updateBoard(99L, command, "hong123"))
                .isInstanceOf(BoardNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("게시글 수정 - 실패 (작성자 불일치)")
    void updateBoard_accessDenied() {
        // given: 게시글 authorId = "hong123"
        Board board = Board.create(1L, "원래 제목", "원래 내용", "hong123");
        UpdateBoardCommand command = new UpdateBoardCommand("새 제목", "새 내용");
        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        // when & then: 다른 userId("other_user")로 수정 시도 → 403 예외
        assertThatThrownBy(() -> boardService.updateBoard(1L, command, "other_user"))
                .isInstanceOf(BoardAccessDeniedException.class);
    }

    @Test
    @DisplayName("게시글 삭제 - 성공")
    void deleteBoard_success() {
        // given: 작성자 "hong123"의 게시글
        Board board = Board.create(1L, "제목", "내용", "hong123");
        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        // when: 동일한 userId로 삭제 요청 → 성공
        boardService.deleteBoard(1L, "hong123");

        // then: 저장소의 deleteById가 호출되었는지 확인
        verify(boardRepository).deleteById(1L);
    }

    @Test
    @DisplayName("게시글 삭제 - 실패 (존재하지 않는 게시글)")
    void deleteBoard_notFound() {
        // given
        given(boardRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> boardService.deleteBoard(99L, "hong123"))
                .isInstanceOf(BoardNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("게시글 삭제 - 실패 (작성자 불일치)")
    void deleteBoard_accessDenied() {
        // given: 게시글 authorId = "hong123"
        Board board = Board.create(1L, "제목", "내용", "hong123");
        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        // when & then: 다른 userId("other_user")로 삭제 시도 → 403 예외
        assertThatThrownBy(() -> boardService.deleteBoard(1L, "other_user"))
                .isInstanceOf(BoardAccessDeniedException.class);
    }
}
