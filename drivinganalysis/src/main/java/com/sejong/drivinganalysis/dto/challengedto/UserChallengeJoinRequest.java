package com.sejong.drivinganalysis.dto.challengedto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저가 공통 챌린지에 참여할 때 사용하는 DTO.
 * → 서버가 userId + challengeId 기반으로 UserChallenge 생성
 */

@Getter
@NoArgsConstructor
public class UserChallengeJoinRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long challengeId;
}
