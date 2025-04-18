
package com.sejong.drivinganalysis.repository;

import com.sejong.drivinganalysis.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    Optional<AnalysisResult> findByVideoVideoId(Long videoId);
    List<AnalysisResult> findByUserUserIdOrderByAnalyzedAtDesc(Long userId);
}