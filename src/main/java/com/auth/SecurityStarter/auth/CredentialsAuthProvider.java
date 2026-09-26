package com.auth.SecurityStarter.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.auth.SecurityStarter.dto.LoginRequest;
import com.auth.SecurityStarter.exception.InvalidCredentialsException;
import com.auth.SecurityStarter.user.User;
import com.auth.SecurityStarter.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class CredentialsAuthProvider implements AuthProvider {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean supports(LoginRequest request) {
        return request.getUsernameOrEmail() != null && request.getPassword() != null;
    }

    @Override 
    public User authenticate(LoginRequest request){
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username/email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username/email or password");
        }

        return user;
    }
}
