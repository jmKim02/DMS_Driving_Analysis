package com.sejong.drivinganalysis.dto.challengedto;

import com.sejong.drivinganalysis.entity.Challenge;
import com.sejong.drivinganalysis.entity.enums.ChallengeCategory;
import com.sejong.drivinganalysis.entity.enums.ChallengeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

// 클라이언트에 반환할 챌린지 정보 DTO
@Getter
@Builder
@AllArgsConstructor
public class ChallengeResponse {
    private Long challengeId;
    private String title;
    private String description;
    private Long targetValue;
    private String targetMetric;
    private ChallengeType challengeType;
    private ChallengeCategory category;
    private String rewardInfo;
    private LocalDate startDate;
    private LocalDate endDate;

    public static ChallengeResponse fromEntity(Challenge c) {
        return ChallengeResponse.builder()
                .challengeId(c.getChallengeId())
                .title(c.getTitle())
                .description(c.getDescription())
                .targetValue(c.getTargetValue())
                .targetMetric(c.getTargetMetric())
                .challengeType(c.getChallengeType())
                .category(c.getCategory())
                .rewardInfo(c.getRewardInfo())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .build();
    }
}
