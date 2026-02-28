# 관리자 로그 기능 구현 계획

## Context
`docs/requirements/admin-log.md` 요구사항에 따라 두 가지 기능을 구현한다.
1. **ROLE_ADMIN 권한 체계** — `/admin/**` URL을 관리자만 접근 가능하게 보호
2. **API 요청 로그 DB 저장 + 관리자 조회** — 모든 HTTP 요청을 DB에 기록하고 관리자가 필터링/페이징 조회

MVP 범위 (추후 처리 섹션 2.4 통계, 2.5 로그 데이터 관리는 제외)

---

## 핵심 설계 결정

### userId 추출 방법 (LoggingFilter)
`LoggingFilter(@Component)`는 Spring Security FilterChain보다 먼저 실행되어 `SecurityContextHolder`가 비어 있음.
→ **`JwtProvider`를 LoggingFilter에 직접 주입**해 `Authorization` 헤더에서 JWT를 직접 파싱

### 에러 정보 캡처 경로
`GlobalExceptionHandler`와 `LoggingFilter`는 서로 다른 실행 레이어.
→ `GlobalExceptionHandler`가 **`HttpServletRequest.setAttribute()`로 에러 정보 저장** → LoggingFilter의 `finally` 블록에서 읽어서 DB 저장

### 동적 필터링
5개 선택적 필터(기간, 상태코드 그룹, userId, URL) → **JPA Specification 패턴** (`JpaSpecificationExecutor`)

### 관리자 메모
`RequestLog` 도메인 엔티티의 나머지 필드는 불변. `adminMemo`만 `updateAdminMemo()` 메서드로 변경 허용

---

## 단계별 구현

### 단계 1: ROLE_ADMIN 권한 체계

**목표:** User에 role 추가 → JWT에 role 포함 → `/admin/**` 접근 제어

**수정 파일:**
- `domain/user/User.java` — `UserRole` enum(ROLE_USER, ROLE_ADMIN) 및 `role` 필드 추가. `createNew()` 기본값 ROLE_USER, `createAdmin()` 신규 팩토리, `reconstruct()` role 포함 버전으로 교체
- `infrastructure/user/JpaUserEntity.java` — `@Enumerated(EnumType.STRING)` role 컬럼 추가, `fromDomain()`/`toDomain()` 업데이트
- `security/JwtProvider.java` — `generateToken(userId, role)` 오버로드, `extractRole(token)` 추가
- `security/JwtAuthenticationFilter.java` — JWT에서 role 추출 → `SimpleGrantedAuthority(role)` 로 `UsernamePasswordAuthenticationToken` 생성
- `security/SecurityConfig.java` — `.requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")` 추가, `accessDeniedHandler` 403 JSON 응답 추가
- `domain/user/AuthService.java` — 토큰 생성 시 `user.getRole().name()` 포함

**신규 파일:**
- `admin/AdminInitializer.java` — `CommandLineRunner` 구현. `existsByUserId()` 체크 후 관리자 계정 1회 생성
- `application.yaml` — `admin.default-id`, `admin.default-password` 설정 추가

---

### 단계 2: RequestLog 도메인 + 인프라

**목표:** 로그 저장소 구축 (DB 테이블 + 도메인 엔티티 + Repository)

**신규 파일:**

`domain/log/RequestLog.java`
```
필드: id, requestId(UUID), requestAt, method, url, userId(nullable),
      ip, userAgent, statusCode, durationMs, errorMessage(nullable),
      stackTrace(nullable), adminMemo(nullable/mutable)
팩토리: create(...), reconstruct(...)
메서드: updateAdminMemo(String)
```

`infrastructure/log/RequestLogRepository.java` (인터페이스)
```
save(RequestLog) → RequestLog
findById(Long) → Optional<RequestLog>
findAll(RequestLogSearchCondition, Pageable) → Page<RequestLog>
```

`infrastructure/log/RequestLogSearchCondition.java` (record)
```
startAt, endAt, statusGroup("2xx"/"4xx"/"5xx"), userId, urlKeyword
```

`infrastructure/log/JpaRequestLogEntity.java` (`@Entity`, `@Table(name="request_logs")`)
```
인덱스: requestAt, statusCode, url, userId
컬럼: TEXT 타입(errorMessage, stackTrace, adminMemo)
fromDomain() / toDomain()
```

`infrastructure/log/RequestLogJpaRepository.java`
```
extends JpaRepository<JpaRequestLogEntity, Long>, JpaSpecificationExecutor<JpaRequestLogEntity>
```

`infrastructure/log/JpaRequestLogRepository.java` (`@Repository`)
```
Specification 빌더: requestAtBetween, statusGroupEquals(BETWEEN 200~299/400~499/500~599),
                    userIdEquals, urlContains(LIKE)
```

---

### 단계 3: LoggingFilter 전면 교체

**목표:** 콘솔 출력만 하던 LoggingFilter를 DB 저장 방식으로 교체

**수정 파일:**
- `security/LoggingFilter.java` — `JwtProvider`, `RequestLogRepository` 주입. `finally` 블록에서 DB 저장. 저장 실패 격리(`try-catch`, 로그만 출력)
- `exception/GlobalExceptionHandler.java` — 모든 `@ExceptionHandler`에 `HttpServletRequest request` 파라미터 추가. `storeErrorInfo(request, e)` 호출 추가

