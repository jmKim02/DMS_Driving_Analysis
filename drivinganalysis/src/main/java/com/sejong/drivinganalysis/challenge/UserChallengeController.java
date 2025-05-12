package com.sejong.drivinganalysis.challenge;

import com.sejong.drivinganalysis.challenge.UserChallengeService;
import com.sejong.drivinganalysis.challenge.dto.UserChallengeCreateRequest;
import com.sejong.drivinganalysis.challenge.dto.UserChallengeJoinRequest;
import com.sejong.drivinganalysis.challenge.dto.UserChallengeResponse;
import com.sejong.drivinganalysis.entity.UserChallenge;
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

    /**
     * [사용자] 공통 챌린지 참여 (JOIN)
     */
    @PostMapping("/join")
    public ResponseEntity<UserChallengeResponse> joinCommonChallenge(
            @RequestBody @Valid UserChallengeJoinRequest request) {
        UserChallenge uc = userChallengeService.joinCommonChallenge(request);
        return ResponseEntity.ok(UserChallengeResponse.fromEntity(uc));
    }

    /**
     *  사용자별 챌린지 목록 조회
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<UserChallengeResponse>> getUserChallenges(
            @PathVariable Long userId) {
        List<UserChallengeResponse> list = userChallengeService.getUserChallenges(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * [관리자] 수동으로 개인화 챌린지 생성
     */
    @PostMapping("/internal/create")
    public ResponseEntity<UserChallengeResponse> createCustomChallengeInternal(
            @RequestBody @Valid UserChallengeCreateRequest request) {
        UserChallenge uc = userChallengeService.createCustomChallenge(request);
        return ResponseEntity.ok(UserChallengeResponse.fromEntity(uc));
    }


    /**
     * [사용자] 챌린지 진행도 수동 업데이트
     */
    @PatchMapping("/{ucId}/progress")
    public ResponseEntity<UserChallengeResponse> updateProgress(
            @PathVariable Long ucId,
            @RequestParam Long value) {
        UserChallenge uc = userChallengeService.updateProgress(ucId, value);
        return ResponseEntity.ok(UserChallengeResponse.fromEntity(uc));
    }
}
