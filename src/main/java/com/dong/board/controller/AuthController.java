package com.dong.board.controller;

import com.dong.board.dto.AuthResponse;
import com.dong.board.dto.LoginRequest;
import com.dong.board.dto.SignUpRequest;
import com.dong.board.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 REST API 컨트롤러 (회원가입/로그인)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/sign-up → 회원가입 (201)
     */
    @PostMapping("/sign-up")
    public ResponseEntity<AuthResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuthResponse.from(authService.signUp(request.toCommand())));
    }

    /**
     * POST /api/auth/login → 로그인 (200)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(AuthResponse.from(authService.login(request.toCommand())));
    }
}
