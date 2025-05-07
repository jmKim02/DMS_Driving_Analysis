package com.sejong.drivinganalysis.controller;

import com.sejong.drivinganalysis.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Slf4j
public class AlertController {

    private final AlertService alertService;

    @GetMapping(value = "/subscribe/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToAlerts(@PathVariable Long userId) {
        log.info("SSE connection request for userId: {}", userId);

        return alertService.createAlertConnection(userId);
    }

    /**
     * SSE 연결에 대한 전용 예외 처리
     * text/event-stream 형식으로 오류 응답 반환
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleSseException(Exception e) {
        log.error("SSE connection error", e);

        // SSE 형식의 오류 이벤트 생성
        String errorEvent = "event: error\ndata: " + e.getMessage() + "\n\n";

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(errorEvent);
    }
}