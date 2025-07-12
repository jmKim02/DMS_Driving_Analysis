// src/main/java/com/sejong/drivinganalysis/challenge/exception/ChallengeExceptionHandler.java
package com.sejong.drivinganalysis.exception;

import com.sejong.drivinganalysis.dto.common.ApiResponse;
import com.sejong.drivinganalysis.exception.ChallengeExceptions.ChallengeNotFoundException;
import com.sejong.drivinganalysis.exception.ChallengeExceptions.DuplicateChallengeException;      // ← 추가
import com.sejong.drivinganalysis.exception.ChallengeExceptions.DuplicateParticipationException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.sejong.drivinganalysis.challenge")
public class ChallengeExceptionHandler {

    @ExceptionHandler(ChallengeNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ChallengeNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateParticipationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAlreadyJoined(DuplicateParticipationException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("ALREADY_JOINED", ex.getMessage()));
    }

    // ← 여기를 추가하세요
    @ExceptionHandler(DuplicateChallengeException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateChallengeBusiness(DuplicateChallengeException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("DUPLICATE_CHALLENGE", ex.getMessage()));
    }

    /** DB unique 제약 위반 (예: 직접 INSERT 시점에) 처리 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateChallengeDb(DataIntegrityViolationException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof ConstraintViolationException cve) {
            String name = cve.getConstraintName();
            if (name != null && name.toLowerCase().contains("uk_challenge_unique")) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(
                                "DUPLICATE_CHALLENGE",
                                "중복된 챌린지입니다. 제목과 기간이 동일한 챌린지를 다시 생성할 수 없습니다."
                        ));
            }
        }
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "SERVER_ERROR",
                        "서버 오류가 발생했습니다. 관리자에게 문의하세요."
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAll(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "SERVER_ERROR",
                        "서버 오류가 발생했습니다. 관리자에게 문의하세요."
                ));
    }
}
