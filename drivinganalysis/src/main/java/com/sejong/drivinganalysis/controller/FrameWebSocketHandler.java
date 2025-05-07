package com.sejong.drivinganalysis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sejong.drivinganalysis.dto.VideoDto;
import com.sejong.drivinganalysis.service.VideoService;
import com.sejong.drivinganalysis.configuration.CustomConfigurator;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 연결을 통해 프레임 배치 수신을 담당하는 핸들러
 * 클라이언트와의 양방향 통신을 처리
 */
@ServerEndpoint(value = "/ws", configurator = CustomConfigurator.class)
@Component
@Slf4j
public class FrameWebSocketHandler {

    // Spring의 DI를 WebSocket 엔드포인트에서 사용하기 위한 정적 참조
    private static VideoService videoService;
    private static ObjectMapper objectMapper;

    // 모든 세션 관리
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @Autowired
    public void setVideoService(VideoService service) {
        FrameWebSocketHandler.videoService = service;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper mapper) {
        FrameWebSocketHandler.objectMapper = mapper;
    }

    /**
     * WebSocket 연결 시작 처리
     */
    @OnOpen
    public void onOpen(Session session) {
        this.sessions.put(session.getId(), session);
        log.info("WebSocket 연결 시작: sessionId={}", session.getId());

        // 바이너리 버퍼 사이즈 설정
        session.setMaxBinaryMessageBufferSize(50 * 1024 * 1024); // 50MB
        session.setMaxTextMessageBufferSize(64 * 1024); // 64KB
    }

    /**
     * 텍스트 메시지 수신 처리
     * 주로 주행 종료 메시지와 같은 제어 메시지 처리
     */
    @OnMessage
    public void onTextMessage(String message, Session session) {
        try {
            log.debug("텍스트 메시지 수신: sessionId={}, message={}", session.getId(), message);

            // 주행 종료 메시지 처리
            if (message.contains("\"type\":\"END_SESSION\"")) {
                VideoDto.DrivingSessionEndRequest endRequest = objectMapper.readValue(message, VideoDto.DrivingSessionEndRequest.class);
                VideoDto.DrivingSessionEndResponse response = videoService.endDrivingSession(endRequest);

                // 응답 전송
                sendTextMessage(session, objectMapper.writeValueAsString(response));
                log.info("주행 종료 처리 완료: userId={}, sessionId={}", endRequest.getUserId(), endRequest.getSessionId());
            } else {
                log.debug("기타 텍스트 메시지: {}", message);
            }
        } catch (Exception e) {
            log.error("텍스트 메시지 처리 중 오류", e);
            sendErrorMessage(session, "메시지 처리 실패: " + e.getMessage());
        }
    }

    /**
     * 바이너리 메시지 수신 처리
     * 프레임 배치 데이터 수신 및 처리
     */
    @OnMessage
    public void onBinaryMessage(ByteBuffer buffer, Session session) {
        try {
            // 플러터 클라이언트에서 전송한 형식에 맞게 파싱
            buffer.order(ByteOrder.BIG_ENDIAN);

            // 1. 메타데이터 JSON 길이 추출 (처음 4바이트)
            int jsonLength = buffer.getInt();

            // 2. JSON 메타데이터 추출
            byte[] jsonBytes = new byte[jsonLength];
            buffer.get(jsonBytes);
            String jsonStr = new String(jsonBytes, "UTF-8");

            // 3. 메타데이터 파싱
            Map<String, Object> metadata = objectMapper.readValue(jsonStr, Map.class);
            log.debug("메타데이터 파싱: {}", metadata);

            // 필수 필드 추출
            Long userId = Long.valueOf(metadata.get("userId").toString());
            Integer batchId = (Integer) metadata.get("batchId");
            Long timestamp = Long.valueOf(metadata.get("timestamp").toString());
            List<Map<String, Object>> frameInfos = (List<Map<String, Object>>) metadata.get("frames");

            // 4. 프레임 데이터 추출
            List<VideoDto.FrameData> frames = new ArrayList<>();
            for (Map<String, Object> frameInfo : frameInfos) {
                Integer frameLength = (Integer) frameInfo.get("length");
                Integer frameId = (Integer) frameInfo.get("frameId");
                byte[] frameData = new byte[frameLength];
                buffer.get(frameData);
                frames.add(VideoDto.FrameData.builder()
                        .data(frameData)
                        .frameId(frameId)
                        .build());
            }

            log.info("프레임 배치 수신 완료: userId={}, batchId={}, frames={}",
                    userId, batchId, frames.size());

            // 5. 프레임 처리 요청
            VideoDto.FrameBatchRequest request = VideoDto.FrameBatchRequest.builder()
                    .userId(userId)
                    .batchId(batchId)
                    .timestamp(timestamp)
                    .frames(frames)
                    .build();

            VideoDto.FrameProcessedResponse response = videoService.processFrameBatch(request);

            request.getFrames().clear();
            frames.clear();

            // 6. 응답 전송
            String responseJson = objectMapper.writeValueAsString(response);
            sendTextMessage(session, responseJson);

        } catch (Exception e) {
            log.error("바이너리 메시지 처리 중 오류", e);
            sendErrorMessage(session, "프레임 처리 실패: " + e.getMessage());
        }
    }

    /**
     * WebSocket 연결 종료 처리
     */
    @OnClose
    public void onClose(Session session) {
        sessions.remove(session.getId());
        // 세션 관련 리소스 명시적 정리
        session.getUserProperties().clear();
        log.info("WebSocket 연결 종료: sessionId={}", session.getId());
    }

    /**
     * WebSocket 오류 처리
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket 오류: sessionId={}", session.getId(), error);
        // 세션이 열려있는 경우에만 오류 메시지 전송 시도
        if (session.isOpen()) {
            sendErrorMessage(session, "연결 오류: " + error.getMessage());
        }
    }

    /**
     * 텍스트 메시지 전송
     */
    private void sendTextMessage(Session session, String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            log.error("메시지 전송 실패", e);
        }
    }

    /**
     * 오류 메시지 전송
     */
    private void sendErrorMessage(Session session, String errorMessage) {
        if (!session.isOpen()) {
            return; // 세션이 이미 닫힌 경우 메시지 전송 시도하지 않음
        }

        try {
            String errorJson = String.format("{\"status\":\"error\",\"message\":\"%s\"}", errorMessage);
            session.getBasicRemote().sendText(errorJson);
        } catch (IOException e) {
            // 로그 레벨을 ERROR에서 WARN으로 낮춤
            log.warn("오류 메시지 전송 실패: {}", e.getMessage());
        }
    }

}