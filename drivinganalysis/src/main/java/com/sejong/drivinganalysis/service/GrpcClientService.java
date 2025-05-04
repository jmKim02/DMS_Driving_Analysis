package com.sejong.drivinganalysis.service;

import com.google.protobuf.ByteString;
import com.sejong.drivinganalysis.dto.VideoDto;
import com.sejong.drivinganalysis.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AI 서버와의 gRPC 통신을 담당하는 서비스
 * 프레임 분석 요청 및 주행 세션 종료 처리를 담당
 */
@Service
@Slf4j
public class GrpcClientService {

    @Value("${ai.server.host:localhost}")
    private String aiServerHost;

    @Value("${ai.server.port:5000}")
    private int aiServerPort;

    @Value("${ai.server.timeout:30}")
    private int timeout;

    @Value("${ai.server.retry.max:3}")
    private int maxRetries;

    private ManagedChannel channel;
    private VideoAnalysisServiceGrpc.VideoAnalysisServiceBlockingStub blockingStub;

    /**
     * gRPC 채널 초기화 및 스텁 생성
     */
    @PostConstruct
    private void init() {
        channel = ManagedChannelBuilder.forAddress(aiServerHost, aiServerPort)
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS) // 연결 유지 설정
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true) // 추가
                .maxInboundMessageSize(10 * 1024 * 1024) // 10MB로 설정
                .build();
        blockingStub = VideoAnalysisServiceGrpc.newBlockingStub(channel);
        log.info("gRPC client initialized, connected to AI server at {}:{}", aiServerHost, aiServerPort);
    }

    /**
     * 프레임 배치를 AI 서버로 전송하여 실시간 분석 요청
     * 주로 졸음 감지 여부만 확인하는 간소화된 응답 반환
     */
    public RealtimeAnalysisResponse analyzeFrames(Long userId, Integer batchId, Long timestamp, List<VideoDto.FrameData> frames) {
        log.info("Sending gRPC request to AI server for userId: {}, batchId: {}, frames: {}",
                userId, batchId, frames.size());

        // 프레임 배치 자체가 null이거나 비어있는지 확인
        if (frames == null || frames.isEmpty()) {
            log.error("AI 서버로 전송할 프레임 배치가 비어 있습니다: userId={}, batchId={}", userId, batchId);
            // 여기서는 처리하지 않고 로그만 남김
        } else {
            // 각 프레임의 데이터 확인
            for (int i = 0; i < frames.size(); i++) {
                VideoDto.FrameData frame = frames.get(i);
                if (frame == null) {
                    log.warn("null 프레임 발견: userId={}, batchId={}, frameIndex={}", userId, batchId, i);
                } else if (frame.getData() == null || frame.getData().length == 0) {
                    log.warn("빈 프레임 데이터 발견: userId={}, batchId={}, frameIndex={}, dataLength={}",
                            userId, batchId, i, frame.getData() != null ? frame.getData().length : 0);
                }
            }
        }

        // 프로토 형식에 맞게 요청 구성
        List<Frame> protoFrames = frames.stream()
                .map(f -> Frame.newBuilder()
                        .setData(ByteString.copyFrom(f.getData()))
                        .setFrameId(f.getFrameId())
                        .build())
                .collect(Collectors.toList());

        FrameBatch request = FrameBatch.newBuilder()
                .setUserId(userId)
                .setBatchId(batchId)
                .setTimestamp(timestamp)
                .addAllFrames(protoFrames)
                .build();

        int retries = 0;
        while (retries < maxRetries) {
            try {
                RealtimeAnalysisResponse response = blockingStub.withDeadlineAfter(timeout, TimeUnit.SECONDS)
                        .analyzeFrames(request);
                log.info("Received gRPC response from AI server for userId: {}, drowsiness detected: {}",
                        userId, response.getDrowsinessDetected());
                return response;
            } catch (StatusRuntimeException e) {
                retries++;
                Status status = e.getStatus();

                // 재시도 가능한 오류인지 확인
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
                    log.error("gRPC call failed with status: {}", status, e);
                    return createRealtimeErrorResponse(userId, e.getMessage());
                }
            } catch (Exception e) {
                log.error("Unexpected error in gRPC call", e);
                return createRealtimeErrorResponse(userId, e.getMessage());
            }
        }

        return createRealtimeErrorResponse(userId, "Max retries exceeded");
    }

    /**
     * 주행 종료 요청 및 최종 분석 결과 수신
     * 전체 통계 정보가 포함된 상세 응답 반환
     */
    public FinalAnalysisResponse endDrivingSession(Long userId, Long sessionId, Long startTimestamp, Long endTimestamp) {
        log.info("Sending gRPC end driving session request to AI server for userId: {}, sessionId: {}",
                userId, sessionId);

        // 프로토 정의에 맞게 요청 구성
        DrivingSessionEnd request = DrivingSessionEnd.newBuilder()
                .setUserId(userId)
                .setSessionId(sessionId)
                .setStartTimestamp(startTimestamp)
                .setEndTimestamp(endTimestamp)
                .build();

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
                    log.error("gRPC end session call failed with status: {}", status, e);
                    return createFinalErrorResponse(userId, e.getMessage());
                }
            } catch (Exception e) {
                log.error("Unexpected error in gRPC end session call", e);
                return createFinalErrorResponse(userId, e.getMessage());
            }
        }

        return createFinalErrorResponse(userId, "Max retries exceeded");
    }

    /**
     * 실시간 분석 오류 응답 생성
     */
    private RealtimeAnalysisResponse createRealtimeErrorResponse(Long userId, String errorMessage) {
        return RealtimeAnalysisResponse.newBuilder()
                .setUserId(userId)
                .setDrowsinessDetected(false)
                .setAnalysisCompleted(false)
                .setErrorMessage("gRPC call failed: " + errorMessage)
                .build();
    }

    /**
     * 최종 분석 오류 응답 생성
     */
    private FinalAnalysisResponse createFinalErrorResponse(Long userId, String errorMessage) {
        return FinalAnalysisResponse.newBuilder()
                .setUserId(userId)
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