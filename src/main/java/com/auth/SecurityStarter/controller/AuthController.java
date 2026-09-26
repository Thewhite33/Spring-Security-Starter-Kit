package com.auth.SecurityStarter.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth.SecurityStarter.auth.AuthProviderRegistry;
import com.auth.SecurityStarter.auth.AuthService;
import com.auth.SecurityStarter.auth.RefreshTokenService;
import com.auth.SecurityStarter.auth.TokenService;
import com.auth.SecurityStarter.config.AppProperties;
import com.auth.SecurityStarter.dto.AuthResponse;
import com.auth.SecurityStarter.dto.LoginRequest;
import com.auth.SecurityStarter.dto.RefreshRequest;
import com.auth.SecurityStarter.dto.RegisterRequest;
import com.auth.SecurityStarter.exception.InvalidCredentialsException;
import com.auth.SecurityStarter.user.RefreshToken;
import com.auth.SecurityStarter.user.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/auth")
@RequiredArgsConstructor 
public class AuthController {
    
    private final AuthService authService;
    private final AuthProviderRegistry authProviderRegistry;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final AppProperties appProperties;

    @PostMapping ("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request){
        authService.register(request);
        return new ResponseEntity<>(
                AuthResponse.builder().message("User registered successfully").build(), 
                HttpStatus.CREATED
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = authProviderRegistry.authenticate(request);
        
        String accessToken = tokenService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return ResponseEntity.ok(buildAuthResponse("Login successful", accessToken, refreshToken.getToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        // 1. Find token in DB
        RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken)
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token not found"));

        // 2. Verify it's not expired or revoked
        refreshTokenService.verifyExpiration(refreshToken);

        // 3. Verify JWT signature and type
        if (!tokenService.validateToken(requestRefreshToken, "refresh")) {
            throw new InvalidCredentialsException("Invalid refresh token signature");
        }

        // 4. Issue new tokens (Token Rotation)
        User user = refreshToken.getUser();
        String newAccessToken = tokenService.generateAccessToken(user);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        return ResponseEntity.ok(buildAuthResponse("Token refreshed successfully", 
                newAccessToken, newRefreshToken.getToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokenService.revokeToken(request.getRefreshToken());
        return ResponseEntity.ok(AuthResponse.builder().message("Logout successful").build());
    }

    private AuthResponse buildAuthResponse(String message, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .message(message)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(appProperties.getExpirationMs() / 1000)
                .refreshExpiresIn(appProperties.getRefreshExpirationMs() / 1000)
                .build();
    }
}
