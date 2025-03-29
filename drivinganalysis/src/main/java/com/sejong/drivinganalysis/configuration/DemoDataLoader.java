package com.sejong.drivinganalysis.configuration;

import com.sejong.drivinganalysis.entity.User;
import com.sejong.drivinganalysis.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 데모 데이터를 로드하는 컴포넌트
 * 단순, 테스트 및 데모 용도 --> 실제 서비스에서는 필요 없다
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DemoDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 기존 데모 사용자가 있는지 확인
        if (userRepository.count() == 0) {
            createDemoUsers();
        }
    }

    private void createDemoUsers() {
        log.info("Creating demo users for testing");

        // 데모 계정 생성
        createUser("driver1", "password1", "driver1@example.com");
        createUser("driver2", "password2", "driver2@example.com");
        createUser("driver3", "password3", "driver3@example.com");
        createUser("driver4", "password4", "driver4@example.com");
        createUser("driver5", "password5", "driver5@example.com");

        log.info("Demo users created successfully");
    }

    /**
     * 사용자 계정을 생성하고 저장하는 메서드
     *
     * @param username 사용자 이름(아이디)
     * @param password 비밀번호 (저장 시 암호화)
     * @param email    이메일 주소
     */
    private void createUser(String username, String password, String email) {
        User user = User.createUser(
                username,
                passwordEncoder.encode(password),
                email
        );
        userRepository.save(user);
        log.info("Created demo user: {}", username);
    }
}
