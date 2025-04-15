package com.sejong.drivinganalysis.scheduler;

import com.sejong.drivinganalysis.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class RankingScheduler {

    private final RankingService rankingService;

    /**
     * 매일 자정(00:00)에 실행되어, 현재 월의 랭킹을 계산 및 저장
     *
     */
    @Scheduled(cron = "0 0 * * * *") //원래 0 0 0 * * *
    public void updateMonthlyRankingAutomatically() {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();
        rankingService.calculateAndSaveMonthlyRanking(year, month);
    }



}
