package com.sejong.drivinganalysis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    // 파일 메타데이터 캐싱을 위한 맵
    private final Map<String, FileMetadata> fileMetadataCache = new ConcurrentHashMap<>();

    // 파일 경로 캐싱을 위한 맵
    private final Map<String, String> filePathCache = new ConcurrentHashMap<>();

    // 서비스 초기화 시 업로드 디렉토리 생성
    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.uploadDir = uploadDir;
        try {
            Files.createDirectories(Paths.get(uploadDir));
            log.info("업로드 디렉토리 생성: {}", uploadDir);
        } catch (IOException e) {
            log.error("업로드 디렉토리 생성 실패", e);
            throw new RuntimeException("업로드 디렉토리를 생성할 수 없습니다.", e);
        }
    }

    // 프레임 데이터를 개별 파일로 저장하고 디렉토리 경로 반환 (스트리밍 방식으로 개선)
    public String uploadFrames(List<MultipartFile> frames, String fileKey, Long userId, Long timestamp) throws IOException {
        // 프레임을 저장할 디렉토리 생성
        Path frameDirPath = Paths.get(uploadDir, fileKey);
        Files.createDirectories(frameDirPath);

        int frameCount = 0;

        // 각 프레임을 개별 파일로 저장 - 스트리밍 방식 사용
        for (int i = 0; i < frames.size(); i++) {
            MultipartFile frame = frames.get(i);
            // 파일명 형식: 0001_user123_1617123456789.jpg
            String frameFileName = String.format("%04d_user%d_%d.jpg", i, userId, timestamp);
            Path frameFilePath = frameDirPath.resolve(frameFileName);

            // 스트리밍 방식으로 파일 저장
            saveFrameWithStreaming(frame, frameFilePath);
            frameCount++;
        }

        String relativePath = fileKey;

        // 메타데이터 캐싱
        fileMetadataCache.put(fileKey, new FileMetadata(relativePath, frameCount, true));
        // 파일 경로 캐싱
        filePathCache.put(fileKey, frameDirPath.toString());

        log.info("프레임 데이터가 디렉토리에 저장됨: {}, 총 {}개 프레임", frameDirPath, frameCount);
        return relativePath;
    }

    // 스트리밍 방식으로 파일 저장 (메모리 사용 최소화)
    private void saveFrameWithStreaming(MultipartFile file, Path targetPath) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             ReadableByteChannel readChannel = Channels.newChannel(inputStream);
             FileChannel writeChannel = FileChannel.open(targetPath,
                     StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

            // 버퍼 크기 8KB로 설정
            final int bufferSize = 8192;
            long position = 0;
            long transferred;

            // 8KB 단위로 데이터 전송
            while ((transferred = writeChannel.transferFrom(readChannel, position, bufferSize)) > 0) {
                position += transferred;
            }
        }
    }

    // 로컬에서 디렉토리와 파일들 삭제 (캐시 정보도 함께 삭제)
    public void deleteFile(String dirKey) {
        if (dirKey == null || dirKey.isEmpty()) {
            log.warn("삭제할 파일 키가 비어있습니다");
            return;
        }

        try {
            // 캐시에서 파일 경로 가져오기
            String fullPath = filePathCache.getOrDefault(dirKey,
                    Paths.get(uploadDir, dirKey).toString());

            Path dirPath = Paths.get(fullPath);

            // 디렉토리 존재 여부 확인 (캐시 먼저 확인)
            boolean exists = isDirectoryExists(dirKey, dirPath);

            if (exists) {
                // 디렉토리 내의 모든 파일 삭제
                Files.walk(dirPath)
                        .sorted((p1, p2) -> -p1.compareTo(p2))  // 역순으로 정렬하여 파일 먼저 삭제
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.error("파일 삭제 실패: {}", path, e);
                            }
                        });

                log.info("로컬에서 디렉토리 삭제됨: {}", dirPath);

                // 캐시에서 제거
                fileMetadataCache.remove(dirKey);
                filePathCache.remove(dirKey);
            } else {
                log.warn("삭제할 디렉토리가 존재하지 않음: {}", dirPath);
            }
        } catch (IOException e) {
            log.error("디렉토리 삭제 실패: {}", dirKey, e);
        }
    }

    // 디렉토리 존재 여부 확인 (캐시 이용)
    private boolean isDirectoryExists(String dirKey, Path dirPath) {
        // 캐시에서 먼저 확인
        FileMetadata metadata = fileMetadataCache.get(dirKey);
        if (metadata != null) {
            return metadata.exists;
        }

        // 캐시에 없으면 파일 시스템에서 확인
        boolean exists = Files.exists(dirPath);

        // 캐시 업데이트
        if (exists) {
            try {
                long frameCount = Files.list(dirPath).count();
                fileMetadataCache.put(dirKey, new FileMetadata(dirKey, frameCount, true));
            } catch (IOException e) {
                log.error("디렉토리 내 파일 개수 확인 실패: {}", dirPath, e);
                fileMetadataCache.put(dirKey, new FileMetadata(dirKey, 0, true));
            }
        } else {
            fileMetadataCache.put(dirKey, new FileMetadata(dirKey, 0, false));
        }

        return exists;
    }

    // 파일 저장 경로의 전체 경로를 반환하는 메소드 (캐시 이용)
    public String getFullPath(String relativePath) {
        // 캐시에서 경로 확인
        String cachedPath = filePathCache.get(relativePath);
        if (cachedPath != null) {
            return cachedPath;
        }

        // 캐시에 없으면 경로 생성 후 캐싱
        String fullPath = Paths.get(uploadDir, relativePath).toString();
        filePathCache.put(relativePath, fullPath);
        return fullPath;
    }

    // 저장된 프레임 개수 확인 (캐시 이용)
    public long getFrameCount(String dirKey) {
        // 캐시에서 먼저 확인
        FileMetadata metadata = fileMetadataCache.get(dirKey);
        if (metadata != null) {
            return metadata.frameCount;
        }

        // 캐시에 없으면 파일 시스템에서 확인
        try {
            Path dirPath = Paths.get(getFullPath(dirKey));
            if (Files.exists(dirPath)) {
                long frameCount = Files.list(dirPath).count();
                // 캐시 업데이트
                fileMetadataCache.put(dirKey, new FileMetadata(dirKey, frameCount, true));
                return frameCount;
            }
        } catch (IOException e) {
            log.error("프레임 개수 확인 실패: {}", dirKey, e);
        }

        return 0;
    }

    // 파일 메타데이터 클래스 (캐싱용)
    private static class FileMetadata {
        private final String path;
        private final long frameCount;
        private final boolean exists;

        public FileMetadata(String path, long frameCount, boolean exists) {
            this.path = path;
            this.frameCount = frameCount;
            this.exists = exists;
        }
    }
}