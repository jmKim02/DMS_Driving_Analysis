package com.sejong.drivinganalysis.service;

import com.sejong.drivinganalysis.dto.VideoDto;
import com.sejong.drivinganalysis.dto.ai.AnalysisRequest;
import com.sejong.drivinganalysis.dto.ai.AnalysisResponse;
import com.sejong.drivinganalysis.entity.AnalysisResult;
import com.sejong.drivinganalysis.entity.DrivingVideo;
import com.sejong.drivinganalysis.entity.User;
import com.sejong.drivinganalysis.entity.enums.AnalysisStatus;
import com.sejong.drivinganalysis.entity.enums.VideoStatus;
import com.sejong.drivinganalysis.exception.ApiException;
import com.sejong.drivinganalysis.exception.ResourceNotFoundException;
import com.sejong.drivinganalysis.repository.AnalysisResultRepository;
import com.sejong.drivinganalysis.repository.DrivingVideoRepository;
import com.sejong.drivinganalysis.repository.UserRepository;
import com.sejong.drivinganalysis.utils.FileDeleteEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final DrivingVideoRepository drivingVideoRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final FileStorageService fileStorageService;
    private final ApplicationEventPublisher eventPublisher; // 이벤트 발행자 추가

    @Value("${ai.server.url}")
    private String aiServerUrl;

    // SSE 연결을 관리하는 Map
    private final Map<Long, SseEmitter> userEmitters = new ConcurrentHashMap<>();

    // 프레임 데이터 처리 및 AI 서버로 전송
    @Transactional
    public VideoDto.UploadResponse processFrames(Long userId, List<MultipartFile> frames, Long timestamp) throws IOException {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // 파일명 생성 (userId_timestamp_uuid)
        String fileKey = String.format("%d_%d_%s", userId, timestamp, UUID.randomUUID().toString().substring(0, 8));

        // 로컬에 프레임 업로드 (최적화된 메서드 사용)
        String filePath = fileStorageService.uploadFrames(frames, fileKey, userId, timestamp);

        // 저장된 프레임 개수 확인 (파일 시스템 직접 접근 대신 서비스 메서드 사용)
        long savedFramesCount = fileStorageService.getFrameCount(filePath);

        if (savedFramesCount == 0) {
            log.error("프레임 저장 실패 또는 저장된 프레임 없음: {}", filePath);
            throw new IOException("프레임을 저장할 수 없습니다");
        }

        log.info("프레임 저장 완료: {} 개의 프레임이 {}에 저장됨", savedFramesCount, filePath);

        // DB에 비디오 정보 저장
        DrivingVideo video = DrivingVideo.createVideo(
                user,
                filePath,
                frames.size() / 30 // 30fps 기준으로 대략적인 동영상 길이(초) 계산
        );

        DrivingVideo savedVideo = drivingVideoRepository.save(video);

        // 파일 저장 확인 후 비동기로 AI 서버에 분석 요청
        sendAnalysisRequest(savedVideo.getVideoId(), filePath, userId);

        return VideoDto.UploadResponse.builder()
                .videoId(savedVideo.getVideoId())
                .fileName(fileKey)
                .fileSize((long) frames.size())
                .uploadedAt(savedVideo.getUploadedAt())
                .status(VideoStatus.UPLOADED.name())
                .build();
    }

    @Transactional
    public void sendAnalysisRequest(Long videoId, String filePath, Long userId) {
        // 절대 경로로 변환 (캐시 활용)
        String fullPath = fileStorageService.getFullPath(filePath);

        log.info("분석 요청 전 절대 경로 확인: {}", fullPath);

        // 경로 확인 시 파일 시스템 직접 접근 대신 캐싱된 정보 활용
        long frameCount = fileStorageService.getFrameCount(filePath);
        boolean exists = frameCount > 0;

        log.info("분석 요청 전 프레임 확인: path={}, exists={}, frameCount={}",
                fullPath, exists, frameCount);

        if (!exists) {
            log.error("분석 요청 실패: 경로가 존재하지 않음 - {}", fullPath);
            updateVideoStatus(videoId, VideoStatus.ERROR);
            return;
        }

        AnalysisRequest request = new AnalysisRequest(videoId, fullPath, userId);

        // 재시도 관련 변수
        final int MAX_RETRY = 2;
        int retryCount = 0;
        boolean requestSuccess = false;

        // 비디오 상태 업데이트
        updateVideoStatus(videoId, VideoStatus.PROCESSING);

        while (!requestSuccess && retryCount <= MAX_RETRY) {
            try {
                // AI 서버에 분석 요청
                log.info("AI 서버에 분석 요청: videoId={}, path={}, 시도 횟수={}",
                        videoId, fullPath, retryCount + 1);

                AnalysisResponse response = restTemplate.postForObject(
                        aiServerUrl + "/api/analyze",
                        request,
                        AnalysisResponse.class
                );

                // 응답 체크
                if (response != null && "success".equals(response.getStatus())) {
                    log.info("AI 서버에 분석 요청 전송 완료: videoId={}", videoId);
                    requestSuccess = true;
                } else {
                    // 응답은 받았지만 성공이 아닌 경우 (서버 측 오류)
                    log.warn("AI 서버 응답 오류: videoId={}, 응답 상태={}, 메시지={}",
                            videoId,
                            response != null ? response.getStatus() : "null",
                            response != null ? response.getMessage() : "응답 없음");

                    retryCount++;
                    if (retryCount <= MAX_RETRY) {
                        // 일시적 오류로 간주하고 잠시 대기 후 재시도
                        waitBeforeRetry(retryCount);
                    }
                }
            } catch (ResourceAccessException e) {
                // 네트워크 연결 문제 (일시적 오류로 간주)
                log.warn("AI 서버 네트워크 연결 실패 (재시도 예정): videoId={}, 시도 횟수={}, 오류={}",
                        videoId, retryCount + 1, e.getMessage());

                retryCount++;
                if (retryCount <= MAX_RETRY) {
                    waitBeforeRetry(retryCount);
                }
            } catch (HttpStatusCodeException e) {
                // HTTP 상태 코드 오류
                int statusCode = e.getStatusCode().value();

                if (statusCode >= 500) {
                    // 5xx 오류는 서버 측 일시적 오류로 간주
                    log.warn("AI 서버 일시적 오류 (재시도 예정): videoId={}, 상태 코드={}, 시도 횟수={}",
                            videoId, statusCode, retryCount + 1);

                    retryCount++;
                    if (retryCount <= MAX_RETRY) {
                        waitBeforeRetry(retryCount);
                    }
                } else {
                    // 4xx 오류는 클라이언트 측 오류로 간주 (재시도 안함)
                    log.error("AI 서버 요청 형식 오류 (재시도 안함): videoId={}, 상태 코드={}", videoId, statusCode);
                    updateVideoStatus(videoId, VideoStatus.ERROR);
                    return;
                }
            } catch (Exception e) {
                // 기타 예외
                log.error("AI 서버 연결 중 예상치 못한 오류: videoId={}, 시도 횟수={}", videoId, retryCount + 1, e);

                retryCount++;
                if (retryCount <= MAX_RETRY) {
                    waitBeforeRetry(retryCount);
                }
            }
        }

        // 모든 재시도 후에도 실패한 경우
        if (!requestSuccess) {
            log.error("최대 재시도 횟수({})를 초과하여 분석 요청 실패: videoId={}", MAX_RETRY, videoId);
            updateVideoStatus(videoId, VideoStatus.ERROR);
        }
    }

    /**
     * 재시도 전 지수 백오프 방식으로 대기
     * @param retryCount 현재 재시도 횟수
     */
    private void waitBeforeRetry(int retryCount) {
        try {
            // 지수 백오프: 첫 번째 재시도는 1초, 두 번째는 2초
            long waitTimeMs = (long) Math.pow(2, retryCount - 1) * 1000;
            log.info("재시도 전 {}ms 대기", waitTimeMs);
            Thread.sleep(waitTimeMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("재시도 대기 중 인터럽트 발생");
        }
    }

    // 영상 상태 업데이트
    @Transactional
    public void updateVideoStatus(Long videoId, VideoStatus status) {
        DrivingVideo video = drivingVideoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("DrivingVideo", "id", videoId));
        video.updateStatus(status);

        if (status == VideoStatus.ANALYZED) {
            video.setProcessedAt(LocalDateTime.now());
        }

        drivingVideoRepository.save(video);
        log.info("비디오 상태 업데이트: videoId={}, status={}", videoId, status);
    }

    // AI 서버로부터의 분석 결과 처리
    @Transactional
    public void processAnalysisResult(Long videoId, AnalysisResponse analysisResult) {
        log.info("분석 결과 처리 시작: videoId={}, drowsinessDetected={}",
                videoId, analysisResult.isDrowsinessDetected());

        try {
            // 비디오 상태 업데이트
            updateVideoStatus(videoId, VideoStatus.ANALYZED);

            DrivingVideo video = drivingVideoRepository.findById(videoId)
                    .orElseThrow(() -> new ResourceNotFoundException("DrivingVideo", "id", videoId));

            User user = video.getUser();

            // 졸음 감지 시 알람 전송
            if (analysisResult.isDrowsinessDetected()) {
                log.info("졸음 감지됨: userId={}", user.getUserId());
                sendDrowsinessAlert(user.getUserId());
            }

            // 분석 결과 DB에 저장
            AnalysisResult result = AnalysisResult.createAnalysisResult(
                    video,
                    user,
                    analysisResult.getDrowsinessCount(),
                    analysisResult.getPhoneUsageCount(),
                    analysisResult.getSmokingCount(),
                    video.getDuration()
            );

            // 운전 점수 계산 로직 호출
            result.calculateScore();

            // 분석 결과 저장
            analysisResultRepository.save(result);

            log.info("분석 결과 저장 완료: videoId={}, score={}", videoId, result.getDrivingScore());


            // 비동기 파일 삭제를 위한 이벤트 발행 (여기가 변경된 부분)
            eventPublisher.publishEvent(new FileDeleteEvent(video.getFilePath(), videoId));
        } catch (Exception e) {
            log.error("분석 결과 처리 중 오류 발생: videoId={}", videoId, e);
        }
    }

    // 비동기 파일 삭제 이벤트 핸들러 (새로 추가)
    @EventListener
    @Async("fileProcessingExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFileDeleteEvent(FileDeleteEvent event) {
        String filePath = event.getFilePath();
        Long videoId = event.getVideoId();

        log.info("비동기 파일 삭제 시작: videoId={}, filePath={}", videoId, filePath);

        int retryCount = 0;
        final int MAX_RETRY = 3;
        boolean deleteSuccess = false;

        while (!deleteSuccess && retryCount < MAX_RETRY) {
            try {
                fileStorageService.deleteFile(filePath);
                deleteSuccess = true;
                log.info("파일 삭제 성공: videoId={}, filePath={}", videoId, filePath);
            } catch (Exception e) {
                retryCount++;
                log.warn("파일 삭제 실패 (재시도 {}/{}): videoId={}, filePath={}, 오류={}",
                        retryCount, MAX_RETRY, videoId, filePath, e.getMessage());

                if (retryCount < MAX_RETRY) {
                    try {
                        // 지수 백오프로 대기
                        Thread.sleep((long) Math.pow(2, retryCount) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        if (!deleteSuccess) {
            // 최대 재시도 후에도 실패한 경우 로그만 남김
            // 추후 필요시 삭제 큐에 추가하는 로직 구현 가능
            log.error("최대 재시도 후에도 파일 삭제 실패: videoId={}, filePath={}", videoId, filePath);
        }
    }

    // 졸음 감지 알람 전송 (SSE)
    private void sendDrowsinessAlert(Long userId) {
        SseEmitter emitter = userEmitters.get(userId);
        if (emitter != null) {
            try {
                log.info("사용자 {}에게 졸음 알람 전송", userId);
                emitter.send(SseEmitter.event()
                        .name("drowsiness")
                        .data("졸음 상태가 감지되었습니다. 안전한 장소에 정차하고 휴식을 취하세요."));
                log.info("사용자 {}에게 졸음 알람 전송 성공", userId);
            } catch (IOException e) {
                log.error("사용자 {}에게 알람 전송 실패", userId, e);
                userEmitters.remove(userId);
            }
        } else {
            log.warn("사용자 {}의 활성 SSE 연결을 찾을 수 없음, 연결 생성 시도", userId);
            // 연결이 없으면 새로 생성 시도
            createSseConnection(userId);
            // 생성 후 다시 알림 전송 시도
            sendDrowsinessAlert(userId);
        }
    }

    // SSE 연결 생성
    public SseEmitter createSseConnection(Long userId) {
        // 기존 연결이 있으면 완료 처리
        SseEmitter oldEmitter = userEmitters.get(userId);
        if (oldEmitter != null) {
            oldEmitter.complete();
        }

        // 새 연결 생성 (30분 타임아웃)
        SseEmitter emitter = new SseEmitter(1_800_000L);

        // 연결 완료, 오류, 타임아웃 시 콜백 등록
        emitter.onCompletion(() -> {
            log.info("사용자 {}의 SSE 연결 완료됨", userId);
            userEmitters.remove(userId);
        });
        emitter.onError(e -> {
            log.error("사용자 {}의 SSE 연결 오류 발생", userId, e);
            userEmitters.remove(userId);
        });
        emitter.onTimeout(() -> {
            log.info("사용자 {}의 SSE 연결 타임아웃됨", userId);
            userEmitters.remove(userId);
        });

        // 연결 유지를 위한 더미 이벤트 전송
        try {
            emitter.send(SseEmitter.event().name("connect").data("Connected"));
        } catch (IOException e) {
            log.error("SSE 연결 초기화 실패: userId={}", userId, e);
            throw new ApiException("SSE_ERROR", "SSE 연결을 초기화하는 데 실패했습니다.");
        }

        // Map에 저장
        userEmitters.put(userId, emitter);
        log.info("사용자 {}의 SSE 연결 생성됨", userId);

        return emitter;
    }

    // 영상 상태 조회
    public VideoDto.StatusResponse getVideoStatus(Long videoId) {
        DrivingVideo video = drivingVideoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("DrivingVideo", "id", videoId));

        return VideoDto.StatusResponse.builder()
                .videoId(video.getVideoId())
                .status(video.getStatus().name())
                .uploadedAt(video.getUploadedAt())
                .processedAt(video.getProcessedAt())
                .build();
    }
}