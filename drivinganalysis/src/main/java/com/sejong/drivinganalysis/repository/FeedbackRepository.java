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

    // 주간 피드백 조회를 위한 메서드 추가
    List<Feedback> findByUserUserIdAndFeedbackCategoryOrderByGeneratedAtDesc(Long userId, String feedbackCategory);

    // 가장 최근의 주간 피드백 조회
    Feedback findFirstByUserUserIdAndFeedbackCategoryOrderByGeneratedAtDesc(Long userId, String feedbackCategory);

    // 특정 기간의 주간 피드백 조회 (기존 메서드 수정)
    @Query("SELECT f FROM Feedback f WHERE f.user.userId = :userId AND f.feedbackCategory = 'WEEKLY' " +
            "AND f.generatedAt BETWEEN :startDate AND :endDate")
    List<Feedback> findWeeklyFeedbackByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}