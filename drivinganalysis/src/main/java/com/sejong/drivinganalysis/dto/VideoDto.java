package com.sejong.drivinganalysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class VideoDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadResponse {
        private Long videoId;
        private String fileName;
        private Long fileSize;
        private LocalDateTime uploadedAt;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusResponse {
        private Long videoId;
        private String status;
        private LocalDateTime uploadedAt;
        private LocalDateTime processedAt;
    }
}