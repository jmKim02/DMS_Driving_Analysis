package com.sejong.drivinganalysis.controller;

import com.sejong.drivinganalysis.dto.ScoreDto;
import com.sejong.drivinganalysis.dto.common.ApiResponse;
import com.sejong.drivinganalysis.service.UserScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserScoreController {

    private final UserScoreService userScoreService;

    @GetMapping("/{userId}/scores")
    public ResponseEntity<ApiResponse<ScoreDto.ScoreResponse>> getUserScores(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "daily") String period,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer week,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Fetching user scores: userId={}, period={}, year={}, month={}, week={}, startDate={}, endDate={}",
                userId, period, year, month, week, startDate, endDate);

        ScoreDto.ScoreResponse scores = userScoreService.getUserScores(
                userId, period, year, month, week, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(scores));
    }

    @GetMapping("/{userId}/scores/statistics")
    public ResponseEntity<ApiResponse<ScoreDto.ScoreStatistics>> getScoreStatistics(
            @PathVariable Long userId) {

        log.info("Fetching score statistics for userId: {}", userId);

        ScoreDto.ScoreStatistics statistics = userScoreService.getScoreStatistics(userId);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
}