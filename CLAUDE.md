# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 빌드 및 테스트 명령어

```bash
# 전체 빌드
./gradlew build

# 전체 테스트 실행
./gradlew test

# 단일 테스트 클래스 실행
./gradlew test --tests "com.dong.board.interfaces.board.BoardControllerTest"

# 단일 테스트 메서드 실행
./gradlew test --tests "com.dong.board.interfaces.board.BoardControllerTest.getBoardList_returns200"

# 애플리케이션 실행 (포트 8080)
./gradlew bootRun
```

## Spring Boot 4.x 필수 주의사항

이 프로젝트는 Spring Boot 4.0.3을 사용하며, 3.x와 패키지명이 다르다.

| 항목 | Spring Boot 3.x | Spring Boot 4.x (이 프로젝트) |
|---|---|---|
| Jackson | `com.fasterxml.jackson.databind.ObjectMapper` | `tools.jackson.databind.ObjectMapper` |
| `@WebMvcTest` | `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` | `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` |
| Mock Bean | `@MockBean` | `@MockitoBean` (`org.springframework.test.context.bean.override.mockito.MockitoBean`) |
| Web 스타터 | `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| 테스트 스타터 | `spring-boot-starter-test` | `spring-boot-starter-webmvc-test` |

## 아키텍처

### 패키지 구조 (현재)
```
com.dong.board/
├── domain/
│   ├── board/          # Board 엔티티, BoardService, BoardResult, Command 객체
│   └── user/           # User 엔티티, AuthService, AuthResult, Command 객체
├── infrastructure/     # Repository 인터페이스 + InMemory 구현체
├── interfaces/
│   ├── auth/api/       # AuthController
│   └── board/api/      # BoardController
├── security/           # JwtProvider, JwtAuthenticationFilter, SecurityConfig
├── exception/          # 커스텀 예외 + GlobalExceptionHandler
└── dto/                # HTTP 요청/응답 DTO (Request/Response)
```

### 데이터 흐름
```
HTTP Request (dto) → Controller → Service (Command 객체) → Repository
HTTP Response (dto) ← Controller ← Service (Result 객체) ← Repository
```

- **Controller**: HTTP 변환만 담당. DTO → Command 변환 후 Service 호출, Result → DTO 변환 후 반환
- **Service**: 도메인 계층에 위치. HTTP DTO에 의존하지 않고 Command/Result 사용
- **Command/Result**: Service와 Controller 사이 경계 객체 (record 타입)
- **DTO**: HTTP 레이어 전용. `toCommand()`, `from(Result)` 정적 팩토리 메서드 포함

### 인증 흐름
- `JwtAuthenticationFilter`가 `Authorization: Bearer {token}` 헤더에서 JWT 추출
- 유효한 토큰이면 `userId`를 `SecurityContextHolder`에 저장
- Controller에서 `auth.getName()` = `userId` (로그인 ID, 예: "hong123")
- `authorId`(게시글 작성자)와 `requestingUserId`(JWT에서 추출한 요청자)를 비교해 수정/삭제 권한 검사

### 핵심 설계 결정
- **`authorId` vs `username`**: 게시글에는 변경 불가능한 `authorId`(로그인 ID)를 저장, 표시용 `userName`(닉네임)은 조회 시 UserRepository에서 별도 조회
- **도메인 불변성**: 엔티티는 `private` 생성자 + `static create()` 팩토리 메서드, Lombok `@Getter`만 사용
- **저장소**: 현재 `ConcurrentHashMap` 인메모리 구현 (JPA 비활성화). `generateId()`로 AtomicLong ID 발급
- **Security**: CSRF 비활성화, Stateless 세션, GET `/api/boards/**` 및 `/api/auth/**`는 인증 불필요

## 테스트 패턴

### Controller 테스트 (`@WebMvcTest`)
```java
@WebMvcTest(BoardController.class)          // Spring Boot 4.x 패키지
class BoardControllerTest {
    @MockitoBean BoardService boardService;  // @MockBean 아닌 @MockitoBean
    @MockitoBean JwtProvider jwtProvider;   // JwtAuthenticationFilter 의존성 충족 필수

    // 인증 필요 테스트
    @WithMockUser(username = "hong123")     // auth.getName() = "hong123"
    void createBoard_returns201() { ... }

    // 쓰기 요청에 .with(csrf()) 필수
    mockMvc.perform(post("/api/boards").with(csrf()) ... )
}
```

### Service 테스트
```java
@ExtendWith(MockitoExtension.class)
class BoardServiceTest { ... }
```

## API 엔드포인트

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/auth/sign-up` | 불필요 | 회원가입 → 201 |
| POST | `/api/auth/login` | 불필요 | 로그인 → 200 |
| GET | `/api/boards` | 불필요 | 목록 조회 → 200 |
| GET | `/api/boards/{id}` | 불필요 | 단건 조회 → 200/404 |
| POST | `/api/boards` | 필요 | 게시글 생성 → 201 |
| PUT | `/api/boards/{id}` | 필요 | 게시글 수정 → 200/403/404 |
| DELETE | `/api/boards/{id}` | 필요 | 게시글 삭제 → 204/403/404 |

## 에러 응답 형식

```json
{ "code": "BOARD_NOT_FOUND", "message": "...", "timestamp": "..." }
```

| 예외 클래스 | HTTP | code |
|---|---|---|
| `BoardNotFoundException` | 404 | `BOARD_NOT_FOUND` |
| `BoardAccessDeniedException` | 403 | `ACCESS_DENIED` |
| `DuplicateUsernameException` | 409 | `DUPLICATE_USERNAME` |
| `InvalidCredentialsException` | 401 | `INVALID_CREDENTIALS` |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` |
