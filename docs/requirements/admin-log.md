## 1. 관리자 인증 / 권한

### 1.1 관리자 계정
- 관리자 계정은 ROLE_ADMIN 권한을 가진 사용자만 접근 가능해야 한다.
- 관리자 API는 일반 사용자 API와 URL Prefix를 분리한다.
    - 예: /admin/**

### 1.2 접근 제어
- ROLE_ADMIN이 아닌 사용자가 관리자 API 접근 시 403 Forbidden 응답
- 관리자 접근 시도 또한 로그로 기록한다.

---

## 2. API 요청 로그 관리

### 2.1 로그 수집
- 모든 API 요청에 대해 아래 정보를 로그로 저장한다.
    - requestId (UUID)
    - 요청 시각 (requestAt)
    - HTTP Method (GET, POST, PUT, DELETE 등)
    - 요청 URL
    - 사용자 ID (비로그인 시 null)
    - IP 주소
    - User-Agent
    - 응답 상태 코드 (statusCode)
    - 응답 시간 (durationMs)

### 2.2 로그 조회 (관리자)
- 기간별 조회 (시작일 ~ 종료일)
- 상태 코드별 필터 (2xx, 4xx, 5xx)
- 특정 사용자 ID로 필터
- 특정 API URL로 검색
- 페이징 처리 (page, size)
- 최신순 정렬 기본

### 2.3 에러 로그 상세
- 4xx, 5xx 요청에 대해 상세 조회 가능해야 한다.
- 에러 발생 시 예외 메시지 및 스택 트레이스를 저장한다.
- 관리자 메모 기능 제공 (장애 원인 기록용)
