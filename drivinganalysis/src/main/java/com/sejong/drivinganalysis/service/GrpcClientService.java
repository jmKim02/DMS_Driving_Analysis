package com.sejong.drivinganalysis.service;

import com.google.protobuf.ByteString;
import com.sejong.drivinganalysis.dto.VideoDto.FrameData;
import com.sejong.drivinganalysis.grpc.*;
import com.sejong.drivinganalysis.grpc.VideoAnalysisServiceGrpc.VideoAnalysisServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AI 서버와의 gRPC 통신을 담당하는 서비스
 * 프레임 분석 요청 및 주행 세션 종료 처리를 담당
 */
@Service
@Slf4j
public class GrpcClientService {

    // AI 서버 연결 설정 (application.yml에서 설정 가능)
    @Value("${ai.server.host:localhost}")
    private String aiServerHost;

    @Value("${ai.server.port:5000}")
    private int aiServerPort;

    @Value("${ai.server.timeout:30}")
    private int timeout;

    @Value("${ai.server.retry.max:3}")
    private int maxRetries;

    // gRPC 통신을 위한 채널과 스텁
    private ManagedChannel channel;
    private VideoAnalysisServiceBlockingStub blockingStub;

    /**
     * gRPC 채널 초기화 및 스텁 생성
     */
    @PostConstruct
    private void init() {
        channel = ManagedChannelBuilder.forAddress(aiServerHost, aiServerPort)
                .usePlaintext()                                         // 암호화 없이 평문 통신 (개발용)
                .keepAliveTime(120, TimeUnit.SECONDS)       // 연결 유지 설정
                .keepAliveTimeout(20, TimeUnit.SECONDS)  // 연결 유지 타임아웃
                .keepAliveWithoutCalls(true)                     // 활성 호출 없이도 연결 유지
                .maxInboundMessageSize(10 * 1024 * 1024)          // 10MB로 설정
                .build();
        blockingStub = VideoAnalysisServiceGrpc.newBlockingStub(channel);
        log.info("gRPC client initialized, connected to AI server at {}:{}", aiServerHost, aiServerPort);
    }

