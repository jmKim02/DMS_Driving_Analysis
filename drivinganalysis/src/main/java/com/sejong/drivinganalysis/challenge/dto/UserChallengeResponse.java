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
    private Long currentValue;
    private ChallengesStatus status;
    private Boolean rewardGiven;
    private String resultSummary;
    private LocalDateTime joinedAt;
    private LocalDateTime completedAt;

    public static UserChallengeResponse fromEntity(UserChallenge uc) {
        return UserChallengeResponse.builder()
                .userChallengeId(uc.getUserChallengeId())
                .challengeId(uc.getChallenge().getChallengeId())
                .title(uc.getChallenge().getTitle())
                .currentValue(uc.getCurrentValue())
                .status(uc.getStatus())
                .rewardGiven(uc.getRewardGiven())
                .resultSummary(uc.getResultSummary())
                .joinedAt(uc.getJoinedAt())
                .completedAt(uc.getCompletedAt())
                .build();
    }
}
