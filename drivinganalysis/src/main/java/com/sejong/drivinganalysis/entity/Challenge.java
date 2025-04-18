package com.sejong.drivinganalysis.entity;

import com.sejong.drivinganalysis.entity.enums.ChallengeCategory;
import com.sejong.drivinganalysis.entity.enums.ChallengeType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "challenges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Challenge extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "challenge_id")
    private Long challengeId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "target_value", nullable = false)
    private Long targetValue;


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

    // 생성 메서드
    public static Challenge createChallenge(String title, String description, Long targetValue,
                                            ChallengeType challengeType, ChallengeCategory category,
                                            String rewardInfo, LocalDate startDate, LocalDate endDate) {
        Challenge challenge = new Challenge();
        challenge.title = title;
        challenge.description = description;
        challenge.targetValue = targetValue;
        challenge.challengeType = challengeType;
        challenge.category = category;
        challenge.rewardInfo = rewardInfo;
        challenge.startDate = startDate;
        challenge.endDate = endDate;
        return challenge;
    }
}
