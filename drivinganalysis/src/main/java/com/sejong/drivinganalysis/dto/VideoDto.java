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
     * 클라이언트에서 백엔드 서버로 전송하는 영상 프레임 배치 정보
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

    /**
     * 개별 프레임 데이터 DTO
     */
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
     * 백엔드 서버에서 프레임 처리 후 클라이언트에 응답하는 정보
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
     * 백엔드 서버에서 주행 종료 처리 후 클라이언트에 응답하는 정보
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

//    /**
//     * 졸음 감지 알림 DTO
//     * 백엔드 서버에서 SSE를 통해 클라이언트에 전송하는 졸음 알림 정보
//     */
//    @Data
//    @Builder
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class DrowsinessAlert {
//        private Long userId;
//        private Long timestamp;
//        private Boolean drowsinessDetected;
//        private String message;
//        private Integer batchId;
//    }

    /**
     * 위험 행동 감지 알림 DTO
     * 백엔드 서버에서 SSE를 통해 클라이언트에 전송하는 위험 행동 알림 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskBehaviorAlert {
        private Long userId;
        private Long timestamp;
        private Boolean drowsinessDetected;
        private Boolean phoneUsageDetected;
        private Boolean smokingDetected;
        private String message;
        private Integer batchId;

        // 어떤 알림 타입인지 쉽게 확인할 수 있는 유틸리티 메서드
        public String getPrimaryAlertType() {
            if (Boolean.TRUE.equals(drowsinessDetected)) {
                return "drowsiness";
            } else if (Boolean.TRUE.equals(phoneUsageDetected)) {
                return "phone_usage";
            } else if (Boolean.TRUE.equals(smokingDetected)) {
                return "smoking";
            }
            return null;
        }
    }
}