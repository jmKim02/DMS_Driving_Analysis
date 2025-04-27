//package com.sejong.drivinganalysis.scheduler.test;
//
//import com.sejong.drivinganalysis.entity.User;
//import com.sejong.drivinganalysis.entity.UserScore;
//import com.sejong.drivinganalysis.repository.UserRepository;
//import com.sejong.drivinganalysis.repository.UserScoreRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDate;
//import java.util.List;
//import java.util.Random;
//
//@Component
//@RequiredArgsConstructor
//public class ScoreTestScheduler {
//
//    private final UserRepository userRepository;
//    private final UserScoreRepository userScoreRepository;
//    private final Random random = new Random();
//
//    /**
//     * 매 분 0초마다 모든 사용자에게 랜덤 점수를 추가 (테스트용).
//     *
//     */
//    @Scheduled(cron = "0 * * * * *") // 매 분 정각에 실행
//    public void addScoreForTesting() {
//        LocalDate today = LocalDate.now();
//        String targetUsername = "driver1";
//
//        userRepository.findByUsername(targetUsername).ifPresent(user -> {
//            int score = 99; //
//            UserScore userScore = UserScore.createUserScore(user, score, today);
//            userScoreRepository.save(userScore);
//
//            System.out.printf(">>> [스케줄] %s의 점수 %d점 저장 완료%n", user.getUsername(), score);
//        });
//    }
//}
