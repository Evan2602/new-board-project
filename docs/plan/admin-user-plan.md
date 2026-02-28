# 관리자 회원 관리 기능 구현 계획

## Context
`docs/requirements/admin-user.md` 요구사항에 따라 관리자 회원 관리 기능을 구현한다.
1. **회원 목록 조회** — 닉네임 검색, 가입일 정렬, 페이징
2. **회원 상세 조회** — 회원 정보 + 작성 게시글 목록 + 최근 로그인 이력
3. **회원 상태 변경 (제재)** — 정상/정지/탈퇴 상태 변경 + 사유 기록 + 제재 이력 관리
4. **강제 로그아웃** — 토큰 블랙리스트 방식으로 인증 토큰 만료 처리
5. **비밀번호 초기화 (선택)** — 임시 비밀번호 발급 + 로그로 이력 기록

---

## 기존 코드 구조 분석

### 아키텍처 패턴 (레이어드 + 도메인 분리)
```
interfaces/   → Controller (HTTP 변환만 담당, 비즈니스 로직 없음)
dto/          → Request/Response DTO (Controller 전용)
domain/       → 도메인 엔티티 + Service + Command/Result (record)
infrastructure/ → Repository 인터페이스 + JPA 구현체 + JpaEntity
exception/    → 커스텀 예외 + GlobalExceptionHandler
security/     → JWT, Filter, SecurityConfig
```

### 코드 스타일 규칙
- **도메인 엔티티**: private 생성자 + `create()`, `createNew()`, `reconstruct()` 정적 팩토리 메서드
- **JPA 엔티티**: `fromDomain()` / `toDomain()` 변환 메서드, `@NoArgsConstructor(access = PROTECTED)`
- **Repository**: 도메인 인터페이스 (infrastructure/) + JPA 구현체 분리
- **Service**: `@Service @Transactional @RequiredArgsConstructor`, 조회는 `@Transactional(readOnly = true)`
- **Controller**: `@RestController @RequestMapping @RequiredArgsConstructor`, `ResponseEntity<>` 반환
- **DTO**: record 사용, `from()` 정적 팩토리 메서드로 변환
- **Command/Result**: record 사용, HTTP 어노테이션 없음
- **테스트**: Controller → `@WebMvcTest` + `@Import(SecurityConfig.class)`, Service → `@ExtendWith(MockitoExtension.class)`
- **주석**: 반드시 한국어로 작성
- **PageResult**: `domain/log/PageResult.java`의 제네릭 record 재사용

---

## 핵심 설계 결정

### 1. User 도메인에 상태(UserStatus) 추가
현재 `User`에는 상태 필드가 없다. `UserStatus` enum (ACTIVE, SUSPENDED, WITHDRAWN)을 추가한다.
- `User.create()`, `createNew()` → 기본 상태 `ACTIVE`
- `User.changeStatus(UserStatus newStatus)` 메서드 추가 (상태 변경 가능한 유일한 방법)
- `reconstruct()` → status 파라미터 추가

### 2. 제재 이력 (UserStatusHistory) 도메인 신설
상태 변경마다 이력을 별도 테이블에 기록한다.
- `user_status_histories` 테이블: id, userId, previousStatus, newStatus, reason, adminId, createdAt
- 도메인 엔티티 `UserStatusHistory` + JPA 엔티티 `JpaUserStatusHistoryEntity`

### 3. 강제 로그아웃 — 토큰 블랙리스트 방식
JWT는 서버에서 무효화할 수 없으므로, **토큰 블랙리스트** 방식을 사용한다.
- `token_blacklist` 테이블: id, userId, invalidatedAt
- 강제 로그아웃 시 해당 userId의 `invalidatedAt`을 현재 시각으로 기록
- `JwtAuthenticationFilter`에서 토큰 검증 시: userId로 블랙리스트 조회 → 토큰 발급 시각(`iat`)이 `invalidatedAt`보다 이전이면 인증 거부
- 이 방식의 장점: 개별 토큰을 저장할 필요 없이 userId 단위로 일괄 무효화 가능

