package com.sejong.drivinganalysis.repository;

import com.sejong.drivinganalysis.entity.DrivingVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DrivingVideoRepository extends JpaRepository<DrivingVideo, Long> {
}