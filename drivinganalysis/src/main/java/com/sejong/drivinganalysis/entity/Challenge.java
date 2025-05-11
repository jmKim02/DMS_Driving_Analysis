package com.sejong.drivinganalysis.entity;

import com.sejong.drivinganalysis.entity.enums.ChallengeCategory;
import com.sejong.drivinganalysis.entity.enums.ChallengeType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "challenges",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_challenge_unique",
                columnNames = {"title", "start_date", "end_date"}
        )
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Challenge extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "challenge_id")
    private Long challengeId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "target_value", nullable = false)
    private Long targetValue;

    @Column(name = "target_metric", nullable = false)
    private String targetMetric;

    /** 새로 추가된 비교 연산자 필드 */
    @Column(nullable = false)
    private String comparator;  // ex: ">=", "<="

    @Enumerated(EnumType.STRING)
    @Column(name = "challenge_type", nullable = false)
    private ChallengeType challengeType;

    @Column(name = "reward_info")
    private String rewardInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private ChallengeCategory category;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;


    // alias getter for convenience
    public Long getId() {
        return this.challengeId;
    }

    /**
     * 생성 메서드에도 comparator 매개변수 추가
     */
    public static Challenge createChallenge(
            String title,
            String description,
            Long targetValue,
            String targetMetric,
            String comparator,
            ChallengeType challengeType,
            ChallengeCategory category,
            String rewardInfo,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Challenge challenge = new Challenge();
        challenge.title = title;
        challenge.description = description;
        challenge.targetValue = targetValue;
        challenge.targetMetric = targetMetric;
        challenge.comparator = comparator;
        challenge.challengeType = challengeType;
        challenge.category = category;
        challenge.rewardInfo = rewardInfo;
        challenge.startDate = startDate;
        challenge.endDate = endDate;
        return challenge;
    }
}