### 4. 비밀번호 초기화
임시 비밀번호(8자 랜덤)를 생성하여 BCrypt 해시 후 저장한다.
- `User.resetPassword(String encodedPassword)` 메서드 추가
- 초기화 이력은 기존 `RequestLog` 시스템의 API 로그로 자동 기록됨 (LoggingFilter)
- 추가로 제재 이력과 동일하게 `UserStatusHistory`에 "비밀번호 초기화" 사유로 기록

### 5. 회원 상세 조회 — 게시글 목록 + 로그인 이력
- 게시글 목록: `BoardRepository`에 `findByAuthorId(String authorId)` 추가
- 로그인 이력: 기존 `RequestLogRepository`에서 `POST /api/auth/login` + userId 조건으로 조회

### 6. 정지 상태 사용자의 로그인 차단
- `AuthService.login()` 에서 사용자 상태 확인 → SUSPENDED/WITHDRAWN이면 예외 발생
- `UserSuspendedException`, `UserWithdrawnException` 커스텀 예외 추가

---

## 단계별 구현

### 단계 1: User 도메인 상태 확장

**목표:** User에 status 필드 추가, 상태 변경 메서드, 비밀번호 초기화 메서드 추가

**수정 파일:**

`domain/user/User.java`
- `UserStatus` enum 추가: `ACTIVE`, `SUSPENDED`, `WITHDRAWN`
- `status` 필드 추가 (기본값 ACTIVE)
- `changeStatus(UserStatus newStatus)` 메서드 추가
- `resetPassword(String encodedPassword)` 메서드 추가
- `create()`, `createNew()`, `createAdmin()` → status=ACTIVE 기본값
- `reconstruct()` → status 파라미터 추가

`infrastructure/user/JpaUserEntity.java`
- `@Enumerated(EnumType.STRING)` status 컬럼 추가 (기본값 ACTIVE)
- `fromDomain()` / `toDomain()` 업데이트

---

### 단계 2: 제재 이력 도메인 + 인프라

**목표:** 상태 변경 이력을 저장하는 도메인과 인프라 계층 구축

**신규 파일:**

`domain/user/UserStatusHistory.java` (도메인 엔티티)
```
필드: id, userId, previousStatus(UserStatus), newStatus(UserStatus),
      reason(String), adminId(String), createdAt(LocalDateTime)
팩토리: create(userId, previousStatus, newStatus, reason, adminId)
        reconstruct(id, userId, previousStatus, newStatus, reason, adminId, createdAt)
```

`infrastructure/user/UserStatusHistoryRepository.java` (인터페이스)
```
save(UserStatusHistory) → UserStatusHistory
findByUserId(String userId) → List<UserStatusHistory>  // 최신순 정렬
```

`infrastructure/user/JpaUserStatusHistoryEntity.java` (@Entity, @Table(name="user_status_histories"))
```
컬럼: id, userId, previousStatus, newStatus, reason, adminId, createdAt
인덱스: userId
fromDomain() / toDomain()
```

`infrastructure/user/UserStatusHistoryJpaRepository.java`
```
extends JpaRepository<JpaUserStatusHistoryEntity, Long>
findByUserIdOrderByCreatedAtDesc(String userId) → List<JpaUserStatusHistoryEntity>
```

`infrastructure/user/JpaUserStatusHistoryRepository.java` (@Repository 구현체)
```
save(), findByUserId() 구현
```

---

### 단계 3: 강제 로그아웃 — 토큰 블랙리스트

**목표:** userId 단위 토큰 무효화 인프라 구축

**신규 파일:**

`domain/user/TokenBlacklist.java` (도메인 엔티티)
```
필드: id, userId, invalidatedAt(LocalDateTime)
팩토리: create(userId), reconstruct(id, userId, invalidatedAt)
```

`infrastructure/user/TokenBlacklistRepository.java` (인터페이스)
```
save(TokenBlacklist) → TokenBlacklist
findByUserId(String userId) → Optional<TokenBlacklist>
```

