package com.auth.SecurityStarter.auth;

import com.auth.SecurityStarter.user.User;

public interface TokenService {
    String generateAccessToken(User user);
    String generateRefreshToken(User user);
    boolean validateToken(String token, String expectedType);
    String extractUsername(String token);
}
