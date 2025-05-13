package com.sejong.drivinganalysis.configuration;

import com.sejong.drivinganalysis.challenge.UserChallengeService;
import com.sejong.drivinganalysis.entity.*;
import com.sejong.drivinganalysis.entity.enums.AnalysisStatus;
import com.sejong.drivinganalysis.entity.enums.VideoStatus;
import com.sejong.drivinganalysis.repository.AnalysisResultRepository;
import com.sejong.drivinganalysis.repository.DrivingVideoRepository;
import com.sejong.drivinganalysis.repository.FeedbackRepository;
import com.sejong.drivinganalysis.repository.UserRepository;
import com.sejong.drivinganalysis.service.FeedbackService;
import com.sejong.drivinganalysis.service.UserScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
    private final FeedbackRepository feedbackRepository;
    private final FeedbackService feedbackService;
    private final UserScoreService userScoreService;

    private final Random random = new Random(42); // 재현 가능한 난수 생성을 위한 시드값 설정

    @Autowired
    private UserChallengeService userChallengeService; // 추가

    @Override
    @Transactional
    public void run(String... args) {
        // 기존 데모 사용자가 있는지 확인
        if (userRepository.count() == 0) {
            createDemoUsers();
        }

        // 기존 데모 데이터가 있는지 확인
        if (drivingVideoRepository.count() == 0) {
            createDemoData();
        }

//        insertMayScores(); // 5월 5일 ~ 9일 사용자 점수 강제 삽입
//
//        // ✅ 사용자별 챌린지 생성 트리거
//        List<User> users = userRepository.findAll();
//        for (User user : users) {
//            userChallengeService.createWeeklyPersonalChallengesForUser(user);
//        }
    }

    private void createDemoUsers() {
        log.info("Creating demo users for testing");

        createUser("driver1", "password1", "driver1@example.com");
        createUser("driver2", "password2", "driver2@example.com");
        createUser("driver3", "password3", "driver3@example.com");
        createUser("driver4", "password4", "driver4@example.com");
        createUser("driver5", "password5", "driver5@example.com");

        log.info("Demo users created successfully");
    }

    private void createDemoData() {
        log.info("Creating demo driving data for testing");

        User user1= userRepository.findByUsername("driver1").orElseThrow();
        User user2 = userRepository.findByUsername("driver2").orElseThrow();
        User user3 = userRepository.findByUsername("driver3").orElseThrow();
        User user4 = userRepository.findByUsername("driver4").orElseThrow();
        User user5 = userRepository.findByUsername("driver5").orElseThrow();

        // 3월부터 5월까지 (3개월) 데이터 생성
        for (int month = 3; month <= 5; month++) {
            createMonthlyData(user1, month);
        }

        createMonthlyData(user2, 5);
        createMonthlyData(user3, 5);
        createMonthlyData(user4, 5);
        createMonthlyData(user5, 5);

        log.info("Demo driving data created successfully");
    }

    /**
     * 특정 월의 데모 데이터 생성
     */
    private void createMonthlyData(User user, int month) {
        log.info("Creating demo data for month {}", month);

        int year = 2025;
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);

        // 해당 월의 모든 주의 시작일(월요일) 계산
        List<LocalDate> mondaysInMonth = new ArrayList<>();
        LocalDate currentMonday = firstDayOfMonth.with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));

        while (currentMonday.getMonth() == firstDayOfMonth.getMonth()) {
            mondaysInMonth.add(currentMonday);
            currentMonday = currentMonday.plusWeeks(1);
        }

        // 각 주에 2일씩 데이터 생성
        for (LocalDate monday : mondaysInMonth) {
            // 첫 번째 날 (월요일)
            createDailyData(user, monday);

            // 두 번째 날 (목요일이나 금요일)
            LocalDate secondDay = monday.plusDays(random.nextInt(2) + 3); // 목요일(+3) 또는 금요일(+4)
            if (secondDay.getMonth() == firstDayOfMonth.getMonth()) {
                createDailyData(user, secondDay);
            }
        }
    }

    /**
     * 특정 날짜의 데모 데이터 생성 (2개의 세션)
     */
    private void createDailyData(User user, LocalDate date) {
        log.info("Creating demo data for date {}", date);

        // 첫 번째 세션 (오전 8시 ~ 12시 사이)
        LocalDateTime morningSession = date.atTime(8 + random.nextInt(5), random.nextInt(60));
        createSession(user, morningSession);

        // 두 번째 세션 (저녁 17시 ~ 22시 사이)
        LocalDateTime eveningSession = date.atTime(17 + random.nextInt(6), random.nextInt(60));
        createSession(user, eveningSession);
    }

    /**
     * 세션 데이터와 피드백 생성
     */
    private void createSession(User user, LocalDateTime sessionTime) {
        // 드라이빙 패턴 데이터 생성
        int durationMinutes = 20 + random.nextInt(41); // 20-60분 사이의 주행 시간

        // 위험 행동 빈도 (합리적인 범위 내에서 생성)
        int drowsinessCount = generateRiskBehaviorCount(5); // 0-4회
        int phoneUsageCount = generateRiskBehaviorCount(4); // 0-3회
        int smokingCount = generateRiskBehaviorCount(3);    // 0-2회

        // 1. 영상 데이터 생성
        DrivingVideo video = createDrivingVideo(user, sessionTime, durationMinutes);
        drivingVideoRepository.save(video);

        // 2. 분석 결과 생성 - 기존 createAnalysisResult 메서드 활용
        AnalysisResult analysisResult = AnalysisResult.createAnalysisResult(
                video, user, drowsinessCount, phoneUsageCount, smokingCount, durationMinutes
        );

        // 분석 시간 설정
        try {
            Field analyzedAtField = AnalysisResult.class.getDeclaredField("analyzedAt");
            analyzedAtField.setAccessible(true);
            analyzedAtField.set(analysisResult, sessionTime.plusMinutes(5));
        } catch (Exception e) {
            log.warn("Could not set analyzed_at field via reflection: {}", e.getMessage());
        }

        analysisResultRepository.save(analysisResult);

        // 3. UserScore 생성/업데이트 - 기존 서비스 활용
        userScoreService.updateUserScoreWithCustomDate(user.getUserId(), analysisResult, sessionTime.toLocalDate());

        // 4. 피드백 생성 - 기존 서비스 활용하되 날짜 수정
        Feedback feedback = feedbackService.generateDrivingFeedback(analysisResult);

        // 피드백의 생성 시간 설정 (세션 시간 + 6분 후로 설정)
        try {
            Field generatedAtField = Feedback.class.getDeclaredField("generatedAt");
            generatedAtField.setAccessible(true);
            generatedAtField.set(feedback, sessionTime.plusMinutes(6));
            feedbackRepository.save(feedback);
        } catch (Exception e) {
            log.warn("Could not set generated_at field via reflection: {}", e.getMessage());
        }
    }

    /**
     * 영상 데이터 생성
     */
    private DrivingVideo createDrivingVideo(User user, LocalDateTime sessionTime, int durationMinutes) {
        DrivingVideo video = DrivingVideo.createVideo(user, "demo_video_" + sessionTime.toString(), durationMinutes);
        video.setStatus(VideoStatus.ANALYZED);
        video.setProcessedAt();

        try {
            Field uploadedAtField = DrivingVideo.class.getDeclaredField("uploadedAt");
            uploadedAtField.setAccessible(true);
            uploadedAtField.set(video, sessionTime);

            Field processedAtField = DrivingVideo.class.getDeclaredField("processedAt");
            processedAtField.setAccessible(true);
            processedAtField.set(video, sessionTime.plusMinutes(5));
        } catch (Exception e) {
            log.warn("Could not set fields via reflection: {}", e.getMessage());
        }

        return video;
    }

    /**
     * 위험 행동 카운트 생성
     */
    private int generateRiskBehaviorCount(int maxValue) {
        // 0이 나올 확률이 높고, 높은 값이 나올 확률은 낮게 생성
        double randomValue = random.nextDouble();
        if (randomValue < 0.5) {
            return 0; // 50% 확률로 0 반환/**/
        } else if (randomValue < 0.75) {
            return 1; // 25% 확률로 1 반환
        } else if (randomValue < 0.9) {
            return 2; // 15% 확률로 2 반환
        } else {
            return random.nextInt(maxValue - 2) + 3; // 10% 확률로 3 이상 반환
        }
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

//    // ✅ 5월 5일부터 9일까지 driver1 ~ driver5 점수 및 위험행동 데모 데이터 삽입
//    private void insertMayScores() {
//        Map<String, int[]> smokingMap = Map.of(
//                "driver1", new int[]{0, 0, 0, 0, 6},
//                "driver2", new int[]{0, 0, 0, 1, 1},
//                "driver3", new int[]{0, 0, 0, 0, 1},
//                "driver4", new int[]{90, 0, 0, 0, 0},
//                "driver5", new int[]{0, 0, 0, 0, 20}
//        );
//        Map<String, int[]> phoneMap = Map.of(
//                "driver1", new int[]{0, 0, 1, 0, 1},
//                "driver2", new int[]{10, 12, 9, 11, 13},
//                "driver3", new int[]{0, 1, 0, 1, 0},
//                "driver4", new int[]{0, 0, 0, 0, 0},
//                "driver5", new int[]{21, 3, 1, 0, 0}  // 일단 3개 중에 제일 합이 높은 애로만 챌린지 생성
//        );
//        Map<String, int[]> drowsyMap = Map.of(
//                "driver1", new int[]{0, 1, 0, 0, 0},
//                "driver2", new int[]{0, 0, 1, 0, 0},
//                "driver3", new int[]{11, 13, 14, 12, 15},
//                "driver4", new int[]{0, 0, 0, 0, 0},
//                "driver5", new int[]{1, 0, 0, 2, 0}  // 합이 150 이상이면 반영 안 함
//        );
//
//        // driver5 추가!
//        for (String driverName : List.of("driver1", "driver2", "driver3", "driver4", "driver5")) {
//            User user = userRepository.findByUsername(driverName).orElseThrow();
//            for (int i = 0; i < 5; i++) {
//                LocalDate date = LocalDate.of(2025, 5, 5 + i);
//                LocalDateTime sessionTime = date.atTime(9, 0);
//
//                int score   = 80 + i;
//                int smoking = smokingMap.get(driverName)[i];
//                int phone   = phoneMap.get(driverName)[i];
//                int drowsy  = drowsyMap.get(driverName)[i];
//                int duration = 3600;
//
//                // DrivingVideo 생성/저장
//                DrivingVideo video = DrivingVideo.createVideo(user, "may_demo_video_" + driverName + "_" + i, duration);
//                video.setStatus(VideoStatus.ANALYZED);
//                video.setProcessedAt();
//                // (필드 조작 생략)
//                drivingVideoRepository.save(video);
//
//                // AnalysisResult 생성/저장
//                AnalysisResult result = new AnalysisResult();
//                result.setUser(user);
//                result.setVideo(video);
//                result.setDrivingScore(score);
//                result.setSmokingCount(smoking);
//                result.setPhoneUsageCount(phone);
//                result.setDrowsinessCount(drowsy);
//                result.setStatus(AnalysisStatus.COMPLETED);
//                result.setTotalDuration(duration);
//                // analyzedAt 설정
//                try {
//                    Field f = AnalysisResult.class.getDeclaredField("analyzedAt");
//                    f.setAccessible(true);
//                    f.set(result, sessionTime.plusMinutes(5));
//                } catch (Exception ignored) {}
//                analysisResultRepository.save(result);
//
//                // 점수 업데이트
//                userScoreService.updateUserScoreWithCustomDate(user.getUserId(), result, date);
//            }
//        }
//
//        log.info("✅ 5월 사용자 점수 및 위험행동 데모 데이터 삽입 완료");
//    }

}