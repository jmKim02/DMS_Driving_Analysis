package com.sejong.drivinganalysis.service;

import com.sejong.drivinganalysis.dto.VideoDto;
import com.sejong.drivinganalysis.entity.AnalysisResult;
import com.sejong.drivinganalysis.entity.DrivingVideo;
import com.sejong.drivinganalysis.entity.Feedback;
import com.sejong.drivinganalysis.entity.User;
import com.sejong.drivinganalysis.entity.enums.VideoStatus;
import com.sejong.drivinganalysis.exception.ApiException;
import com.sejong.drivinganalysis.grpc.FinalAnalysisResponse;
import com.sejong.drivinganalysis.grpc.RealtimeAnalysisResponse;
import com.sejong.drivinganalysis.repository.AnalysisResultRepository;
import com.sejong.drivinganalysis.repository.DrivingVideoRepository;
import com.sejong.drivinganalysis.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * 영상 처리 및 분석 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final UserRepository userRepository;
    private final DrivingVideoRepository drivingVideoRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final GrpcClientService grpcClientService;
    private final AlertService alertService;
    private final UserScoreService userScoreService;
    private final FeedbackService feedbackService;

    // 세션별 시작 타임스탬프와 마지막 프레임 타임스탬프 관리
    private final Map<Long, Long> userSessionStartTimes = new ConcurrentHashMap<>();
    private final Map<Long, Long> userLastFrameTimestamps = new ConcurrentHashMap<>();

    /**
     * WebSocket을 통해 수신된 프레임 배치를 처리하고 AI 분석 요청을 보냄
     * 메모리에서 즉시 처리하고 실시간 알림만 처리
     */
    public VideoDto.FrameProcessedResponse processFrameBatch(VideoDto.FrameBatchRequest frameBatch) {
        try {
            // 사용자 존재 확인
            User user = userRepository.findById(frameBatch.getUserId())
                    .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found: " + frameBatch.getUserId()));

            log.info("Processing frame batch for user: {}, batchId: {}, frames count: {}",
                    user.getUsername(), frameBatch.getBatchId(), frameBatch.getFrames().size());

            // 세션 시작 시간 기록 (첫 프레임인 경우)
            if (frameBatch.getBatchId() == 0 || !userSessionStartTimes.containsKey(user.getUserId())) {
                userSessionStartTimes.put(user.getUserId(), frameBatch.getTimestamp());
                log.info("New driving session started for userId: {}", user.getUserId());
            }

            // 마지막 프레임 타임스탬프 업데이트
            userLastFrameTimestamps.put(user.getUserId(), frameBatch.getTimestamp());

            // 프레임 데이터 검증 로그 추가
            boolean hasEmptyFrames = false;
            boolean hasInvalidFrames = false;
            int emptyFrameCount = 0;

            for (int i = 0; i < frameBatch.getFrames().size(); i++) {
                VideoDto.FrameData frame = frameBatch.getFrames().get(i);
                if (frame.getData() == null || frame.getData().length == 0) {
                    hasEmptyFrames = true;
                    emptyFrameCount++;
                    log.warn("Empty frame detected in batch - userId: {}, batchId: {}, frameIndex: {}, frameId: {}",
                            user.getUserId(), frameBatch.getBatchId(), i, frame.getFrameId());
                } else if (frame.getData().length < 100) { // 최소 프레임 크기 검증 (예: 100바이트 미만인 경우)
                    hasInvalidFrames = true;
                    log.warn("Suspiciously small frame detected - userId: {}, batchId: {}, frameIndex: {}, frameId: {}, size: {} bytes",
                            user.getUserId(), frameBatch.getBatchId(), i, frame.getFrameId(), frame.getData().length);
                }
            }

            if (hasEmptyFrames || hasInvalidFrames) {
                log.warn("Frame quality issues detected - userId: {}, batchId: {}, emptyFrames: {}, totalFrames: {}",
                        user.getUserId(), frameBatch.getBatchId(), emptyFrameCount, frameBatch.getFrames().size());
            }

            // 비동기로 AI 분석 요청 - 실시간 졸음 감지를 위한 간소화된 분석
            CompletableFuture.runAsync(() -> {
                try {
                    RealtimeAnalysisResponse response = grpcClientService.analyzeFrames(
                            user.getUserId(),
                            frameBatch.getBatchId(),
                            frameBatch.getTimestamp(),
                            frameBatch.getFrames()
                    );

                    // 분석 실패 시 로깅만 처리
                    if (!response.getAnalysisCompleted() && response.getErrorMessage() != null) {
                        log.warn("Frame analysis incomplete for userId: {}, error: {}",
                                user.getUserId(), response.getErrorMessage());
                        return;
                    }

                    // 졸음 감지 시에만 알림 전송
                    if (response.getDrowsinessDetected()) {
                        log.info("Drowsiness detected for userId: {}, sending alert", user.getUserId());
                        alertService.sendDrowsinessAlert(user.getUserId(), true);
                    }
                } catch (Exception e) {
                    log.error("Error in async frame analysis for userId: {}", user.getUserId(), e);
                }
            }).exceptionally(ex -> {
                log.error("Uncaught exception in frame analysis for userId: {}", user.getUserId(), ex);
                return null;
            });

            // 응답 반환
            return VideoDto.FrameProcessedResponse.builder()
                    .userId(frameBatch.getUserId())
                    .batchId(frameBatch.getBatchId())
                    .timestamp(frameBatch.getTimestamp())
                    .processed(true)
                    .build();

        } catch (Exception e) {
            log.error("Error processing frame batch", e);
            throw new ApiException("FRAME_PROCESSING_ERROR", "Failed to process frame batch: " + e.getMessage());
        }
    }

    /**
     * 주행 종료 시 호출되는 메서드
     * AI 서버로부터 최종 통계를 받아 DB에 저장
     */
    @Transactional
    public VideoDto.DrivingSessionEndResponse endDrivingSession(VideoDto.DrivingSessionEndRequest request) {
        try {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found: " + request.getUserId()));

            // 세션 시작 타임스탬프 및 마지막 프레임 타임스탬프 확인
            Long startTimestamp = userSessionStartTimes.getOrDefault(request.getUserId(), 0L);
            Long lastFrameTimestamp = userLastFrameTimestamps.getOrDefault(request.getUserId(), startTimestamp);

            log.info("Ending driving session for user: {}, sessionId: {}", user.getUsername(), request.getSessionId());

            // AI 서버에 주행 종료 요청 및 최종 분석 결과 수신
            FinalAnalysisResponse finalAnalysis = grpcClientService.endDrivingSession(
                    user.getUserId(),
                    request.getSessionId(),
                    startTimestamp,
                    request.getEndTimestamp()
            );

            // 세션 정보 정리
            userSessionStartTimes.remove(request.getUserId());
            userLastFrameTimestamps.remove(request.getUserId());

            // SSE 연결 제거
            alertService.removeConnection(request.getUserId());

            // 분석 실패 시 오류 응답
            if (!finalAnalysis.getAnalysisCompleted()) {
                throw new ApiException("ANALYSIS_FAILED",
                        "Failed to analyze driving session: " + finalAnalysis.getErrorMessage());
            }

            // 주행 영상 레코드 생성
            int durationSeconds = (int)((request.getEndTimestamp() - startTimestamp) / 1000);
            DrivingVideo video = DrivingVideo.createVideo(
                    user,
                    "session_" + request.getSessionId(),
                    durationSeconds
            );
            video.setStatus(VideoStatus.ANALYZED);
            video.setProcessedAt();
            drivingVideoRepository.save(video);

            log.info("Created driving video record: {}", video.getVideoId());

            // 분석 결과 저장
            AnalysisResult result = AnalysisResult.createAnalysisResult(
                    video,
                    user,
                    finalAnalysis.getDrowsinessCount(),
                    finalAnalysis.getPhoneUsageCount(),
                    finalAnalysis.getSmokingCount(),
                    video.getDuration()
            );
            analysisResultRepository.save(result);

            log.info("Saved analysis result: {}", result.getResultId());

            // 운전 점수 업데이트 추가
            userScoreService.updateUserScore(user.getUserId(), result);
            log.info("Updated user score for userId: {}", user.getUserId());

            // 피드백 생성 (추가된 부분)
            Feedback feedback = feedbackService.generateDrivingFeedback(result);
            log.info("Generated driving feedback: {}", feedback.getFeedbackId());

            // 응답 반환
            return VideoDto.DrivingSessionEndResponse.builder()
                    .userId(request.getUserId())
                    .sessionId(request.getSessionId())
                    .drowsinessCount(finalAnalysis.getDrowsinessCount())
                    .phoneUsageCount(finalAnalysis.getPhoneUsageCount())
                    .smokingCount(finalAnalysis.getSmokingCount())
                    .drivingScore(result.getDrivingScore())
                    .saved(true)
                    .build();

        } catch (ApiException e) {
            log.error("API error ending driving session", e);
            throw e;
        } catch (Exception e) {
            log.error("Error ending driving session", e);
            throw new ApiException("SESSION_END_ERROR", "Failed to end driving session: " + e.getMessage());
        }
    }
}