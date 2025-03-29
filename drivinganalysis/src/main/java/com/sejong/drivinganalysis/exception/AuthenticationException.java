package com.sejong.drivinganalysis.exception;

/**
 * 인증 관련 예외 클래스
 * 로그인 실패, 토큰 오류 등 인증 과정에서 발생하는 예외 처리
 */
public class AuthenticationException extends ApiException {
    public AuthenticationException(String message) {
        super("AUTH_ERROR", message); // 인증 오류에는 AUTH_ERROR 코드 사용
    }
}