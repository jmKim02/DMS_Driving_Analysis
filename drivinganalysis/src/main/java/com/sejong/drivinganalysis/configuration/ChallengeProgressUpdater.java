package com.sejong.drivinganalysis.configuration;

import com.sejong.drivinganalysis.entity.AnalysisResult;
import com.sejong.drivinganalysis.service.UserChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChallengeProgressUpdater {

    private final UserChallengeService userChallengeService;

    public void updateFrom(AnalysisResult result) {
        Long userId = result.getUser().getUserId();

        userChallengeService.updateProgressByMetric(userId, "drowsiness_count", toLong(result.getDrowsinessCount()));
        userChallengeService.updateProgressByMetric(userId, "phone_usage_count", toLong(result.getPhoneUsageCount()));
        userChallengeService.updateProgressByMetric(userId, "smoking_count", toLong(result.getSmokingCount()));
        userChallengeService.updateProgressByMetric(userId, "driving_score", toLong(result.getDrivingScore()));
    }

    private Long toLong(Integer val) {
        return val != null ? val.longValue() : 0L;
    }
}
