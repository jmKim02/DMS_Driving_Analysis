package com.sejong.drivinganalysis.repository;

import com.sejong.drivinganalysis.entity.UserScore;
import org.springframework.data.jpa.repositor
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserScoreRepository extends JpaRepository<UserScore, Long> {
    Optional<UserScore> findByUserUserIdAndScoreDate(Long userId, LocalDate scoreDate);

    List<UserScore> findByUserUserIdAndScoreDateBetweenOrderByScoreDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    List<UserScore> findByUserUserIdAndScoreDateAfterOrderByScoreDate(
            Long userId, LocalDate startDate);

    Optional<UserScore> findFirstByUserUserIdAndScoreDateLessThanEqualOrderByScoreDateDesc(Long userId, LocalDate date);

    List<UserScore> findByUserUserIdAndScoreDateBetweenOrderByScoreDateAsc(Long userId, LocalDate startDate, LocalDate endDate);
  
    List<UserScore> findByScoreDateBetween(LocalDate startDate, LocalDate endDate);
}