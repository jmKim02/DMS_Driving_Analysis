/*
package com.sejong.drivinganalysis.scheduler;

import com.sejong.drivinganalysis.entity.User;
import com.sejong.drivinganalysis.entity.UserScore;
import com.sejong.drivinganalysis.repository.UserRepository;
import com.sejong.drivinganalysis.repository.UserScoreRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScoreGeneratorScheduler {

    private final UserRepository userRepository;
    private final UserScoreRepository userScoreRepository;
    private final Random random = new Random();

    @Transactional
    @Scheduled(cron = "* * * * * *")
    public void generateTestScore() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.warn("[테스트 점수 생성] 유저가 없습니다.");
            return;
        }

        User user = users.get(random.nextInt(users.size()));
        LocalDate today = LocalDate.now();

        UserScore score = userScoreRepository
                .findByUserUserIdAndScoreDate(user.getUserId(), today)
                .orElse(UserScore.createUserScore(user, null, today));

        int randomScore = 50 + random.nextInt(51);
        score.setDailyScore(randomScore);
        score.setWeeklyScore(randomScore);
        score.setMonthlyScore(randomScore);

        userScoreRepository.save(score);

        log.info("[테스트 점수 생성] {}님에게 점수 {}점 부여 완료", user.getUsername(), randomScore);
    }
}
*/
