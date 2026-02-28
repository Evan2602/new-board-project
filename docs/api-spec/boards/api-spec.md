# API 명세서

이 문서는 백엔드의 AuthController와 BoardController를 분석하여 프론트가 바로 작업할 수 있도록 정리한 API 명세서입니다.

기본 정보
- Base URL: /api
- 인증: JWT (응답의 accessToken을 Authorization: Bearer <token> 헤더에 설정)
- 에러 응답 형식: { "code": "...", "message": "...", "timestamp": "YYYY-MM-DDTHH:MM:SS" }

1) 인증 (Auth)

1.1 회원가입
- 경로: POST /api/auth/sign-up
- 설명: 회원가입 후 accessToken 반환
- 요청 헤더: Content-Type: application/json
- 요청 바디(JSON):
  {
    "userId": "hong123",
    "username": "홍길동",
    "password": "password1!"
  }
- 유효성
  - userId: 필수
  - username: 필수
  - password: 필수, 최소 8자
- 응답(201 Created) JSON:
  {
    "accessToken": "<token>",
    "tokenType": "Bearer",
    "userId": "hong123",
    "username": "홍길동"
  }
- 오류
  - 400 Bad Request: validation 실패
  - 409 Conflict: DUPLICATE_USERNAME (중복 사용자)

예시 curl:
curl -X POST http://{host}/api/auth/sign-up \
  -H "Content-Type: application/json" \
  -d '{"userId":"hong123","username":"홍길동","password":"password1!"}'

1.2 로그인
- 경로: POST /api/auth/login
- 설명: 로그인하여 accessToken 반환
- 요청 헤더: Content-Type: application/json
- 요청 바디(JSON):
  {
    "userId": "hong123",
    "password": "password1!"
  }
- 유효성
  - userId: 필수
  - password: 필수
- 응답(200 OK) JSON:
  {
    "accessToken": "<token>",
    "tokenType": "Bearer",
    "userId": "hong123",
    "username": "홍길동"
  }
- 오류
  - 400 Bad Request: validation 실패
  - 401 Unauthorized: INVALID_CREDENTIALS

예시 curl:
curl -X POST http://{host}/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"userId":"hong123","password":"password1!"}'

2) 게시글 (Boards)

공통
- 모든 게시글 생성/수정/삭제 요청은 인증 필요 (Authorization: Bearer <token>)
- BoardResponse 필드:
  - id: Long
  - title: String
  - content: String
  - authorId: String
  - createdAt: ISO-8601 datetime
  - updatedAt: ISO-8601 datetime

2.1 게시글 목록 조회
- 경로: GET /api/boards
- 인증: 선택(공개 API)
- 요청 헤더: 없음 (또는 Authorization 가능)
- 응답(200 OK) JSON: Array of BoardResponse
  [
    {
      "id": 1,
      "title": "제목",
      "content": "내용",
      "authorId": "hong123",
      "createdAt": "2025-01-01T12:00:00",
      "updatedAt": "2025-01-01T12:00:00"
    },
    ...
  ]

2.2 게시글 단건 조회
- 경로: GET /api/boards/{id}
- 경로 변수: id (Long)
- 응답(200 OK) JSON: BoardResponse
- 오류
  - 404 Not Found: BOARD_NOT_FOUND

예시 curl:
curl http://{host}/api/boards/1

2.3 게시글 생성
- 경로: POST /api/boards
- 인증: 필요 (Authorization: Bearer <token>)
- 요청 헤더:
  - Content-Type: application/json
  - Authorization: Bearer <accessToken>
- 요청 바디(JSON):
  {
    "title": "새 글 제목",
    "content": "새 글 내용"
  }
- 유효성
  - title: 필수
  - content: 필수
- 응답(201 Created) JSON: BoardResponse (생성된 게시글)
- 오류
  - 400 Bad Request: validation 실패
  - 401 Unauthorized: 인증 없음 또는 토큰 만료

예시 curl:
curl -X POST http://{host}/api/boards \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"title":"새 글","content":"내용"}'

2.4 게시글 수정
- 경로: PUT /api/boards/{id}
- 인증: 필요 (Authorization: Bearer <token>)
- 경로 변수: id (Long)
- 요청 헤더:
  - Content-Type: application/json
  - Authorization: Bearer <accessToken>
- 요청 바디(JSON):
  {
    "title": "수정된 제목",
    "content": "수정된 내용"
  }
- 응답(200 OK) JSON: BoardResponse (수정된 게시글)
- 오류
  - 400 Bad Request: validation 실패
  - 404 Not Found: BOARD_NOT_FOUND
  - 403 Forbidden: BOARD_ACCESS_DENIED (작성자와 JWT의 userId 불일치)

예시 curl:
curl -X PUT http://{host}/api/boards/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"title":"수정","content":"수정 내용"}'

2.5 게시글 삭제
- 경로: DELETE /api/boards/{id}
- 인증: 필요 (Authorization: Bearer <token>)
- 경로 변수: id (Long)
- 응답(204 No Content): 본문 없음
- 오류
  - 404 Not Found: BOARD_NOT_FOUND
  - 403 Forbidden: BOARD_ACCESS_DENIED

예시 curl:
curl -X DELETE http://{host}/api/boards/1 \
  -H "Authorization: Bearer <token>"

3) 에러 코드 정리 (예상)
- BOARD_NOT_FOUND: 게시글을 찾을 수 없음 (404)
- BOARD_ACCESS_DENIED: 수정/삭제 권한 없음 (403)
- INVALID_CREDENTIALS: 로그인 실패 (401)
- DUPLICATE_USERNAME: 회원가입 시 중복 (409)
- VALIDATION_ERROR: 입력값 검증 실패 (400)

4) 프론트 구현 시 주의사항
- 회원가입/로그인 응답에서 accessToken을 로컬 스토리지나 메모리에 저장 후 Authorization 헤더에 사용
- JWT의 subject는 userId로 가정(auth.getName()이 userId를 반환)
- 생성/수정/삭제 API 호출 시 401/403/404 처리를 사용자에게 적절히 노출
- 날짜 포맷은 ISO-8601(LocalDateTime) 형태

끝.

