package com.sejong.drivinganalysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 영상 처리 관련 DTO 클래스 모음
 */
public class VideoDto {

    /**
     * 프레임 배치 요청 DTO
     * 클라이언트에서 전송한 프레임 배치 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FrameBatchRequest {
        private Long userId;
        private Integer batchId;
        private Long timestamp;
        private List<FrameData> frames;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FrameData {
        private byte[] data;
        private Integer frameId;  // 개별 프레임 ID 포함
    }

    /**
     * 프레임 처리 응답 DTO
     * 서버에서 프레임 처리 후 클라이언트에 응답하는 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FrameProcessedResponse {
        private Long userId;
        private Integer batchId;
        private Long timestamp;
        private Boolean processed;
    }

    /**
     * 주행 세션 종료 요청 DTO
     * 클라이언트에서 주행 종료 시 전송하는 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrivingSessionEndRequest {
        private Long userId;
        private Long sessionId;
        private Long endTimestamp;
    }

    /**
     * 주행 세션 종료 응답 DTO
     * 서버에서 주행 종료 처리 후 클라이언트에 응답하는 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrivingSessionEndResponse {
        private Long userId;
        private Long sessionId;
        private Integer drowsinessCount;
        private Integer phoneUsageCount;
        private Integer smokingCount;
        private Integer drivingScore;
        private Boolean saved;
    }

    /**
     * 졸음 감지 알림 DTO
     * 서버에서 SSE를 통해 클라이언트에 전송하는 졸음 알림 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrowsinessAlert {
        private Long userId;
        private Long timestamp;
        private Boolean drowsinessDetected;
        private String message;
    }
}