package com.sejong.drivinganalysis.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 인증 관련 DTO 클래스들을 포함하는 외부 클래스
 * 로그인 요청/응답, 사용자 정보 등의 DTO 정의
 */
public class AuthDto {

    /**
     * 로그인 요청을 위한 DTO 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "사용자명은 필수입니다")
        private String username;

        @NotBlank(message = "비밀번호는 필수입니다")
        private String password;
    }

    /**
     * 로그인 응답을 위한 DTO 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResponse {
        private String token;
        private UserDto user;
    }

    /**
     * 사용자 정보를 위한 DTO 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private Long userId;
        private String username;

        @Email(message = "유효한 이메일 형식이어야 합니다")
        private String email;
    }
}
