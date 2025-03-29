package com.sejong.drivinganalysis.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {
    private String status;
    private String message;
    private Long videoId;
    private boolean drowsinessDetected;
    private int drowsinessCount;
    private int phoneUsageCount;
    private int smokingCount;
    private boolean analysisCompleted;
    private LocalDateTime completedAt;
    private String errorMessage;
}