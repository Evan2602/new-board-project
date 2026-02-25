package com.dong.board.service;

import com.dong.board.domain.Board;
import com.dong.board.dto.BoardResponse;
import com.dong.board.dto.CreateBoardRequest;
import com.dong.board.dto.UpdateBoardRequest;
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
        // given
        Board board = Board.create(1L, "제목", "내용", "작성자");
        given(boardRepository.findById(1L)).willReturn(Optional.of(board));

        // when
        BoardResponse response = boardService.getBoard(1L);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.content()).isEqualTo("내용");
        assertThat(response.author()).isEqualTo("작성자");
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
        // given
        Board board1 = Board.create(1L, "제목1", "내용1", "작성자1");
        Board board2 = Board.create(2L, "제목2", "내용2", "작성자2");
        given(boardRepository.findAll()).willReturn(List.of(board1, board2));

        // when
        List<BoardResponse> responses = boardService.getBoardList();

        // then
        assertThat(responses).hasSize(2);
    }

    @Test
    @DisplayName("게시글 생성 - 성공")
    void createBoard_success() {
        // given
        CreateBoardRequest request = new CreateBoardRequest("제목", "내용", "작성자");
        Board board = Board.create(1L, "제목", "내용", "작성자");
        given(boardRepository.generateId()).willReturn(1L);
        given(boardRepository.save(any(Board.class))).willReturn(board);

        // when
        BoardResponse response = boardService.createBoard(request);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("제목");
    }

    @Test
    @DisplayName("게시글 수정 - 성공")
    void updateBoard_success() {
        // given
        Board board = Board.create(1L, "원래 제목", "원래 내용", "작성자");
        UpdateBoardRequest request = new UpdateBoardRequest("새 제목", "새 내용");
        given(boardRepository.findById(1L)).willReturn(Optional.of(board));
        given(boardRepository.save(any(Board.class))).willReturn(board);

        // when
        BoardResponse response = boardService.updateBoard(1L, request);

        // then
        assertThat(response.title()).isEqualTo("새 제목");
        assertThat(response.content()).isEqualTo("새 내용");
    }

    @Test
    @DisplayName("게시글 수정 - 실패 (존재하지 않는 게시글)")
    void updateBoard_notFound() {
        // given
        UpdateBoardRequest request = new UpdateBoardRequest("새 제목", "새 내용");
        given(boardRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> boardService.updateBoard(99L, request))
                .isInstanceOf(BoardNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("게시글 삭제 - 성공")
    void deleteBoard_success() {
        // given
        given(boardRepository.existsById(1L)).willReturn(true);

        // when
        boardService.deleteBoard(1L);

        // then
        verify(boardRepository).deleteById(1L);
    }

    @Test
    @DisplayName("게시글 삭제 - 실패 (존재하지 않는 게시글)")
    void deleteBoard_notFound() {
        // given
        given(boardRepository.existsById(99L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> boardService.deleteBoard(99L))
                .isInstanceOf(BoardNotFoundException.class)
                .hasMessageContaining("99");
    }
}
