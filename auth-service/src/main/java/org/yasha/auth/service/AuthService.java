package org.yasha.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yasha.auth.entity.User;
import org.yasha.auth.exception.EmailAlreadyTakenException;
import org.yasha.auth.exception.InvalidTokenException;
import org.yasha.auth.repository.UserRepository;
import org.yasha.common.jwt.JwtService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthTokens register(String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyTakenException(email);
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user = userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public AuthTokens login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("invalid credentials"));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("invalid credentials");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthTokens refresh(String rawRefreshToken) {
        UUID userId = refreshTokenService.validateAndRotate(rawRefreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("user not found"));
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeByRawToken(rawRefreshToken);
    }

    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokenService.revokeAllByUser(userId);
    }

    @Transactional(readOnly = true)
    public MeResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("user not found"));
        return new MeResponse(user.getId().toString(), user.getEmail(), user.getRole());
    }

    private AuthTokens issueTokens(User user) {
        String access = jwtService.generateAccess(user.getId(), user.getRole());
        String refresh = refreshTokenService.create(user.getId());
        return new AuthTokens(access, refresh);
    }

    public record AuthTokens(String accessToken, String refreshToken) {}
    public record MeResponse(String userId, String email, String role) {}
}