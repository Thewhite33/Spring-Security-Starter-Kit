package com.auth.SecurityStarter.auth;

import com.auth.SecurityStarter.dto.LoginRequest;
import com.auth.SecurityStarter.user.User;

public interface AuthProvider {
    boolean supports(LoginRequest request);
    User authenticate(LoginRequest request);
}
