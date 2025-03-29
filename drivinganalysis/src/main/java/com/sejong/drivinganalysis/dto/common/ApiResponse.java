package com.sejong.drivinganalysis.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 응답 형식을 표준화하기 위한 클래스
 * 모든 API 응답은 이 클래스를 통해 일관된 형식으로 제공
 *
 * @param <T> 응답 데이터 타입
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private String status; // "success" 또는 "error"
    private T data;        // 성공 시 반환할 데이터
    private ErrorResponse error; // 오류 발생 시 오류 정보

    /**
     * 성공 응답을 생성하는 정적 팩토리 메서드
     *
     * @param data 응답에 포함할 데이터
     * @return 성공 상태의 API 응답 객체
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status("success")
                .data(data)
                .build();
    }

    /**
     * 오류 응답을 생성하는 정적 팩토리 메서드
     *
     * @param code 오류 코드
     * @param message 오류 메시지
     * @return 오류 상태의 API 응답 객체
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .status("error")
                .error(new ErrorResponse(code, message))
                .build();
    }

    /**
     * API 오류 응답에 포함되는 오류 정보를 담는 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse {
        private String code;    // 오류 코드
        private String message; // 오류 메시지
    }
}