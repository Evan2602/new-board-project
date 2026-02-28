# MySQL + JPA 연동 개발 계획서

## 개요

현재 프로젝트는 `ConcurrentHashMap` 기반 인메모리 저장소를 사용한다.
이 계획서는 WSL Docker로 MySQL 8.x 컨테이너를 실행하고, Spring Data JPA를 통해 실제 DB와 연동하는 과정을 단계별로 기술한다.

## 개발 환경

| 항목 | 내용 |
|---|---|
| DB | MySQL 8.4 |
| 실행 환경 | WSL2 + Docker Compose |
| ORM | Spring Data JPA (Hibernate 7.x) |
| 테스트 DB | H2 인메모리 (MySQL 호환 모드) |

---

## 아키텍처 결정: 도메인 분리형 JPA 엔티티

### 선택 방안: 인프라 레이어에 JPA 엔티티 분리

```
infrastructure/
├── board/
│   ├── JpaBoardEntity.java     ← @Entity (신규)
│   ├── BoardJpaRepository.java ← extends JpaRepository (신규)
│   ├── JpaBoardRepository.java ← implements BoardRepository (신규, InMemory 대체)
│   └── BoardRepository.java    ← 인터페이스 (수정: generateId 제거)
└── user/
    ├── JpaUserEntity.java      ← @Entity (신규)
    ├── UserJpaRepository.java  ← extends JpaRepository (신규)
    ├── JpaUserRepository.java  ← implements UserRepository (신규, InMemory 대체)
    └── UserRepository.java     ← 인터페이스 (수정: generateId 제거)
```

### 이유

- 현재 도메인 엔티티(`Board`, `User`)는 `private final` 필드와 `private` 생성자를 사용한다.
- JPA는 리플렉션을 위한 `protected` 기본 생성자를 필수로 요구한다.
- 도메인 엔티티에 `@Entity`를 직접 추가하면 도메인 레이어가 JPA에 의존하게 된다.
- 인프라 레이어에 별도 JPA 엔티티를 두면 도메인 레이어를 변경하지 않고 JPA 제약을 격리할 수 있다.

---

## 단계별 구현 계획

### 1단계: Docker Compose 작성

**파일:** `docker-compose.yml` (프로젝트 루트, 신규)

```yaml
services:
  mysql:
    image: mysql:8.4
    container_name: board-mysql
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: boarddb
      MYSQL_USER: boarduser
      MYSQL_PASSWORD: boardpass
    ports:
      - "3306:3306"
    volumes:
      - board-mysql-data:/var/lib/mysql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

volumes:
  board-mysql-data:
```

**WSL 실행 방법:**
```bash
# WSL 터미널에서 프로젝트 디렉토리로 이동 후
docker compose up -d

# 상태 확인
docker compose ps

# 로그 확인
docker compose logs mysql

# 종료
docker compose down
```

---

### 2단계: 의존성 추가

**파일:** `build.gradle`

추가할 의존성:
```groovy
// JPA
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'

// MySQL 드라이버 (런타임 전용)
runtimeOnly 'com.mysql:mysql-connector-j'

// 테스트용 H2 인메모리 DB
testImplementation 'com.h2database:h2'
```

---

### 3단계: 설정 파일 수정

**파일:** `src/main/resources/application.yaml`

```yaml
spring:
  application:
    name: board
  datasource:
    url: jdbc:mysql://localhost:3306/boarddb?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: boarduser
    password: boardpass
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update          # 개발 중: 스키마 자동 생성/변경
    show-sql: true
    properties:
      hibernate:
        format_sql: true

jwt:
  secret: "board-jwt-secret-key-must-be-32-chars-minimum!!"
  expiration-ms: 86400000
```

**파일:** `src/test/resources/application.yaml` (신규)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect

jwt:
  secret: "board-jwt-secret-key-must-be-32-chars-minimum!!"
  expiration-ms: 86400000
```

---

### 4단계: 도메인 엔티티 수정 (최소 변경)

**파일:** `domain/board/Board.java`

추가할 팩토리 메서드:

```java
/**
 * 신규 게시글 생성 (id=null, JPA가 AUTO_INCREMENT로 발급)
 */
public static Board createNew(String title, String content, String authorId) {
    LocalDateTime now = LocalDateTime.now();
    return new Board(null, title, content, authorId, now, now);
}

/**
 * DB 조회 결과로부터 도메인 객체 복원
 * JPA 인프라 레이어에서만 사용
 */
