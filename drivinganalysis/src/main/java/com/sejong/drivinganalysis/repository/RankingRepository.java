package com.sejong.drivinganalysis.repository;

import com.sejong.drivinganalysis.entity.Ranking;
import com.sejong.drivinganalysis.entity.enums.RankingType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RankingRepository extends JpaRepository<Ranking, Long> {

    // 조회: period 기준으로 페이징
    Page<Ranking> findByRankingTypeAndPeriodOrderByRankPositionAsc(RankingType rankingType, String period, Pageable pageable);

    // 삭제: 유니크 제약 조건은 user_id + ranking_type + period
    List<Ranking> findByRankingTypeAndPeriodOrderByScoreDesc(RankingType rankingType, String period);

    // 내 랭킹 검색
    Optional<Ranking> findByUser_UserIdAndRankingTypeAndPeriod(Long userId, RankingType rankingType, String period);
}
