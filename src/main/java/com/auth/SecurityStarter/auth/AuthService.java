package com.auth.SecurityStarter.auth;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.auth.SecurityStarter.dto.RegisterRequest;
import com.auth.SecurityStarter.exception.AuthException;
import com.auth.SecurityStarter.user.Role;
import com.auth.SecurityStarter.user.User;
import com.auth.SecurityStarter.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void register(RegisterRequest request){
        // Check for duplicates
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AuthException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email is already registered");
        }

        Set<Role> roles = new HashSet<>();
        roles.add(Role.ROLE_USER);

        User user = User.builder()
                        .username(request.getUsername())
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .roles(roles)
                        .build();

        userRepository.save(user);
    }
}
