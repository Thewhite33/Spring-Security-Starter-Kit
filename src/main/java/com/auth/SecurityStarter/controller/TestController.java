package com.auth.SecurityStarter.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController 
@RequestMapping ("/api")
public class TestController {
    
    // Protected: any authenticated user can access
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
            "message", "pong",
            "user", authentication.getName(),
            "roles", authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()),
            "status", "You are authenticated! Security is working."
        ));
    }

    // Protected: only users with ROLE_ADMIN can access
    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminOnly() {
        return ResponseEntity.ok(Map.of(
            "message", "Welcome, Admin!",
            "status", "You have admin privileges."
        ));
    }
}
