package com.sejong.drivinganalysis.dto.challengedto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * [내부 전용] 서버가 사용자별 개인화 챌린지를 생성할 때 사용하는 DTO.
 * → 사용자가 직접 요청해서는 안 됨.
 */

@Getter
@NoArgsConstructor
public class UserChallengeCreateRequest {

    /** 요청을 하는 사용자 ID */
    @NotNull
    private Long userId;

    /** 공통 챌린지 기반 참여 시에만 세팅, 커스텀 챌린지는 null */
    private Long challengeId;

    /** 챌린지 제목 */
    @NotBlank
    private String title;

    /** 진행 측정 기준 (예: "drowsiness", "driving_score") */
    @NotBlank
    private String targetMetric;

    /** 목표 값 */
    @NotNull
    private Long targetValue;

    /** 비교 연산자 (">=", "<=" 등) */
    @NotBlank
    private String comparator;

    /** 보상 정보    */
    private String rewardInfo;

    /** 챌린지 시작일 */
    @NotNull
    @FutureOrPresent
    private LocalDate startDate;

    /** 챌린지 종료일 */
    @NotNull
    @FutureOrPresent
    private LocalDate endDate;
}
