package com.sejong.drivinganalysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

public class ScoreDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreResponse {
        private Long userId;
        private List<ScoreData> scores;
        private Integer averageScore;

        // 아래 세 개는 위험행동 기반 챌린지 생성을 위한 필드 (nullable 가능)
        private Long smokingCount;
        private Long drowsinessCount;
        private Long phoneUsageCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreData {
        private Long scoreId;
        private LocalDate scoreDate;
        private Integer scoreValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreStatistics {
        private Long userId;
        private List<DayOfWeekStat> dayOfWeekStats;
        private List<MonthlyStat> monthlyTrend;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayOfWeekStat {
        private String dayOfWeek;
        private Integer averageScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyStat {
        private String month;
        private Integer year;
        private Integer averageScore;
    }
}