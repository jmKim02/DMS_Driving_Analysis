package com.sejong.drivinganalysis.service;

import com.sejong.drivinganalysis.configuration.ChallengeProgressUpdater;
import com.sejong.drivinganalysis.dto.VideoDto;
import com.sejong.drivinganalysis.dto.VideoDto.DrivingSessionEndResponse;
import com.sejong.drivinganalysis.dto.VideoDto.FrameBatchRequest;
import com.sejong.drivinganalysis.dto.VideoDto.FrameData;
import com.sejong.drivinganalysis.dto.VideoDto.FrameProcessedResponse;
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

import java.util.ArrayList;
import java.util.List;
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
    private final ChallengeProgressUpdater challengeProgressUpdater;

    // 세션 관리를 위한 메모리 내 맵 (스레드 안전)
    private final Map<Long, Long> userSessionStartTimes = new ConcurrentHashMap<>();
    private final Map<Long, Long> userLastFrameTimestamps = new ConcurrentHashMap<>();

    // 디버그용 필드 추가
//    private static final String DEBUG_FRAME_DIR = "/tmp/frame_debug";
//    private static final int MAX_DEBUG_FRAMES = 15; // 디버그용으로 최대 5개 프레임만 저장

    /**
     * WebSocket을 통해 수신된 프레임 배치를 처리하고 AI 분석 요청을 보냄
     * 메모리에서 즉시 처리하고 실시간 알림만 처리
     */
    public FrameProcessedResponse processFrameBatch(FrameBatchRequest frameBatch) {
        try {
            if (frameBatch == null) {
                throw new ApiException("INVALID_REQUEST", "Frame batch request is null");
            }

            if (frameBatch.getUserId() == null) {
                throw new ApiException("INVALID_REQUEST", "Missing required parameter: userId");
            }

            // 사용자 존재 확인
            User user = userRepository.findById(frameBatch.getUserId())
                    .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found: " + frameBatch.getUserId()));

//            // 디버그용 프레임 저장 - 몇 개의 배치만 확인
//            if (frameBatch.getBatchId() < 3) { // 처음 몇 개 배치만 디버그
//                saveFramesForDebug(frameBatch, user.getUserId());
//            }

            // 프레임 유효성 검증
            List<FrameData> validFrames = validateFrames(frameBatch, user);

            log.info("Processing frame batch for user: {}, batchId: {}, valid frames count: {}/{}",
                    user.getUsername(), frameBatch.getBatchId(), validFrames.size(),
                    frameBatch.getFrames() != null ? frameBatch.getFrames().size() : 0);

            // 세션 시작 시간 기록 (첫 프레임인 경우)
            synchronized(userSessionStartTimes) {
                if (frameBatch.getBatchId() == 0 || !userSessionStartTimes.containsKey(user.getUserId())) {
                    userSessionStartTimes.put(user.getUserId(), frameBatch.getTimestamp());
                    log.info("New driving session started for userId: {}", user.getUserId());
                }
            }

            // 마지막 프레임 타임스탬프 업데이트
            synchronized(userLastFrameTimestamps) {
                userLastFrameTimestamps.put(user.getUserId(), frameBatch.getTimestamp());
            }

            // 비동기로 AI 분석 요청 - 실시간 졸음 감지를 위한 간소화된 분석
            if (!validFrames.isEmpty()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        // 유효한 프레임만 복사하여 새로운 리스트 생성 (원본 데이터 변경 방지)
                        final List<FrameData> framesCopy = new ArrayList<>(validFrames);

                        // AI 서버에 분석 요청
                        RealtimeAnalysisResponse response = grpcClientService.analyzeFrames(
                                user.getUserId(),
                                frameBatch.getBatchId(),
                                frameBatch.getTimestamp(),
                                framesCopy
                        );

                        // 메모리 관리: 복사본 사용 후 해제
                        framesCopy.clear();

                        // 분석 실패 시 로깅만 처리
                        if (!response.getAnalysisCompleted() && response.getErrorMessage() != null) {
                            log.warn("Frame analysis incomplete for userId: {}, error: {}",
                                    user.getUserId(), response.getErrorMessage());
                            return;
                        }

                        // 졸음 감지 시 알림 전송
                        if (response.getDrowsinessDetected()) {
                            log.info("Drowsiness detected for userId: {}, batchId: {}",
                                    user.getUserId(), frameBatch.getBatchId());
                            alertService.sendRiskBehaviorAlert(
                                    user.getUserId(), "drowsiness", true, frameBatch.getBatchId());
                        }

                        // 휴대폰 사용 감지 시 알림 전송
                        if (response.getPhoneUsageDetected()) {
                            log.info("Phone usage detected for userId: {}, batchId: {}",
                                    user.getUserId(), frameBatch.getBatchId());
                            alertService.sendRiskBehaviorAlert(
                                    user.getUserId(), "phone_usage", true, frameBatch.getBatchId());
                        }

                        // 흡연 감지 시 알림 전송
                        if (response.getSmokingDetected()) {
                            log.info("Smoking detected for userId: {}, batchId: {}",
                                    user.getUserId(), frameBatch.getBatchId());
                            alertService.sendRiskBehaviorAlert(
                                    user.getUserId(), "smoking", true, frameBatch.getBatchId());
                        }
                    } catch (Exception e) {
                        log.error("Error in async frame analysis for userId: {}", user.getUserId(), e);
                    }
                }).exceptionally(ex -> {
                    log.warn("Uncaught exception in frame analysis for userId: {}", user.getUserId(), ex);
                    return null;
                });
            } else {
                log.warn("프레임 분석 건너뜀: 유효한 프레임이 없음 - userId: {}, batchId: {}",
                        user.getUserId(), frameBatch.getBatchId());
            }

            // 응답 반환
            return FrameProcessedResponse.builder()
                    .userId(frameBatch.getUserId())
                    .batchId(frameBatch.getBatchId())
                    .timestamp(frameBatch.getTimestamp())
                    .processed(true)
                    .build();

        } catch (ApiException e) {
            log.warn("API 오류: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("프레임 배치 처리 중 예상치 못한 오류", e);
            throw new ApiException("FRAME_PROCESSING_ERROR", "Failed to process frame batch: " + e.getMessage());
        }
    }

    /**
     * 프레임 배치의 유효성을 검증하고 유효한 프레임만 반환
     */
    private List<FrameData> validateFrames(FrameBatchRequest frameBatch, User user) {
        List<FrameData> frames = frameBatch.getFrames();

        if (frames == null || frames.isEmpty()) {
            log.warn("빈 프레임 배치 - userId: {}, batchId: {}",
                    user.getUserId(), frameBatch.getBatchId());
            return new ArrayList<>();
        }

        // 유효한 프레임만 필터링
        List<FrameData> validFrames = new ArrayList<>();
        int nullFrames = 0;
        int emptyDataFrames = 0;

        for (int i = 0; i < frames.size(); i++) {
            FrameData frame = frames.get(i);

            if (frame == null) {
                nullFrames++;
                continue;
            }

            if (frame.getData() == null || frame.getData().length == 0) {
                emptyDataFrames++;
                log.warn("빈 프레임 데이터 - userId: {}, batchId: {}, frameIndex: {}, frameId: {}",
                        user.getUserId(), frameBatch.getBatchId(), i, frame.getFrameId());
                continue;
            }

            validFrames.add(frame);
        }

        if (nullFrames > 0 || emptyDataFrames > 0) {
            log.warn("프레임 검증 결과 - userId: {}, batchId: {}, 총 프레임: {}, null 프레임: {}, 빈 데이터 프레임: {}, 유효한 프레임: {}",
                    user.getUserId(), frameBatch.getBatchId(), frames.size(), nullFrames, emptyDataFrames, validFrames.size());
        }

        return validFrames;
    }

