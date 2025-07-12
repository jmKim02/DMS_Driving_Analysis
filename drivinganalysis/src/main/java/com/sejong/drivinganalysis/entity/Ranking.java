package com.sejong.drivinganalysis.entity;

import com.sejong.drivinganalysis.entity.enums.RankingType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "rankings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "ranking_type", "period"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ranking extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ranking_id")
    private Long rankingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer score;  // 계산된(평균) 점수

    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;  // 부여된 순위

    @Enumerated(EnumType.STRING)
    @Column(name = "ranking_type", nullable = false)
    private RankingType rankingType; // 여기서는 주로 MONTHLY 사용

    @Column(name = "ranking_date", nullable = false)
    private LocalDate rankingDate;   // 집계 기준일 (예: 월의 마지막 날)

    @Column(nullable = false)
    private String period;  // 예: "2025-04"로 저장

    // 생성 메서드 (모든 필드를 한 번에 설정)
    public static Ranking createRanking(User user, Integer score, Integer rankPosition,
                                        RankingType rankingType, LocalDate rankingDate, String period) {
        Ranking ranking = new Ranking();
        ranking.user = user;
        ranking.score = score;
        ranking.rankPosition = rankPosition;
        ranking.rankingType = rankingType;
        ranking.rankingDate = rankingDate;
        ranking.period = period;
        return ranking;
    }

    // 순위 업데이트용 메서드
    public void updateRankPosition(int rankPosition) {
        this.rankPosition = rankPosition;
    }

    // Setter for period (필요시 사용)
    public void setPeriod(String period) {
        this.period = period;
    }
}
