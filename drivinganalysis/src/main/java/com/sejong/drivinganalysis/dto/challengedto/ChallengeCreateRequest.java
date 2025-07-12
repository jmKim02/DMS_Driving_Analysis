package com.sejong.drivinganalysis.dto.challengedto;

import com.sejong.drivinganalysis.entity.enums.ChallengeCategory;
import com.sejong.drivinganalysis.entity.enums.ChallengeType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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

    @NotBlank
    private String comparator; // 예: ">=", "<="

    @NotNull
    private ChallengeType challengeType;

    @NotNull
    private ChallengeCategory category;

    private String rewardInfo;

    @NotNull @FutureOrPresent
    private LocalDate startDate;

    @NotNull @FutureOrPresent
    private LocalDate endDate;
}
