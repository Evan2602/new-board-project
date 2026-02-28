# 백엔드 수정 요청 사항

## 요청 배경

게시글 카드에 작성자 아이디(`authorId`) 대신 닉네임(`userName`)을 표시하기 위해
`BoardResponse` DTO에 `userName` 필드 추가가 필요합니다.

---

## 수정 요청

### `BoardResponse` DTO에 `userName` 필드 추가

**현재 응답:**

```json
{
    "id": 1,
    "title": "제목",
    "content": "내용",
    "authorId": "hong123",
    "createdAt": "2026-02-26T12:00:00",
    "updatedAt": "2026-02-26T12:00:00"
}
```

**변경 후 응답:**

```json
{
    "id": 1,
    "title": "제목",
    "content": "내용",
    "authorId": "hong123",
    "userName": "홍길동",
    "createdAt": "2026-02-26T12:00:00",
    "updatedAt": "2026-02-26T12:00:00"
}
```

---

## 영향받는 엔드포인트

모든 `BoardResponse`를 반환하는 엔드포인트에 동일하게 적용 필요합니다.

| 엔드포인트         | 메서드 | 설명               |
| ------------------ | ------ | ------------------ |
| `/api/boards`      | GET    | 게시글 목록 조회   |
| `/api/boards/{id}` | GET    | 게시글 상세 조회   |
| `/api/boards`      | POST   | 게시글 생성 (응답) |
| `/api/boards/{id}` | PUT    | 게시글 수정 (응답) |

---

## 구현 참고 (Spring 기준)

`Board` 엔티티가 `User`와 연관관계가 있다면 아래와 같이 `username`을 가져올 수 있습니다.

```java
// BoardResponse DTO
public class BoardResponse {
    private Long id;
    private String title;
    private String content;
    private String authorId;
    private String userName; // 추가 필드
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 생성자 또는 정적 팩토리 메서드에서 매핑
    public static BoardResponse from(Board board) {
        return BoardResponse.builder()
            .id(board.getId())
            .title(board.getTitle())
            .content(board.getContent())
            .authorId(board.getUser().getUserId())
            .userName(board.getUser().getUsername()) // 추가
            .createdAt(board.getCreatedAt())
            .updatedAt(board.getUpdatedAt())
            .build();
    }
}
```
