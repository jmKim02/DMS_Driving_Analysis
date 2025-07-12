package com.sejong.drivinganalysis.controller;

import com.sejong.drivinganalysis.repository.UserChallengeRepository;
import com.sejong.drivinganalysis.service.UserChallengeService;
import com.sejong.drivinganalysis.dto.challengedto.UserChallengeCreateRequest;
import com.sejong.drivinganalysis.dto.challengedto.UserChallengeJoinRequest;
import com.sejong.drivinganalysis.dto.challengedto.UserChallengeResponse;
import com.sejong.drivinganalysis.entity.UserChallenge;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-challenges")
@RequiredArgsConstructor
public class UserChallengeController {

    private final UserChallengeService userChallengeService;
    private final UserChallengeRepository userChallengeRepository;

    /**
     * [사용자] 공통 챌린지 참여 (JOIN)
     */
    @PostMapping("/join")
    public ResponseEntity<UserChallengeResponse> joinCommonChallenge(
            @RequestBody @Valid UserChallengeJoinRequest request) {
        UserChallenge uc = userChallengeService.joinCommonChallenge(request);
        Long displayValue = userChallengeService.calculateDisplayValue(uc);
        return ResponseEntity.ok(UserChallengeResponse.fromEntity(uc, displayValue));
    }

    /**
     * [사용자] 내가 참여한 챌린지 목록 조회 (displayValue 포함)
     */
    @GetMapping("/joined/{userId}")
    public ResponseEntity<List<UserChallengeResponse>> getJoinedUserChallenges(
            @PathVariable Long userId) {
        List<UserChallengeResponse> list = userChallengeService.getUserChallengeResponsesWithDisplayValue(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * [사용자] 현재 참여 가능한 챌린지 목록 조회
     * 조건: startDate <= 오늘 && endDate >= 오늘 && 아직 참여하지 않은 챌린지
     */
    @GetMapping("/available/{userId}")
    public ResponseEntity<List<UserChallengeResponse>> getAvailableUserChallenges(
            @PathVariable Long userId) {
        List<UserChallengeResponse> list = userChallengeService.getAvailableUserChallenges(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * [관리자] 수동으로 개인화 챌린지 생성 (테스터용)
     */
    @PostMapping("/internal/create")
    public ResponseEntity<UserChallengeResponse> createCustomChallengeInternal(
            @RequestBody @Valid UserChallengeCreateRequest request) {
        UserChallenge uc = userChallengeService.createCustomChallenge(request);
        Long displayValue = userChallengeService.calculateDisplayValue(uc);
        return ResponseEntity.ok(UserChallengeResponse.fromEntity(uc, displayValue));
    }

    /**
     * [사용자] 챌린지 진행도 수동 업데이트 (테스터용)
     */
    @PatchMapping("/{ucId}/progress")
    public ResponseEntity<UserChallengeResponse> updateProgress(
            @PathVariable Long ucId,
            @RequestParam Long value) {
        UserChallenge uc = userChallengeService.updateProgress(ucId, value);
        Long displayValue = userChallengeService.calculateDisplayValue(uc);
        return ResponseEntity.ok(UserChallengeResponse.fromEntity(uc, displayValue));
    }

    /**
     * [사용자용] 내가 참여한 챌린지 상세 조회
     */
    @GetMapping("/detail/{userChallengeId}")
    public ResponseEntity<UserChallengeResponse> getUserChallengeDetail(
            @PathVariable Long userChallengeId) {
        UserChallenge uc = userChallengeRepository.findById(userChallengeId)
                .orElseThrow(() -> new EntityNotFoundException("UserChallenge가 없습니다. ID=" + userChallengeId));
        Long displayValue = userChallengeService.calculateDisplayValue(uc);
        return ResponseEntity.ok(UserChallengeResponse.fromEntity(uc, displayValue));
    }

}
