package com.sejong.drivinganalysis.service;

import com.sejong.drivinganalysis.dto.challengedto.UserChallengeCreateRequest;
import com.sejong.drivinganalysis.dto.challengedto.UserChallengeJoinRequest;
import com.sejong.drivinganalysis.dto.challengedto.UserChallengeResponse;
import com.sejong.drivinganalysis.dto.ScoreDto.ScoreResponse;
import com.sejong.drivinganalysis.entity.Challenge;
import com.sejong.drivinganalysis.entity.User;
import com.sejong.drivinganalysis.entity.UserChallenge;
import com.sejong.drivinganalysis.entity.enums.ChallengesStatus;
import com.sejong.drivinganalysis.repository.ChallengeRepository;
import com.sejong.drivinganalysis.repository.UserChallengeRepository;
import com.sejong.drivinganalysis.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserChallengeServiceImpl implements UserChallengeService {

    private final UserRepository userRepository;
    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final UserScoreService userScoreService;

    @Override
    @Transactional
    public UserChallenge joinCommonChallenge(UserChallengeJoinRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다. ID=" + request.getUserId()));
        Challenge ch = challengeRepository.findById(request.getChallengeId())
                .orElseThrow(() -> new EntityNotFoundException("챌린지를 찾을 수 없습니다. ID=" + request.getChallengeId()));

        boolean exists = userChallengeRepository
                .existsByUser_UserIdAndChallenge_ChallengeIdAndStartDate(user.getUserId(), ch.getChallengeId(), ch.getStartDate());
        if (exists) {
            throw new IllegalArgumentException("이미 참여한 챌린지입니다.");
        }

        return userChallengeRepository.save(UserChallenge.fromChallenge(user, ch));
    }

    @Override
    @Transactional
    public UserChallenge createCustomChallenge(UserChallengeCreateRequest request) {
        return createUserChallenge(request);
    }

    @Override
    @Transactional
    public UserChallenge createUserChallenge(UserChallengeCreateRequest req) {
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다. ID=" + req.getUserId()));

        if (req.getChallengeId() != null) {
            Challenge ch = challengeRepository.findById(req.getChallengeId())
                    .orElseThrow(() -> new EntityNotFoundException("챌린지를 찾을 수 없습니다. ID=" + req.getChallengeId()));
            if (userChallengeRepository.existsByUser_UserIdAndChallenge_ChallengeIdAndStartDate(
                    user.getUserId(), ch.getChallengeId(), req.getStartDate())) {
                throw new IllegalArgumentException("이미 참여한 공통 챌린지입니다.");
            }
            return userChallengeRepository.save(UserChallenge.fromChallenge(user, ch));
        }

        if (userChallengeRepository.existsByUser_UserIdAndTargetMetricAndStartDate(
                user.getUserId(), req.getTargetMetric(), req.getStartDate())) {
            throw new IllegalArgumentException("이미 생성된 개인 챌린지입니다.");
        }
        if (req.getTargetValue() < 0) {
            throw new IllegalArgumentException("targetValue는 0 이상이어야 합니다.");
        }

        UserChallenge custom = UserChallenge.createCustom(
                user,
                req.getTitle(),
                req.getTargetMetric(),
                req.getTargetValue(),
                req.getComparator(),
                req.getRewardInfo(),
                req.getStartDate(),
                req.getEndDate()
        );
        return userChallengeRepository.save(custom);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserChallengeResponse> getUserChallengeResponsesWithDisplayValue(Long userId) {
        return userChallengeRepository.findByUser_UserId(userId).stream()
                .map(uc -> UserChallengeResponse.fromEntity(uc, calculateDisplayValue(uc)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserChallengeResponse> getAvailableUserChallenges(Long userId) {
        LocalDate today = LocalDate.now();

        List<Challenge> candidates = challengeRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(today, today);

        Set<Long> joinedIds = userChallengeRepository.findByUser_UserId(userId).stream()
                .map(uc -> uc.getChallenge() != null ? uc.getChallenge().getChallengeId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return candidates.stream()
                .filter(ch -> !joinedIds.contains(ch.getChallengeId()))
                .map(UserChallengeResponse::fromChallengeOnly)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserChallenge updateProgress(Long userChallengeId, Long newValue) {
        UserChallenge uc = userChallengeRepository.findById(userChallengeId)
                .orElseThrow(() -> new EntityNotFoundException("UserChallenge를 찾을 수 없습니다. ID=" + userChallengeId));

        long updated = (uc.getCurrentValue() != null ? uc.getCurrentValue() : 0L) + newValue;
        uc.setCurrentValue(updated);

        String cmp = uc.getComparator();
        if (cmp != null) {
            boolean done = switch (cmp) {
                case ">=" -> updated >= uc.getTargetValue();
                case "<=" -> updated <= uc.getTargetValue();
                default -> false;
            };
            if (done && uc.getStatus() != ChallengesStatus.COMPLETED) {
                uc.setStatus(ChallengesStatus.COMPLETED);
                uc.setCompletedAt(LocalDateTime.now());
            }
        }
        return uc;
    }

    @Override
    @Transactional
    public void updateProgressByMetric(Long userId, String targetMetric, Long value) {
        List<UserChallenge> list = userChallengeRepository
                .findByUser_UserIdAndTargetMetricAndStatus(userId, targetMetric, ChallengesStatus.IN_PROGRESS);

        for (UserChallenge uc : list) {
            if ("driving_score".equals(targetMetric)) {
                continue;
            }

            long curr = uc.getCurrentValue() != null ? uc.getCurrentValue() : 0L;
            long updated = curr + value;
            uc.setCurrentValue(updated);

            String cmp = uc.getComparator();
            if (cmp == null) continue;

            boolean done = switch (cmp) {
                case ">=" -> updated >= uc.getTargetValue();
                case "<=" -> updated <= uc.getTargetValue();
                default -> false;
            };
            if (done) {
                uc.setStatus(ChallengesStatus.COMPLETED);
                uc.setCompletedAt(LocalDateTime.now());
            }
        }
    }

    @Override
    @Transactional
    public void evaluateChallenges() {
        LocalDate today = LocalDate.now();
        DayOfWeek todayDay = today.getDayOfWeek();

        List<UserChallenge> expired = userChallengeRepository
                .findByStatusAndEndDateBefore(ChallengesStatus.IN_PROGRESS, today);

        for (UserChallenge uc : expired) {
            String metric = uc.getTargetMetric();
            long curr = uc.getCurrentValue() != null ? uc.getCurrentValue() : 0L;
            long target = uc.getTargetValue();
            String cmp = uc.getComparator();

            boolean isDrivingScore = "driving_score".equals(metric);

            if (isDrivingScore) {
                if (todayDay != DayOfWeek.MONDAY) continue;

                ScoreResponse resp = userScoreService.getUserScores(
                        uc.getUser().getUserId(),
                        "weekly", null, null, null, null, null);

                Integer avgScore = resp.getAverageScore();
                if (avgScore == null) continue;
                curr = avgScore;
            }

            boolean success = switch (cmp) {
                case ">=" -> curr >= target;
                case "<=" -> curr <= target;
                default -> false;
            };

            uc.setStatus(success ? ChallengesStatus.COMPLETED : ChallengesStatus.FAILED);
            if (success && Boolean.FALSE.equals(uc.getRewardGiven())) {
                uc.setRewardGiven(true);
            }
        }
    }

    @Override
    @Transactional
    public void createWeeklyPersonalChallengesForUser(User user) {
        ScoreResponse resp = userScoreService.getUserScores(
                user.getUserId(), "weekly", null, null, null, null, null);

        Integer avg = resp.getAverageScore();
        if (avg != null && avg > 0) {
            long target = Math.min(avg + 10L, 100L);
            LocalDate start = LocalDate.now().with(DayOfWeek.MONDAY);
            LocalDate end = start.plusDays(6);

            if (!userChallengeRepository.existsByUser_UserIdAndTargetMetricAndStartDate(
                    user.getUserId(), "driving_score", start)) {
                UserChallenge uc = UserChallenge.createCustom(
                        user, "주간 점수 향상 챌린지", "driving_score", target, ">=",
                        "이번 주 드라이브 마스터", start, end);
                userChallengeRepository.save(uc);
            }
        }

        Map<String, Long> metrics = new HashMap<>();
        Optional.ofNullable(resp.getSmokingCount()).ifPresent(v -> metrics.put("smoking_count", v));
        Optional.ofNullable(resp.getDrowsinessCount()).ifPresent(v -> metrics.put("drowsiness_count", v));
        Optional.ofNullable(resp.getPhoneUsageCount()).ifPresent(v -> metrics.put("phone_usage_count", v));

        String maxMetric = null;
        long maxValue = -1;
        for (var e : metrics.entrySet()) {
            if (e.getValue() > maxValue) {
                maxMetric = e.getKey();
                maxValue = e.getValue();
            }
        }

        if (maxMetric != null) {
            final int CHALLENGE_THRESHOLD = 10;
            final int SUSPICIOUS_THRESHOLD = 100;

            if (maxValue >= CHALLENGE_THRESHOLD && maxValue < SUSPICIOUS_THRESHOLD) {
                long target = maxValue / 2;
                LocalDate start = LocalDate.now();
                LocalDate end = start.plusDays(6);

                if (!userChallengeRepository.existsByUser_UserIdAndTargetMetricAndStartDate(
                        user.getUserId(), maxMetric, start)) {

                    String title = switch (maxMetric) {
                        case "smoking_count"     -> "흡연 줄이기 챌린지";
                        case "drowsiness_count"  -> "졸음 줄이기 챌린지";
                        case "phone_usage_count" -> "폰 사용 줄이기 챌린지";
                        default -> "위험 행동 줄이기";
                    };

                    UserChallenge uc = UserChallenge.createCustom(
                            user, title, maxMetric, target, "<=",
                            "개인 맞춤 리워드", start, end);
                    userChallengeRepository.save(uc);
                }
            } else if (maxValue >= SUSPICIOUS_THRESHOLD) {
                log.warn("⚠️ 사용자 {}의 {} 수치가 {}회 이상입니다. 의심 계정 가능성.",
                        user.getUserId(), maxMetric, maxValue);
            }
        }
    }

    @Override
    public Long calculateDisplayValue(UserChallenge uc) {
        if (!"driving_score".equals(uc.getTargetMetric())) {
            return uc.getCurrentValue();
        }
        ScoreResponse resp = userScoreService.getUserScores(
                uc.getUser().getUserId(), "weekly", null, null, null, null, null);
        return Optional.ofNullable(resp.getAverageScore()).map(Long::valueOf).orElse(0L);
    }

    @Override
    @Transactional(readOnly = true)
    public UserChallengeResponse getUserChallengeDetail(Long userChallengeId) {
        UserChallenge uc = userChallengeRepository.findById(userChallengeId)
                .orElseThrow(() -> new EntityNotFoundException("UserChallenge가 없습니다. ID=" + userChallengeId));
        Long displayValue = calculateDisplayValue(uc);
        return UserChallengeResponse.fromEntity(uc, displayValue);
    }

}
