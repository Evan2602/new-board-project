# Admin API 명세서

이 문서는 백엔드의 관리자(Admin) 관련 API를 프론트엔드가 바로 구현할 수 있도록 정리한 명세서입니다.

기본 정보
- Base URL: /api
- 인증: JWT (응답의 accessToken을 Authorization: Bearer <token> 헤더에 설정)
- 관리자 권한 필요: ROLE_ADMIN(일반 사용자 접근 시 403 Forbidden)
- 에러 응답 형식: { "code": "...", "message": "...", "timestamp": "YYYY-MM-DDTHH:MM:SS" }

참고: 기본 관리자 계정(개발용 초기값)
- userId: admin
- password: Admin1234!
(실제 배포시 초기화 또는 변경 필요)

1) 관리자 로그 (Admin Logs)

공통
- 모든 Admin API 호출은 Authorization: Bearer <accessToken> 헤더에 관리자 JWT 필요
- 목록 응답은 페이지네이션으로 PageResult로 래핑된 AdminLogListResponse 반환

1.1 관리자 로그 목록 조회
- 경로: GET /api/admin/logs
- 설명: 서버에 기록된 요청 로그 목록 조회(검색/필터링 가능)
- 인증: 필요 (ROLE_ADMIN)
- 쿼리 파라미터(선택)
  - startDate: YYYY-MM-DD (시작일)
  - endDate: YYYY-MM-DD (종료일)
  - statusGroup: String (예: 2xx, 4xx, 5xx)
  - userId: String (요청자 아이디)
  - urlKeyword: String (URL 포함 키워드)
  - page: Integer (기본 0)
  - size: Integer (기본 20)
- 응답(200 OK) JSON: PageResult<AdminLogListResponse>
  - AdminLogListResponse 필드 예시
    - id: Long
    - timestamp: ISO-8601 datetime
    - method: String
    - url: String
    - status: Integer
    - userId: String
    - durationMs: Long
    - adminMemo: String (관리자 메모, 목록에는 포함될 수 있음)
- 오류
  - 400 Bad Request: 파라미터 검증 실패
  - 401 Unauthorized: 인증 없음 또는 토큰 만료
  - 403 Forbidden: 관리자 권한 없음

예시 curl:
curl "http://{host}/api/admin/logs?page=0&size=20&statusGroup=4xx" \
  -H "Authorization: Bearer <token>"

1.2 관리자 로그 상세 조회
- 경로: GET /api/admin/logs/{id}
- 경로 변수: id (Long)
- 인증: 필요 (ROLE_ADMIN)
- 응답(200 OK) JSON: AdminLogDetailResponse
  - 필드 예시
    - id: Long
    - timestamp: ISO-8601 datetime
    - method: String
    - url: String
    - requestHeaders: Object
    - requestBody: String|null
    - responseStatus: Integer
    - responseBody: String|null
    - stackTrace: String|null
    - adminMemo: String|null
- 오류
  - 404 Not Found: 로그를 찾을 수 없음
  - 401 Unauthorized / 403 Forbidden

예시 curl:
curl "http://{host}/api/admin/logs/123" \
  -H "Authorization: Bearer <token>"

1.3 관리자 메모 수정
- 경로: PATCH /api/admin/logs/{id}/memo
- 경로 변수: id (Long)
- 인증: 필요 (ROLE_ADMIN)
- 요청 헤더: Content-Type: application/json
- 요청 바디(JSON):
  {
    "memo": "조사 결과 이상 없음"
  }
- 유효성
  - memo: 필수
- 응답(200 OK) JSON: AdminLogDetailResponse (수정된 항목 반환)
- 오류
  - 400 Bad Request: validation 실패
  - 404 Not Found: 로그를 찾을 수 없음
  - 401 Unauthorized / 403 Forbidden

예시 curl:
curl -X PATCH "http://{host}/api/admin/logs/123/memo" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"memo":"검토 완료 - 추가 조치 불필요"}'

2) 에러 코드 정리 (예상)
- LOG_NOT_FOUND: 관리자 로그를 찾을 수 없음 (404)
- VALIDATION_ERROR: 입력값 검증 실패 (400)
- ACCESS_DENIED: 관리자 권한 없음 (403)

3) 프론트 구현 시 주의사항
- 관리 페이지는 관리자 전용이므로 사용자 권한 확인 후 접근 제어
- 목록 조회 시 필터/페이지 파라미터를 통해 서버 비용 절감
- 상세 조회에서 민감한 정보(예: stackTrace)는 관리자 화면에서만 노출
- 메모 수정은 PATCH 사용, 실패 시 사용자에게 명확한 에러 노출

끝.

