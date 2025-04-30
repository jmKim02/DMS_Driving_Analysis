package com.sejong.drivinganalysis.challenge.dto;
import com.sejong.drivinganalysis.entity.enums.ChallengeCategory;
import com.sejong.drivinganalysis.entity.enums.ChallengeType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
// 관리자용 챌린지 생성 요청 DTO
@Getter
@NoArgsConstructor
public class ChallengeCreateRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Long targetValue;

    @NotBlank
    private String targetMetric;

    @NotNull
    private ChallengeType challengeType;

    private String rewardInfo;

    @NotNull
    private ChallengeCategory category;

    @NotNull @FutureOrPresent
    private LocalDate startDate;

    @NotNull @FutureOrPresent
    private LocalDate endDate;
}