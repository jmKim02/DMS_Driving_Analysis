package com.sejong.drivinganalysis.repository;

import com.sejong.drivinganalysis.entity.UserScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface UserScoreRepository extends JpaRepository<UserScore, Long> {
    List<UserScore> findByScoreDateBetween(LocalDate startDate, LocalDate endDate);
}
