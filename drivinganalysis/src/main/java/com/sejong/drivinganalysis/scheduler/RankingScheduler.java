package com.sejong.drivinganalysis.scheduler;

import com.sejong.drivinganalysis.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScheduler {

    private final RankingService rankingService;

    @Scheduled(cron = "0 0 0 * * *")
//    @Scheduled(cron = "5 * * * * *")
    public void updateMonthlyRankingAutomatically() {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();

        log.info("[스케줄러] 월간 랭킹 자동 업데이트 시작: {}년 {}월", year, month);
        rankingService.calculateAndSaveMonthlyRanking(year, month);
        log.info("[스케줄러] 월간 랭킹 자동 업데이트 완료");
    }
}
