package com.sejong.drivinganalysis.entity;

import com.sejong.drivinganalysis.entity.enums.FeedbackType;
import com.sejong.drivinganalysis.entity.enums.SeverityLevel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback extends BaseTimeEntity{

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedbackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false)
    private FeedbackType feedbackType;

    @Column(nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity_level")
    private SeverityLevel severityLevel;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    // 주간 피드백 등 특별 피드백 유형을 구분하기 위한 필드 추가
    @Column(name = "feedback_category")
    private String feedbackCategory;

    // 연관관계 메서드
    public void setUser(User user) {
        this.user = user;
        user.getFeedbacks().add(this);
    }

    // 생성 메서드
    public static Feedback createFeedback(User user, FeedbackType feedbackType,
                                          String content, SeverityLevel severityLevel) {
        Feedback feedback = new Feedback();
        feedback.setUser(user); // 연관관계 메서드 사용
        feedback.feedbackType = feedbackType;
        feedback.content = content;
        feedback.severityLevel = severityLevel;
        feedback.generatedAt = LocalDateTime.now();
        feedback.feedbackCategory = null; // 기본값은 null (일반 피드백)
        return feedback;
    }

    // 주간 피드백 생성 메서드 추가
    public static Feedback createWeeklyFeedback(User user, FeedbackType feedbackType,
                                                String content, SeverityLevel severityLevel) {
        Feedback feedback = createFeedback(user, feedbackType, content, severityLevel);
        feedback.feedbackCategory = "WEEKLY";
        return feedback;
    }

    // 피드백 카테고리 설정 메서드 (필요시 사용)
    public void setFeedbackCategory(String category) {
        this.feedbackCategory = category;
    }
}