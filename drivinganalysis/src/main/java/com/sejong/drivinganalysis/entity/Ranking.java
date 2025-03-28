package com.sejong.drivinganalysis.entity;

import com.sejong.drivinganalysis.entity.enums.RankingType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rankings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "ranking_type", "ranking_date"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ranking extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ranking_id")
    private Long rankingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;

    @Enumerated(EnumType.STRING)
    @Column(name = "ranking_type", nullable = false)
    private RankingType rankingType;

    @Column(name = "ranking_date", nullable = false)
    private LocalDate rankingDate;

    // 생성 메서드
    public static Ranking createRanking(User user, Integer score, Integer rankPosition,
                                        RankingType rankingType, LocalDate rankingDate) {
        Ranking ranking = new Ranking();
        ranking.user = user;
        ranking.score = score;
        ranking.rankPosition = rankPosition;
        ranking.rankingType = rankingType;
        ranking.rankingDate = rankingDate;
        return ranking;
    }
}
