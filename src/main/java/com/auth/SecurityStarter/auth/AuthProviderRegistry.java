package com.auth.SecurityStarter.auth;

import java.util.List;

import org.springframework.stereotype.Component;

import com.auth.SecurityStarter.dto.LoginRequest;
import com.auth.SecurityStarter.exception.InvalidCredentialsException;
import com.auth.SecurityStarter.user.User;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class AuthProviderRegistry {

    private final List<AuthProvider> authProviders;
    
    public User authenticate(LoginRequest request) {
        return authProviders.stream()
                .filter(provider -> provider.supports(request))
                .findFirst()
                .map(provider -> provider.authenticate(request))
                .orElseThrow(() -> new InvalidCredentialsException("Unsupported authentication method"));
    }
}
