package com.sejong.drivinganalysis.controller;

import com.sejong.drivinganalysis.dto.ai.AnalysisResponse;
import com.sejong.drivinganalysis.dto.common.ApiResponse;
import com.sejong.drivinganalysis.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class AnalysisCallbackController {

    private final VideoService videoService;

    @PostMapping("/videos/{videoId}/analysis-result")
    public ResponseEntity<ApiResponse<String>> receiveAnalysisResult(
            @PathVariable Long videoId,
            @RequestBody AnalysisResponse analysisResult) {

        log.info("AI 서버로부터 분석 결과 수신: videoId={}, drowsinessDetected={}, " +
                        "drowsinessCount={}, phoneUsageCount={}, smokingCount={}",
                videoId, analysisResult.isDrowsinessDetected(),
                analysisResult.getDrowsinessCount(),
                analysisResult.getPhoneUsageCount(),
                analysisResult.getSmokingCount());

        videoService.processAnalysisResult(videoId, analysisResult);

        return ResponseEntity.ok(ApiResponse.success("Analysis result received"));
    }
}