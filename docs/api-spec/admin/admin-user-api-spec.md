# 관리자 회원 관리 API 명세서

프론트엔드 개발자를 위한 관리자 회원 관리 API 명세서입니다.

---

## 공통 정보

| 항목 | 내용 |
|------|------|
| Base URL | `http://{host}` |
| 인증 방식 | JWT Bearer Token |
| 요청 헤더 | `Authorization: Bearer <accessToken>` |
| 필요 권한 | `ROLE_ADMIN` (일반 사용자 접근 시 403 응답) |
| Content-Type | `application/json` |

### 공통 에러 응답 형식
```json
{
  "code": "에러코드",
  "message": "에러 메시지",
  "timestamp": "2024-01-01T12:00:00"
}
```

### 계정 상태(UserStatus) 값
| 값 | 설명 |
|----|------|
| `ACTIVE` | 정상 (로그인 및 모든 기능 이용 가능) |
| `SUSPENDED` | 정지 (로그인 불가, 관리자에 의해 정지된 상태) |
| `WITHDRAWN` | 탈퇴 (로그인 불가, 탈퇴 처리된 상태) |

---

## 1. 회원 목록 조회

### `GET /admin/users`

닉네임 검색, 가입일 정렬, 페이징을 지원하는 회원 목록 조회 API입니다.

#### 요청 파라미터 (Query String)

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `nickname` | String | N | null | 닉네임 부분 일치 검색 (없으면 전체 조회) |
| `sort` | String | N | `DESC` | 가입일 정렬 방향 (`ASC`: 오래된순 / `DESC`: 최신순) |
| `page` | Integer | N | `0` | 페이지 번호 (0부터 시작) |
| `size` | Integer | N | `20` | 페이지 크기 |

#### 응답 (200 OK)

```json
{
  "content": [
    {
      "id": 1,
      "userId": "hong123",
      "username": "홍길동",
      "createdAt": "2024-01-01T09:00:00",
      "status": "ACTIVE",
      "lastLoginAt": "2024-06-15T14:30:00"
    },
    {
      "id": 2,
      "userId": "kim456",
      "username": "김철수",
      "createdAt": "2024-02-10T11:00:00",
      "status": "SUSPENDED",
      "lastLoginAt": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

#### 응답 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `content[].id` | Long | 회원 DB 고유 ID |
| `content[].userId` | String | 로그인 ID |
| `content[].username` | String | 닉네임 |
| `content[].createdAt` | DateTime | 가입일 |
| `content[].status` | String | 계정 상태 (`ACTIVE` / `SUSPENDED` / `WITHDRAWN`) |
| `content[].lastLoginAt` | DateTime\|null | 마지막 로그인 시각 (이력 없으면 null) |
| `page` | Integer | 현재 페이지 번호 (0부터) |
| `size` | Integer | 페이지 크기 |
| `totalElements` | Long | 전체 회원 수 |
| `totalPages` | Integer | 전체 페이지 수 |

#### 에러 응답

| 상태 코드 | code | 설명 |
|-----------|------|------|
| 401 | `UNAUTHORIZED` | 인증 토큰 없음 또는 만료 |
| 403 | `ACCESS_DENIED` | 관리자 권한 없음 |

#### 예시 요청

```bash
# 전체 회원 목록 (최신 가입순)
curl "http://{host}/admin/users" \
  -H "Authorization: Bearer <token>"

# 닉네임 "홍" 검색, 오래된 가입순
curl "http://{host}/admin/users?nickname=홍&sort=ASC&page=0&size=10" \
  -H "Authorization: Bearer <token>"
