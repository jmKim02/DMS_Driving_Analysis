package com.sejong.drivinganalysis.repository;

import com.sejong.drivinganalysis.entity.DrivingVideo;
import com.sejong.drivinganalysis.entity.enums.VideoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DrivingVideoRepository extends JpaRepository<DrivingVideo, Long> {

    /**
     * 사용자 ID로 모든 영상 조회
     * @param userId 사용자 ID
     * @return 사용자가 업로드한 영상 목록
     */
    List<DrivingVideo> findByUserUserIdOrderByUploadedAtDesc(Long userId);

    /**
     * 특정 상태의 영상 조회
     * @param status 영상 상태
     * @return 해당 상태의 영상 목록
     */
    List<DrivingVideo> findByStatus(VideoStatus status);

    /**
     * 영상 ID와 사용자 ID로 영상 조회
     * @param videoId 영상 ID
     * @param userId 사용자 ID
     * @return 조건에 맞는 영상
     */
    Optional<DrivingVideo> findByVideoIdAndUserUserId(Long videoId, Long userId);
}