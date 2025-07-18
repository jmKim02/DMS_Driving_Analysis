package com.sejong.drivinganalysis.repository;

import com.sejong.drivinganalysis.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 공통 챌린지를 위한 JPA Repository
 */
@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    /**
     * 제목과 기간이 동일한 챌린지 존재 여부 확인
     */
    boolean existsByTitleAndStartDateAndEndDate(String title, LocalDate startDate, LocalDate endDate);

    /**
     * 주어진 날짜에 진행 중인 챌린지 조회 (관리자용, 대시보드 등)
     * 조건: startDate <= today && endDate >= today
     */
    List<Challenge> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate start, LocalDate end);

}