```

---

## 2. 회원 상세 조회

### `GET /admin/users/{id}`

특정 회원의 상세 정보를 조회합니다.  
기본 정보 + 작성 게시글 목록 + 마지막 로그인 시각 + 상태 변경 이력을 포함합니다.

#### 경로 변수

| 변수 | 타입 | 설명 |
|------|------|------|
| `id` | Long | 회원 DB 고유 ID |

#### 응답 (200 OK)

```json
{
  "id": 1,
  "userId": "hong123",
  "username": "홍길동",
  "createdAt": "2024-01-01T09:00:00",
  "status": "ACTIVE",
  "lastLoginAt": "2024-06-15T14:30:00",
  "boards": [
    {
      "id": 10,
      "title": "첫 번째 게시글입니다",
      "createdAt": "2024-03-01T10:00:00"
    },
    {
      "id": 7,
      "title": "안녕하세요",
      "createdAt": "2024-02-20T09:30:00"
    }
  ],
  "statusHistories": [
    {
      "previousStatus": "ACTIVE",
      "newStatus": "SUSPENDED",
      "reason": "스팸 게시글 반복 등록",
      "adminId": "admin",
      "createdAt": "2024-06-01T10:00:00"
    },
    {
      "previousStatus": "SUSPENDED",
      "newStatus": "ACTIVE",
      "reason": "소명 완료 후 정지 해제",
      "adminId": "admin",
      "createdAt": "2024-06-10T09:00:00"
    }
  ]
}
```

#### 응답 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | 회원 DB 고유 ID |
| `userId` | String | 로그인 ID |
| `username` | String | 닉네임 |
| `createdAt` | DateTime | 가입일 |
| `status` | String | 현재 계정 상태 |
| `lastLoginAt` | DateTime\|null | 마지막 로그인 시각 |
| `boards[]` | Array | 작성 게시글 목록 (최신순) |
| `boards[].id` | Long | 게시글 ID |
| `boards[].title` | String | 게시글 제목 |
| `boards[].createdAt` | DateTime | 게시글 작성일 |
| `statusHistories[]` | Array | 상태 변경 이력 (최신순) |
| `statusHistories[].previousStatus` | String | 변경 전 상태 |
| `statusHistories[].newStatus` | String | 변경 후 상태 |
| `statusHistories[].reason` | String | 변경 사유 |
| `statusHistories[].adminId` | String | 처리 관리자 로그인 ID |
| `statusHistories[].createdAt` | DateTime | 처리 시각 |

#### 에러 응답

| 상태 코드 | code | 설명 |
|-----------|------|------|
| 401 | `UNAUTHORIZED` | 인증 토큰 없음 또는 만료 |
| 403 | `ACCESS_DENIED` | 관리자 권한 없음 |
| 404 | `USER_NOT_FOUND` | 해당 ID의 회원이 존재하지 않음 |

#### 예시 요청

```bash
curl "http://{host}/admin/users/1" \
  -H "Authorization: Bearer <token>"
