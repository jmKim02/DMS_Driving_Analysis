package com.sejong.drivinganalysis.exception;

import lombok.Getter;

/**
 * API 예외의 기본 클래스
 * 모든 비즈니스 로직 예외는 이 클래스를 상속받아 구현
 */
@Getter
public class ApiException extends RuntimeException {
    private final String errorCode; // 오류 코드

    /**
     * API 예외 생성자
     *
     * @param errorCode 오류 코드
     * @param message   오류 메시지
     */
    public ApiException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}