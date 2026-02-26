package com.dong.board.service;

/**
 * 로그인 서비스 요청 커맨드
 */
public record LoginCommand(String username, String password) {}
