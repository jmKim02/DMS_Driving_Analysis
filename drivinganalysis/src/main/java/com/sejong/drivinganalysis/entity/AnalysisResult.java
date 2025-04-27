package com.sejong.drivinganalysis.entity;

import com.sejong.drivinganalysis.entity.enums.AnalysisStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_results")
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class AnalysisResult extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private DrivingVideo video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "drowsiness_count")
    private Integer drowsinessCount;

    @Column(name = "phone_usage_count")
    private Integer phoneUsageCount;

    @Column(name = "smoking_count")
    private Integer smokingCount;

    @Column(name = "driving_score")
    private Integer drivingScore;

    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;

    @Column(name = "total_duration")
    private Integer totalDuration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus status;

    // 생성 메서드
    public static AnalysisResult createAnalysisResult(DrivingVideo video, User user,
                                                      Integer drowsinessCount, Integer phoneUsageCount,
                                                      Integer smokingCount, Integer totalDuration) {
        AnalysisResult result = new AnalysisResult();
        result.video = video;
        result.user = user;
        result.drowsinessCount = drowsinessCount;
        result.phoneUsageCount = phoneUsageCount;
        result.smokingCount = smokingCount;
        result.totalDuration = totalDuration;
        result.analyzedAt = LocalDateTime.now();
        result.status = AnalysisStatus.COMPLETED;
        result.calculateScore();
        return result;
    }

    // 점수 계산 비즈니스 로직
    public void calculateScore() {
        int baseScore = 100; // 기본 점수
        double durationFactor = calculateDurationFactor();

        int drowsinessDeduction = calculateDrowsinessDeduction(durationFactor);
        int phoneDeduction = calculatePhoneUsageDeduction(durationFactor);
        int smokingDeduction = calculateSmokingDeduction(durationFactor);

        int totalScore = baseScore - drowsinessDeduction - phoneDeduction - smokingDeduction;

        // 최종 점수는 0-100 사이
        this.drivingScore = Math.max(0, Math.min(100, totalScore));
    }

    // 운전 시간에 따른 가중치 계산
    private double calculateDurationFactor() {
        if (totalDuration == null) {
            return 1.0;
        }

        if (totalDuration < 1800) { // 30분 미만
            return 0.8; // 짧은 운전은 감점 영향 감소
        } else if (totalDuration > 3600) { // 1시간 초과
            return 1.2; // 장시간 운전은 감점 영향 증가
        }

        return 1.0; // 30분~1시간: 기본 가중치
    }

    // 졸음 감지에 따른 감점 계산
    private int calculateDrowsinessDeduction(double durationFactor) {
        if (drowsinessCount == null || drowsinessCount <= 0) {
            return 0;
        }

        int deduction;

        if (drowsinessCount == 1) {
            deduction = 8; // 1회는 8점 감점
        } else if (drowsinessCount == 2) {
            deduction = 18; // 2회는 18점 감점
        } else {
            // 3회 이상은 25점 + 추가 5점씩
            deduction = 25 + (drowsinessCount - 3) * 5;
            deduction = Math.min(deduction, 40); // 최대 40점까지 감점
        }

        return (int)(deduction * durationFactor);
    }

    // 휴대폰 사용에 따른 감점 계산
    private int calculatePhoneUsageDeduction(double durationFactor) {
        if (phoneUsageCount == null || phoneUsageCount <= 0) {
            return 0;
        }

        int deduction;

        if (phoneUsageCount == 1) {
            deduction = 5; // 1회는 5점 감점
        } else if (phoneUsageCount == 2) {
            deduction = 12; // 2회는 12점 감점
        } else {
            // 3회 이상은 20점 + 추가 3점씩
            deduction = 20 + (phoneUsageCount - 3) * 3;
            deduction = Math.min(deduction, 30); // 최대 30점까지 감점
        }

        return (int)(deduction * durationFactor);
    }

    // 흡연에 따른 감점 계산
    private int calculateSmokingDeduction(double durationFactor) {
        if (smokingCount == null || smokingCount <= 0) {
            return 0;
        }

        int deduction = 3 * smokingCount; // 흡연 1회당 3점
        deduction = Math.min(deduction, 15); // 최대 15점까지 감점

        return (int)(deduction * durationFactor);
    }
}