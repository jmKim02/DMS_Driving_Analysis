package com.sejong.drivinganalysis.entity;

import com.sejong.drivinganalysis.entity.enums.ChallengesStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_challenges", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "challenge_id"})
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserChallenge extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_challenge_id")
    private Long userChallengeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id") // ✅ 개인화 챌린지는 null 허용
    private Challenge challenge;

    @Column(name = "title", nullable = false)
    private String title; // ✅ 공통 챌린지에서 복사 or 개인 정의

    @Column(name = "target_metric", nullable = false)
    private String targetMetric;

    @Column(name = "target_value", nullable = false)
    private Long targetValue;

    @Column(nullable = false)
    private String comparator; // ex: ">=", "<="

    @Column(name = "current_value")
    private Long currentValue = 0L;

    @Column(name = "reward_info")
    private String rewardInfo; // ✅ 공통에서 복사 or 개인 지정

    @Column(name = "reward_given")
    private Boolean rewardGiven = false;

    @Column(name = "result_summary")
    private String resultSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengesStatus status;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // 생성 메서드 (공통 챌린지 기반)
    public static UserChallenge fromChallenge(User user, Challenge challenge) {
        UserChallenge uc = new UserChallenge();
        uc.user = user;
        uc.challenge = challenge;
        uc.title = challenge.getTitle();
        uc.targetMetric = challenge.getTargetMetric();
        uc.targetValue = challenge.getTargetValue();
        uc.comparator = ">="; // 혹은 challenge에 있는 값
        uc.rewardInfo = challenge.getRewardInfo();
        uc.status = ChallengesStatus.IN_PROGRESS;
        uc.joinedAt = LocalDateTime.now();
        uc.startDate = challenge.getStartDate();
        uc.endDate = challenge.getEndDate();
        return uc;
    }

    // 생성 메서드 (개인화 챌린지)
    public static UserChallenge createCustom(User user, String title, String metric, Long target,
                                             String comparator, String rewardInfo,
                                             LocalDate start, LocalDate end) {
        UserChallenge uc = new UserChallenge();
        uc.user = user;
        uc.title = title;
        uc.targetMetric = metric;
        uc.targetValue = target;
        uc.comparator = comparator;
        uc.rewardInfo = rewardInfo;
        uc.status = ChallengesStatus.IN_PROGRESS;
        uc.joinedAt = LocalDateTime.now();
        uc.startDate = start;
        uc.endDate = end;
        return uc;
    }
}
