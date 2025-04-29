package com.sejong.drivinganalysis.scheduler;

import com.sejong.drivinganalysis.entity.AnalysisResult;
import com.sejong.drivinganalysis.entity.User;
import com.sejong.drivinganalysis.repository.AnalysisResultRepository;
import com.sejong.drivinganalysis.repository.UserRepository;
import com.sejong.drivinganalysis.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 정기적인 피드백 생성을 담당하는 스케줄러
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeedbackScheduler {

    private final FeedbackService feedbackService;
    private final UserRepository userRepository;
    private final AnalysisResultRepository analysisResultRepository;

    /**
     * 매주 월요일 오전 8시에 실행되는 주간 피드백 생성 작업
     * 지난 주 운전 데이터가 있는 사용자를 대상으로 피드백 생성
     */
    @Scheduled(cron = "0 0 8 * * MON") // 매주 월요일 오전 8시
    public void generateWeeklyFeedbacks() {
        log.info("Starting weekly feedback generation job");

        try {
            // 지난 주 기간 계산 (지난 월요일부터 일요일까지)
            LocalDate today = LocalDate.now();
            LocalDate previousMonday = today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate previousSunday = previousMonday.plusDays(6);

            log.info("Generating weekly feedbacks for period: {} to {}", previousMonday, previousSunday);

            // 1. 분석 결과가 있는 사용자 ID만 효율적으로 조회
            // 중복 제거된 사용자 ID만 가져오는 쿼리로 최적화 (distinct 사용)
            List<Long> userIds = analysisResultRepository.findDistinctUserIdsByAnalyzedAtBetween(
                    previousMonday.atStartOfDay(),
                    previousSunday.atTime(LocalTime.MAX)
            );

            if (userIds.isEmpty()) {
                log.info("No users with analysis results found for the previous week");
                return;
            }

            log.info("Found {} users with driving data in the previous week", userIds.size());

            // 2. 배치 처리 (한 번에 너무 많은 사용자를 처리하지 않도록)
            int batchSize = 50; // 적절한 배치 크기 설정
            for (int i = 0; i < userIds.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, userIds.size());
                List<Long> batchUserIds = userIds.subList(i, endIndex);

                // 배치 단위로 피드백 생성 (하나의 트랜잭션으로 처리)
                processBatchWithTransaction(batchUserIds);

                log.info("Processed batch {}/{} of weekly feedback generation",
                        (i / batchSize) + 1, (userIds.size() + batchSize - 1) / batchSize);
            }

            log.info("Completed weekly feedback generation job");
        } catch (Exception e) {
            log.error("Critical error in weekly feedback generation job", e);
        }
    }

    /**
     * 배치 단위로 트랜잭션 처리
     */
    @Transactional
    public void processBatchWithTransaction(List<Long> userIds) {
        feedbackService.generateWeeklyFeedbacksBatch(userIds);
    }
}