```

---

## 3. 회원 상태 변경

### `PATCH /admin/users/{id}/status`

회원의 계정 상태를 변경합니다 (정상 ↔ 정지 ↔ 탈퇴).  
상태 변경 사유를 반드시 함께 전송해야 하며, 변경 이력이 자동으로 기록됩니다.

#### 경로 변수

| 변수 | 타입 | 설명 |
|------|------|------|
| `id` | Long | 회원 DB 고유 ID |

#### 요청 바디

```json
{
  "newStatus": "SUSPENDED",
  "reason": "스팸 게시글 반복 등록으로 인한 이용 정지"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `newStatus` | String | **필수** | 변경할 상태 (`ACTIVE` / `SUSPENDED` / `WITHDRAWN`) |
| `reason` | String | **필수** | 상태 변경 사유 (빈 문자열 불가) |

#### 응답 (200 OK)

응답 바디 없음 (빈 body)

#### 에러 응답

| 상태 코드 | code | 설명 |
|-----------|------|------|
| 400 | `VALIDATION_ERROR` | `newStatus` 누락 또는 `reason` 빈 문자열 |
| 401 | `UNAUTHORIZED` | 인증 토큰 없음 또는 만료 |
| 403 | `ACCESS_DENIED` | 관리자 권한 없음 |
| 404 | `USER_NOT_FOUND` | 해당 ID의 회원이 존재하지 않음 |

#### 예시 요청

```bash
# 회원 정지
curl -X PATCH "http://{host}/admin/users/1/status" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"newStatus": "SUSPENDED", "reason": "스팸 게시글 반복 등록"}'

# 정지 해제 (정상으로 복구)
curl -X PATCH "http://{host}/admin/users/1/status" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"newStatus": "ACTIVE", "reason": "소명 완료 후 정지 해제"}'

# 탈퇴 처리
curl -X PATCH "http://{host}/admin/users/1/status" \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"newStatus": "WITHDRAWN", "reason": "본인 탈퇴 요청"}'
```

---

## 4. 강제 로그아웃

### `POST /admin/users/{id}/force-logout`

특정 회원의 현재 유효한 JWT 토큰을 모두 무효화합니다.  
이 시각 이전에 발급된 모든 토큰은 즉시 사용 불가 처리됩니다.

> **동작 원리:** 토큰 블랙리스트 방식 — 서버에 `invalidatedAt` 시각을 기록하고,  
> 이 시각 이전에 발급된 토큰은 다음 API 요청 시 자동으로 인증 거부됩니다.

#### 경로 변수

| 변수 | 타입 | 설명 |
|------|------|------|
| `id` | Long | 회원 DB 고유 ID |

#### 요청 바디

없음

#### 응답 (200 OK)

응답 바디 없음 (빈 body)

#### 에러 응답

| 상태 코드 | code | 설명 |
|-----------|------|------|
| 401 | `UNAUTHORIZED` | 인증 토큰 없음 또는 만료 |
| 403 | `ACCESS_DENIED` | 관리자 권한 없음 |
| 404 | `USER_NOT_FOUND` | 해당 ID의 회원이 존재하지 않음 |

#### 예시 요청

```bash
curl -X POST "http://{host}/admin/users/1/force-logout" \
  -H "Authorization: Bearer <token>"
```

---

## 5. 비밀번호 초기화

### `POST /admin/users/{id}/reset-password`

특정 회원의 비밀번호를 임시 비밀번호(영문 대소문자 + 숫자 10자리)로 초기화합니다.  
응답으로 반환된 임시 비밀번호를 회원에게 별도 안내해주세요.  
초기화 이력은 상태 변경 이력에 자동으로 기록됩니다.

#### 경로 변수

| 변수 | 타입 | 설명 |
|------|------|------|
| `id` | Long | 회원 DB 고유 ID |

#### 요청 바디

없음

#### 응답 (200 OK)

```json
{
  "temporaryPassword": "aB3xK7mNpQ",
  "message": "비밀번호가 초기화되었습니다. 임시 비밀번호를 회원에게 안내해주세요."
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `temporaryPassword` | String | 생성된 임시 비밀번호 (영문 대소문자 + 숫자 10자리) |
| `message` | String | 안내 메시지 |

#### 에러 응답

| 상태 코드 | code | 설명 |
|-----------|------|------|
| 401 | `UNAUTHORIZED` | 인증 토큰 없음 또는 만료 |
| 403 | `ACCESS_DENIED` | 관리자 권한 없음 |
| 404 | `USER_NOT_FOUND` | 해당 ID의 회원이 존재하지 않음 |

#### 예시 요청

```bash
curl -X POST "http://{host}/admin/users/1/reset-password" \
  -H "Authorization: Bearer <token>"
```

---

## 6. 에러 코드 전체 목록

| code | HTTP 상태 | 설명 | 발생 케이스 |
|------|-----------|------|-------------|
| `USER_NOT_FOUND` | 404 | 회원을 찾을 수 없음 | 존재하지 않는 ID로 요청 |
| `USER_SUSPENDED` | 403 | 정지된 계정 | 정지 상태 회원이 로그인 시도 |
| `USER_WITHDRAWN` | 403 | 탈퇴한 계정 | 탈퇴 상태 회원이 로그인 시도 |
| `VALIDATION_ERROR` | 400 | 입력값 검증 실패 | 필수 필드 누락, 빈 문자열 등 |
| `UNAUTHORIZED` | 401 | 인증 필요 | 토큰 없음 또는 만료 |
| `ACCESS_DENIED` | 403 | 권한 없음 | 일반 사용자가 관리자 API 호출 |

---

## 7. 로그인 차단 동작 변경사항

> 기존 로그인 API에 상태 검증 로직이 추가되었습니다.

### `POST /api/auth/login` 추가 에러 응답

| 상태 코드 | code | 설명 |
|-----------|------|------|
| 403 | `USER_SUSPENDED` | 정지된 계정으로 로그인 시도 |
| 403 | `USER_WITHDRAWN` | 탈퇴한 계정으로 로그인 시도 |

#### 응답 예시 (정지된 계정)

```json
{
  "code": "USER_SUSPENDED",
  "message": "계정이 정지되었습니다. 관리자에게 문의하세요.",
  "timestamp": "2024-06-15T14:30:00"
}
```

---

## 8. 프론트엔드 구현 가이드

### 상태 뱃지 표시 권장 색상

| status | 표시 색상 | 표시 텍스트 |
|--------|-----------|-------------|
| `ACTIVE` | 초록색 | 정상 |
| `SUSPENDED` | 주황색 | 정지 |
| `WITHDRAWN` | 회색 | 탈퇴 |

### 주요 구현 포인트

1. **회원 목록 페이지**
   - `sort` 파라미터로 가입일 정렬 토글 버튼 구현 (`ASC` ↔ `DESC`)
   - `lastLoginAt`이 `null`이면 "로그인 이력 없음"으로 표시
   - 상태별 색상 뱃지 표시

2. **회원 상세 페이지**
   - `boards` 배열이 비어있으면 "작성한 게시글이 없습니다" 표시
   - `statusHistories` 배열이 비어있으면 "상태 변경 이력이 없습니다" 표시
   - 이력 최신순 표시 (API가 이미 최신순으로 정렬하여 반환)

3. **상태 변경**
   - `reason` 필드는 반드시 입력받도록 UI 처리 (빈 문자열 제출 시 400 오류)
   - 변경 전 확인 모달 팝업 권장

4. **강제 로그아웃**
   - 처리 후 "강제 로그아웃 처리되었습니다" 토스트 메시지 표시
   - 이 API는 즉시 효력 발생 (다음 API 요청 시 해당 토큰 거부)

5. **비밀번호 초기화**
   - 응답의 `temporaryPassword`를 화면에 표시하여 관리자가 회원에게 안내할 수 있도록 처리
   - 보안상 화면에 표시 후 "복사" 버튼 제공 권장
   - 페이지 벗어나면 임시 비밀번호 확인 불가 안내 문구 표시 권장

6. **강제 로그아웃 + 상태 변경 연동**
   - 회원을 `SUSPENDED`로 변경 후 강제 로그아웃을 함께 처리하는 UI 플로우 권장  
     (상태 변경만으로는 기존 토큰이 즉시 만료되지 않음)

