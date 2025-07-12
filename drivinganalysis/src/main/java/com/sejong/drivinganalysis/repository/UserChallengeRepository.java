package com.sejong.drivinganalysis.repository;

import com.sejong.drivinganalysis.entity.UserChallenge;
import com.sejong.drivinganalysis.entity.enums.ChallengesStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserChallengeRepository extends JpaRepository<UserChallenge, Long> {

    /** 사용자의 모든 챌린지 조회 */
    List<UserChallenge> findByUser_UserId(Long userId);

    /** 챌린지 내역을 최신 시작일 기준으로 정렬 */
    List<UserChallenge> findByUser_UserIdOrderByStartDateDesc(Long userId);

    /** 공통 챌린지 중복 참여 여부 확인 */
    boolean existsByUser_UserIdAndChallenge_ChallengeIdAndStartDate(Long userId, Long challengeId, LocalDate startDate);

    /** 개인화 챌린지 중복 생성 여부 확인 */
    boolean existsByUser_UserIdAndTargetMetricAndStartDate(Long userId, String targetMetric, LocalDate startDate);

    /** 사용자의 특정 기간 내 챌린지 조회 */
    List<UserChallenge> findByUser_UserIdAndStartDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    /** 메트릭 및 상태 기준으로 진행 중인 챌린지 조회 */
    List<UserChallenge> findByUser_UserIdAndTargetMetricAndStatus(Long userId, String targetMetric, ChallengesStatus status);

    /** comparator가 null이 아닌 메트릭+상태 필터 챌린지 조회 */
    List<UserChallenge> findByUser_UserIdAndTargetMetricAndStatusAndComparatorIsNotNull(Long userId, String targetMetric, ChallengesStatus status);

    /** 상태별 전체 챌린지 조회 (예: IN_PROGRESS, COMPLETED 등) */
    List<UserChallenge> findByStatus(ChallengesStatus status);

    /** 특정 챌린지에 참여한 사용자 수 조회 */
    long countByChallenge_ChallengeId(Long challengeId);

    /** 마감일이 오늘 이전인 챌린지 중 특정 상태(IN_PROGRESS 등) 필터링 */
    List<UserChallenge> findByStatusAndEndDateBefore(ChallengesStatus status, LocalDate date);
}
