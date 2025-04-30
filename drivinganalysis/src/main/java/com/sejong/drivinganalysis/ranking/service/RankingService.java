package com.sejong.drivinganalysis.ranking.service;

import com.sejong.drivinganalysis.entity.Ranking;
import com.sejong.drivinganalysis.entity.User;
import com.sejong.drivinganalysis.entity.UserScore;
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

    // 조회: 월간 랭킹 (Ranking 테이블 기반 조회)
    public RankingListResponseDto getMonthlyRankingWithMyRank(int year, int month, int page, int size, Long myUserId) {
        String period = String.format("%d-%02d", year, month);
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

    // Overload
    public RankingListResponseDto getMonthlyRanking(int year, int month, int page, int size) {
        return getMonthlyRankingWithMyRank(year, month, page, size, null);
    }

    // 조회: 일간 랭킹 (UserScore 기반)
    public RankingListResponseDto getDailyRankingWithMyRank(String date, int page, int size, Long myUserId) {
        LocalDate targetDate = LocalDate.parse(date);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dailyScore"));
        Page<UserScore> scorePage = userScoreRepository.findByScoreDateBetween(targetDate, targetDate, pageable);

        RankingListResponseDto response = RankingListResponseDto.builder()
                .rankingType(RankingType.DAILY)
                .period(date)
                .rankings(scorePage.getContent().stream()
                        .map(us -> RankingSummaryDto.builder()
                                .rankPosition(null)
                                .userId(us.getUser().getUserId())
                                .username(us.getUser().getUsername())
                                .averageScore(us.getDailyScore() != null ? us.getDailyScore().doubleValue() : 0.0)
                                .build())
                        .collect(Collectors.toList()))
                .totalElements(scorePage.getTotalElements())
                .totalPages(scorePage.getTotalPages())
                .page(scorePage.getNumber())
                .size(scorePage.getSize())
                .build();

        if (myUserId != null) {
            List<UserScore> allScores = userScoreRepository.findByScoreDateBetween(targetDate, targetDate);
            allScores.sort(Comparator.comparing(UserScore::getDailyScore, Comparator.nullsLast(Comparator.reverseOrder())));

            for (int i = 0; i < allScores.size(); i++) {
                if (allScores.get(i).getUser().getUserId().equals(myUserId)) {
                    response.setMyRankPosition(i + 1);
                    response.setMyScore(allScores.get(i).getDailyScore().doubleValue());
                    break;
                }
            }
        }

        return response;
    }

    // 조회: 주간 랭킹 (UserScore 기반)
    public RankingListResponseDto getWeeklyRankingWithMyRank(int year, int week, int page, int size, Long myUserId) {
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate startOfWeek = startOfYear.plusWeeks(week - 1);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "weeklyScore"));
        Page<UserScore> scorePage = userScoreRepository.findByScoreDateBetween(startOfWeek, endOfWeek, pageable);

        String period = String.format("%d-W%d", year, week);

        RankingListResponseDto response = RankingListResponseDto.builder()
                .rankingType(RankingType.WEEKLY)
                .period(period)
                .rankings(scorePage.getContent().stream()
                        .map(us -> RankingSummaryDto.builder()
                                .rankPosition(null)
                                .userId(us.getUser().getUserId())
                                .username(us.getUser().getUsername())
                                .averageScore(us.getWeeklyScore() != null ? us.getWeeklyScore().doubleValue() : 0.0)
                                .build())
                        .collect(Collectors.toList()))
                .totalElements(scorePage.getTotalElements())
                .totalPages(scorePage.getTotalPages())
                .page(scorePage.getNumber())
                .size(scorePage.getSize())
                .build();

        if (myUserId != null) {
            List<UserScore> allScores = userScoreRepository.findByScoreDateBetween(startOfWeek, endOfWeek);
            allScores.sort(Comparator.comparing(UserScore::getWeeklyScore, Comparator.nullsLast(Comparator.reverseOrder())));

            for (int i = 0; i < allScores.size(); i++) {
                if (allScores.get(i).getUser().getUserId().equals(myUserId)) {
                    response.setMyRankPosition(i + 1);
                    response.setMyScore(allScores.get(i).getWeeklyScore().doubleValue());
                    break;
                }
            }
        }

        return response;
    }


    @Transactional
    public void calculateAndSaveMonthlyRanking(int year, int month) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.with(TemporalAdjusters.lastDayOfMonth());
        String period = String.format("%d-%02d", year, month);

        // 기존 랭킹 데이터 제거
        List<Ranking> existing = rankingRepository.findByRankingTypeAndPeriodOrderByScoreDesc(RankingType.MONTHLY, period);
        if (!existing.isEmpty()) {
            rankingRepository.deleteAll(existing);
            rankingRepository.flush();
        }

        // 해당 월의 모든 UserScore 가져오기
        List<UserScore> scores = userScoreRepository.findByScoreDateBetween(monthStart, monthEnd);

        // 유저별로 그룹화 후 dailyScore 기준 평균 계산
        Map<User, List<UserScore>> grouped = scores.stream()
                .collect(Collectors.groupingBy(UserScore::getUser));

        List<AggregatedScore> aggregatedScores = new ArrayList<>();

        for (Map.Entry<User, List<UserScore>> entry : grouped.entrySet()) {
            double avg = entry.getValue().stream()
                    .mapToInt(us -> us.getDailyScore() != null ? us.getDailyScore() : 0)
                    .average()
                    .orElse(0.0);

            aggregatedScores.add(new AggregatedScore(entry.getKey(), avg));
        }

        // 평균 점수 내림차순 정렬
        aggregatedScores.sort(Comparator.comparingDouble(AggregatedScore::getAverage).reversed());

        // 랭킹 저장
        int rank = 1;
        for (AggregatedScore as : aggregatedScores) {
            Ranking ranking = Ranking.createRanking(
                    as.getUser(),
                    (int) Math.round(as.getAverage()),
                    rank++,
                    RankingType.MONTHLY,
                    monthEnd,
                    period
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
