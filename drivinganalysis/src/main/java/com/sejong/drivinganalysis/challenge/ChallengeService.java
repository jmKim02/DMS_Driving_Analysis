package com.sejong.drivinganalysis.challenge;

import com.sejong.drivinganalysis.challenge.dto.ChallengeCreateRequest;
import com.sejong.drivinganalysis.challenge.dto.ChallengeResponse;
import com.sejong.drivinganalysis.challenge.dto.UserChallengeResponse;

import java.util.List;

public interface ChallengeService {

    /**
     * 관리자용 챌린지 생성
     * @param request 생성 정보
     * @return 생성된 챌린지 ID
     */
    Long createChallenge(ChallengeCreateRequest request);

    /**
     * 전체 챌린지 조회
     */
    List<ChallengeResponse> getAllChallenges();

    /**
     * 키워드 검색 챌린지 조회
     */
    List<ChallengeResponse> searchChallengesByKeyword(String keyword);

    /**
     * 단건 챌린지 조회
     */
    ChallengeResponse getChallengeById(Long challengeId);

    /**
     * 챌린지 참여 처리
     * @return true: 참여 성공 (처음 참여), false: 이미 참여(중복)
     */
    boolean joinChallenge(Long challengeId, Long userId);

    /**
     * 내가 참여 중인 챌린지 조회
     */
    List<UserChallengeResponse> getMyChallenges(Long userId);
}
