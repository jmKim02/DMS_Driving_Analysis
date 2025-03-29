package com.sejong.drivinganalysis.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequest {
    private Long videoId;
    private String videoUrl;
    private Long userId;
}