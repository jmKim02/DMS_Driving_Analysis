package com.sejong.drivinganalysis.challenge;

import com.sejong.drivinganalysis.challenge.dto.ChallengeCreateRequest;
import com.sejong.drivinganalysis.challenge.dto.ChallengeResponse;
import com.sejong.drivinganalysis.challenge.dto.UserChallengeResponse;
import com.sejong.drivinganalysis.challenge.exception.ChallengeExceptions;
import com.sejong.drivinganalysis.challenge.exception.ChallengeExceptions.DuplicateChallengeException;
import com.sejong.drivinganalysis.entity.Challenge;
import com.sejong.drivinganalysis.entity.User;
import com.sejong.drivinganalysis.entity.UserChallenge;
import com.sejong.drivinganalysis.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final UserRepository userRepository;

    /**
     * 관리자용 챌린지 생성
     * 동일한 제목·기간의 챌린지 중복 생성 방지
     */
    @Override
    @Transactional
    public Long createChallenge(ChallengeCreateRequest request) {
        // 사전 중복 검사
        boolean exists = challengeRepository.existsByTitleAndStartDateAndEndDate(
                request.getTitle(),
                request.getStartDate(),
                request.getEndDate()
        );
        if (exists) {
            throw new DuplicateChallengeException(
                    "중복된 챌린지입니다. 같은 제목·기간의 챌린지를 다시 생성할 수 없습니다."
            );
        }

        // 정상 저장
        Challenge c = Challenge.createChallenge(
                request.getTitle(),
                request.getDescription(),
                request.getTargetValue(),
                request.getTargetMetric(),
                request.getChallengeType(),
                request.getCategory(),
                request.getRewardInfo(),
                request.getStartDate(),
                request.getEndDate()
        );
        return challengeRepository.save(c).getChallengeId();
    }

    /** 전체 챌린지 조회 */
    @Override
    public List<ChallengeResponse> getAllChallenges() {
        return challengeRepository.findAll().stream()
                .map(ChallengeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /** 키워드 검색 */
    @Override
    public List<ChallengeResponse> searchChallengesByKeyword(String keyword) {
        return challengeRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(ChallengeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /** 단건 조회 */
    @Override
    public ChallengeResponse getChallengeById(Long challengeId) {
        Challenge c = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new EntityNotFoundException("챌린지를 찾을 수 없습니다. id=" + challengeId));
        return ChallengeResponse.fromEntity(c);
    }

    /** 챌린지 참여 */
    @Override
    @Transactional
    public boolean joinChallenge(Long challengeId, Long userId) {
        if (userChallengeRepository.existsByUserUserIdAndChallengeChallengeId(userId, challengeId)) {
            throw new ChallengeExceptions.DuplicateParticipationException(userId, challengeId);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. id=" + userId));
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new EntityNotFoundException("챌린지를 찾을 수 없습니다. id=" + challengeId));

        UserChallenge uc = UserChallenge.joinChallenge(user, challenge);
        userChallengeRepository.save(uc);
        return true;
    }

    /** 내가 참여한 챌린지 조회 */
    @Override
    public List<UserChallengeResponse> getMyChallenges(Long userId) {
        return userChallengeRepository.findByUserUserId(userId).stream()
                .map(UserChallengeResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
