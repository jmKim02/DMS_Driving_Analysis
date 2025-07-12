package com.sejong.drivinganalysis.service;

import com.sejong.drivinganalysis.dto.FeedbackDto;
import com.sejong.drivinganalysis.entity.AnalysisResult;
import com.sejong.drivinganalysis.entity.Feedback;
import com.sejong.drivinganalysis.entity.User;
import com.sejong.drivinganalysis.entity.enums.FeedbackType;
import com.sejong.drivinganalysis.entity.enums.SeverityLevel;
import com.sejong.drivinganalysis.exception.ApiException;
import com.sejong.drivinganalysis.repository.AnalysisResultRepository;
import com.sejong.drivinganalysis.repository.FeedbackRepository;
import com.sejong.drivinganalysis.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final AnalysisResultRepository analysisResultRepository;

    /**
     * 주행 후 피드백 생성
     */
    @Transactional
    public Feedback generateDrivingFeedback(AnalysisResult analysisResult) {
        User user = analysisResult.getUser();
        int score = analysisResult.getDrivingScore();

        FeedbackType feedbackType = determineMainFeedbackType(analysisResult);
        SeverityLevel severityLevel = determineSeverityLevel(score);

        String content = generateFeedbackContent(analysisResult, feedbackType, severityLevel);

        Feedback feedback = Feedback.createFeedback(user, feedbackType, content, severityLevel);
        return feedbackRepository.save(feedback);
    }

    /**
     * 주 단위 피드백 생성 (최적화된 버전)
     */
    @Transactional
    public List<Feedback> generateWeeklyFeedbacksBatch(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Generating batch weekly feedbacks for {} users", userIds.size());

        // 지난 주 기간 계산 (월요일부터 일요일까지)
        LocalDate today = LocalDate.now();
        LocalDate previousMonday = today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate previousSunday = previousMonday.plusDays(6);

        LocalDateTime startDateTime = previousMonday.atStartOfDay();
        LocalDateTime endDateTime = previousSunday.atTime(LocalTime.MAX);

        // 사용자 정보 한 번에 조회 (N+1 문제 방지)
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getUserId, user -> user));

        // 모든 사용자의 지난 주 분석 결과를 한 번에 조회
        // 쿼리 최적화: IN 절 사용하여 다수의 사용자 결과를 한 번에 가져옴
        List<AnalysisResult> allResults = analysisResultRepository.findByUserUserIdInAndAnalyzedAtBetween(
                userIds, startDateTime, endDateTime);

        // 사용자별로 결과 그룹화
        Map<Long, List<AnalysisResult>> userResultsMap = allResults.stream()
                .collect(Collectors.groupingBy(result -> result.getUser().getUserId()));

        List<Feedback> generatedFeedbacks = new ArrayList<>();

        // 각 사용자별로 피드백 생성
        for (Long userId : userIds) {
            try {
                User user = userMap.get(userId);
                if (user == null) {
                    log.warn("User not found for ID: {}", userId);
                    continue;
                }

                List<AnalysisResult> userResults = userResultsMap.getOrDefault(userId, Collections.emptyList());
                if (userResults.isEmpty()) {
                    log.info("No analysis results found for user {} in the previous week", userId);
                    continue;
                }

                Feedback feedback = generateWeeklyFeedbackForUser(user, userResults, previousMonday, previousSunday);
                if (feedback != null) {
                    generatedFeedbacks.add(feedback);
                }
            } catch (Exception e) {
                log.error("Error generating weekly feedback for userId: {}", userId, e);
            }
        }

        // 생성된 피드백들을 한 번에 저장 (벌크 저장)
        if (!generatedFeedbacks.isEmpty()) {
            generatedFeedbacks = feedbackRepository.saveAll(generatedFeedbacks);
        }

        log.info("Generated {} weekly feedbacks", generatedFeedbacks.size());
        return generatedFeedbacks;
    }


    /**
     * 단일 사용자에 대한 주간 피드백 생성 (내부 메서드)
     */
    private Feedback generateWeeklyFeedbackForUser(User user, List<AnalysisResult> weekResults,
                                                   LocalDate previousMonday, LocalDate previousSunday) {
        // 가장 빈번한 위험 행동 유형 분석
        Map<FeedbackType, Integer> riskBehaviorCounts = analyzeRiskBehaviors(weekResults);
        Map.Entry<FeedbackType, Integer> mostFrequentRisk = findMostFrequentRisk(riskBehaviorCounts);

        if (mostFrequentRisk == null) {
            log.info("No risk behaviors detected for user {} in the previous week", user.getUserId());
            return null;
        }

        // 요일/시간대 패턴 분석
        String timePattern = analyzeTimePattern(weekResults, mostFrequentRisk.getKey());

        String content = generateWeeklyFeedbackContent(
                mostFrequentRisk.getKey(),
                mostFrequentRisk.getValue(),
                timePattern,
                previousMonday,
                previousSunday
        );

        // createWeeklyFeedback 메서드 사용 (수정된 부분)
        return Feedback.createWeeklyFeedback(
                user,
                mostFrequentRisk.getKey(),
                content,
                determineSeverityForWeeklyFeedback(mostFrequentRisk.getValue())
        );
    }

    /**
     * 사용자 피드백 목록 조회
     */
    @Transactional(readOnly = true)
    public FeedbackDto.FeedbackListResponse getUserFeedbacks(Long userId, String type, int page, int size) {
        // 사용자 확인
        if (!userRepository.existsById(userId)) {
            throw new ApiException("USER_NOT_FOUND", "User not found: " + userId);
        }

        // 페이징 및 정렬 설정
        Pageable pageable = PageRequest.of(page, size, Sort.by("generatedAt").descending());

        // 피드백 조회
        Page<Feedback> feedbackPage;
        if (type != null && !type.isEmpty()) {
            try {
                FeedbackType feedbackType = FeedbackType.valueOf(type.toUpperCase());
                feedbackPage = feedbackRepository.findByUserUserIdAndFeedbackType(userId, feedbackType, pageable);
            } catch (IllegalArgumentException e) {
                throw new ApiException("INVALID_FEEDBACK_TYPE", "Invalid feedback type: " + type);
            }
        } else {
            feedbackPage = feedbackRepository.findByUserUserId(userId, pageable);
        }

        // DTO 변환
        List<FeedbackDto.FeedbackResponse> content = feedbackPage.getContent().stream()
                .map(this::convertToFeedbackResponse)
                .collect(Collectors.toList());

        return FeedbackDto.FeedbackListResponse.builder()
                .content(content)
                .totalElements(feedbackPage.getTotalElements())
                .totalPages(feedbackPage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    /**
     * 피드백 상세 조회
     */
    @Transactional(readOnly = true)
    public FeedbackDto.FeedbackDetailResponse getFeedbackDetail(Long userId, Long feedbackId) {
        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found: " + userId));

        // 피드백 존재 확인
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ApiException("FEEDBACK_NOT_FOUND", "Feedback not found: " + feedbackId));

        // 피드백이 해당 사용자의 것인지 확인
        if (!feedback.getUser().getUserId().equals(userId)) {
            throw new ApiException("ACCESS_DENIED", "This feedback does not belong to the specified user");
        }

        // 연관된 분석 결과 조회 (피드백 생성 직후 가장 가까운 분석 결과)
        Optional<AnalysisResult> relatedResult = analysisResultRepository.findByUserUserIdAndAnalyzedAtBetween(
                userId,
                feedback.getGeneratedAt().minus(1, ChronoUnit.HOURS),
                feedback.getGeneratedAt().plus(1, ChronoUnit.MINUTES)
        ).stream().findFirst();

        return FeedbackDto.FeedbackDetailResponse.builder()
                .feedbackId(feedback.getFeedbackId())
                .userId(feedback.getUser().getUserId())
                .feedbackType(feedback.getFeedbackType().toString())
                .content(feedback.getContent())
                .severityLevel(feedback.getSeverityLevel().toString())
                .generatedAt(feedback.getGeneratedAt())
                .videoId(relatedResult.map(r -> r.getVideo().getVideoId()).orElse(null))
                .drivingScore(relatedResult.map(AnalysisResult::getDrivingScore).orElse(null))
                .build();
    }

    /**
     * 주간 피드백 조회
     */
    @Transactional(readOnly = true)
    public FeedbackDto.WeeklyFeedbackResponse getWeeklyFeedback(Long userId) {
        // 사용자 확인
        if (!userRepository.existsById(userId)) {
            throw new ApiException("USER_NOT_FOUND", "User not found: " + userId);
        }

        // 지난 주 기간 계산
        LocalDate today = LocalDate.now();
        LocalDate previousMonday = today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate previousSunday = previousMonday.plusDays(6);

        // 지난 주 분석 결과 조회
        List<AnalysisResult> weekResults = analysisResultRepository.findByUserUserIdAndAnalyzedAtBetween(
                userId,
                previousMonday.atStartOfDay(),
                previousSunday.atTime(LocalTime.MAX)
        );

        if (weekResults.isEmpty()) {
            return FeedbackDto.WeeklyFeedbackResponse.builder()
                    .userId(userId)
                    .mostFrequentRiskBehavior("해당 기간 주행 데이터가 없습니다.")
                    .count(0)
                    .riskBehaviorCounts(new HashMap<>())
                    .riskTimePattern("데이터 없음")
                    .generatedAt(LocalDateTime.now())
                    .build();
        }

        // 위험 행동 분석
        Map<FeedbackType, Integer> riskBehaviorCounts = analyzeRiskBehaviors(weekResults);
        Map.Entry<FeedbackType, Integer> mostFrequentRisk = findMostFrequentRisk(riskBehaviorCounts);

        // 요일/시간대 패턴 분석
        String timePattern = "데이터 없음";
        String mostFrequentRiskName = "위험 행동이 감지되지 않았습니다.";
        int count = 0;

        if (mostFrequentRisk != null) {
            timePattern = analyzeTimePattern(weekResults, mostFrequentRisk.getKey());
            mostFrequentRiskName = getFeedbackTypeName(mostFrequentRisk.getKey());
            count = mostFrequentRisk.getValue();
        }

        Map<String, Integer> riskBehaviorCountsString = new HashMap<>();
        riskBehaviorCounts.forEach((key, value) ->
                riskBehaviorCountsString.put(getFeedbackTypeName(key), value));

        return FeedbackDto.WeeklyFeedbackResponse.builder()
                .userId(userId)
                .mostFrequentRiskBehavior(mostFrequentRiskName)
                .count(count)
                .riskBehaviorCounts(riskBehaviorCountsString)
                .riskTimePattern(timePattern)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 저장된 주간 피드백 조회
     * 스케줄러에 의해 생성된 가장 최근의 주간 피드백을 조회합니다.
     */
    @Transactional(readOnly = true)
    public FeedbackDto.WeeklyFeedbackResponse getStoredWeeklyFeedback(Long userId) {
        // 사용자 확인
        if (!userRepository.existsById(userId)) {
            throw new ApiException("USER_NOT_FOUND", "User not found: " + userId);
        }

        // 가장 최근의 주간 피드백 조회
        Feedback weeklyFeedback = feedbackRepository.findFirstByUserUserIdAndFeedbackCategoryOrderByGeneratedAtDesc(
                userId, "WEEKLY");

        if (weeklyFeedback == null) {
            // 저장된 주간 피드백이 없는 경우
            return FeedbackDto.WeeklyFeedbackResponse.builder()
                    .userId(userId)
                    .mostFrequentRiskBehavior("저장된 주간 피드백이 없습니다.")
                    .count(0)
                    .riskBehaviorCounts(new HashMap<>())
                    .riskTimePattern("데이터 없음")
                    .generatedAt(LocalDateTime.now())
                    .build();
        }

        // 피드백 내용 분석하여 응답 구성
        // (주: 실제 구현에서는 더 견고한 방식으로 데이터를 저장/파싱하는 것이 좋습니다)
        String content = weeklyFeedback.getContent();
        String mostFrequentRiskName = getFeedbackTypeName(weeklyFeedback.getFeedbackType());

        // 내용에서 위험 행동 횟수 추출 (예: "총 5회 감지되었습니다"에서 숫자 추출)
        int count = 0;
        try {
            String countPattern = "총 (\\d+)회 감지";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(countPattern);
            java.util.regex.Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                count = Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            log.warn("Failed to extract count from weekly feedback content", e);
        }

        // 시간 패턴 추출 (예: "이 행동은 월요일 오전(9시경)에 가장 빈번하게 발생했습니다.")
        String timePattern = "데이터 없음";
        try {
            String timePatternRegex = "이 행동은 ([^.]+)에 가장 빈번하게 발생";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(timePatternRegex);
            java.util.regex.Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                timePattern = matcher.group(1);
            }
        } catch (Exception e) {
            log.warn("Failed to extract time pattern from weekly feedback content", e);
        }

        // 단순화된 위험 행동 카운트 맵 (실제로는 더 정확한 데이터가 필요할 수 있음)
        Map<String, Integer> riskBehaviorCounts = new HashMap<>();
        riskBehaviorCounts.put(mostFrequentRiskName, count);

        return FeedbackDto.WeeklyFeedbackResponse.builder()
                .userId(userId)
                .mostFrequentRiskBehavior(mostFrequentRiskName)
                .count(count)
                .riskBehaviorCounts(riskBehaviorCounts)
                .riskTimePattern(timePattern)
                .generatedAt(weeklyFeedback.getGeneratedAt())
                .build();
    }

    // =============== 헬퍼 메서드 ===============

    /**
     * 주요 피드백 유형 결정
     */
    private FeedbackType determineMainFeedbackType(AnalysisResult result) {
        // 가장 빈번하거나 심각한 위험 행동 유형 결정
        if (result.getDrowsinessCount() > 0) {
            return FeedbackType.DROWSINESS;
        } else if (result.getPhoneUsageCount() > 0) {
            return FeedbackType.PHONE_USAGE;
        } else if (result.getSmokingCount() > 0) {
            return FeedbackType.SMOKING;
        } else {
            return FeedbackType.GENERAL;
        }
    }

    /**
     * 점수 기반 심각도 결정
     */
    private SeverityLevel determineSeverityLevel(int score) {
        if (score >= 90) {
            return SeverityLevel.LOW;
        } else if (score >= 75) {
            return SeverityLevel.LOW;
        } else if (score >= 60) {
            return SeverityLevel.MEDIUM;
        } else {
            return SeverityLevel.HIGH;
        }
    }

    /**
     * 주행 피드백 내용 생성
     */
    private String generateFeedbackContent(AnalysisResult result, FeedbackType feedbackType, SeverityLevel severityLevel) {
        int score = result.getDrivingScore();
        StringBuilder feedback = new StringBuilder();

        // 점수 구간별 피드백
        if (score >= 90) {
            feedback.append("안전 운전을 실천하고 계십니다! ");

            if (feedbackType == FeedbackType.GENERAL) {
                feedback.append("안전 운전 습관을 계속 유지해주세요.");
            } else {
                feedback.append("다만, ");
                appendRiskBehaviorFeedback(feedback, feedbackType, result, false);
            }
        } else if (score >= 75) {
            feedback.append("대체로 안전하게 운전하고 계십니다. ");
            appendRiskBehaviorFeedback(feedback, feedbackType, result, true);
        } else if (score >= 60) {
            feedback.append("주의가 필요한 운전 습관이 감지되었습니다. ");
            appendRiskBehaviorFeedback(feedback, feedbackType, result, true);
        } else {
            feedback.append("위험한 운전 습관이 다수 감지되었습니다. ");
            appendRiskBehaviorFeedback(feedback, feedbackType, result, true);
            feedback.append(" 안전 운전에 각별한 주의를 기울여 주세요.");
        }

        return feedback.toString();
    }

    /**
     * 위험 행동별 세부 피드백 추가
     */
    private void appendRiskBehaviorFeedback(StringBuilder feedback, FeedbackType feedbackType, AnalysisResult result, boolean isMainFeedback) {
        switch (feedbackType) {
            case DROWSINESS:
                if (isMainFeedback) {
                    feedback.append("졸음운전이 ").append(result.getDrowsinessCount()).append("회 감지되었습니다. ");
                    feedback.append("장거리 운전 시 2시간마다 휴식을 취하고, 충분한 수면 후 운전하세요.");
                } else {
                    feedback.append("졸음운전이 감지되었으니 주의하세요.");
                }
                break;
            case PHONE_USAGE:
                if (isMainFeedback) {
                    feedback.append("운전 중 휴대폰 사용이 ").append(result.getPhoneUsageCount()).append("회 감지되었습니다. ");
                    feedback.append("운전 중 휴대폰 사용은 사고 위험을 크게 높입니다.");
                } else {
                    feedback.append("운전 중 휴대폰 사용은 매우 위험합니다.");
                }
                break;
            case SMOKING:
                if (isMainFeedback) {
                    feedback.append("운전 중 흡연이 ").append(result.getSmokingCount()).append("회 감지되었습니다. ");
                    feedback.append("흡연은 주의력을 분산시키고 화재 위험을 증가시킵니다.");
                } else {
                    feedback.append("운전 중 흡연은 주의력 분산의 원인이 됩니다.");
                }
                break;
            case GENERAL:
                feedback.append("전반적인 운전 습관을 점검해보세요.");
                break;
        }
    }

    /**
     * 피드백 DTO 변환
     */
    private FeedbackDto.FeedbackResponse convertToFeedbackResponse(Feedback feedback) {
        return FeedbackDto.FeedbackResponse.builder()
                .feedbackId(feedback.getFeedbackId())
                .userId(feedback.getUser().getUserId())
                .feedbackType(feedback.getFeedbackType().toString())
                .content(feedback.getContent())
                .severityLevel(feedback.getSeverityLevel().toString())
                .generatedAt(feedback.getGeneratedAt())
                .build();
    }

    /**
     * 위험 행동 분석
     */
    private Map<FeedbackType, Integer> analyzeRiskBehaviors(List<AnalysisResult> results) {
        // 입력 검증 추가
        if (results == null || results.isEmpty()) {
            return new EnumMap<>(FeedbackType.class); // 빈 맵 반환
        }

        EnumMap<FeedbackType, Integer> riskCounts = new EnumMap<>(FeedbackType.class);
        riskCounts.put(FeedbackType.DROWSINESS, 0);
        riskCounts.put(FeedbackType.PHONE_USAGE, 0);
        riskCounts.put(FeedbackType.SMOKING, 0);

        for (AnalysisResult result : results) {

            if (result == null) continue;

            riskCounts.computeIfPresent(FeedbackType.DROWSINESS,
                    (key, value) -> value + getCountSafely(result.getDrowsinessCount()));
            riskCounts.computeIfPresent(FeedbackType.PHONE_USAGE,
                    (key, value) -> value + getCountSafely(result.getPhoneUsageCount()));
            riskCounts.computeIfPresent(FeedbackType.SMOKING,
                    (key, value) -> value + getCountSafely(result.getSmokingCount()));
        }

        // 0인 항목 제거
        riskCounts.entrySet().removeIf(entry -> entry.getValue() == 0);

        return riskCounts;
    }

    private int getCountSafely(Integer count) {
        return count != null ? count : 0;
    }

    /**
     * 가장 빈번한 위험 행동 찾기
     */
    private Map.Entry<FeedbackType, Integer> findMostFrequentRisk(Map<FeedbackType, Integer> riskCounts) {
        if (riskCounts.isEmpty()) {
            return null;
        }

        return riskCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
    }

    /**
     * 최적화된 시간 패턴 분석 메서드
     */
    private String analyzeTimePattern(List<AnalysisResult> results, FeedbackType riskType) {
        // 입력 검증
        if (results == null || results.isEmpty() || riskType == null) {
            return "패턴이 명확하지 않습니다.";
        }

        try {
            // 스트림을 사용하여 한 번의 순회로 필터링 및 카운팅
            Map<DayOfWeek, Long> dayOfWeekCounts = results.stream()
                    .filter(result -> result != null && result.getAnalyzedAt() != null)
                    .filter(result -> getRiskTypeCount(result, riskType) > 0)
                    .collect(Collectors.groupingBy(
                            result -> result.getAnalyzedAt().getDayOfWeek(),
                            Collectors.summingLong(result -> getRiskTypeCount(result, riskType))
                    ));

            Map<Integer, Long> hourOfDayCounts = results.stream()
                    .filter(result -> result != null && result.getAnalyzedAt() != null)
                    .filter(result -> getRiskTypeCount(result, riskType) > 0)
                    .collect(Collectors.groupingBy(
                            result -> result.getAnalyzedAt().getHour(),
                            Collectors.summingLong(result -> getRiskTypeCount(result, riskType))
                    ));

            if (dayOfWeekCounts.isEmpty() || hourOfDayCounts.isEmpty()) {
                return "패턴이 명확하지 않습니다.";
            }

            // 최대값 찾기
            DayOfWeek mostFrequentDay = dayOfWeekCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            Integer mostFrequentHour = hourOfDayCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (mostFrequentDay == null || mostFrequentHour == null) {
                return "패턴이 명확하지 않습니다.";
            }

            String dayName = getDayOfWeekName(mostFrequentDay);
            String timeOfDay = getTimeOfDayName(mostFrequentHour);

            return dayName + " " + timeOfDay + "에 가장 빈번하게 발생했습니다.";
        } catch (Exception e) {
            log.error("시간 패턴 분석 중 오류 발생", e);
            return "패턴을 분석하는 중 오류가 발생했습니다.";
        }
    }

    /**
     * 위험 행동 타입별 카운트 추출 (캐싱 고려)
     */
    private int getRiskTypeCount(AnalysisResult result, FeedbackType riskType) {
        if (result == null) return 0;

        switch (riskType) {
            case DROWSINESS:
                return result.getDrowsinessCount() != null ? result.getDrowsinessCount() : 0;
            case PHONE_USAGE:
                return result.getPhoneUsageCount() != null ? result.getPhoneUsageCount() : 0;
            case SMOKING:
                return result.getSmokingCount() != null ? result.getSmokingCount() : 0;
            default:
                return 0;
        }
    }

    /**
     * 주간 피드백 심각도 결정
     */
    private SeverityLevel determineSeverityForWeeklyFeedback(int count) {
        if (count <= 2) {
            return SeverityLevel.LOW;
        } else if (count <= 5) {
            return SeverityLevel.MEDIUM;
        } else {
            return SeverityLevel.HIGH;
        }
    }

    /**
     * 주간 피드백 내용 생성
     */
    private String generateWeeklyFeedbackContent(FeedbackType riskType, int count, String timePattern,
                                                 LocalDate startDate, LocalDate endDate) {
        StringBuilder content = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM월 dd일");

        content.append(startDate.format(formatter))
                .append("부터 ")
                .append(endDate.format(formatter))
                .append("까지의 주간 운전 분석 결과입니다. ");

        String behaviorName = getFeedbackTypeName(riskType);

        content.append("가장 빈번하게 감지된 위험 행동은 ")
                .append(behaviorName)
                .append("으로, 총 ")
                .append(count)
                .append("회 감지되었습니다. ");

        content.append("이 행동은 ").append(timePattern).append(" ");

        // 위험 행동별 맞춤 조언
        switch (riskType) {
            case DROWSINESS:
                content.append("졸음운전은 치명적인 사고의 주요 원인입니다. ")
                        .append("충분한 휴식을 취하고, 장시간 운전 시 정기적으로 휴식을 취하세요.");
                break;
            case PHONE_USAGE:
                content.append("운전 중 휴대폰 사용은 주의력을 크게 저하시킵니다. ")
                        .append("운전 전 휴대폰을 무음 모드로 전환하거나 운전 모드를 활성화하세요.");
                break;
            case SMOKING:
                content.append("운전 중 흡연은 한 손으로 운전하게 되고 화재 위험도 있습니다. ")
                        .append("목적지에 도착한 후 흡연하는 습관을 들이세요.");
                break;
            default:
                content.append("안전 운전을 위해 주의를 기울이세요.");
        }

        return content.toString();
    }

    /**
     * 가장 빈번한 키 찾기
     */
    private <T> T findMostFrequentKey(Map<T, Integer> countMap) {
        // 입력 검증
        if (countMap == null || countMap.isEmpty()) {
            return null;
        }

        try {
            return countMap.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
        } catch (Exception e) {
            // 예외 발생 시 로깅 및 null 반환
            log.error("최빈값 계산 중 오류 발생", e);
            return null;
        }
    }

    /**
     * 요일 이름 변환
     */
    private String getDayOfWeekName(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return "월요일";
            case TUESDAY: return "화요일";
            case WEDNESDAY: return "수요일";
            case THURSDAY: return "목요일";
            case FRIDAY: return "금요일";
            case SATURDAY: return "토요일";
            case SUNDAY: return "일요일";
            default: return "알 수 없음";
        }
    }

    /**
     * 시간대 이름 변환
     */
    private String getTimeOfDayName(int hour) {
        if (hour >= 5 && hour < 12) {
            return "오전(" + hour + "시경)";
        } else if (hour >= 12 && hour < 18) {
            return "오후(" + hour + "시경)";
        } else if (hour >= 18 && hour < 22) {
            return "저녁(" + hour + "시경)";
        } else {
            return "심야(" + hour + "시경)";
        }
    }

    /**
     * 피드백 유형 이름 반환
     */
    private String getFeedbackTypeName(FeedbackType type) {
        switch (type) {
            case DROWSINESS: return "졸음운전";
            case PHONE_USAGE: return "휴대폰 사용";
            case SMOKING: return "흡연";
            case GENERAL: return "일반적인 운전 습관";
            default: return "알 수 없음";
        }
    }
}