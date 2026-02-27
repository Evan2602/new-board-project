package com.dong.board.interfaces.user.api;

import com.dong.board.dto.AuthResponse;
import com.dong.board.dto.LoginRequest;
import com.dong.board.dto.SignUpRequest;
import com.dong.board.domain.user.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 REST API 컨트롤러 (회원가입/로그인)
 * 역할: HTTP 요청을 받아 서비스에 전달하고, 결과를 JSON으로 반환
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // 인증 비즈니스 로직을 처리하는 서비스
    private final AuthService authService;

    // 생성자 주입
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/sign-up → 회원가입 (201 Created)
     *
     * 요청 본문: { "userId": "hong123", "username": "홍길동", "password": "password1!" }
     * 응답: { "accessToken": "...", "tokenType": "Bearer", "userId": "hong123", "username": "홍길동" }
     *
     * @Valid: 요청 본문의 @NotBlank, @Size 등 검증 규칙을 자동으로 실행
     */
    @PostMapping("/sign-up")
    public ResponseEntity<AuthResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        // DTO → Command 변환 후 서비스 호출, 결과를 AuthResponse로 변환
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuthResponse.from(authService.signUp(request.toCommand())));
    }

    /**
     * POST /api/auth/login → 로그인 (200 OK)
     *
     * 요청 본문: { "userId": "hong123", "password": "password1!" }
     * 응답: { "accessToken": "...", "tokenType": "Bearer", "userId": "hong123", "username": "홍길동" }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(AuthResponse.from(authService.login(request.toCommand())));
    }
}
