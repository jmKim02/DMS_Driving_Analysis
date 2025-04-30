package com.sejong.drivinganalysis.challenge;

import com.sejong.drivinganalysis.entity.UserChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserChallengeRepository extends JpaRepository<UserChallenge, Long> {

    /**
     * 특정 사용자가 참여한 모든 챌린지 조회
     */
    List<UserChallenge> findByUserUserId(Long userId);

    /**
     * 이미 참여 중인지 확인 (중복 참여 방지)
     */
    boolean existsByUserUserIdAndChallengeChallengeId(Long userId, Long challengeId);
}
