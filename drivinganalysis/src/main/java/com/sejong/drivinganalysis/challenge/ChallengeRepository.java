package com.sejong.drivinganalysis.challenge;

import com.sejong.drivinganalysis.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    boolean existsByTitleAndStartDateAndEndDate(String title, LocalDate startDate, LocalDate endDate);
    List<Challenge> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);
}