    /**
     * 프레임 배치를 AI 서버로 전송하여 실시간 분석 요청
     * 주로 졸음 감지 여부만 확인하는 간소화된 응답 반환
     */
    public RealtimeAnalysisResponse analyzeFrames(Long userId, Integer batchId, Long timestamp, List<FrameData> frames) {
        if (userId == null || batchId == null) {
            log.warn("필수 파라미터가 누락되었습니다: userId={}, batchId={}", userId, batchId);
            return createRealtimeErrorResponse(userId, "Missing required parameters");
        }

        // 프레임 배치 자체가 null이거나 비어있는지 확인
        if (frames == null || frames.isEmpty()) {
            log.warn("AI 서버로 전송할 프레임 배치가 비어 있습니다: userId={}, batchId={}", userId, batchId);
            return createRealtimeErrorResponse(userId, "No frames to analyze");
        }

        log.info("Sending gRPC request to AI server for userId: {}, batchId: {}, frames: {}",
                userId, batchId, frames.size());

        try {
            // 유효한 프레임만 필터링 (null, 빈 데이터 제외)
            List<FrameData> validFrames = frames.stream()
                    .filter(Objects::nonNull)  // null 프레임 필터링
                    .filter(f -> f.getData() != null && f.getData().length > 0)  // 빈 데이터 필터링
                    .collect(Collectors.toList());

            // 유효한 프레임 검증
            if (validFrames.isEmpty()) {
                log.warn("유효한 프레임이 없습니다: userId={}, batchId={}", userId, batchId);
                return createRealtimeErrorResponse(userId, "No valid frames to analyze");
            }

            // DTO → Proto 변환 (안전하게 각 프레임별 변환 오류 처리)
            List<Frame> protoFrames = new ArrayList<>();
            for (FrameData frame : validFrames) {
                try {
                    Frame protoFrame = Frame.newBuilder()
                            .setData(ByteString.copyFrom(frame.getData()))
                            .setFrameId(frame.getFrameId() != null ? frame.getFrameId() : 0)
                            .build();
                    protoFrames.add(protoFrame);
                } catch (Exception e) {
                    log.warn("프레임 변환 중 오류 발생: frameId={}, error={}", frame.getFrameId(), e.getMessage());
                }
            }

            // 프로토 객체 유효성 검증
            if (protoFrames.isEmpty()) {
                log.warn("모든 프레임 변환에 실패했습니다: userId={}, batchId={}", userId, batchId);
                return createRealtimeErrorResponse(userId, "Failed to convert frames");
            }

            // gRPC 요청 객체 생성
            FrameBatch request = FrameBatch.newBuilder()
                    .setUserId(userId)
                    .setBatchId(batchId)
                    .setTimestamp(timestamp != null ? timestamp : System.currentTimeMillis())
                    .addAllFrames(protoFrames)
                    .build();

            // AI 서버 요청 및 재시도 로직
            int retries = 0;
            while (retries < maxRetries) {
                try {
                    // 타임아웃 설정과 함께 AI 서버 호출
                    RealtimeAnalysisResponse response = blockingStub.withDeadlineAfter(timeout, TimeUnit.SECONDS)
                            .analyzeFrames(request);

                    log.info("Received gRPC response for userId: {}, drowsiness detected: {}, phone detected: {}, smoking: {}",
                            userId, response.getDrowsinessDetected(), response.getPhoneUsageDetected(), response.getSmokingDetected());
                    return response;
                } catch (StatusRuntimeException e) {
                    retries++;
                    Status status = e.getStatus();

                    // 재시도 가능한 오류 (서버 일시적 장애, 타임아웃)인 경우 재시도
                    if (retries < maxRetries &&
                            (status.getCode() == Status.Code.UNAVAILABLE ||
                                    status.getCode() == Status.Code.DEADLINE_EXCEEDED)) {
                        log.warn("Retrying gRPC call after error: {} (retry {}/{})", status, retries, maxRetries);
                        try {
                            Thread.sleep(1000 * retries); // 지수 백오프
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        // 재시도 불가능한 오류 또는 최대 재시도 횟수 초과
                        log.warn("gRPC call failed with status: {}", status, e);
                        return createRealtimeErrorResponse(userId, e.getMessage());
                    }
                }
            }

            // 최대 재시도 횟수 초과
            return createRealtimeErrorResponse(userId, "Max retries exceeded");
        } catch (Exception e) {
            log.warn("프레임 분석 중 예상치 못한 오류: {}", e.getMessage(), e);
            return createRealtimeErrorResponse(userId, "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * 주행 종료 요청 및 최종 분석 결과 수신
     * 전체 통계 정보가 포함된 상세 응답 반환
     */
    public FinalAnalysisResponse endDrivingSession(Long userId, Long sessionId, Long startTimestamp, Long endTimestamp) {
        if (userId == null || sessionId == null) {
            log.warn("필수 파라미터가 누락되었습니다: userId={}, sessionId={}", userId, sessionId);
            return createFinalErrorResponse(userId, "Missing required parameters");
        }

        log.info("Sending gRPC end driving session request to AI server for userId: {}, sessionId: {}",
                userId, sessionId);

        // 타임스탬프 보정 (null인 경우 현재 시간 사용)
        Long safeStartTimestamp = startTimestamp != null ? startTimestamp : System.currentTimeMillis() - 3600000; // 1시간 전
        Long safeEndTimestamp = endTimestamp != null ? endTimestamp : System.currentTimeMillis();

        try {
            // gRPC 요청 객체 생성
            DrivingSessionEnd request = DrivingSessionEnd.newBuilder()
                    .setUserId(userId)
                    .setSessionId(sessionId)
                    .setStartTimestamp(safeStartTimestamp)
                    .setEndTimestamp(safeEndTimestamp)
                    .build();

            // 호출 및 재시도 로직 (실시간 분석과 유사)
            int retries = 0;
            while (retries < maxRetries) {
                try {
                    FinalAnalysisResponse response = blockingStub.withDeadlineAfter(timeout, TimeUnit.SECONDS)
                            .endDrivingSession(request);

                    log.info("Received final analysis for userId: {}, drowsiness count: {}, phone usage: {}, smoking: {}",
                            userId, response.getDrowsinessCount(), response.getPhoneUsageCount(), response.getSmokingCount());

                    return response;
                } catch (StatusRuntimeException e) {
                    retries++;
                    Status status = e.getStatus();

                    // 재시도 가능한 오류인지 확인
                    if (retries < maxRetries &&
                            (status.getCode() == Status.Code.UNAVAILABLE ||
                                    status.getCode() == Status.Code.DEADLINE_EXCEEDED)) {
                        log.warn("Retrying gRPC end session call after error: {} (retry {}/{})",
                                status, retries, maxRetries);
                        try {
                            Thread.sleep(1000 * retries); // 지수 백오프
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        log.warn("gRPC end session call failed with status: {}", status, e);
                        return createFinalErrorResponse(userId, e.getMessage());
                    }
                }
            }

            return createFinalErrorResponse(userId, "Max retries exceeded");
        } catch (Exception e) {
            log.warn("주행 세션 종료 중 예상치 못한 오류: {}", e.getMessage(), e);
            return createFinalErrorResponse(userId, "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * 실시간 분석 오류 응답 생성
     */
    private RealtimeAnalysisResponse createRealtimeErrorResponse(Long userId, String errorMessage) {
        return RealtimeAnalysisResponse.newBuilder()
                .setUserId(userId != null ? userId : 0L)
                .setDrowsinessDetected(false)
                .setPhoneUsageDetected(false)
                .setSmokingDetected(false)
                .setAnalysisCompleted(false)
                .setErrorMessage("gRPC call failed: " + errorMessage)
                .build();
    }

    /**
     * 최종 분석 오류 응답 생성
     */
    private FinalAnalysisResponse createFinalErrorResponse(Long userId, String errorMessage) {
        return FinalAnalysisResponse.newBuilder()
                .setUserId(userId != null ? userId : 0L)
                .setDrowsinessCount(0)
                .setPhoneUsageCount(0)
                .setSmokingCount(0)
                .setAnalysisCompleted(false)
                .setErrorMessage("gRPC call failed: " + errorMessage)
                .build();
    }

    /**
     * gRPC 채널 종료 처리
     */
    @PreDestroy
    private void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            log.info("Shutting down gRPC channel");
            channel.shutdown();
            try {
                // 5초 내에 정상 종료되지 않으면 강제 종료
                if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                    channel.shutdownNow();
                    log.warn("gRPC channel forced shutdown after timeout");
                }
            } catch (InterruptedException e) {
                log.error("Error shutting down gRPC channel", e);
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}