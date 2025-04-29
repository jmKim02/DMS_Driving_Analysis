package com.sejong.drivinganalysis.repository;

import com.sejong.drivinganalysis.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    Optional<AnalysisResult> findByVideoVideoId(Long videoId);

    List<AnalysisResult> findByUserUserIdOrderByAnalyzedAtDesc(Long userId);

    List<AnalysisResult> findByUserUserIdAndAnalyzedAtBetween(
            Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime);

    long countByUserUserIdAndAnalyzedAtBetween(
            Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime);

    List<AnalysisResult> findByAnalyzedAtBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);

    @Query("SELECT DISTINCT result.user.userId FROM AnalysisResult result WHERE result.analyzedAt BETWEEN :startDateTime AND :endDateTime")
    List<Long> findDistinctUserIdsByAnalyzedAtBetween(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    List<AnalysisResult> findByUserUserIdInAndAnalyzedAtBetween(
            List<Long> userIds,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );
}