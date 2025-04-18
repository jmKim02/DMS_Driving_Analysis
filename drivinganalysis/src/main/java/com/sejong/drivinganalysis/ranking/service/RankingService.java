package com.sejong.drivinganalysis.ranking.service;

import com.sejong.drivinganalysis.entity.Ranking;
import com.sejong.drivinganalysis.entity.UserScore;
import com.sejong.drivinganalysis.entity.User;
import com.sejong.drivinganalysis.entity.enums.RankingType;
import com.sejong.drivinganalysis.ranking.dto.RankingListResponseDto;
import com.sejong.drivinganalysis.ranking.dto.RankingSummaryDto;
import com.sejong.drivinganalysis.ranking.repository.RankingRepository;
import com.sejong.drivinganalysis.repository.UserScoreRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final RankingRepository rankingRepository;
    private final UserScoreRepository userScoreRepository;

    // 조회: 월간 랭킹 (전체 목록 + 내 랭킹 정보 포함)
    public RankingListResponseDto getMonthlyRankingWithMyRank(int year, int month, int page, int size, Long myUserId) {
        String period = String.format("%d-%02d", year, month);  // 예: "2025-04"
        Pageable pageable = PageRequest.of(page, size, Sort.by("rankPosition").ascending());
        Page<Ranking> rankingPage = rankingRepository.findByRankingTypeAndPeriodOrderByRankPositionAsc(RankingType.MONTHLY, period, pageable);

        RankingListResponseDto response = RankingListResponseDto.builder()
                .rankingType(RankingType.MONTHLY)
                .period(period)
                .rankings(rankingPage.getContent().stream().map(r ->
                        RankingSummaryDto.builder()
                                .rankPosition(r.getRankPosition())
                                .userId(r.getUser().getUserId())
                                .username(r.getUser().getUsername())
                                .averageScore(r.getScore().doubleValue())
                                .build()
                ).collect(Collectors.toList()))
                .totalElements(rankingPage.getTotalElements())
                .totalPages(rankingPage.getTotalPages())
                .page(rankingPage.getNumber())
                .size(rankingPage.getSize())
                .build();

        if (myUserId != null) {
            Optional<Ranking> myRankingOpt = rankingRepository.findByUser_UserIdAndRankingTypeAndPeriod(myUserId, RankingType.MONTHLY, period);
            if (myRankingOpt.isPresent()) {
                response.setMyRankPosition(myRankingOpt.get().getRankPosition());
                response.setMyScore(myRankingOpt.get().getScore().doubleValue());
            }
        }
        return response;
    }

    // Overload: if myUserId is not provided
    public RankingListResponseDto getMonthlyRanking(int year, int month, int page, int size) {
        return getMonthlyRankingWithMyRank(year, month, page, size, null);
    }

    // 계산 API: 월간 랭킹 재계산 및 저장 (수동 트리거용; 매일 자동 업데이트 가능하도록 @Scheduled 적용 가능)
    @Transactional
    public void calculateAndSaveMonthlyRanking(int year, int month) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.with(TemporalAdjusters.lastDayOfMonth());
        String period = String.format("%d-%02d", year, month);

        // 1. 기존 월간 랭킹 데이터 삭제 (해당 period)
        // 기존 데이터 삭제를 위해 period 기반으로 조회하고 삭제합니다.
        List<Ranking> existing = rankingRepository.findByRankingTypeAndPeriodOrderByScoreDesc(RankingType.MONTHLY, period);
        if (!existing.isEmpty()) {
            rankingRepository.deleteAll(existing);
            rankingRepository.flush();  // 삭제를 DB에 즉시 반영
        }

        // 2. 해당 월의 모든 UserScore 레코드 조회
        List<UserScore> scores = userScoreRepository.findByScoreDateBetween(monthStart, monthEnd);

        // 3. 사용자별로 그룹핑하여, 점수 평균 계산 (하루 여러 번 들어온 점수를 평균)
        Map<User, List<UserScore>> grouped = scores.stream()
                .collect(Collectors.groupingBy(UserScore::getUser));
        List<AggregatedScore> aggregatedScores = new ArrayList<>();
        for (Map.Entry<User, List<UserScore>> entry : grouped.entrySet()) {
            double avg = entry.getValue().stream()
                    .mapToInt(UserScore::getScore)
                    .average()
                    .orElse(0.0);
            aggregatedScores.add(new AggregatedScore(entry.getKey(), avg));
        }

        // 4. 평균 점수를 내림차순으로 정렬 (높은 점수가 1등)
        aggregatedScores.sort(Comparator.comparingDouble(AggregatedScore::getAverage).reversed());

        // 5. 순위 부여하고 Ranking 엔티티 생성 후 저장
        int rank = 1;
        for (AggregatedScore as : aggregatedScores) {
            Ranking ranking = Ranking.createRanking(
                    as.getUser(),
                    (int)Math.round(as.getAverage()),  // 평균 점수를 반올림해서 정수로 저장
                    rank++,
                    RankingType.MONTHLY,
                    monthEnd,      // 집계 기준일 (예: 해당 월의 마지막 날)
                    period         // "2025-04"
            );
            rankingRepository.save(ranking);
        }
    }

    @Getter
    @AllArgsConstructor
    private static class AggregatedScore {
        private final User user;
        private final double average;
    }
}
