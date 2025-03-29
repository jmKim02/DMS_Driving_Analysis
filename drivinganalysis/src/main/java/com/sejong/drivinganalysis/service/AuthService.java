package com.sejong.drivinganalysis.service;

import com.sejong.drivinganalysis.dto.AuthDto;
import com.sejong.drivinganalysis.entity.User;
import com.sejong.drivinganalysis.exception.AuthenticationException;
import com.sejong.drivinganalysis.repository.UserRepository;
import com.sejong.drivinganalysis.utils.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;


    /**
     * 로그인 처리 메서드
     * 사용자 인증 후 JWT 토큰 발급
     *
     * @param loginRequest 로그인 요청 정보
     * @return 토큰과 사용자 정보를 포함한 로그인 응답
     * @throws AuthenticationException 인증 실패 시 발생
     */
    public AuthDto.LoginResponse login(AuthDto.LoginRequest loginRequest) {
        try {
            // 사용자 인증
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 인증된 사용자 정보 조회
            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new AuthenticationException("사용자를 찾을 수 없습니다: " + loginRequest.getUsername()));

            // Jwt 토큰 생성 - 사용자 ID 포함
            String token = jwtTokenProvider.createToken(
                    user.getUsername(),
                    user.getUserId().toString(),
                    user.getRole()
            );

            log.info("User logged in successfully: {}", user.getUsername());

            // 응답 DTO 생성
            AuthDto.UserDto userDto = AuthDto.UserDto.builder()
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .build();

            return AuthDto.LoginResponse.builder()
                    .token(token)
                    .user(userDto)
                    .build();
        } catch (BadCredentialsException e) {
            log.warn("Login failed: Invalid credentials for user {}", loginRequest.getUsername());
            throw new AuthenticationException("아이디 또는 비밀번호가 올바르지 않습니다");
        } catch (org.springframework.security.core.AuthenticationException e) {
            log.error("Authentication failed", e);
            throw new AuthenticationException("인증에 실패했습니다: " + e.getMessage());
        }
    }
}