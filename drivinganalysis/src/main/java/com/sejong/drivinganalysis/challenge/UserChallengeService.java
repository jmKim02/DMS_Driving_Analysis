package com.sejong.drivinganalysis.challenge;

import com.sejong.drivinganalysis.challenge.dto.UserChallengeCreateRequest;
import com.sejong.drivinganalysis.challenge.dto.UserChallengeResponse;
import com.sejong.drivinganalysis.challenge.dto.UserChallengeJoinRequest;
import com.sejong.drivinganalysis.entity.User;
import com.sejong.drivinganalysis.entity.UserChallenge;

import java.util.List;

public interface UserChallengeService {


    /**
     * 공통 챌린지 또는 개인화 챌린지 생성/참여 처리
     */
    UserChallenge createUserChallenge(UserChallengeCreateRequest req);

    /**
     * 사용자 챌린지 전체 조회
     */
    List<UserChallengeResponse> getUserChallenges(Long userId);

    /**
     * 개별 챌린지를 ID로 직접 업데이트하는 기능
     */
    UserChallenge updateProgress(Long userChallengeId, Long newValue);

    UserChallenge joinCommonChallenge(UserChallengeJoinRequest request);

    UserChallenge createCustomChallenge(UserChallengeCreateRequest request);


    /**
     * 메트릭 기준으로 진행 중인 챌린지 업데이트
     */
    void updateProgressByMetric(Long userId, String targetMetric, Long value);

    /**
     * 기한이 지난 챌린지 평가 및 보상 지급 처리
     */
    void evaluateChallenges();


    /**
     * 매주 주어진 사용자에 대해 개인화된 챌린지를 자동 생성
     * (예: 주간 평균 점수 기반 +10점 목표)
     */
    void createWeeklyPersonalChallengesForUser(User user);

}
