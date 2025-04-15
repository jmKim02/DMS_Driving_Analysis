package com.sejong.drivinganalysis.ranking.dto;

import com.sejong.drivinganalysis.entity.enums.RankingType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingListResponseDto {
    private RankingType rankingType;   // MONTHLY
    private String period;             // 예: "2025-04"
    private List<RankingSummaryDto> rankings;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
    private Integer myRankPosition;
    private Double myScore;
}
