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

        }

        private void createDemoUsers()
        {
            log.info("Creating demo users for testing");
            for (int i = 1; i <= 20; i++) {
                createUser("driver" + i, "password" + i, "driver" + i + "@example.com");
            }
            log.info("Demo users created successfully");
        }

        /*private void createDemoData() {
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
        }*/
        private void createDemoData() {
            log.info("Creating demo driving data for testing");
            for (int i = 1; i <= 20; i++) {
                User user = userRepository.findByUsername("driver" + i).orElseThrow();
                if (i <= 3) {
                    for (int month = 3; month <= 5; month++) {
                        createMonthlyData(user, month);
                    }
                } else {
                    createMonthlyData(user, 5);
                }
            }
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
            int drowsinessCount = generateRiskBehaviorCount(15); // 0-14회
            int phoneUsageCount = generateRiskBehaviorCount(13); // 0-12회
            int smokingCount = generateRiskBehaviorCount(12);    // 0-10회

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

    }