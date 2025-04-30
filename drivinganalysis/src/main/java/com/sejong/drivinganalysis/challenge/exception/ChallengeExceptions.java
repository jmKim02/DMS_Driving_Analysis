package com.sejong.drivinganalysis.challenge.exception;

import com.sejong.drivinganalysis.dto.common.ApiResponse;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


public class ChallengeExceptions {

    //───────────────────────────────────────────────────
    // 1) 예외 정의
    //───────────────────────────────────────────────────

    public static class ChallengeNotFoundException extends RuntimeException {
        public ChallengeNotFoundException(Long id) {
            super("챌린지를 찾을 수 없습니다. id=" + id);
        }
    }

    public static class DuplicateParticipationException extends RuntimeException {
        public DuplicateParticipationException(Long userId, Long challengeId) {
            super("사용자(id=" + userId + ")가 이미 챌린지(id=" + challengeId + ")에 참여했습니다.");
        }
    }


    public static class DuplicateChallengeException extends RuntimeException {
        public DuplicateChallengeException(String message) {
            super(message);
        }
    }
    //───────────────────────────────────────────────────
    // 2) 예외 처리 핸들러
    //───────────────────────────────────────────────────

    @RestControllerAdvice(basePackages = "com.sejong.drivinganalysis.challenge")
    public static class ChallengeExceptionHandler {

        @ExceptionHandler(ChallengeNotFoundException.class)
        public ResponseEntity<ApiResponse<Void>> handleNotFound(ChallengeNotFoundException ex) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("NOT_FOUND", ex.getMessage()));
        }

        @ExceptionHandler(DuplicateParticipationException.class)
        public ResponseEntity<ApiResponse<Void>> handleConflict(DuplicateParticipationException ex) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("ALREADY_JOINED", ex.getMessage()));
        }

        @ExceptionHandler(DuplicateChallengeException.class)
        public ResponseEntity<ApiResponse<Void>> handleDupChallenge(DuplicateChallengeException ex) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("DUPLICATE_CHALLENGE", ex.getMessage()));
        }
        /**
         * DB 유니크 제약(중복 챌린지) 위반 처리
         */
        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ApiResponse<Void>> handleDuplicateChallenge(DataIntegrityViolationException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ConstraintViolationException cve
                    && "uk_challenge_unique".equals(cve.getConstraintName())) {
                String msg = "중복된 챌린지입니다. 같은 제목·기간의 챌린지를 다시 생성할 수 없습니다.";
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error("DUPLICATE_CHALLENGE", msg));
            }
            // 다른 무결성 오류는 서버 에러로 위임
            return handleAll(ex);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleAll(Exception ex) {
            ex.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "서버 오류가 발생했습니다. 관리자에게 문의하세요."));
        }
    }
}
