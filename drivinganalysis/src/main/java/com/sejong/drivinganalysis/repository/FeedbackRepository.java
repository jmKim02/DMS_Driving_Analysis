package com.sejong.drivinganalysis.repository;

import com.sejong.drivinganalysis.entity.Feedback;
import com.sejong.drivinganalysis.entity.enums.FeedbackType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Page<Feedback> findByUserUserId(Long userId, Pageable pageable);

    Page<Feedback> findByUserUserIdAndFeedbackType(Long userId, FeedbackType feedbackType, Pageable pageable);

    List<Feedback> findByUserUserIdAndGeneratedAtBetween(Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime);

    @Query("SELECT f FROM Feedback f WHERE f.user.userId = :userId AND f.feedbackType = :feedbackType " +
            "AND f.generatedAt = (SELECT MAX(f2.generatedAt) FROM Feedback f2 WHERE f2.user.userId = :userId AND f2.feedbackType = :feedbackType)")
    Feedback findLatestByUserIdAndFeedbackType(@Param("userId") Long userId, @Param("feedbackType") FeedbackType feedbackType);

    @Query("SELECT f FROM Feedback f WHERE f.user.userId = :userId AND f.generatedAt = " +
            "(SELECT MAX(f2.generatedAt) FROM Feedback f2 WHERE f2.user.userId = :userId)")
    Feedback findLatestByUserId(@Param("userId") Long userId);

    // 주간 피드백 관련 쿼리
    @Query("SELECT f FROM Feedback f WHERE f.user.userId = :userId AND f.feedbackType = 'GENERAL' " +
            "AND f.content LIKE '%주간 운전 분석%' AND f.generatedAt BETWEEN :startDate AND :endDate")
    List<Feedback> findWeeklyFeedbackByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

}