package com.dong.board.admin;

import com.dong.board.domain.user.User;
import com.dong.board.infrastructure.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 기본 관리자 계정을 자동 생성하는 초기화 컴포넌트
 * - 이미 관리자 계정이 존재하면 건너뜀 (중복 생성 방지)
 * - 운영 환경에서는 환경 변수로 admin.default-id, admin.default-password 오버라이드 권장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // application.yaml의 admin.default-id 값 (기본값: "admin")
    @Value("${admin.default-id:admin}")
    private String adminId;

    // application.yaml의 admin.default-password 값 (기본값: "Admin1234!")
    @Value("${admin.default-password:Admin1234!}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        // 이미 해당 ID로 등록된 계정이 있으면 중복 생성하지 않음
        if (userRepository.existsByUserId(adminId)) {
            log.info("[AdminInitializer] 관리자 계정이 이미 존재합니다: {}", adminId);
            return;
        }

        // 비밀번호를 BCrypt로 해시 후 관리자 계정 생성
        String encodedPassword = passwordEncoder.encode(adminPassword);
        User admin = User.createAdmin(adminId, "관리자", encodedPassword);
        userRepository.save(admin);

        log.info("[AdminInitializer] 기본 관리자 계정 생성 완료: {}", adminId);
    }
}
