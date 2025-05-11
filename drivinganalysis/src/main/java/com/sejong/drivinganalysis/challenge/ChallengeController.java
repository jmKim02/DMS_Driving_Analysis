package com.sejong.drivinganalysis.challenge;
import com.sejong.drivinganalysis.challenge.ChallengeService;
import com.sejong.drivinganalysis.challenge.dto.ChallengeCreateRequest;
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
     * 공통 챌린지 생성
     */
    @PostMapping
    public ResponseEntity<Challenge> createChallenge(
            @RequestBody @Valid ChallengeCreateRequest request) {
        Challenge created = challengeService.createChallenge(request);
        return ResponseEntity.ok(created);
    }

    /**
     * 전체 챌린지 조회
     */
    @GetMapping
    public ResponseEntity<List<Challenge>> getAllChallenges() {
        List<Challenge> list = challengeService.getAllChallenges();
        return ResponseEntity.ok(list);
    }

    /**
     * 오늘 기준 진행중인 챌린지 조회
     */
    @GetMapping("/active")
    public ResponseEntity<List<Challenge>> getActiveChallenges() {
        List<Challenge> active = challengeService.getActiveChallenges(LocalDate.now());
        return ResponseEntity.ok(active);
    }

    /**
     * 단일 챌린지 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<Challenge> getChallenge(@PathVariable Long id) {
        Challenge challenge = challengeService.getChallenge(id);
        return ResponseEntity.ok(challenge);
    }

    /**
     * 챌린지 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChallenge(@PathVariable Long id) {
        challengeService.deleteChallenge(id);
        return ResponseEntity.noContent().build();
    }


}