public static Board reconstruct(Long id, String title, String content, String authorId,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
    return new Board(id, title, content, authorId, createdAt, updatedAt);
}
```

> 기존 `create(Long id, ...)` 메서드는 유지하거나 제거한다.
> `id` 필드는 `final` 유지 가능 (Java에서 `final Long id = null` 허용).

**파일:** `domain/user/User.java`

추가할 팩토리 메서드:

```java
/**
 * 신규 사용자 생성 (id=null)
 */
public static User createNew(String userId, String username, String encodedPassword) {
    return new User(null, userId, username, encodedPassword, LocalDateTime.now());
}

/**
 * DB 조회 결과로부터 도메인 객체 복원
 */
public static User reconstruct(Long id, String userId, String username,
                               String password, LocalDateTime createdAt) {
    return new User(id, userId, username, password, createdAt);
}
```

---

### 5단계: Repository 인터페이스 수정

**파일:** `infrastructure/board/BoardRepository.java`

```java
public interface BoardRepository {
    Board save(Board board);
    Optional<Board> findById(Long id);
    List<Board> findAll();
    void deleteById(Long id);
    boolean existsById(Long id);
    // generateId() 제거 — JPA AUTO_INCREMENT가 담당
}
```

**파일:** `infrastructure/user/UserRepository.java`

```java
public interface UserRepository {
    User save(User user);
    Optional<User> findByUserId(String userId);
    boolean existsByUserId(String userId);
    // generateId() 제거
}
```

---

### 6단계: JPA 엔티티 클래스 생성

**파일:** `infrastructure/board/JpaBoardEntity.java` (신규)

```java
@Entity
@Table(name = "boards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JpaBoardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 100)
    private String authorId;   // 작성자 로그인 ID

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** 도메인 Board → JPA 엔티티 변환 */
    public static JpaBoardEntity fromDomain(Board board) { ... }

    /** JPA 엔티티 → 도메인 Board 변환 */
    public Board toDomain() {
        return Board.reconstruct(id, title, content, authorId, createdAt, updatedAt);
    }
}
```

**파일:** `infrastructure/user/JpaUserEntity.java` (신규)

```java
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JpaUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String userId;   // 로그인 ID (유니크)

    @Column(nullable = false, length = 50)
    private String username; // 닉네임

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static JpaUserEntity fromDomain(User user) { ... }

    public User toDomain() {
        return User.reconstruct(id, userId, username, password, createdAt);
    }
}
```

---

### 7단계: Spring Data JPA Repository 인터페이스 생성

**파일:** `infrastructure/board/BoardJpaRepository.java` (신규)

```java
public interface BoardJpaRepository extends JpaRepository<JpaBoardEntity, Long> {
    // findById, findAll, save, deleteById, existsById → 기본 제공
}
```

**파일:** `infrastructure/user/UserJpaRepository.java` (신규)

```java
public interface UserJpaRepository extends JpaRepository<JpaUserEntity, Long> {
    Optional<JpaUserEntity> findByUserId(String userId);
    boolean existsByUserId(String userId);
}
```

---

### 8단계: JPA 기반 Repository 구현체 + InMemory 비활성화

**파일:** `infrastructure/board/JpaBoardRepository.java` (신규)

```java
@Repository
@RequiredArgsConstructor
public class JpaBoardRepository implements BoardRepository {

    private final BoardJpaRepository boardJpaRepository;

    @Override
    public Board save(Board board) {
        JpaBoardEntity entity = JpaBoardEntity.fromDomain(board);
        return boardJpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Board> findById(Long id) {
        return boardJpaRepository.findById(id).map(JpaBoardEntity::toDomain);
    }

    @Override
    public List<Board> findAll() {
        return boardJpaRepository.findAll().stream()
                .map(JpaBoardEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        boardJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return boardJpaRepository.existsById(id);
    }
}
```

**파일:** `infrastructure/user/JpaUserRepository.java` (신규)

```java
@Repository
@RequiredArgsConstructor
public class JpaUserRepository implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public User save(User user) {
        return userJpaRepository.save(JpaUserEntity.fromDomain(user)).toDomain();
    }

    @Override
    public Optional<User> findByUserId(String userId) {
        return userJpaRepository.findByUserId(userId).map(JpaUserEntity::toDomain);
    }

    @Override
    public boolean existsByUserId(String userId) {
        return userJpaRepository.existsByUserId(userId);
    }
}
```

**InMemory 구현체 비활성화:**
`InMemoryBoardRepository`, `InMemoryUserRepository`에서 `@Repository` 어노테이션 제거.
(두 구현체가 동시에 활성화되면 `NoUniqueBeanDefinitionException` 발생)

---

### 9단계: 서비스 코드 수정

**파일:** `domain/board/BoardService.java`

```java
@Service
public class BoardService {

