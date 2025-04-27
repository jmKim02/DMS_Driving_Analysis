package com.sejong.drivinganalysis.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "user_scores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserScore extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id")
    private Long scoreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /*@Column(name = "daily_score")
    private Integer dailyScore;

    @Column(name = "weekly_score")
    private Integer weeklyScore;

    @Column(name = "monthly_score")
    private Integer monthlyScore;
*/
    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "score_date", nullable = false)
    private LocalDate scoreDate;

    // 생성 메서드
    public static UserScore createUserScore(User user, Integer score, LocalDate scoreDate) {
        UserScore userScore = new UserScore();
        userScore.user = user;
        userScore.score = score;
        userScore.scoreDate = scoreDate;
        return userScore;
    }
  
    // Setter 메서드
    public void setDailyScore(Integer dailyScore) {
        this.dailyScore = dailyScore;
    }

    public void setWeeklyScore(Integer weeklyScore) {
        this.weeklyScore = weeklyScore;
    }

    public void setMonthlyScore(Integer monthlyScore) {
        this.monthlyScore = monthlyScore;
    }
}

