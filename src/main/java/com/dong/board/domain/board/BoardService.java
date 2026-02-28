package com.dong.board.domain.board;

import com.dong.board.domain.user.User;
import com.dong.board.exception.BoardAccessDeniedException;
import com.dong.board.exception.BoardNotFoundException;
import com.dong.board.infrastructure.board.BoardRepository;
import com.dong.board.infrastructure.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 게시글 비즈니스 로직 서비스
 * HTTP 관련 DTO(Request/Response)에 의존하지 않고 Command/Result 객체를 사용합니다
 *
 * 권한 검사 방식:
 * - 게시글의 authorId(작성자 로그인 ID)와 요청자의 userId(로그인 ID)를 비교
 * - 일치하면 수정/삭제 허용, 불일치하면 403 예외 발생
 */
@Service
@Transactional         // 클래스 기본: 쓰기 트랜잭션 (조회 메서드는 readOnly로 재정의)
@RequiredArgsConstructor
public class BoardService {

    // 게시글 저장/조회/삭제를 담당하는 저장소
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    /**
     * 게시글 단건 조회
     *
     * @param id 조회할 게시글 ID
     * @return 게시글 정보 (BoardResult)
     * @throws BoardNotFoundException 해당 ID의 게시글이 없을 때
     */
    @Transactional(readOnly = true) // 읽기 전용: 영속성 컨텍스트 플러시 생략으로 성능 향상
    public BoardResult getBoard(Long id) {
        // 저장소에서 게시글 조회, 없으면 404 예외 발생
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new BoardNotFoundException(id));
        // 도메인 엔티티를 서비스 결과 객체로 변환해서 반환
        return toResult(board);
    }

    /**
     * 게시글 목록 전체 조회
     *
     * @return 모든 게시글 목록
     */
    @Transactional(readOnly = true)
    public List<BoardResult> getBoardList() {
        // 모든 게시글을 조회해서 각각 BoardResult로 변환
        return boardRepository.findAll().stream()
                .map(this::toResult)
                .toList();
    }

    /**
     * 게시글 생성
     * 작성자 ID는 JWT에서 추출한 값이 command에 담겨 옵니다
     *
     * @param command 제목, 내용, authorId(작성자 로그인 ID)
     * @return 생성된 게시글 정보
     */
    public BoardResult createBoard(CreateBoardCommand command) {
        // id=null로 생성 → save() 호출 후 DB가 AUTO_INCREMENT로 ID 발급
        Board board = Board.createNew(command.title(), command.content(), command.authorId());
        // 저장소에 저장 후 결과 반환 (반환된 board에는 DB 발급 ID가 포함됨)
        Board saved = boardRepository.save(board);
        return toResult(saved);
    }

    /**
     * 게시글 수정 (작성자 본인만 가능)
     * authorId(작성자 로그인 ID)와 requestingUserId(요청자 로그인 ID)를 비교해서 권한 검사
     *
     * @param id               수정할 게시글 ID
     * @param command          새 제목, 새 내용
     * @param requestingUserId JWT에서 꺼낸 요청자의 로그인 ID (예: "hong123")
     * @return 수정된 게시글 정보
     * @throws BoardNotFoundException     게시글이 없을 때
     * @throws BoardAccessDeniedException 작성자가 아닐 때 (403)
     */
    public BoardResult updateBoard(Long id, UpdateBoardCommand command, String requestingUserId) {
        // 1. 게시글 조회 (없으면 404)
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new BoardNotFoundException(id));

        // 2. 권한 검사: 게시글 authorId와 요청자 userId 비교
        // authorId: 게시글 저장 시 기록된 작성자 로그인 ID (예: "hong123")
        // requestingUserId: 현재 요청의 JWT 토큰에서 추출한 로그인 ID
        if (!board.getAuthorId().equals(requestingUserId)) {
            // 일치하지 않으면 403 Forbidden
            throw new BoardAccessDeniedException();
        }

        // 3. 내용 수정 (updatedAt 자동 갱신)
        board.update(command.title(), command.content());
        Board saved = boardRepository.save(board);
        return toResult(saved);
    }

    /**
     * 게시글 삭제 (작성자 본인만 가능)
     *
     * @param id               삭제할 게시글 ID
     * @param requestingUserId JWT에서 꺼낸 요청자의 로그인 ID
     * @throws BoardNotFoundException     게시글이 없을 때
     * @throws BoardAccessDeniedException 작성자가 아닐 때 (403)
     */
    public void deleteBoard(Long id, String requestingUserId) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new BoardNotFoundException(id));
        if (!board.getAuthorId().equals(requestingUserId)) {
            throw new BoardAccessDeniedException();
        }
        boardRepository.deleteById(id);
    }

    /**
     * Board 엔티티 → BoardResult 변환
     * authorId로 User를 조회해 userName(닉네임)을 함께 담습니다
     * 사용자를 찾지 못하면 authorId를 대체값으로 사용합니다
     */
    private BoardResult toResult(Board board) {
        String userName = userRepository.findByUserId(board.getAuthorId())
                .map(User::getUsername)
                .orElse(board.getAuthorId());

        return new BoardResult(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getAuthorId(),
                userName,
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }
}
