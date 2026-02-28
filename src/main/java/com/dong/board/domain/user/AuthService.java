package com.dong.board.domain.user;

import com.dong.board.exception.DuplicateUsernameException;
import com.dong.board.exception.InvalidCredentialsException;
import com.dong.board.infrastructure.user.UserRepository;
import com.dong.board.security.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 인증 비즈니스 로직 서비스 (회원가입/로그인)
 *
 * 책임:
 * - 회원가입: 아이디 중복 확인 → 비밀번호 해시 → 사용자 저장 → JWT 발급
 * - 로그인: 사용자 조회 → 비밀번호 검증 → JWT 발급
 */
@Service
public class AuthService {

    // 사용자 저장/조회를 담당하는 저장소
    private final UserRepository userRepository;

    // JWT 토큰 생성/검증을 담당하는 컴포넌트
    private final JwtProvider jwtProvider;

    // 비밀번호 해싱(암호화) 및 검증을 담당하는 인코더
    private final PasswordEncoder passwordEncoder;

    // 생성자 주입 (Spring이 세 개의 빈을 자동으로 주입)
    public AuthService(UserRepository userRepository,
                       JwtProvider jwtProvider,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 회원가입 처리
     * 흐름: 아이디 중복 확인 → 비밀번호 BCrypt 해시 → User 생성 → 저장 → JWT 발급
     *
     * @param command userId(로그인 ID), username(표시 이름), password(원문 비밀번호)
     * @return accessToken(JWT), userId, username
     * @throws DuplicateUsernameException 이미 사용 중인 아이디인 경우
     */
    public AuthResult signUp(SignUpCommand command) {
        // 1. 아이디 중복 확인: 같은 userId로 이미 가입한 사람이 있으면 예외 발생
        if (userRepository.existsByUserId(command.userId())) {
            throw new DuplicateUsernameException(command.userId());
        }

        // 2. 비밀번호를 BCrypt로 해시 (원문을 DB에 저장하면 안 됨!)
        // 해시 예시: "password123" → "$2a$10$xyz..." (복원 불가)
        String encodedPassword = passwordEncoder.encode(command.password());

        // 3. 새 User 엔티티 생성 (id=null → save() 후 DB가 AUTO_INCREMENT로 발급)
        // - userId: 로그인 ID (예: "hong123")
        // - username: 표시 이름 (예: "홍길동")
        // - encodedPassword: 해시된 비밀번호
        User user = User.createNew(command.userId(), command.username(), encodedPassword);

        // 4. 저장소에 사용자 저장
        userRepository.save(user);

        // 5. JWT 토큰 생성: userId(로그인 ID)와 role(권한)을 페이로드에 저장
        String token = jwtProvider.generateToken(user.getUserId(), user.getRole().name());

        // 6. 결과 반환: 토큰 + userId + username 모두 포함
        return new AuthResult(token, user.getUserId(), user.getUsername());
    }

    /**
     * 로그인 처리
     * 흐름: userId로 사용자 조회 → 비밀번호 일치 확인 → JWT 발급
     *
     * @param command userId(로그인 ID), password(원문 비밀번호)
     * @return accessToken(JWT), userId, username
     * @throws InvalidCredentialsException 사용자가 없거나 비밀번호가 틀린 경우
     */
    public AuthResult login(LoginCommand command) {
        // 1. userId로 사용자 조회, 없으면 인증 실패 예외 발생
        User user = userRepository.findByUserId(command.userId())
                .orElseThrow(InvalidCredentialsException::new);

        // 2. 입력한 비밀번호와 저장된 해시 비교
        // passwordEncoder.matches("원문", "해시") → BCrypt가 내부적으로 복호화 없이 비교
        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        // 3. JWT 토큰 생성 후 반환 (userId + role 포함)
        String token = jwtProvider.generateToken(user.getUserId(), user.getRole().name());
        return new AuthResult(token, user.getUserId(), user.getUsername());
    }
}
