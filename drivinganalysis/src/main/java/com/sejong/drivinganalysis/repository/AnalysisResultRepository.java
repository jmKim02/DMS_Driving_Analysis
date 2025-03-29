package com.sejong.drivinganalysis.repository;

import com.sejong.drivinganalysis.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    /**
     * 비디오 ID로 분석 결과 조회
     */
    Optional<AnalysisResult> findByVideoVideoId(Long videoId);

    /**
     * 사용자 ID로 분석 결과 목록 조회
     */
    List<AnalysisResult> findByUserUserIdOrderByCompletedAtDesc(Long userId);
}