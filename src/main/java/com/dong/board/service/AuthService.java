package com.dong.board.service;

import com.dong.board.domain.User;
import com.dong.board.exception.DuplicateUsernameException;
import com.dong.board.exception.InvalidCredentialsException;
import com.dong.board.repository.UserRepository;
import com.dong.board.security.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 인증 비즈니스 로직 서비스 (회원가입/로그인)
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       JwtProvider jwtProvider,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 회원가입: 중복 확인 → BCrypt 인코딩 → 저장 → 토큰 발급
     */
    public AuthResult signUp(SignUpCommand command) {
        if (userRepository.existsByUserId(command.userId())) {
            throw new DuplicateUsernameException(command.userId());
        }
        String encodedPassword = passwordEncoder.encode(command.password());
        Long id = userRepository.generateId();
        User user = User.create(id, command.userId(), command.username(), encodedPassword);
        userRepository.save(user);

        String token = jwtProvider.generateToken(user.getUserId());
        return new AuthResult(token, user.getUserId(), user.getUsername());
    }

    /**
     * 로그인: 사용자 조회 → 비밀번호 검증 → 토큰 발급
     */
    public AuthResult login(LoginCommand command) {
        User user = userRepository.findByUserId(command.userId())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtProvider.generateToken(user.getUserId());
        return new AuthResult(token, user.getUserId(), user.getUsername());
    }
}
