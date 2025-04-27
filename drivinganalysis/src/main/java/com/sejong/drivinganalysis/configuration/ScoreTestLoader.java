package com.sejong.drivinganalysis.configuration;

import com.sejong.drivinganalysis.entity.User;
import com.sejong.drivinganalysis.entity.UserScore;
import com.sejong.drivinganalysis.repository.UserRepository;
import com.sejong.drivinganalysis.repository.UserScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class ScoreTestLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserScoreRepository userScoreRepository;

    @Override
    public void run(String... args) {
        LocalDate today = LocalDate.now();
        List<User> users = userRepository.findAll();

        for (User user : users) {
            System.out.println("== 유저 점수 생성: " + user.getUsername());

            for (int i = 0; i < 3; i++) {
                int score = 60 + new Random().nextInt(40);  // 60~99점
                UserScore userScore = UserScore.createUserScore(user, score, today);
                userScoreRepository.save(userScore);

                System.out.printf("  > %s [%s점] 저장 완료%n", user.getUsername(), score);
            }
        }
    }
}
