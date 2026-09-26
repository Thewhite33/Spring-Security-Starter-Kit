package com.auth.SecurityStarter.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth.SecurityStarter.auth.AuthService;
import com.auth.SecurityStarter.dto.AuthResponse;
import com.auth.SecurityStarter.dto.RegisterRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/auth")
@RequiredArgsConstructor 
public class AuthController {
    
    private final AuthService authService;

    @PostMapping ("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request){
        authService.register(request);

        AuthResponse response = AuthResponse.builder()
            .message("User registered successfully")
            .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED); 
    }
}