`infrastructure/user/JpaTokenBlacklistEntity.java` (@Entity, @Table(name="token_blacklist"))
```
컬럼: id, userId(unique), invalidatedAt
fromDomain() / toDomain()
```

`infrastructure/user/TokenBlacklistJpaRepository.java`
```
extends JpaRepository<JpaTokenBlacklistEntity, Long>
findByUserId(String userId) → Optional<JpaTokenBlacklistEntity>
```

`infrastructure/user/JpaTokenBlacklistRepository.java` (@Repository 구현체)

**수정 파일:**

`security/JwtProvider.java`
- `extractIssuedAt(String token)` 메서드 추가 (토큰 발급 시각 추출)

`security/JwtAuthenticationFilter.java`
- `TokenBlacklistRepository` 주입
- 토큰 검증 후: userId로 블랙리스트 조회 → `iat < invalidatedAt`이면 인증 거부

---

### 단계 4: UserRepository 확장 + 회원 관리 Service

**목표:** 회원 목록/상세 조회, 상태 변경, 강제 로그아웃, 비밀번호 초기화 서비스 구현

**수정 파일:**

`infrastructure/user/UserRepository.java` — 메서드 추가
```
findById(Long id) → Optional<User>
findAll(String nicknameKeyword, Pageable pageable) → Page<User>  // 닉네임 검색 + 페이징
```

`infrastructure/user/UserJpaRepository.java` — 메서드 추가
```
findByUsernameContaining(String keyword, Pageable pageable) → Page<JpaUserEntity>
findAll(Pageable pageable) → Page<JpaUserEntity>  // JpaRepository 기본 제공
```

`infrastructure/user/JpaUserRepository.java` — 신규 메서드 구현

`infrastructure/board/BoardRepository.java` — 메서드 추가
```
findByAuthorId(String authorId) → List<Board>
```

`infrastructure/board/BoardJpaRepository.java` — 메서드 추가
`infrastructure/board/JpaBoardRepository.java` — 구현 추가

**신규 파일:**

`domain/user/AdminUserService.java` (@Service @Transactional)
```
- searchUsers(AdminUserSearchCommand) → PageResult<AdminUserResult>
    닉네임 검색 + 가입일 정렬(ASC/DESC) + 페이징

- getUser(Long userId) → AdminUserDetailResult
    회원 상세 정보 + 작성 게시글 목록 + 최근 로그인 이력 + 제재 이력

- changeUserStatus(ChangeUserStatusCommand) → void
    상태 변경 + UserStatusHistory 기록

- forceLogout(Long userId, String adminId) → void
    TokenBlacklist에 현재 시각 기록 (이전 토큰 전부 무효화)

- resetPassword(Long userId, String adminId) → String (임시 비밀번호 반환)
    8자 랜덤 비밀번호 생성 → BCrypt 해시 → 저장
    UserStatusHistory에 "비밀번호 초기화" 기록
```

`domain/user/AdminUserSearchCommand.java` (record)
```
nicknameKeyword(String), sortDirection(String: "ASC"/"DESC"), page(int), size(int)
```

`domain/user/AdminUserResult.java` (record) — 목록용
```
id, userId, username, createdAt, status, lastLoginAt(LocalDateTime)
from(User, LocalDateTime lastLoginAt)
```

`domain/user/AdminUserDetailResult.java` (record) — 상세용
```
id, userId, username, createdAt, status, lastLoginAt,
boards(List<BoardSummary>), statusHistories(List<StatusHistorySummary>)

BoardSummary: id, title, createdAt
StatusHistorySummary: previousStatus, newStatus, reason, adminId, createdAt
```

`domain/user/ChangeUserStatusCommand.java` (record)
```
userId(Long), newStatus(UserStatus), reason(String), adminId(String)
```

---

### 단계 5: 관리자 회원 관리 API 컨트롤러

**목표:** 관리자 전용 회원 관리 REST API 엔드포인트

