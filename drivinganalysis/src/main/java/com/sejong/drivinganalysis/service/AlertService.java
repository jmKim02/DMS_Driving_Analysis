package com.sejong.drivinganalysis.service;

import com.sejong.drivinganalysis.dto.VideoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
    private static final long IDLE_TIMEOUT = 30 * 60 * 1000L; // 30분 동안 활동이 없으면 연결 종료
    private static final long RECONNECT_DELAY = 1000L; // 클라이언트 재연결 지연 시간(ms)

    // 사용자 ID와 SSE 이미터 매핑
    private final Map<Long, SseEmitter> userEmitters = new ConcurrentHashMap<>();

    // 연결 시간 추적
    private final Map<Long, Instant> connectionTimes = new ConcurrentHashMap<>();

    // 마지막 활동 시간 추적
    private final Map<Long, Instant> lastActivityTimes = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * 생성자: 연결 유지를 위한 핑 이벤트 스케줄링
     */
    public AlertService() {
        // 연결 유지를 위한 핑 이벤트 스케줄링 (30초마다)
        scheduler.scheduleAtFixedRate(this::sendKeepAliveToAll, 30, 30, TimeUnit.SECONDS);

        // 오래된 연결 정리 (15분마다)
        scheduler.scheduleAtFixedRate(this::cleanupStaleConnections, 15, 15, TimeUnit.MINUTES);

        log.info("AlertService initialized with ping interval=30s, cleanup interval=15m");
    }

    /**
     * SSE 연결 생성
     * 클라이언트와의 지속적인 연결을 위한 SSE 이미터 생성
     */
    public SseEmitter createAlertConnection(Long userId) {
        // 입력 검증
        if (userId == null || userId <= 0) {
            log.error("Invalid userId for SSE connection: {}", userId);
            throw new IllegalArgumentException("유효하지 않은 사용자 ID");
        }

        // 기존 연결이 있는지 확인
        SseEmitter existingEmitter = userEmitters.get(userId);

        // 기존 연결이 있고 여전히 유효한 경우 재사용 시도
        if (existingEmitter != null) {
            try {
                // 핑 이벤트 전송하여 연결 상태 확인
                existingEmitter.send(SseEmitter.event()
                        .name("ping")
                        .data(Map.of("timestamp", System.currentTimeMillis(), "message", "Connection check")));

                // 핑이 성공적으로 전송되면 연결이 유효하므로 그대로 반환
                lastActivityTimes.put(userId, Instant.now());
                log.info("Reusing existing SSE connection for userId: {}", userId);
                return existingEmitter;
            } catch (Exception e) {
                // 오류 발생 시 기존 연결 정리 후 새 연결 생성
                log.info("Unable to reuse existing SSE connection for userId: {}, creating new one", userId);
                cleanupUser(userId);
            }
        }

        Instant now = Instant.now();

        // 새 이미터 생성 - 타임아웃 설정
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 완료, 타임아웃, 에러 콜백 설정
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for userId: {}", userId);
            cleanupUser(userId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE connection timeout for userId: {}", userId);
            cleanupUser(userId);
        });

        emitter.onError(e -> {
            log.error("SSE connection error for userId: {}: {}", userId, e.getMessage());
            cleanupUser(userId);
        });

        try {
            // 연결 성공 이벤트 전송
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data(Map.of(
                            "message", "Connected successfully",
                            "timestamp", System.currentTimeMillis(),
                            "connectionId", userId.toString(),
                            "reconnectDelay", RECONNECT_DELAY
                    )));

            // 모든 연결 정보 추적 맵에 저장 - 성공적으로 초기 이벤트를 보낸 후에만 저장
            userEmitters.put(userId, emitter);
            connectionTimes.put(userId, now);
            lastActivityTimes.put(userId, now);

            log.info("SSE connection established for userId: {}", userId);
        } catch (IOException e) {
            log.error("Error sending initial SSE event: {}", e.getMessage());
            emitter.completeWithError(e);
            return emitter; // 실패한 이미터 반환 - 클라이언트 측에서 오류 처리
        }

        return emitter;
    }

    /**
     * 모든 연결에 핑 이벤트 전송 (연결 유지용)
     * ConcurrentModificationException 방지를 위한 안전한 구현
     */
    private void sendKeepAliveToAll() {
        try {
            // 현재 활성 연결 수 확인
            int connectionCount = userEmitters.size();
            if (connectionCount == 0) {
                return; // 활성 연결이 없으면 건너뛰기
            }

            // 제거할 사용자 ID 집합
            Set<Long> usersToRemove = new HashSet<>();

            userEmitters.forEach((userId, emitter) -> {
                try {
                    // 전송 전 유효성 검사
                    if (emitter == null) {
                        usersToRemove.add(userId);
                        return;
                    }

                    // 핑 이벤트 전송
                    emitter.send(SseEmitter.event()
                            .name("ping")
                            .data(Map.of("timestamp", System.currentTimeMillis())));

                    // 핑 전송 성공 시 마지막 활동 시간 업데이트
                    lastActivityTimes.put(userId, Instant.now());

                    log.debug("Sent keep-alive ping to userId: {}", userId);
                } catch (IOException e) {
                    log.warn("Failed to send ping to userId: {}, removing connection: {}", userId, e.getMessage());
                    usersToRemove.add(userId);
                } catch (Exception e) {
                    log.error("Unexpected error sending ping to userId: {}: {}", userId, e.getMessage());
                    usersToRemove.add(userId);
                }
            });

            // 문제가 있는 연결 일괄 정리
            for (Long userId : usersToRemove) {
                cleanupUser(userId);
            }

            if (!usersToRemove.isEmpty()) {
                log.info("Cleaned up {} connections during ping", usersToRemove.size());
            }

            // 주기적으로 연결 상태 로깅 (5회마다 한 번)
            if (connectionCount % 5 == 0) {
                log.info("Connection status: active={}, tracking={}, activity={}",
                        userEmitters.size(), connectionTimes.size(), lastActivityTimes.size());
            }
        } catch (Exception e) {
            log.error("Critical error in keep-alive process", e);
        }
    }

    /**
     * 일정 시간마다 실행되어 오래된 연결 정리
     */
    private void cleanupStaleConnections() {
        try {
            Instant now = Instant.now();
            Set<Long> usersToRemove = new HashSet<>();

            // 모든 연결 검사
            lastActivityTimes.forEach((userId, lastActivity) -> {
                // 마지막 활동으로부터 유휴 타임아웃 이상 지났는지 확인
                if (Duration.between(lastActivity, now).toMillis() > IDLE_TIMEOUT) {
                    log.info("Connection idle timeout for userId: {}, last activity: {}",
                            userId, lastActivity);
                    usersToRemove.add(userId);
                }
            });

            // 정리 대상 연결 처리
            for (Long userId : usersToRemove) {
                SseEmitter emitter = userEmitters.get(userId);
                if (emitter != null) {
                    try {
                        // 타임아웃 알림 전송 시도
                        emitter.send(SseEmitter.event()
                                .name("timeout")
                                .data(Map.of(
                                        "message", "Connection closed due to inactivity",
                                        "timestamp", System.currentTimeMillis()
                                )));
                    } catch (Exception e) {
                        log.debug("Error sending timeout notification: {}", e.getMessage());
                    } finally {
                        cleanupUser(userId);
                    }
                }
            }

            if (!usersToRemove.isEmpty()) {
                log.info("Cleaned up {} stale connections", usersToRemove.size());
            }

            // 데이터 일관성 확인 - 모든 맵의 크기가 일치해야 함
            if (userEmitters.size() != connectionTimes.size() ||
                    userEmitters.size() != lastActivityTimes.size()) {

                log.warn("Connection tracking inconsistency detected: emitters={}, connections={}, activity={}",
                        userEmitters.size(), connectionTimes.size(), lastActivityTimes.size());

                // 일관성 복구 시도
                syncConnectionTracking();
            }
        } catch (Exception e) {
            log.error("Error during stale connection cleanup", e);
        }
    }

    /**
     * 연결 추적 데이터 동기화 - 일관성 복구
     */
    private void syncConnectionTracking() {
        try {
            log.info("Synchronizing connection tracking data");

            // 활성 연결만 유지
            Set<Long> activeUserIds = new HashSet<>(userEmitters.keySet());

            // 연결 시간 맵 정리
            Set<Long> connectionTimeKeys = new HashSet<>(connectionTimes.keySet());
            for (Long userId : connectionTimeKeys) {
                if (!activeUserIds.contains(userId)) {
                    connectionTimes.remove(userId);
                }
            }

            // 활동 시간 맵 정리
            Set<Long> activityTimeKeys = new HashSet<>(lastActivityTimes.keySet());
            for (Long userId : activityTimeKeys) {
                if (!activeUserIds.contains(userId)) {
                    lastActivityTimes.remove(userId);
                }
            }

            // 누락된 항목 복구
            Instant now = Instant.now();
            for (Long userId : activeUserIds) {
                if (!connectionTimes.containsKey(userId)) {
                    connectionTimes.put(userId, now);
                }
                if (!lastActivityTimes.containsKey(userId)) {
                    lastActivityTimes.put(userId, now);
                }
            }

            log.info("Connection tracking synchronized: emitters={}, connections={}, activity={}",
                    userEmitters.size(), connectionTimes.size(), lastActivityTimes.size());
        } catch (Exception e) {
            log.error("Error during connection tracking synchronization", e);
        }
    }

    /**
     * 졸음 감지 알림 전송
     * AI 분석 결과 졸음이 감지된 경우 사용자에게 실시간 알림 전송
     */
    // AlertService.java 파일 내의 메소드 수정
    public void sendDrowsinessAlert(Long userId, boolean drowsinessDetected, Integer batchId) { // batchId 매개변수 추가
        if (userId == null) {
            log.error("Attempted to send alert to null userId");
            return;
        }

        SseEmitter emitter = userEmitters.get(userId);

        if (emitter != null) {
            try {
                VideoDto.DrowsinessAlert alert = VideoDto.DrowsinessAlert.builder()
                        .userId(userId)
                        .timestamp(System.currentTimeMillis())
                        .drowsinessDetected(drowsinessDetected)
                        .message("졸음 상태가 감지되었습니다. 안전한 곳에 차량을 정차하고 휴식을 취하세요.")
                        .batchId(batchId) // 이 필드 추가
                        .build();

                emitter.send(SseEmitter.event()
                        .name("drowsiness")
                        .data(alert));

                // 알림 전송 성공 시 마지막 활동 시간 업데이트
                lastActivityTimes.put(userId, Instant.now());

                log.info("Drowsiness alert sent to userId: {}, batchId: {}", userId, batchId); // 로그 메시지 수정
            } catch (IOException e) {
                log.error("Error sending drowsiness alert to userId: {}, batchId: {}: {}", userId, batchId, e.getMessage());
                cleanupUser(userId);
            }
        } else {
            log.warn("No active SSE connection for userId: {}", userId);
        }
    }

    /**
     * 연결 제거
     */
    public void removeConnection(Long userId) {
        if (userId == null) {
            return;
        }

        SseEmitter emitter = userEmitters.get(userId);
        if (emitter != null) {
            try {
                // 클라이언트에게 명시적 종료 알림 전송
                emitter.send(SseEmitter.event()
                        .name("close")
                        .data(Map.of(
                                "message", "Connection closed by server",
                                "timestamp", System.currentTimeMillis(),
                                "reconnectDelay", RECONNECT_DELAY
                        )));
                log.debug("Sent close event to userId: {}", userId);
            } catch (Exception e) {
                log.debug("Error sending close event: {}", e.getMessage());
            } finally {
                // 어떤 경우에도 반드시 연결 종료 및 관련 데이터 정리
                cleanupUser(userId);
                log.info("Removed existing SSE connection for userId: {}", userId);
            }
        }
    }

    /**
     * 단일 사용자의 모든 연결 관련 데이터 정리
     */
    private void cleanupUser(Long userId) {
        try {
            // 이미터 가져오기 (제거하지 않고 확인만)
            SseEmitter emitter = userEmitters.get(userId);

            // 모든 추적 데이터 제거 (먼저 제거)
            userEmitters.remove(userId);
            connectionTimes.remove(userId);
            lastActivityTimes.remove(userId);

            if (emitter != null) {
                // 이미터가 이미 완료되지 않았다면 완료 처리
                try {
                    emitter.complete();
                    log.debug("Emitter completed for userId: {}", userId);
                } catch (Exception e) {
                    log.warn("Error completing emitter: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Error during user cleanup: {}", e.getMessage());
        }
    }

    /**
     * 활성 연결 수 반환 (모니터링/디버깅용)
     */
    public int getActiveConnectionsCount() {
        return userEmitters.size();
    }

    /**
     * 특정 사용자의 연결 상태 확인
     */
    public boolean isConnected(Long userId) {
        if (userId == null) {
            return false;
        }
        return userEmitters.containsKey(userId) && lastActivityTimes.containsKey(userId);
    }

    /**
     * 특정 사용자의 마지막 활동 시간 업데이트 (외부에서 호출 가능)
     */
    public void updateUserActivity(Long userId) {
        if (userId != null && isConnected(userId)) {
            lastActivityTimes.put(userId, Instant.now());
            log.debug("Updated activity timestamp for userId: {}", userId);
        }
    }

}