package com.sejong.drivinganalysis.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingSummaryDto {
    private Integer rankPosition;
    private Long userId;
    private String username;
    private Double averageScore;  // 평균 점수를 소수로 저장
}
