package com.sejong.drivinganalysis.ranking.controller;

import com.sejong.drivinganalysis.ranking.dto.RankingListResponseDto;
import com.sejong.drivinganalysis.dto.common.ApiResponse;
import com.sejong.drivinganalysis.ranking.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    /**
     * 일간 랭킹 조회 API
     * 요청 예시: GET /rankings/daily?date=2025-04-29&page=0&size=50&myUserId=3
     */
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<RankingListResponseDto>> getDailyRanking(
            @RequestParam String date,  // yyyy-MM-dd 형식
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Long myUserId
    ) {
        RankingListResponseDto result = rankingService.getDailyRankingWithMyRank(date, page, size, myUserId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 주간 랭킹 조회 API
     * 요청 예시: GET /rankings/weekly?year=2025&week=18&page=0&size=50&myUserId=3
     */
    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<RankingListResponseDto>> getWeeklyRanking(
            @RequestParam int year,
            @RequestParam int week,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Long myUserId
    ) {
        RankingListResponseDto result = rankingService.getWeeklyRankingWithMyRank(year, week, page, size, myUserId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 월간 랭킹 조회 API
     * 요청 예시: GET /rankings/monthly?year=2025&month=4&page=0&size=50&myUserId=3
     */
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<RankingListResponseDto>> getMonthlyRanking(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Long myUserId
    ) {
        RankingListResponseDto result = rankingService.getMonthlyRankingWithMyRank(year, month, page, size, myUserId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 월간 랭킹 계산 API (수동 트리거용)
     */
    @PostMapping("/monthly/calculate")
    public ResponseEntity<ApiResponse<String>> calculateMonthlyRanking(
            @RequestParam int year,
            @RequestParam int month
    ) {
        rankingService.calculateAndSaveMonthlyRanking(year, month);
        return ResponseEntity.ok(ApiResponse.success("월간 랭킹 계산 완료"));
    }
}