    // createBoard - generateId() 제거
    public BoardResult createBoard(CreateBoardCommand command) {
        Board board = Board.createNew(command.title(), command.content(), command.authorId());
        Board saved = boardRepository.save(board);
        return toResult(saved);
    }

    // 조회 메서드에 @Transactional(readOnly = true) 추가 권장
    // 쓰기 메서드에 @Transactional 추가 권장
}
```

**파일:** `domain/user/AuthService.java`

```java
public AuthResult signUp(SignUpCommand command) {
    if (userRepository.existsByUserId(command.userId())) {
        throw new DuplicateUsernameException(command.userId());
    }
    String encodedPassword = passwordEncoder.encode(command.password());
    // generateId() 제거
    User user = User.createNew(command.userId(), command.username(), encodedPassword);
    userRepository.save(user);
    String token = jwtProvider.generateToken(user.getUserId());
    return new AuthResult(token, user.getUserId(), user.getUsername());
}
```

---

### 10단계: 테스트 코드 수정 (최소 변경)

**파일:** `service/BoardServiceTest.java`

```java
// 제거할 라인
given(boardRepository.generateId()).willReturn(1L);

// save() 목킹 시 id가 설정된 Board 반환
Board board = Board.reconstruct(1L, "제목", "내용", "hong123",
        LocalDateTime.now(), LocalDateTime.now());
given(boardRepository.save(any(Board.class))).willReturn(board);
```

**파일:** `service/AuthServiceTest.java`

```java
// 제거할 라인
given(userRepository.generateId()).willReturn(1L);
```

> `@WebMvcTest` 기반 Controller 테스트는 Service를 목킹하므로 **변경 없음**.

---

## 최종 파일 변경 목록

| 작업 | 파일 |
|---|---|
| 신규 | `docker-compose.yml` |
| 수정 | `build.gradle` |
| 수정 | `src/main/resources/application.yaml` |
| 신규 | `src/test/resources/application.yaml` |
| 수정 | `domain/board/Board.java` |
| 수정 | `domain/user/User.java` |
| 수정 | `infrastructure/board/BoardRepository.java` |
| 수정 | `infrastructure/user/UserRepository.java` |
| 신규 | `infrastructure/board/JpaBoardEntity.java` |
| 신규 | `infrastructure/user/JpaUserEntity.java` |
| 신규 | `infrastructure/board/BoardJpaRepository.java` |
| 신규 | `infrastructure/user/UserJpaRepository.java` |
| 신규 | `infrastructure/board/JpaBoardRepository.java` |
| 신규 | `infrastructure/user/JpaUserRepository.java` |
| 수정 | `infrastructure/board/InMemoryBoardRepository.java` (`@Repository` 제거) |
| 수정 | `infrastructure/user/InMemoryUserRepository.java` (`@Repository` 제거) |
| 수정 | `domain/board/BoardService.java` |
| 수정 | `domain/user/AuthService.java` |
| 수정 | `src/test/java/.../service/BoardServiceTest.java` |
| 수정 | `src/test/java/.../service/AuthServiceTest.java` |

---

## 검증 방법

### 1. Docker MySQL 기동 확인
```bash
# WSL에서
docker compose up -d
docker compose ps
# State가 Up이면 정상
```

### 2. 빌드 및 테스트
```bash
./gradlew build
./gradlew test
```

### 3. 애플리케이션 기동 및 API 검증

```bash
./gradlew bootRun
```

```bash
# 회원가입
curl -X POST http://localhost:8080/api/auth/sign-up \
  -H "Content-Type: application/json" \
  -d '{"userId":"hong123","username":"홍길동","password":"password1!"}'

# 로그인
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"userId":"hong123","password":"password1!"}'

# 게시글 생성 (JWT 토큰 필요)
curl -X POST http://localhost:8080/api/boards \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"title":"테스트 제목","content":"테스트 내용"}'

# 목록 조회
curl http://localhost:8080/api/boards
```

### 4. DB 직접 확인 (선택)
```bash
docker exec -it board-mysql mysql -u boarduser -pboardpass boarddb
# MySQL 접속 후
SELECT * FROM users;
SELECT * FROM boards;
```

---

## 주의사항

- **`ddl-auto: update`**: 개발 초기에만 사용. 운영 환경에서는 반드시 `validate` 또는 `none`으로 변경.
- **트랜잭션**: `@Transactional`을 서비스 메서드에 추가해 데이터 일관성 보장.
- **InMemory 비활성화**: JPA 구현체와 InMemory 구현체가 동시에 `@Repository`이면 빈 충돌 발생.
- **WSL + Docker Desktop**: `localhost:3306`으로 접근 가능 (WSL2 포트 포워딩 자동).
