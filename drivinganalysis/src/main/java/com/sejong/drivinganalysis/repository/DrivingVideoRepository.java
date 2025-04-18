package com.sejong.drivinganalysis.repository;

import com.sejong.drivinganalysis.entity.DrivingVideo;
import com.sejong.drivinganalysis.entity.enums.VideoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrivingVideoRepository extends JpaRepository<DrivingVideo, Long> {
    List<DrivingVideo> findByUserUserIdOrderByUploadedAtDesc(Long userId);
    List<DrivingVideo> findByStatus(VideoStatus status);
}