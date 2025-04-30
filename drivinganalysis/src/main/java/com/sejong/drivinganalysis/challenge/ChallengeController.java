package com.sejong.drivinganalysis.challenge;

import com.sejong.drivinganalysis.dto.common.ApiResponse;
import com.sejong.drivinganalysis.challenge.dto.ChallengeCreateRequest;
import com.sejong.drivinganalysis.challenge.dto.ChallengeResponse;
import com.sejong.drivinganalysis.challenge.dto.UserChallengeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    /**
     * 1. [관리자] 챌린지 생성
     *    - 201 Created
     *    - Location: /api/challenges/{id}
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createChallenge(
            @RequestBody @Valid ChallengeCreateRequest request
    ) {
        Long challengeId = challengeService.createChallenge(request);
        // ApiResponse.success 에 Long(id) 를 담아서 보냄
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(challengeId));
    }

    /**
     * 2. [유저] 전체 챌린지 조회 (키워드 검색 optional)
     *
     */
    @GetMapping
    public ResponseEntity<List<ChallengeResponse>> getAllChallenges(
            @RequestParam(required = false) String keyword
    ) {
        List<ChallengeResponse> list = (keyword != null && !keyword.isBlank())
                ? challengeService.searchChallengesByKeyword(keyword)
                : challengeService.getAllChallenges();
        return ResponseEntity.ok(list);
    }

    /**
     * 3. [유저] 챌린지 단건 조회
     *    - GET /api/challenges/{challengeId}
     */
    @GetMapping("/{challengeId}")
    public ResponseEntity<ChallengeResponse> getChallengeById(
            @PathVariable Long challengeId
    ) {
        ChallengeResponse dto = challengeService.getChallengeById(challengeId);
        return ResponseEntity.ok(dto);
    }

    /**
     * 4. [유저] 특정 챌린지 참여
     *    - 이미 참여한 경우 409 Conflict
     */
    @PostMapping("/{challengeId}/join")
    public ResponseEntity<ApiResponse<Void>> joinChallenge(
            @PathVariable Long challengeId,
            @RequestParam Long userId
    ) {
        challengeService.joinChallenge(challengeId, userId);
        // 제네릭 타입 힌트 <Void> 를 넣어줍니다
        return ResponseEntity
                .ok(ApiResponse.<Void>success(null));
    }


    /**
     * 5. [유저] 내 참여중 챌린지 조회
     *    - GET /api/challenges/my?userId=...
     */
    @GetMapping("/my")
    public ResponseEntity<List<UserChallengeResponse>> getMyChallenges(
            @RequestParam Long userId
    ) {
        List<UserChallengeResponse> myList = challengeService.getMyChallenges(userId);
        return ResponseEntity.ok(myList);
    }
}
