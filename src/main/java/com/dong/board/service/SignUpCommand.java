package com.dong.board.service;

/**
 * 회원가입 서비스 요청 커맨드
 */
public record SignUpCommand(String username, String password) {}
