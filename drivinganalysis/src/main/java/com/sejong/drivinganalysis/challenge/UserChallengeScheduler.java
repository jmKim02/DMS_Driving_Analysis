package com.sejong.drivinganalysis.challenge;

import com.sejong.drivinganalysis.entity.User;
import com.sejong.drivinganalysis.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserChallengeScheduler {

    private final UserRepository userRepository;
    private final UserChallengeService userChallengeService;

    /**
     * 매주 월요일 00:00 개인화 챌린지 자동 생성
     */
    @Scheduled(cron = "0 0 0 * * MON")
    //@Scheduled(cron = "*/15 * * * * *")
    public void weeklyCreatePersonalChallenges() {
        // 모든 유저를 조회해서, 각 유저마다 주간 개인화 챌린지를 만듭니다.

        log.info("[스케줄러] 주간 개인화 챌린지 자동 생성 시작");

        for (User user : userRepository.findAll()) {
            userChallengeService.createWeeklyPersonalChallengesForUser(user);
        }

        log.info("[스케줄러] 주간 개인화 챌린지 자동 생성 완료");
    }

    /**
     * 매일 01:00 만료된 챌린지 평가 및 보상 처리
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void dailyEvaluateChallenges() {
        log.info("[스케줄러] 만료된 챌린지 평가 및 보상 처리 시작");

        userChallengeService.evaluateChallenges();

        log.info("[스케줄러] 만료된 챌린지 평가 및 보상 처리 완료");
    }
}