**신규 파일:**

`interfaces/admin/api/AdminUserController.java`
```
GET    /admin/users                → 회원 목록 조회 (닉네임 검색, 정렬, 페이징)
GET    /admin/users/{id}           → 회원 상세 조회
PATCH  /admin/users/{id}/status    → 회원 상태 변경
POST   /admin/users/{id}/force-logout  → 강제 로그아웃
POST   /admin/users/{id}/reset-password → 비밀번호 초기화
```

`dto/AdminUserListResponse.java` (record) — 목록 응답
```
content(List<UserSummary>), page, size, totalElements, totalPages
UserSummary: id, userId, username, createdAt, status, lastLoginAt
from(PageResult<AdminUserResult>)
```

`dto/AdminUserDetailResponse.java` (record) — 상세 응답
```
id, userId, username, createdAt, status, lastLoginAt,
boards(List<BoardItem>), statusHistories(List<HistoryItem>)
from(AdminUserDetailResult)
```

`dto/ChangeUserStatusRequest.java` (record) — 상태 변경 요청
```
@NotNull UserStatus newStatus
@NotBlank String reason
```

`dto/ResetPasswordResponse.java` (record) — 임시 비밀번호 응답
```
String temporaryPassword
String message
```

---

### 단계 6: 로그인 차단 + 예외 처리

**목표:** 정지/탈퇴 상태 사용자의 로그인을 차단하고 적절한 에러 응답 반환

**수정 파일:**

`domain/user/AuthService.java` — login() 메서드에 상태 검증 추가
```java
// 사용자 조회 후 상태 확인
if (user.getStatus() == UserStatus.SUSPENDED) {
    throw new UserSuspendedException();
}
if (user.getStatus() == UserStatus.WITHDRAWN) {
    throw new UserWithdrawnException();
}
```

**신규 파일:**

`exception/UserSuspendedException.java`
```
"계정이 정지되었습니다. 관리자에게 문의하세요."
```

`exception/UserWithdrawnException.java`
```
"탈퇴한 계정입니다."
```

`exception/UserNotFoundException.java`
```
"회원을 찾을 수 없습니다. id: {id}"
```

**수정 파일:**

`exception/GlobalExceptionHandler.java` — 신규 예외 핸들러 추가
```
UserSuspendedException → 403 FORBIDDEN, "USER_SUSPENDED"
UserWithdrawnException → 403 FORBIDDEN, "USER_WITHDRAWN"
UserNotFoundException  → 404 NOT_FOUND, "USER_NOT_FOUND"
```

---

### 단계 7: 테스트

**신규 파일:**

`test/.../domain/user/AdminUserServiceTest.java`
```
@ExtendWith(MockitoExtension.class)
- 회원 목록 조회 - 닉네임 검색
- 회원 목록 조회 - 가입일 정렬
- 회원 상세 조회 - 성공
- 회원 상세 조회 - 실패 (존재하지 않는 회원)
- 회원 상태 변경 - 성공 (정상→정지)
- 회원 상태 변경 - 실패 (존재하지 않는 회원)
- 강제 로그아웃 - 성공
- 비밀번호 초기화 - 성공
```

`test/.../interfaces/admin/AdminUserControllerTest.java`
```
@WebMvcTest(AdminUserController.class) + @Import(SecurityConfig.class)
- GET /admin/users → 200 (ROLE_ADMIN)
- GET /admin/users → 403 (ROLE_USER)
- GET /admin/users → 401 (미인증)
- GET /admin/users/{id} → 200 (상세 조회)
- GET /admin/users/{id} → 404 (존재하지 않는 회원)
- PATCH /admin/users/{id}/status → 200 (상태 변경)
- POST /admin/users/{id}/force-logout → 200 (강제 로그아웃)
- POST /admin/users/{id}/reset-password → 200 (비밀번호 초기화)
```

---

## API 엔드포인트 요약

