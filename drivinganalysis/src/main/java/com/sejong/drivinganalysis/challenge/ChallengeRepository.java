package com.sejong.drivinganalysis.challenge;

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

    // 제목과 기간이 동일한 챌린지 존재 여부 확인
    boolean existsByTitleAndStartDateAndEndDate(String title, LocalDate startDate, LocalDate endDate);

    // 주어진 날짜에 활성화된 챌린지 조회
    List<Challenge> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate start, LocalDate end);
}
