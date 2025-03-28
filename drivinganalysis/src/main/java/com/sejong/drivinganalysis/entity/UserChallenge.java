package com.sejong.drivinganalysis.entity;

import com.sejong.drivinganalysis.entity.enums.ChallengesStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_challenges", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "challenge_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserChallenge extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_challenge_id")
    private Long userChallengeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Column(name = "current_value")
    private Long currentValue;

    @Column(name = "reward_given")
    private Boolean rewardGiven = false;

    @Column(name = "result_summary")
    private String resultSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengesStatus status;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // 연관관계 메서드
    public void setUser(User user) {
        this.user = user;
        user.getChallenges().add(this);
    }

    // 생성 메서드
    public static UserChallenge joinChallenge(User user, Challenge challenge) {
        UserChallenge userChallenge = new UserChallenge();
        userChallenge.setUser(user); // 연관관계 메서드 사용
        userChallenge.challenge = challenge;
        userChallenge.currentValue = 0L;
        userChallenge.status = ChallengesStatus.IN_PROGRESS;
        userChallenge.joinedAt = LocalDateTime.now();
        return userChallenge;
    }
}