| Method | URL | 설명 | 권한 |
|--------|-----|------|------|
| GET | `/admin/users` | 회원 목록 조회 | ROLE_ADMIN |
| GET | `/admin/users/{id}` | 회원 상세 조회 | ROLE_ADMIN |
| PATCH | `/admin/users/{id}/status` | 회원 상태 변경 | ROLE_ADMIN |
| POST | `/admin/users/{id}/force-logout` | 강제 로그아웃 | ROLE_ADMIN |
| POST | `/admin/users/{id}/reset-password` | 비밀번호 초기화 | ROLE_ADMIN |

### 쿼리 파라미터 (GET /admin/users)
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| nickname | String | N | null | 닉네임 검색 (부분 일치) |
| sort | String | N | "DESC" | 가입일 정렬 (ASC/DESC) |
| page | int | N | 0 | 페이지 번호 |
| size | int | N | 20 | 페이지 크기 |

---

## 전체 파일 목록

### 신규 생성 (21개)
```
# 도메인 (6개)
domain/user/UserStatusHistory.java
domain/user/AdminUserService.java
domain/user/AdminUserSearchCommand.java
domain/user/AdminUserResult.java
domain/user/AdminUserDetailResult.java
domain/user/ChangeUserStatusCommand.java

# 인프라 (6개)
infrastructure/user/UserStatusHistoryRepository.java
infrastructure/user/JpaUserStatusHistoryEntity.java
infrastructure/user/UserStatusHistoryJpaRepository.java
infrastructure/user/JpaUserStatusHistoryRepository.java
infrastructure/user/TokenBlacklist.java  → domain/user/ 로 이동
infrastructure/user/TokenBlacklistRepository.java
infrastructure/user/JpaTokenBlacklistEntity.java
infrastructure/user/TokenBlacklistJpaRepository.java
infrastructure/user/JpaTokenBlacklistRepository.java

# 인터페이스 + DTO (5개)
interfaces/admin/api/AdminUserController.java
dto/AdminUserListResponse.java
dto/AdminUserDetailResponse.java
dto/ChangeUserStatusRequest.java
dto/ResetPasswordResponse.java

# 예외 (3개)
exception/UserSuspendedException.java
exception/UserWithdrawnException.java
exception/UserNotFoundException.java

# 테스트 (2개)
test/.../domain/user/AdminUserServiceTest.java
test/.../interfaces/admin/AdminUserControllerTest.java
```

### 수정 (10개)
```
domain/user/User.java                       — UserStatus enum, status 필드, changeStatus(), resetPassword()
infrastructure/user/JpaUserEntity.java       — status 컬럼 추가
infrastructure/user/UserRepository.java      — findById(), findAll(검색+페이징) 추가
infrastructure/user/UserJpaRepository.java   — 검색 쿼리 메서드 추가
infrastructure/user/JpaUserRepository.java   — 신규 메서드 구현
infrastructure/board/BoardRepository.java    — findByAuthorId() 추가
infrastructure/board/BoardJpaRepository.java — findByAuthorId() 추가 (Spring Data JPA)
infrastructure/board/JpaBoardRepository.java — findByAuthorId() 구현
security/JwtProvider.java                    — extractIssuedAt() 추가
security/JwtAuthenticationFilter.java        — 토큰 블랙리스트 검증 추가
domain/user/AuthService.java                 — 로그인 시 상태 검증 추가
exception/GlobalExceptionHandler.java        — 신규 예외 핸들러 추가
```

---

## 구현 순서 요약

```
단계 1: User 도메인 상태 확장 (UserStatus enum + 필드 + 메서드)
   ↓
단계 2: 제재 이력 도메인 + 인프라 (UserStatusHistory)
   ↓
단계 3: 강제 로그아웃 인프라 (TokenBlacklist + JwtFilter 수정)
   ↓
단계 4: UserRepository 확장 + AdminUserService 구현
   ↓
단계 5: AdminUserController + DTO 구현
   ↓
단계 6: 로그인 차단 + 예외 처리
   ↓
단계 7: 테스트 작성
```

