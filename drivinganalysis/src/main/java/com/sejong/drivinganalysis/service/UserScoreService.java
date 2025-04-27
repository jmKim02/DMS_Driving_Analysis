package com.sejong.drivinganalysis.service;

import com.sejong.drivinganalysis.dto.ScoreDto;
import com.sejong.drivinganalysis.entity.AnalysisResult;
import com.sejong.drivinganalysis.entity.User;
import com.sejong.drivinganalysis.entity.UserScore;
import com.sejong.drivinganalysis.exception.ApiException;
import com.sejong.drivinganalysis.repository.AnalysisResultRepository;
import com.sejong.drivinganalysis.repository.UserRepository;
import com.sejong.drivinganalysis.repository.UserScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserScoreService {

    private final UserScoreRepository userScoreRepository;
    private final UserRepository userRepository;
    private final AnalysisResultRepository analysisResultRepository;

    /**
     * 분석 결과를 반영하여 사용자 점수 업데이트
     */
    @Transactional
    public void updateUserScore(Long userId, AnalysisResult newResult) {
        User user = validateUser(userId);
        LocalDate today = LocalDate.now();

        // 1. 오늘의 모든 분석 결과를 가져와 일간 점수 계산
        List<AnalysisResult> todayResults = analysisResultRepository.findByUserUserIdAndAnalyzedAtBetween(
                userId,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );

        // 일간 점수 계산 - 모든 세션의 단순 평균
        int dailyScore;
        if (!todayResults.isEmpty()) {
            int totalScore = todayResults.stream()
                    .mapToInt(AnalysisResult::getDrivingScore)
                    .sum();
            dailyScore = totalScore / todayResults.size();
        } else {
            // 결과가 없는 경우 (이론상 발생하지 않음)
            dailyScore = newResult.getDrivingScore();
        }

        // 오늘 날짜의 사용자 점수 레코드 조회 (없으면 새로 생성)
        UserScore userScore = userScoreRepository.findByUserUserIdAndScoreDate(userId, today)
                .orElseGet(() -> createNewUserScore(user, today));

        // 일간 점수 업데이트
        userScore.setDailyScore(dailyScore);

        // 2. 주간 점수 계산
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today;

        // 이번 주의 모든 점수 (오늘 포함)
        List<UserScore> weekScores = userScoreRepository.findByUserUserIdAndScoreDateBetweenOrderByScoreDateDesc(
                user.getUserId(), startOfWeek, endOfWeek);

        // 주간 점수 - 오늘의 일간 점수 포함
        if (weekScores.isEmpty()) {
            // 주 내 첫 데이터인 경우 일간 점수와 동일하게 설정
            userScore.setWeeklyScore(dailyScore);
        } else {
            // 기존 데이터가 있으면 평균 계산
            // 오늘 데이터를 먼저 삭제 (새로 업데이트된 점수를 사용하기 위해)
            weekScores = weekScores.stream()
                    .filter(score -> !score.getScoreDate().isEqual(today))
                    .collect(Collectors.toList());

            // 기존 데이터의 일간 점수 합계
            int weeklyScoreSum = weekScores.stream()
                    .mapToInt(UserScore::getDailyScore)
                    .sum();

            // 오늘 점수 포함 평균 계산
            int weeklyScore = (weeklyScoreSum + dailyScore) / (weekScores.size() + 1);
            userScore.setWeeklyScore(weeklyScore);
        }

        // 3. 월간 점수 계산
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today;

        // 이번 달의 모든 점수 (오늘 포함)
        List<UserScore> monthScores = userScoreRepository.findByUserUserIdAndScoreDateBetweenOrderByScoreDateDesc(
                user.getUserId(), startOfMonth, endOfMonth);

        // 월간 점수 - 오늘의 일간 점수 포함
        if (monthScores.isEmpty()) {
            // 월 내 첫 데이터인 경우 일간 점수와 동일하게 설정
            userScore.setMonthlyScore(dailyScore);
        } else {
            // 기존 데이터가 있으면 평균 계산
            // 오늘 데이터를 먼저 삭제 (새로 업데이트된 점수를 사용하기 위해)
            monthScores = monthScores.stream()
                    .filter(score -> !score.getScoreDate().isEqual(today))
                    .collect(Collectors.toList());

            // 기존 데이터의 일간 점수 합계
            int monthlyScoreSum = monthScores.stream()
                    .mapToInt(UserScore::getDailyScore)
                    .sum();

            // 오늘 점수 포함 평균 계산
            int monthlyScore = (monthlyScoreSum + dailyScore) / (monthScores.size() + 1);
            userScore.setMonthlyScore(monthlyScore);
        }

        // 최종 저장
        userScoreRepository.save(userScore);
        log.info("Updated user score for userId: {}, scoreDate: {}, dailyScore: {}, weeklyScore: {}, monthlyScore: {}",
                userId, today, userScore.getDailyScore(), userScore.getWeeklyScore(), userScore.getMonthlyScore());
    }

    /**
     * 통합된 사용자 점수 조회 메서드
     * 파라미터 조합에 따라 다른 형태의 점수 데이터 반환
     */
    @Transactional(readOnly = true)
    public ScoreDto.ScoreResponse getUserScores(Long userId, String period,
                                                Integer year, Integer month, Integer week,
                                                LocalDate startDate, LocalDate endDate) {
        // 사용자 검증
        validateUser(userId);

        // 특정 연도, 월, 주 기준 조회 (우선순위)
        if (year != null) {
            if (month != null) {
                if (week != null) {
                    // 특정 연도, 월, 주의 일별 점수
                    return getScoresForWeek(userId, year, month, week);
                } else {
                    // 특정 연도, 월의 일별 점수
                    return getScoresForMonth(userId, year, month);
                }
            } else {
                // 특정 연도의 월별 점수
                return getScoresForYear(userId, year);
            }
        }

        // 기간 기준 조회
        List<UserScore> scores;
        if (startDate != null && endDate != null) {
            scores = userScoreRepository.findByUserUserIdAndScoreDateBetweenOrderByScoreDateDesc(
                    userId, startDate, endDate);
        } else {
            // 기본값은 최근 7일
            LocalDate defaultEndDate = LocalDate.now();
            LocalDate defaultStartDate = defaultEndDate.minusDays(6); // 7일치 데이터
            scores = userScoreRepository.findByUserUserIdAndScoreDateBetweenOrderByScoreDateDesc(
                    userId, defaultStartDate, defaultEndDate);
        }

        // 응답 DTO 변환
        List<ScoreDto.ScoreData> scoreDataList = scores.stream()
                .map(score -> {
                    Integer scoreValue;
                    if ("daily".equals(period)) {
                        scoreValue = score.getDailyScore();
                    } else if ("weekly".equals(period)) {
                        scoreValue = score.getWeeklyScore();
                    } else if ("monthly".equals(period)) {
                        scoreValue = score.getMonthlyScore();
                    } else {
                        // 기본값은 daily
                        scoreValue = score.getDailyScore();
                    }

                    return ScoreDto.ScoreData.builder()
                            .scoreId(score.getScoreId())
                            .scoreDate(score.getScoreDate())
                            .scoreValue(scoreValue != null ? scoreValue : 0)  // null인 경우 0으로 처리
                            .build();
                })
                .collect(Collectors.toList());

        // 평균 점수 계산
        double averageScore = calculateAverageScore(scoreDataList);

        return ScoreDto.ScoreResponse.builder()
                .userId(userId)
                .scores(scoreDataList)
                .averageScore((int) Math.round(averageScore))
                .build();
    }

    /**
     * 점수 통계 조회
     */
    @Transactional(readOnly = true)
    public ScoreDto.ScoreStatistics getScoreStatistics(Long userId) {
        validateUser(userId);

        // 요일별 통계
        LocalDate startOfLastMonth = LocalDate.now().minusMonths(1);
        List<UserScore> recentScores = userScoreRepository.findByUserUserIdAndScoreDateAfterOrderByScoreDate(
                userId, startOfLastMonth);

        List<ScoreDto.DayOfWeekStat> dayOfWeekStats = calculateDayOfWeekStats(recentScores);
        List<ScoreDto.MonthlyStat> monthlyStats = calculateMonthlyStats(userId);

        // 요일 순서 정렬 (월요일부터 일요일)
        dayOfWeekStats.sort(Comparator.comparingInt(stat ->
                DayOfWeek.valueOf(stat.getDayOfWeek()).getValue()));

        return ScoreDto.ScoreStatistics.builder()
                .userId(userId)
                .dayOfWeekStats(dayOfWeekStats)
                .monthlyTrend(monthlyStats)
                .build();
    }

    /**
     * 특정 연도의 월별 점수 조회
     */
    @Transactional(readOnly = true)
    public ScoreDto.ScoreResponse getScoresForYear(Long userId, int year) {
        validateYearMonth(year, 1); // 1월을 기준으로 연도 검증

        // 월별 점수 맵 가져오기
        Map<Integer, Integer> monthlyScores = new HashMap<>();

        // 1월부터 12월까지 (또는 현재 월까지)
        int currentYear = LocalDate.now().getYear();
        int maxMonth = (year == currentYear) ? LocalDate.now().getMonthValue() : 12;

        for (int month = 1; month <= maxMonth; month++) {
            // 월별 마지막 점수 가져오기
            Integer score = getMonthScore(userId, year, month);
            monthlyScores.put(month, score);
        }

        List<ScoreDto.ScoreData> scoreDataList = new ArrayList<>();
        double totalScore = 0;
        int validScoreCount = 0;

        for (Map.Entry<Integer, Integer> entry : monthlyScores.entrySet()) {
            Integer month = entry.getKey();
            Integer score = entry.getValue();

            if (score > 0) {
                totalScore += score;
                validScoreCount++;
            }

            // 각 월의 1일을 대표 날짜로 사용
            LocalDate monthDate = LocalDate.of(year, month, 1);

            scoreDataList.add(ScoreDto.ScoreData.builder()
                    .scoreId(null) // 특정 레코드 ID가 아닌 집계 결과
                    .scoreDate(monthDate)
                    .scoreValue(score)
                    .build());
        }

        // 평균 계산
        double averageScore = validScoreCount > 0 ? totalScore / validScoreCount : 0;

        // 월 순으로 정렬
        scoreDataList.sort(Comparator.comparing(ScoreDto.ScoreData::getScoreDate));

        return ScoreDto.ScoreResponse.builder()
                .userId(userId)
                .scores(scoreDataList)
                .averageScore((int) Math.round(averageScore))
                .build();
    }

    /**
     * 특정 연도, 월의 일별 점수 조회 - 개선된 버전
     */
    @Transactional(readOnly = true)
    public ScoreDto.ScoreResponse getScoresForMonth(Long userId, int year, int month) {
        validateYearMonth(year, month);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        // 날짜별 점수를 한 번에 조회
        List<UserScore> scores = userScoreRepository.findByUserUserIdAndScoreDateBetweenOrderByScoreDateAsc(
                userId, startDate, endDate);

        // 날짜-점수 맵을 만들어 O(1) 접근 가능하게 함
        Map<LocalDate, UserScore> scoreMap = scores.stream()
                .collect(Collectors.toMap(
                        UserScore::getScoreDate,
                        score -> score,
                        // 중복 키가 있을 경우 처리 (거의 없을 것이지만 안전하게)
                        (existing, replacement) -> existing
                ));

        List<ScoreDto.ScoreData> scoreDataList = new ArrayList<>();
        // 해당 월의 일수 계산
        int daysInMonth = endDate.getDayOfMonth();

        // 모든 날짜에 대해 O(n) 루프 (n은 해당 월의 일수)
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentDate = LocalDate.of(year, month, day);
            UserScore score = scoreMap.get(currentDate);

            scoreDataList.add(ScoreDto.ScoreData.builder()
                    .scoreId(score != null ? score.getScoreId() : null)
                    .scoreDate(currentDate)
                    .scoreValue(score != null && score.getDailyScore() != null ? score.getDailyScore() : 0)
                    .build());
        }

        double averageScore = calculateAverageScore(scoreDataList);

        return ScoreDto.ScoreResponse.builder()
                .userId(userId)
                .scores(scoreDataList)
                .averageScore((int) Math.round(averageScore))
                .build();
    }

    /**
     * 특정 연도, 월, 주의 일별 점수 조회
     */
    @Transactional(readOnly = true)
    public ScoreDto.ScoreResponse getScoresForWeek(Long userId, int year, int month, int weekOfMonth) {
        validateYearMonth(year, month);

        // 해당 월의 첫 날
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);

        // 해당 월의 첫 주 시작일 계산
        LocalDate firstMondayOfMonth = firstDayOfMonth;
        if (firstDayOfMonth.getDayOfWeek() != DayOfWeek.MONDAY) {
            firstMondayOfMonth = firstDayOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        }

        // 주 계산 (0부터 시작하는 경우 조정 필요)
        LocalDate weekStart = firstMondayOfMonth.plusWeeks(weekOfMonth - 1);
        LocalDate weekEnd = weekStart.plusDays(6);

        // 해당 월에 속하는지 확인
        if (weekStart.getMonthValue() != month && weekEnd.getMonthValue() != month) {
            throw new ApiException("INVALID_WEEK", "The specified week is not in the given month");
        }

        List<UserScore> scores = userScoreRepository.findByUserUserIdAndScoreDateBetweenOrderByScoreDateAsc(
                userId, weekStart, weekEnd);

        List<ScoreDto.ScoreData> scoreDataList = new ArrayList<>();

        // 해당 주의 모든 날짜에 대해 루프
        LocalDate currentDate = weekStart;
        while (!currentDate.isAfter(weekEnd)) {
            final LocalDate loopDate = currentDate;

            // 현재 날짜에 해당하는 점수 찾기
            Optional<UserScore> dayScore = scores.stream()
                    .filter(score -> score.getScoreDate().equals(loopDate))
                    .findFirst();

            // 점수가 존재하면 추가, 없으면 0으로 추가
            scoreDataList.add(ScoreDto.ScoreData.builder()
                    .scoreId(dayScore.map(UserScore::getScoreId).orElse(null))
                    .scoreDate(currentDate)
                    .scoreValue(dayScore.map(UserScore::getDailyScore).orElse(0))
                    .build());

            currentDate = currentDate.plusDays(1);
        }

        // 평균 계산
        double averageScore = calculateAverageScore(scoreDataList);

        return ScoreDto.ScoreResponse.builder()
                .userId(userId)
                .scores(scoreDataList)
                .averageScore((int) Math.round(averageScore))
                .build();
    }

    /**
     * 특정 월의 점수 조회 (마지막 기록 기준)
     */
    @Transactional(readOnly = true)
    public Integer getMonthScore(Long userId, int year, int month) {
        // 해당 월의 마지막 날 계산
        LocalDate monthEnd = YearMonth.of(year, month).atEndOfMonth();

        // 월의 마지막 날 또는 그보다 이전 가장 가까운 날의 점수 조회
        return userScoreRepository
                .findFirstByUserUserIdAndScoreDateLessThanEqualOrderByScoreDateDesc(userId, monthEnd)
                .map(UserScore::getMonthlyScore)
                .orElse(0);
    }

    /**
     * 특정 주의 점수 조회 (마지막 기록 기준)
     */
    @Transactional(readOnly = true)
    public Integer getWeekScore(Long userId, int year, int weekNumber) {
        // 해당 주의 시작일과 종료일 계산
        LocalDate weekStart = LocalDate.ofYearDay(year, 1)
                .with(WeekFields.ISO.weekOfYear(), weekNumber)
                .with(WeekFields.ISO.dayOfWeek(), 1); // 월요일

        LocalDate weekEnd = weekStart.plusDays(6); // 일요일

        // 주의 마지막 날 또는 그보다 이전 가장 가까운 날의 점수 조회
        return userScoreRepository
                .findFirstByUserUserIdAndScoreDateLessThanEqualOrderByScoreDateDesc(userId, weekEnd)
                .map(UserScore::getWeeklyScore)
                .orElse(0);
    }

    /**
     * 새로운 사용자 점수 레코드 생성
     */
    private UserScore createNewUserScore(User user, LocalDate scoreDate) {
        UserScore userScore = UserScore.createUserScore(
                user,
                0, // dailyScore 초기값
                0, // weeklyScore 초기값
                0, // monthlyScore 초기값
                scoreDate
        );

        return userScoreRepository.save(userScore);
    }

    /**
     * 데모데이터 생성 (특정 날짜로 점수 업데이트)
     */
    @Transactional
    public void updateUserScoreWithCustomDate(Long userId, AnalysisResult newResult, LocalDate customDate) {
        User user = validateUser(userId);

        // customDate를 오늘 날짜 대신 사용
        LocalDate today = customDate;

        // 이 날짜의 모든 분석 결과를 가져와 일간 점수 계산
        List<AnalysisResult> todayResults = analysisResultRepository.findByUserUserIdAndAnalyzedAtBetween(
                userId,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );

        // 일간 점수 계산 - 모든 세션의 단순 평균
        int dailyScore;
        if (!todayResults.isEmpty()) {
            int totalScore = todayResults.stream()
                    .mapToInt(AnalysisResult::getDrivingScore)
                    .sum();
            dailyScore = totalScore / todayResults.size();
        } else {
            // 결과가 없는 경우 (이론상 발생하지 않음)
            dailyScore = newResult.getDrivingScore();
        }

        // 오늘 날짜의 사용자 점수 레코드 조회 (없으면 새로 생성)
        UserScore userScore = userScoreRepository.findByUserUserIdAndScoreDate(userId, today)
                .orElseGet(() -> createNewUserScore(user, today));

        // 일간 점수 업데이트
        userScore.setDailyScore(dailyScore);

        // 주간 점수 계산
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today;

        // 이번 주의 모든 점수 (오늘 포함)
        List<UserScore> weekScores = userScoreRepository.findByUserUserIdAndScoreDateBetweenOrderByScoreDateDesc(
                user.getUserId(), startOfWeek, endOfWeek);

        // 주간 점수 - 오늘의 일간 점수 포함
        if (weekScores.isEmpty()) {
            // 주 내 첫 데이터인 경우 일간 점수와 동일하게 설정
            userScore.setWeeklyScore(dailyScore);
        } else {
            // 기존 데이터가 있으면 평균 계산
            // 오늘 데이터를 먼저 삭제 (새로 업데이트된 점수를 사용하기 위해)
            weekScores = weekScores.stream()
                    .filter(score -> !score.getScoreDate().isEqual(today))
                    .collect(Collectors.toList());

            // 기존 데이터의 일간 점수 합계
            int weeklyScoreSum = weekScores.stream()
                    .mapToInt(UserScore::getDailyScore)
                    .sum();

            // 오늘 점수 포함 평균 계산
            int weeklyScore = (weeklyScoreSum + dailyScore) / (weekScores.size() + 1);
            userScore.setWeeklyScore(weeklyScore);
        }

        // 월간 점수 계산
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today;

        // 이번 달의 모든 점수 (오늘 포함)
        List<UserScore> monthScores = userScoreRepository.findByUserUserIdAndScoreDateBetweenOrderByScoreDateDesc(
                user.getUserId(), startOfMonth, endOfMonth);

        // 월간 점수 - 오늘의 일간 점수 포함
        if (monthScores.isEmpty()) {
            // 월 내 첫 데이터인 경우 일간 점수와 동일하게 설정
            userScore.setMonthlyScore(dailyScore);
        } else {
            // 기존 데이터가 있으면 평균 계산
            // 오늘 데이터를 먼저 삭제 (새로 업데이트된 점수를 사용하기 위해)
            monthScores = monthScores.stream()
                    .filter(score -> !score.getScoreDate().isEqual(today))
                    .collect(Collectors.toList());

            // 기존 데이터의 일간 점수 합계
            int monthlyScoreSum = monthScores.stream()
                    .mapToInt(UserScore::getDailyScore)
                    .sum();

            // 오늘 점수 포함 평균 계산
            int monthlyScore = (monthlyScoreSum + dailyScore) / (monthScores.size() + 1);
            userScore.setMonthlyScore(monthlyScore);
        }

        // 최종 저장
        userScoreRepository.save(userScore);
        log.info("Updated user score for custom date - userId: {}, scoreDate: {}, dailyScore: {}, weeklyScore: {}, monthlyScore: {}",
                userId, today, userScore.getDailyScore(), userScore.getWeeklyScore(), userScore.getMonthlyScore());
    }

    // 헬퍼 메서드들

    /**
     * 평균 점수 계산 (0보다 큰 값만 고려)
     */
    private double calculateAverageScore(List<ScoreDto.ScoreData> scoreDataList) {
        return scoreDataList.stream()
                .mapToInt(ScoreDto.ScoreData::getScoreValue)
                .filter(score -> score > 0)
                .average()
                .orElse(0);
    }

    /**
     * 요일별 통계 계산
     */
    private List<ScoreDto.DayOfWeekStat> calculateDayOfWeekStats(List<UserScore> recentScores) {
        Map<DayOfWeek, List<Integer>> dayOfWeekScores = new HashMap<>();

        // 요일별 초기화 (모든 요일에 대해 통계를 보여주기 위해)
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            dayOfWeekScores.put(dayOfWeek, new ArrayList<>());
        }

        for (UserScore score : recentScores) {
            DayOfWeek dayOfWeek = score.getScoreDate().getDayOfWeek();
            if (score.getDailyScore() != null && score.getDailyScore() > 0) {
                dayOfWeekScores.get(dayOfWeek).add(score.getDailyScore());
            }
        }

        List<ScoreDto.DayOfWeekStat> dayOfWeekStats = new ArrayList<>();
        for (Map.Entry<DayOfWeek, List<Integer>> entry : dayOfWeekScores.entrySet()) {
            int averageScore = 0;
            if (!entry.getValue().isEmpty()) {
                double avg = entry.getValue().stream()
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0);
                averageScore = (int) Math.round(avg);
            }

            dayOfWeekStats.add(
                    ScoreDto.DayOfWeekStat.builder()
                            .dayOfWeek(entry.getKey().toString())
                            .averageScore(averageScore)
                            .build()
            );
        }

        return dayOfWeekStats;
    }

    /**
     * 월별 통계 계산 (최근 6개월)
     */
    private List<ScoreDto.MonthlyStat> calculateMonthlyStats(Long userId) {
        List<ScoreDto.MonthlyStat> monthlyStats = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = YearMonth.now().minusMonths(i);
            LocalDate startOfMonth = month.atDay(1);
            LocalDate endOfMonth = month.atEndOfMonth();

            List<UserScore> monthScores = userScoreRepository.findByUserUserIdAndScoreDateBetweenOrderByScoreDateDesc(
                    userId, startOfMonth, endOfMonth);

            int averageScore = 0;
            if (!monthScores.isEmpty()) {
                double avgScore = monthScores.stream()
                        .map(UserScore::getDailyScore)  // 일별 점수로 계산
                        .filter(score -> score != null && score > 0) // null 체크 및 0초과 값만 필터링
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0);
                averageScore = (int) Math.round(avgScore);
            }

            monthlyStats.add(
                    ScoreDto.MonthlyStat.builder()
                            .month(month.getMonth().toString())
                            .year(month.getYear())
                            .averageScore(averageScore)
                            .build()
            );
        }

        return monthlyStats;
    }

    /**
     * 사용자 유효성 검증
     */
    private User validateUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found: " + userId));
    }

    /**
     * 연도와 월 유효성 검증
     */
    private void validateYearMonth(int year, int month) {
        if (year < 2000 || year > 2100) {
            throw new ApiException("INVALID_YEAR", "Year must be between 2000 and 2100");
        }
        if (month < 1 || month > 12) {
            throw new ApiException("INVALID_MONTH", "Month must be between 1 and 12");
        }
    }
}