package com.sejong.drivinganalysis.controller;

import com.sejong.drivinganalysis.dto.FeedbackDto;
import com.sejong.drivinganalysis.dto.common.ApiResponse;
import com.sejong.drivinganalysis.exception.ApiException;
import com.sejong.drivinganalysis.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class FeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping("/users/{userId}/feedbacks")
    public ResponseEntity<ApiResponse<FeedbackDto.FeedbackListResponse>> getUserFeedbacks(
            @PathVariable Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching feedbacks for userId: {}, type: {}, page: {}, size: {}", userId, type, page, size);

        // 권한 검증
        if (!isAuthorizedUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "해당 사용자의 피드백에 접근할 권한이 없습니다."));
        }

        FeedbackDto.FeedbackListResponse feedbacks = feedbackService.getUserFeedbacks(userId, type, page, size);
        return ResponseEntity.ok(ApiResponse.success(feedbacks));
    }

    @GetMapping("/users/{userId}/feedbacks/{feedbackId}")
    public ResponseEntity<ApiResponse<FeedbackDto.FeedbackDetailResponse>> getFeedbackDetail(
            @PathVariable Long userId,
            @PathVariable Long feedbackId) {

        log.info("Fetching feedback detail for userId: {}, feedbackId: {}", userId, feedbackId);

        try {
            // 파라미터 유효성 검증
            if (feedbackId == null || feedbackId <= 0) {
                log.warn("Invalid feedback ID: {}", feedbackId);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("INVALID_FEEDBACK_ID", "유효하지 않은 피드백 ID입니다."));
            }

            // 권한 검증
            if (!isAuthorizedUser(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("ACCESS_DENIED", "해당 사용자의 피드백에 접근할 권한이 없습니다."));
            }

            FeedbackDto.FeedbackDetailResponse feedback = feedbackService.getFeedbackDetail(userId, feedbackId);
            return ResponseEntity.ok(ApiResponse.success(feedback));
        } catch (ApiException e) {
            // 이미 처리된 API 예외
            log.warn("API Exception while fetching feedback detail for userId: {}, feedbackId: {}: {}",
                    userId, feedbackId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            // 예상치 못한 오류
            log.error("Unexpected error while fetching feedback detail for userId: {}, feedbackId: {}",
                    userId, feedbackId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "서버 내부 오류가 발생했습니다."));
        }
    }

    @GetMapping("/users/{userId}/weekly-feedback")
    public ResponseEntity<ApiResponse<FeedbackDto.WeeklyFeedbackResponse>> getWeeklyFeedback(
            @PathVariable Long userId) {

        log.info("Fetching weekly feedback for userId: {}", userId);

        // 권한 검증
        if (!isAuthorizedUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "해당 사용자의 피드백에 접근할 권한이 없습니다."));
        }

        FeedbackDto.WeeklyFeedbackResponse weeklyFeedback = feedbackService.getWeeklyFeedback(userId);
        return ResponseEntity.ok(ApiResponse.success(weeklyFeedback));
    }

    /**
     * 저장된 주간 피드백 조회
     * 스케줄러에 의해 생성된 주간 피드백을 조회합니다.
     */
    @GetMapping("/users/{userId}/stored-weekly-feedback")
    public ResponseEntity<ApiResponse<FeedbackDto.WeeklyFeedbackResponse>> getStoredWeeklyFeedback(
            @PathVariable Long userId) {

        log.info("Fetching stored weekly feedback for userId: {}", userId);

        // 권한 검증
        if (!isAuthorizedUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("ACCESS_DENIED", "해당 사용자의 피드백에 접근할 권한이 없습니다."));
        }

        FeedbackDto.WeeklyFeedbackResponse weeklyFeedback = feedbackService.getStoredWeeklyFeedback(userId);
        return ResponseEntity.ok(ApiResponse.success(weeklyFeedback));
    }

    /**
     * 현재 인증된 사용자가 요청한 사용자 ID에 접근 권한이 있는지 확인
     */
    private boolean isAuthorizedUser(Long userId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return false;
            }

            // 현재 사용자의 ID 가져오기 (실제 구현은 SecurityConfig와 인증 방식에 따라 다를 수 있음)
            String username = authentication.getName();
            // 여기서는 간단한 예시로, username이 "driver"+userId와 일치하는지 확인
            // 실제로는 UserRepository를 주입받아 username으로 userId를 조회하는 등의 방식으로 구현
            return username.equals("driver" + userId) || "ROLE_ADMIN".equals(authentication.getAuthorities().toString());

            // 또는 JWT 토큰에서 userId를 추출하는 방식
            // JwtTokenProvider를 주입받아 사용
            // return jwtTokenProvider.getUserId(token).equals(userId);
        } catch (Exception e) {
            log.error("Error during authorization check", e);
            return false;
        }
    }
}