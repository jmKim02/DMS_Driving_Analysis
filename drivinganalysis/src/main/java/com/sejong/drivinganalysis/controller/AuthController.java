package com.sejong.drivinganalysis.controller;

import com.sejong.drivinganalysis.dto.AuthDto;
import com.sejong.drivinganalysis.service.AuthService;
import com.sejong.drivinganalysis.dto.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 API 컨트롤러
 * 로그인 및 인증 관련 요청을 처리
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 로그인 요청을 처리하는 엔드포인트
     *
     * @param loginRequest 로그인 요청 정보 (아이디, 비밀번호)
     * @return 인증 토큰과 사용자 정보를 포함한 응답
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.LoginResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest loginRequest) {
        AuthDto.LoginResponse loginResponse = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }
}