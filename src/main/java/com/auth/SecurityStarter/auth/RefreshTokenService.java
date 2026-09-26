package com.auth.SecurityStarter.auth;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auth.SecurityStarter.config.AppProperties;
import com.auth.SecurityStarter.exception.TokenRefreshException;
import com.auth.SecurityStarter.user.RefreshToken;
import com.auth.SecurityStarter.user.RefreshTokenRepository;
import com.auth.SecurityStarter.user.User;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final AppProperties appProperties;

    @Transactional 
    public RefreshToken createRefreshToken(User user) {
        // Revoke existing tokens (single active session policy)
        refreshTokenRepository.deleteByUser(user);

        String tokenString = tokenService.generateRefreshToken(user);
        
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenString)
                .expiryDate(Instant.now().plusMillis(appProperties.getRefreshExpirationMs()))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked() || token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(), 
                "Refresh token was expired or revoked. Please sign in again.");
        }
        return token;
    }

    @Transactional
    public void revokeToken(String tokenString) {
        refreshTokenRepository.findByToken(tokenString).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }
}