`LoggingFilter` 핵심 흐름:
```
요청 시: requestId(UUID), method, url, ip, userAgent, userId(JWT파싱) 수집
filterChain.doFilter() 실행
finally: statusCode, durationMs, request attribute(errorMessage, stackTrace) 읽어 DB 저장
```

`GlobalExceptionHandler` 추가 로직:
```
request.setAttribute("LOG_ERROR_MESSAGE", e.getMessage())
request.setAttribute("LOG_STACK_TRACE", sw.toString()) // StringWriter로 변환, 10000자 제한
```

---

### 단계 4: 관리자 로그 조회 API

**목표:** 관리자 전용 로그 조회/상세/메모 API 구현

**신규 파일:**

`domain/log/RequestLogSearchCommand.java` (record): startDate, endDate, statusGroup, userId, urlKeyword, page, size

`domain/log/RequestLogResult.java` (record): `from(RequestLog)` 팩토리 포함

`domain/log/PageResult.java` (제네릭 record): content, page, size, totalElements, totalPages

`domain/log/RequestLogService.java` (`@Service @Transactional`)
```
searchLogs(RequestLogSearchCommand) → PageResult<RequestLogResult>
  - LocalDate → LocalDateTime 변환 (시작: 00:00:00, 종료: 23:59:59)
  - Sort.by(DESC, "requestAt") 기본 정렬
getLog(Long id) → RequestLogResult
updateAdminMemo(Long id, String memo) → RequestLogResult
```

`interfaces/admin/api/AdminLogController.java`
```
GET  /admin/logs            → AdminLogListResponse  (쿼리파라미터: startDate, endDate, statusGroup, userId, urlKeyword, page, size)
GET  /admin/logs/{id}       → AdminLogDetailResponse
PATCH /admin/logs/{id}/memo → AdminLogDetailResponse
```

`dto/AdminLogListResponse.java` — stackTrace 제외 목록 응답 (PageResult 래핑)

`dto/AdminLogDetailResponse.java` — stackTrace, adminMemo 포함 상세 응답

`dto/AdminMemoRequest.java` — `@NotNull String memo`

---

### 단계 5: 예외 처리 보완 + 테스트

**신규 파일:**
- `exception/RequestLogNotFoundException.java` — "로그를 찾을 수 없습니다. id: {id}"
- `GlobalExceptionHandler.java` 에 `LOG_NOT_FOUND` 핸들러 추가 (404)

**테스트 파일:**
- `test/.../interfaces/admin/AdminLogControllerTest.java` — `@WebMvcTest`, `@WithMockUser(roles={"ADMIN"})` 로 200, `@WithMockUser(roles={"USER"})` 로 403 검증
- `test/.../domain/log/RequestLogServiceTest.java` — `@ExtendWith(MockitoExtension.class)` 조회/메모 수정 검증

---

## 전체 파일 목록

### 신규 생성 (18개)
```
domain/log/RequestLog.java
domain/log/RequestLogSearchCommand.java
domain/log/RequestLogResult.java
domain/log/PageResult.java
domain/log/RequestLogService.java
infrastructure/log/RequestLogRepository.java
infrastructure/log/RequestLogSearchCondition.java
infrastructure/log/JpaRequestLogEntity.java
infrastructure/log/RequestLogJpaRepository.java
infrastructure/log/JpaRequestLogRepository.java
interfaces/admin/api/AdminLogController.java
dto/AdminLogListResponse.java
dto/AdminLogDetailResponse.java
dto/AdminMemoRequest.java
exception/RequestLogNotFoundException.java
admin/AdminInitializer.java
test/.../interfaces/admin/AdminLogControllerTest.java
test/.../domain/log/RequestLogServiceTest.java
```

### 수정 (9개)
```
domain/user/User.java                    — UserRole enum, role 필드
infrastructure/user/JpaUserEntity.java   — role 컬럼
security/JwtProvider.java                — role claim
security/JwtAuthenticationFilter.java    — GrantedAuthority
security/SecurityConfig.java             — /admin/** 보호, 403 핸들러
security/LoggingFilter.java              — DB 저장으로 교체
exception/GlobalExceptionHandler.java    — storeErrorInfo() 추가
domain/user/AuthService.java             — role 포함 JWT 생성
src/main/resources/application.yaml     — admin 설정 추가
```

---

## 검증 방법

```bash
# 1. 전체 빌드 및 테스트
./gradlew test

# 2. 관리자 계정으로 로그인
POST /api/auth/login  {"userId":"admin","password":"Admin1234!"}
→ accessToken 에 role=ROLE_ADMIN 포함 확인

# 3. 일반 사용자로 관리자 API 접근 → 403
GET /admin/logs  (Authorization: Bearer {USER_TOKEN})

# 4. 관리자로 로그 조회 → 200
GET /admin/logs?statusGroup=4xx&page=0&size=10  (Authorization: Bearer {ADMIN_TOKEN})

# 5. 에러 로그 상세 확인 (4xx/5xx 요청 후)
GET /api/boards/99999  → 404 발생
GET /admin/logs?statusGroup=4xx  → errorMessage 필드에 예외 메시지 확인

# 6. 관리자 메모 작성
PATCH /admin/logs/{id}/memo  {"memo":"장애 원인 조사 완료"}  → 200

# 7. DB 직접 확인
SELECT * FROM request_logs ORDER BY request_at DESC LIMIT 10;
SELECT * FROM users WHERE role = 'ROLE_ADMIN';
```
