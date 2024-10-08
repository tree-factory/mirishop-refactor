package com.mirishop.userservice.application.service;

import com.mirishop.userservice.api.dto.request.LoginRequest;
import com.mirishop.userservice.api.dto.response.TokenResponse;
import com.mirishop.userservice.infrastructure.security.JwtTokenProvider;
import com.mirishop.userservice.common.exception.CustomException;
import com.mirishop.userservice.common.exception.ErrorCode;
import com.mirishop.userservice.infrastructure.redis.RedisService;
import com.mirishop.userservice.domain.user.UserEntity;
import com.mirishop.userservice.domain.user.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AuthService {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RedisService redisService;

    /**
     * 로그인 하는 메소드
     * @param loginRequest      : email과 password를 받습니다.
     */
    @Transactional
    public TokenResponse login(final LoginRequest loginRequest) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        UserEntity userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        validatePassword(password, userEntity.getPassword());

        return getNewTokenResponse(userEntity.getEmail());
    }

    /**
     * refresh token 재발급 메소드
     */
    @Transactional
    public TokenResponse reissue(final String refreshToken) {
        // 리프레시 토큰으로부터 사용자 이메일 추출
        String email = jwtTokenProvider.extractEmail(refreshToken);

        // Redis에 저장된 리프레시 토큰과 비교
        String storedRefreshToken = redisService.getValue("TOKEN=" + email);

        if (!refreshToken.equals(storedRefreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        UserEntity userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return getNewTokenResponse(userEntity.getEmail());
    }

    // email을 담은 access token, refresh token 발급 메소드
    private TokenResponse getNewTokenResponse(String email) {
        String accessToken = jwtTokenProvider.generateAccessToken(email);
        String refreshToken = jwtTokenProvider.generateRefreshToken(email);
        Long refreshTokenExpiration = jwtTokenProvider.extractTokenExpiration(refreshToken);

        redisService.setDataExpire("TOKEN=" + email, refreshToken, refreshTokenExpiration);

        return new TokenResponse(accessToken, refreshToken);
    }

    // 비밀번호 검증 메소드
    private void validatePassword(String password, String savedPassword) {
        if (!bCryptPasswordEncoder.matches(password, savedPassword)) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }
    }
}
