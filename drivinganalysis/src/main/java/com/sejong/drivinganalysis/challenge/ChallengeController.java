package com.sejong.drivinganalysis.challenge;

import com.sejong.drivinganalysis.challenge.dto.ChallengeCreateRequest;
import com.sejong.drivinganalysis.challenge.dto.ChallengeResponse;
import com.sejong.drivinganalysis.entity.Challenge;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    /**
     * [관리자용] 공통 챌린지 생성
     */
    @PostMapping
    public ResponseEntity<Challenge> createChallenge(
            @RequestBody @Valid ChallengeCreateRequest request) {
        Challenge created = challengeService.createChallenge(request);
        return ResponseEntity.ok(created);
    }

    /**
     * [관리자용] 모든 공통 챌린지 조회 (참여 여부와 관계없이 전부)
     */
    @GetMapping
    public ResponseEntity<List<Challenge>> getAllChallenges() {
        List<Challenge> list = challengeService.getAllChallenges();
        return ResponseEntity.ok(list);
    }

    /**
     * [관리자용] 챌린지 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChallenge(@PathVariable Long id) {
        challengeService.deleteChallenge(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * [관리자용] 오늘 기준 진행 중인 챌린지 목록 조회
     * 조건: startDate <= today && endDate >= today
     */
    @GetMapping("/active")
    public ResponseEntity<List<Challenge>> getActiveChallenges() {
        List<Challenge> active = challengeService.getActiveChallenges(LocalDate.now());
        return ResponseEntity.ok(active);
    }

    /**
     * [사용자용] 챌린지 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChallengeResponse> getChallenge(@PathVariable Long id) {
        Challenge challenge = challengeService.getChallenge(id);
        return ResponseEntity.ok(ChallengeResponse.fromEntity(challenge));
    }
}
