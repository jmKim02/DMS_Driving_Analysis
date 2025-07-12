package com.sejong.drivinganalysis.dto;

import com.sejong.drivinganalysis.entity.enums.RankingType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingListResponseDto {
    private RankingType rankingType;   //  DAILY, WEEKLY, MONTHLY
    private String period;             // "2025-04-29"(daily), "2025-W18"(weekly), "2025-04"(monthly)
    private List<RankingSummaryDto> rankings;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
    private Integer myRankPosition;
    private Double myScore;
}
