package com.sejong.drivinganalysis.challenge.dto;

import com.sejong.drivinganalysis.entity.UserChallenge;
import com.sejong.drivinganalysis.entity.enums.ChallengesStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class UserChallengeResponse {

    private Long userChallengeId;
    private Long challengeId;
    private String title;
    private Long currentValue;           // DB에 저장되는 값 (누적 방지)
    private Long displayValue;           // 사용자에게 보여줄 값
    private ChallengesStatus status;
    private Boolean rewardGiven;
    private String resultSummary;
    private LocalDateTime joinedAt;
    private LocalDateTime completedAt;
    private boolean isCustomChallenge;
    private Long targetValue;


    public static UserChallengeResponse fromEntity(UserChallenge uc, Long displayValue) {
        boolean isCustom = (uc.getChallenge() == null);
        return UserChallengeResponse.builder()
                .userChallengeId(uc.getUserChallengeId())
                .challengeId(!isCustom ? uc.getChallenge().getChallengeId() : null)
                .title(!isCustom ? uc.getChallenge().getTitle() : uc.getTitle())
                .currentValue(uc.getCurrentValue())
                .displayValue(displayValue) // 추가
                .status(uc.getStatus())
                .rewardGiven(uc.getRewardGiven())
                .resultSummary(uc.getResultSummary())
                .joinedAt(uc.getJoinedAt())
                .completedAt(uc.getCompletedAt())
                .isCustomChallenge(isCustom)
                .targetValue(uc.getTargetValue())
                .build();
    }
}
