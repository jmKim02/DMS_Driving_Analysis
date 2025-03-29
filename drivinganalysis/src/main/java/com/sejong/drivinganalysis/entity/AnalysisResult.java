package com.sejong.drivinganalysis.entity;

import com.sejong.drivinganalysis.entity.enums.AnalysisStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Column(name = "processing_time")
    private Long processingTime;

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
        result.completedAt = LocalDateTime.now();
        result.status = AnalysisStatus.COMPLETED;
        result.calculateScore();
        return result;
    }

    // 점수 계산 비즈니스 로직
    public void calculateScore() {
        int score = 100; // 기본 점수

        // 졸음 감지 횟수에 따른 감점 (1회당 10점)
        if (drowsinessCount != null) {
            score -= drowsinessCount * 10;
        }

        // 휴대폰 사용 횟수에 따른 감점 (1회당 8점)
        if (phoneUsageCount != null) {
            score -= phoneUsageCount * 8;
        }

        // 흡연 횟수에 따른 감점 (1회당 5점)
        if (smokingCount != null) {
            score -= smokingCount * 5;
        }

        // 최소 점수는 0점으로 설정
        this.drivingScore = Math.max(0, score);
    }
}