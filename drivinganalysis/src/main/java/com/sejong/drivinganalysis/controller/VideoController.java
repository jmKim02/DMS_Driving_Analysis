package com.sejong.drivinganalysis.controller;

import com.sejong.drivinganalysis.dto.VideoDto;
import com.sejong.drivinganalysis.dto.common.ApiResponse;
import com.sejong.drivinganalysis.service.VideoService;
import com.sejong.drivinganalysis.utils.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class VideoController {

    private final VideoService videoService;
    private final JwtTokenProvider jwtTokenProvider;

    // 실시간 영상 프레임 데이터 수신 엔드포인트
    @PostMapping("/videos/frames")
    public ResponseEntity<ApiResponse<VideoDto.UploadResponse>> uploadFrames(
            @RequestHeader("Authorization") String token,
            @RequestParam("frames") List<MultipartFile> frames,
            @RequestParam("userId") Long userId,
            @RequestParam("timestamp") Long timestamp) {

        log.info("프레임 업로드 요청: userId={}, frames={}, timestamp={}",
                userId, frames.size(), timestamp);

        // 토큰에서 사용자 ID 추출하여 검증
        String jwtToken = token.substring(7); // "Bearer " 제거
        Long tokenUserId = jwtTokenProvider.getUserId(jwtToken);

        if (!tokenUserId.equals(userId)) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("INVALID_USER", "요청 사용자 ID가 일치하지 않습니다."));
        }

        try {
            VideoDto.UploadResponse response = videoService.processFrames(userId, frames, timestamp);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IOException e) {
            log.error("프레임 처리 중 오류 발생", e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("PROCESSING_ERROR", "프레임 처리 중 오류가 발생했습니다."));
        }
    }

    // SSE 연결 엔드포인트
    @GetMapping(value = "/alerts/subscribe/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToAlerts(@PathVariable Long userId) {
        log.info("사용자 {}의 알림 구독 요청", userId);
        return videoService.createSseConnection(userId);
    }

    // 영상 분석 상태 조회
    @GetMapping("/videos/{videoId}/status")
    public ResponseEntity<ApiResponse<VideoDto.StatusResponse>> getVideoStatus(
            @PathVariable Long videoId) {
        VideoDto.StatusResponse response = videoService.getVideoStatus(videoId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}