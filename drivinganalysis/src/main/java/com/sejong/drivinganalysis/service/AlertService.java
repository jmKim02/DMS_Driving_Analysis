package com.sejong.drivinganalysis.service;

import com.sejong.drivinganalysis.dto.VideoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 실시간 알림 전송을 담당하는 서비스
 * SSE(Server-Sent Events)를 이용한 실시간 통신 관리
 */
@Service
@Slf4j
public class AlertService {

    private static final long SSE_TIMEOUT = 60 * 60 * 1000L; // 1시간 타임아웃
    private final Map<Long, SseEmitter> userEmitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * 생성자: 연결 유지를 위한 핑 이벤트 스케줄링
     */
    public AlertService() {
        // 연결 유지를 위한 핑 이벤트 스케줄링 (30초마다)
        scheduler.scheduleAtFixedRate(this::sendKeepAliveToAll, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * SSE 연결 생성
     * 클라이언트와의 지속적인 연결을 위한 SSE 이미터 생성
     */
    public SseEmitter createAlertConnection(Long userId) {
        // 기존 연결이 있다면 제거
        removeConnection(userId);

        // 새 이미터 생성
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 완료, 타임아웃, 에러 콜백 설정
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for userId: {}", userId);
            userEmitters.remove(userId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE connection timeout for userId: {}", userId);
            userEmitters.remove(userId);
        });

        emitter.onError(e -> {
            log.error("SSE connection error for userId: {}", userId, e);
            userEmitters.remove(userId);
        });

        // 사용자 맵에 이미터 저장
        userEmitters.put(userId, emitter);

        try {
            // 연결 성공 이벤트 전송
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("Connected successfully"));

            log.info("SSE connection established for userId: {}", userId);
        } catch (IOException e) {
            log.error("Error sending initial SSE event", e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 모든 연결에 핑 이벤트 전송 (연결 유지용)
     */
    // AlertService.java에 추가
    private void sendKeepAliveToAll() {
        userEmitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("ping")
                        .data(""));
                log.debug("Sent keep-alive ping to userId: {}", userId);
            } catch (IOException e) {
                log.warn("Failed to send ping to userId: {}, removing connection", userId);
                // 명시적으로 완료 처리 추가
                emitter.complete();
                userEmitters.remove(userId);
            } catch (Exception e) {
                log.error("Unexpected error sending ping to userId: {}", userId, e);
                emitter.complete();
                userEmitters.remove(userId);
            }
        });
    }

    /**
     * 졸음 감지 알림 전송
     * AI 분석 결과 졸음이 감지된 경우 사용자에게 실시간 알림 전송
     */
    public void sendDrowsinessAlert(Long userId, boolean drowsinessDetected) {
        SseEmitter emitter = userEmitters.get(userId);

        if (emitter != null) {
            try {
                VideoDto.DrowsinessAlert alert = VideoDto.DrowsinessAlert.builder()
                        .userId(userId)
                        .timestamp(System.currentTimeMillis())
                        .drowsinessDetected(drowsinessDetected)
                        .message("졸음 상태가 감지되었습니다. 안전한 곳에 차량을 정차하고 휴식을 취하세요.")
                        .build();

                emitter.send(SseEmitter.event()
                        .name("drowsiness")
                        .data(alert));

                log.info("Drowsiness alert sent to userId: {}", userId);
            } catch (IOException e) {
                log.error("Error sending drowsiness alert to userId: {}", userId, e);
                emitter.completeWithError(e);
                userEmitters.remove(userId);
            }
        } else {
            log.warn("No active SSE connection for userId: {}", userId);
        }
    }

    /**
     * 연결 제거
     */
    public void removeConnection(Long userId) {
        SseEmitter emitter = userEmitters.remove(userId);
        if (emitter != null) {
            try {
                // 클라이언트에게 명시적 종료 알림 전송
                emitter.send(SseEmitter.event()
                        .name("close")
                        .data(Map.of("message", "Connection closed by server",
                                "timestamp", System.currentTimeMillis())));
            } catch (Exception e) {
                log.debug("Error sending close event: {}", e.getMessage());
            } finally {
                // 어떤 경우에도 반드시 연결 종료
                emitter.complete();
                log.info("Removed existing SSE connection for userId: {}", userId);
            }
        }
    }

    /**
     * 활성 연결 수 반환 (모니터링/디버깅용)
     */
    public int getActiveConnectionsCount() {
        return userEmitters.size();
    }
}