//    /**
//     * 디버그용 프레임 저장 메소드
//     */
//    private void saveFramesForDebug(VideoDto.FrameBatchRequest frameBatch, Long userId) {
//        try {
//            // 디렉토리 생성
//            File debugDir = new File(DEBUG_FRAME_DIR);
//            if (!debugDir.exists()) {
//                debugDir.mkdirs();
//            }
//
//            // 사용자별 디렉토리
//            File userDir = new File(debugDir, "user_" + userId);
//            if (!userDir.exists()) {
//                userDir.mkdirs();
//            }
//
//            // 배치별 디렉토리
//            File batchDir = new File(userDir, "batch_" + frameBatch.getBatchId());
//            if (!batchDir.exists()) {
//                batchDir.mkdirs();
//            }
//
//            // 프레임 정보 저장
//            File infoFile = new File(batchDir, "info.txt");
//            try (FileWriter writer = new FileWriter(infoFile)) {
//                writer.write("BatchId: " + frameBatch.getBatchId() + "\n");
//                writer.write("Timestamp: " + frameBatch.getTimestamp() + "\n");
//                writer.write("Total Frames: " + (frameBatch.getFrames() != null ? frameBatch.getFrames().size() : 0) + "\n");
//            }
//
//            // 몇 개의 프레임만 저장 (용량 제한)
//            if (frameBatch.getFrames() != null) {
//                int count = 0;
//                for (VideoDto.FrameData frame : frameBatch.getFrames()) {
//                    if (frame != null && frame.getData() != null && frame.getData().length > 0) {
//                        // 프레임 이미지 저장
//                        File frameFile = new File(batchDir, "frame_" + count + ".jpg");
//                        try (FileOutputStream fos = new FileOutputStream(frameFile)) {
//                            fos.write(frame.getData());
//                        }
//
//                        // 프레임 메타데이터 저장
//                        File frameInfoFile = new File(batchDir, "frame_" + count + "_info.txt");
//                        try (FileWriter writer = new FileWriter(frameInfoFile)) {
//                            writer.write("FrameId: " + frame.getFrameId() + "\n");
//                            writer.write("Data Length: " + frame.getData().length + " bytes\n");
//                        }
//
//                        count++;
//                        if (count >= MAX_DEBUG_FRAMES) break; // 최대 5개만 저장
//                    }
//                }
//                log.info("디버그용 프레임 {} 개 저장 완료: userId={}, batchId={}, 경로={}",
//                        count, userId, frameBatch.getBatchId(), batchDir.getAbsolutePath());
//            }
//        } catch (Exception e) {
//            log.error("디버그용 프레임 저장 중 오류: {}", e.getMessage(), e);
//            // 디버그 코드이므로 예외 처리 후 계속 진행
//        }
//    }

    /**
     * 주행 종료 시 호출되는 메서드
     * AI 서버로부터 최종 통계를 받아 DB에 저장
     */
    @Transactional
    public DrivingSessionEndResponse endDrivingSession(VideoDto.DrivingSessionEndRequest request) {
        try {
            if (request == null) {
                throw new ApiException("INVALID_REQUEST", "Session end request is null");
            }

            if (request.getUserId() == null || request.getSessionId() == null) {
                throw new ApiException("INVALID_REQUEST", "Missing required parameters: userId or sessionId");
            }

            // 사용자 존재 확인
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found: " + request.getUserId()));

            // 세션 시작 타임스탬프 및 마지막 프레임 타임스탬프 확인
            Long startTimestamp;
            Long lastFrameTimestamp;

            synchronized(userSessionStartTimes) {
                startTimestamp = userSessionStartTimes.getOrDefault(request.getUserId(), 0L);
            }

            synchronized(userLastFrameTimestamps) {
                lastFrameTimestamp = userLastFrameTimestamps.getOrDefault(request.getUserId(), startTimestamp);
            }

            log.info("Ending driving session for user: {}, sessionId: {}", user.getUsername(), request.getSessionId());

            // AI 서버에 주행 종료 요청 및 최종 분석 결과 수신
            FinalAnalysisResponse finalAnalysis = grpcClientService.endDrivingSession(
                    user.getUserId(),
                    request.getSessionId(),
                    startTimestamp,
                    request.getEndTimestamp() != null ? request.getEndTimestamp() : System.currentTimeMillis()
            );

            // 세션 정보 정리
            synchronized(userSessionStartTimes) {
                userSessionStartTimes.remove(request.getUserId());
            }

            synchronized(userLastFrameTimestamps) {
                userLastFrameTimestamps.remove(request.getUserId());
            }

            // SSE 연결 제거
            try {
                alertService.removeConnection(request.getUserId());
            } catch (Exception e) {
                log.warn("SSE 연결 제거 중 오류: {}", e.getMessage());
            }

            // 분석 실패 시에도 기본 분석 결과 생성
            if (!finalAnalysis.getAnalysisCompleted() && finalAnalysis.getErrorMessage() != null) {
                log.warn("주행 분석 불완전 - userId: {}, error: {}",
                        user.getUserId(), finalAnalysis.getErrorMessage());
            }

            // 주행 영상 레코드 생성
            int durationSeconds = (int)((lastFrameTimestamp - startTimestamp) / 1000);
            durationSeconds = Math.max(1, durationSeconds); // 최소 1초 보장

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
            try {
                userScoreService.updateUserScore(user.getUserId(), result);
                log.info("Updated user score for userId: {}", user.getUserId());
            } catch (Exception e) {
                log.error("사용자 점수 업데이트 중 오류: {}", e.getMessage(), e);
            }

            // 피드백 생성
            Feedback feedback = null;
            try {
                feedback = feedbackService.generateDrivingFeedback(result);
                log.info("Generated driving feedback: {}", feedback.getFeedbackId());
            } catch (Exception e) {
                log.error("피드백 생성 중 오류: {}", e.getMessage(), e);
            }

            // 챌린지 진행 업데이트 추가
            try {
                challengeProgressUpdater.updateFrom(result);
                log.info("챌린지 진행도 업데이트 완료 - userId: {}", user.getUserId());
            } catch (Exception e) {
                log.error("챌린지 진행도 업데이트 중 오류 발생 - userId: {}, 오류: {}", user.getUserId(), e.getMessage());
            }

            // 응답 반환
            return DrivingSessionEndResponse.builder()
                    .userId(request.getUserId())
                    .sessionId(request.getSessionId())
                    .drowsinessCount(finalAnalysis.getDrowsinessCount())
                    .phoneUsageCount(finalAnalysis.getPhoneUsageCount())
                    .smokingCount(finalAnalysis.getSmokingCount())
                    .drivingScore(result.getDrivingScore())
                    .saved(true)
                    .build();

        } catch (ApiException e) {
            log.warn("API 오류: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("주행 세션 종료 중 예상치 못한 오류", e);
            throw new ApiException("SESSION_END_ERROR", "Failed to end driving session: " + e.getMessage());
        }
    }
}