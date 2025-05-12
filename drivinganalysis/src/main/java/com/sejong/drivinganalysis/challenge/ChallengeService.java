package com.sejong.drivinganalysis.challenge;

import com.sejong.drivinganalysis.challenge.dto.ChallengeCreateRequest;
import com.sejong.drivinganalysis.entity.Challenge;

import java.time.LocalDate;
import java.util.List;

/**
 * 공통 챌린지(CHALLENGE) 관리용 Service 인터페이스
 */
public interface ChallengeService {

    /**
     * 새로운 공통 챌린지를 생성합니다.
     * @param request 챌린지 생성 요청 DTO
     * @return 생성된 Challenge 엔티티
     */
    Challenge createChallenge(ChallengeCreateRequest request);

    /**
     * 단일 챌린지를 조회합니다.
     * @param challengeId 챌린지 ID
     * @return 조회된 Challenge
     */
    Challenge getChallenge(Long challengeId);

    /**
     * 모든 챌린지를 조회합니다.
     * @return 모든 Challenge 리스트
     */
    List<Challenge> getAllChallenges();

    /**
     * 지정한 날짜에 활성(진행중)인 챌린지 목록을 조회합니다.
     * @param date 조회 기준 날짜
     * @return 지정일자 진행중인 챌린지
     */
    List<Challenge> getActiveChallenges(LocalDate date);

    /**
     * 챌린지를 삭제합니다.
     * @param challengeId 삭제할 챌린지 ID
     */
    void deleteChallenge(Long challengeId);
}

