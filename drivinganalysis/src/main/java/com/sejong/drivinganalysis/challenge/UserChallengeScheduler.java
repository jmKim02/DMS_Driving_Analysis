package com.sejong.drivinganalysis.challenge;

import com.sejong.drivinganalysis.entity.User;
import com.sejong.drivinganalysis.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserChallengeScheduler {

    private final UserRepository userRepository;
    private final UserChallengeService userChallengeService;

    /**
     * 매주 월요일 00:00 개인화 챌린지 자동 생성
     */
    @Scheduled(cron = "0 0 0 * * MON")
    public void weeklyCreatePersonalChallenges() {
        // 모든 유저를 조회해서, 각 유저마다 주간 개인화 챌린지를 만듭니다.
        for (User user : userRepository.findAll()) {
            userChallengeService.createWeeklyPersonalChallengesForUser(user);
        }
    }

        /**
         * 매일 01:00 만료된 챌린지 평가 및 보상 처리
         */
        @Scheduled(cron = "0 0 1 * * *")
        public void dailyEvaluateChallenges() {
            userChallengeService.evaluateChallenges();
        }
}
