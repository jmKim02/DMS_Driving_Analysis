package com.sejong.drivinganalysis.configuration;

import com.sejong.drivinganalysis.entity.*;
import com.sejong.drivinganalysis.entity.enums.AnalysisStatus;
import com.sejong.drivinganalysis.entity.enums.VideoStatus;
import com.sejong.drivinganalysis.repository.AnalysisResultRepository;
import com.sejong.drivinganalysis.repository.DrivingVideoRepository;
import com.sejong.drivinganalysis.repository.UserRepository;
import com.sejong.drivinganalysis.repository.UserScoreRepository;
import com.sejong.drivinganalysis.service.UserScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private final DrivingVideoRepository drivingVideoRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final UserScoreService userScoreService;

    @Override
    @Transactional // 트랜잭션 추가
    public void run(String... args) {
        // 기존 데모 사용자가 있는지 확인
        if (userRepository.count() == 0) {
            createDemoUsers();
        }

        // 기존 데모 데이터가 있는지 확인
        if (drivingVideoRepository.count() == 0) {
            createDemoData();
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

    private void createDemoData() {
        log.info("Creating demo driving data for testing");

        // driver1에 대한 데모 데이터 생성
        User user = userRepository.findByUsername("driver1").orElseThrow();

        // 3월 26일 세션 2개
        LocalDate march26 = LocalDate.of(2025, 3, 26);
        createSession(user, march26.atTime(10, 0), 30, 75, 2, 1, 0);
        createSession(user, march26.atTime(15, 0), 45, 85, 1, 0, 1);

        // 3월 28일 세션 2개 (어제)
        LocalDate march28 = LocalDate.of(2025, 3, 28);
        createSession(user, march28.atTime(9, 0), 25, 65, 3, 2, 0);
        createSession(user, march28.atTime(17, 0), 40, 90, 0, 1, 0);

        // 4월 10일 세션 2개
        LocalDate april10 = LocalDate.of(2025, 4, 10);
        createSession(user, april10.atTime(10, 0), 30, 75, 2, 1, 0);
        createSession(user, april10.atTime(15, 0), 45, 85, 1, 0, 1);

        // 4월 23일 세션 2개
        LocalDate april23 = LocalDate.of(2025, 4, 23);
        createSession(user, april23.atTime(10, 0), 30, 75, 2, 1, 0);
        createSession(user, april23.atTime(15, 0), 45, 85, 1, 0, 1);

        // 4월 26일 세션 2개 (어제)
        LocalDate april26 = LocalDate.of(2025, 4, 26);
        createSession(user, april26.atTime(9, 0), 25, 65, 3, 2, 0);
        createSession(user, april26.atTime(17, 0), 40, 90, 0, 1, 0);

        log.info("Demo driving data created successfully");
    }

    private void createSession(
            User user,
            LocalDateTime sessionTime,
            int durationMinutes,
            int drivingScore,
            int drowsinessCount,
            int phoneUsageCount,
            int smokingCount) {

        // 영상 데이터 생성 - 연관관계 메서드 사용 대신 직접 설정
        DrivingVideo video = new DrivingVideo();
        video.setUser(user); // 연관관계를 직접 설정
        video.setFilePath("demo_video_" + sessionTime.toString());
        video.setDuration(durationMinutes);
        video.setStatus(VideoStatus.ANALYZED);
        video.setProcessedAt();

        // 업로드/처리 시간 설정 (분석 시간과 같게 설정)
        try {
            java.lang.reflect.Field uploadedAtField = DrivingVideo.class.getDeclaredField("uploadedAt");
            uploadedAtField.setAccessible(true);
            uploadedAtField.set(video, sessionTime);

            java.lang.reflect.Field processedAtField = DrivingVideo.class.getDeclaredField("processedAt");
            processedAtField.setAccessible(true);
            processedAtField.set(video, sessionTime.plusMinutes(5));
        } catch (Exception e) {
            log.warn("Could not set fields via reflection: {}", e.getMessage());
        }

        drivingVideoRepository.save(video);

        // 분석 결과 생성
        AnalysisResult analysisResult = new AnalysisResult();
        analysisResult.setVideo(video);
        analysisResult.setUser(user);
        analysisResult.setDrowsinessCount(drowsinessCount);
        analysisResult.setPhoneUsageCount(phoneUsageCount);
        analysisResult.setSmokingCount(smokingCount);
        analysisResult.setTotalDuration(durationMinutes);
        analysisResult.setDrivingScore(drivingScore);
        analysisResult.setAnalyzedAt(sessionTime);
        analysisResult.setStatus(AnalysisStatus.COMPLETED);

        analysisResultRepository.save(analysisResult);

        // UserScore 생성/업데이트
        userScoreService.updateUserScoreWithCustomDate(user.getUserId(), analysisResult, sessionTime.toLocalDate());
    }

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