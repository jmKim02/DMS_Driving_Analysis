package com.sejong.drivinganalysis.challenge;
import com.sejong.drivinganalysis.challenge.dto.ChallengeCreateRequest;
import com.sejong.drivinganalysis.challenge.ChallengeService;
import com.sejong.drivinganalysis.entity.Challenge;
import com.sejong.drivinganalysis.challenge.ChallengeRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

    private final ChallengeRepository challengeRepository;

    @Override
    @Transactional
    public Challenge createChallenge(ChallengeCreateRequest request) {
        validateChallengeRequest(request);
        Challenge challenge = Challenge.createChallenge(
                request.getTitle(),
                request.getDescription(),
                request.getTargetValue(),
                request.getTargetMetric(),
                request.getComparator(),
                request.getChallengeType(),
                request.getCategory(),
                request.getRewardInfo(),
                request.getStartDate(),
                request.getEndDate()
        );
        return challengeRepository.save(challenge);
    }

    @Override
    @Transactional(readOnly = true)
    public Challenge getChallenge(Long challengeId) {
        return challengeRepository.findById(challengeId)
                .orElseThrow(() -> new EntityNotFoundException("챌린지를 찾을 수 없습니다. ID=" + challengeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Challenge> getAllChallenges() {
        return challengeRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Challenge> getActiveChallenges(LocalDate date) {
        return challengeRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date);
    }

    @Override
    @Transactional
    public void deleteChallenge(Long challengeId) {
        if (!challengeRepository.existsById(challengeId)) {
            throw new EntityNotFoundException("삭제할 챌린지를 찾을 수 없습니다. ID=" + challengeId);
        }
        challengeRepository.deleteById(challengeId);
    }

    private void validateChallengeRequest(ChallengeCreateRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ValidationException("챌린지 종료일은 시작일 이후여야 합니다.");
        }
        if (!("<=".equals(request.getComparator()) || ">=".equals(request.getComparator()))) {
            throw new ValidationException("comparator는 '<=' 또는 '>='만 허용됩니다.");
        }
        boolean duplicate = challengeRepository
                .existsByTitleAndStartDateAndEndDate(
                        request.getTitle(), request.getStartDate(), request.getEndDate());
        if (duplicate) {
            throw new ValidationException("동일한 제목과 기간의 챌린지가 이미 존재합니다.");
        }
    }
}
