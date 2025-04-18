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
     * 월간 랭킹 조회 API
     * 요청 예시: GET /rankings/monthly?year=2025&month=4&page=0&size=50&myUserId=3
     * - 전체 상위 1~50위와 함께, (옵션으로) 현재 사용자의 순위 및 점수를 함께 반환
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
     * 요청 예시: POST /rankings/monthly/calculate?year=2025&month=4
     * - 해당 월의 모든 운전자 점수를 집계하여 랭킹을 재계산하고 업데이트
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
