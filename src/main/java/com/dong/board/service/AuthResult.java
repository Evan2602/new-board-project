package com.dong.board.service;

/**
 * 인증 서비스 응답 결과 (회원가입/로그인 공통)
 */
public record AuthResult(String accessToken, String username) {}
