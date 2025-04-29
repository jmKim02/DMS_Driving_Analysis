package com.sejong.drivinganalysis.dto;

import com.sejong.drivinganalysis.entity.enums.FeedbackType;
import com.sejong.drivinganalysis.entity.enums.SeverityLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class FeedbackDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackResponse {
        private Long feedbackId;
        private Long userId;
        private String feedbackType;
        private String content;
        private String severityLevel;
        private LocalDateTime generatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackListResponse {
        private List<FeedbackResponse> content;
        private long totalElements;
        private int totalPages;
        private int page;
        private int size;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackDetailResponse {
        private Long feedbackId;
        private Long userId;
        private String feedbackType;
        private String content;
        private String severityLevel;
        private LocalDateTime generatedAt;
        private Long videoId;
        private Integer drivingScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyFeedbackResponse {
        private Long userId;
        private String mostFrequentRiskBehavior;
        private Integer count;
        private Map<String, Integer> riskBehaviorCounts;
        private String riskTimePattern;
        private LocalDateTime generatedAt;
    }
}