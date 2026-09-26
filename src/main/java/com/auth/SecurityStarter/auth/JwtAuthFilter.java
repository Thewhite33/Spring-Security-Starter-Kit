package com.auth.SecurityStarter.auth;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j 
@Component 
@RequiredArgsConstructor 
public class JwtAuthFilter extends OncePerRequestFilter {
    
    private final TokenService tokenService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull  HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Extract the token from the Authorization header
        String token = extractTokenFromRequest(request);

        // 2. If no token is present, skip authentication.
        //    Spring Security will handle the 401 if the route is protected.
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 3. Validate the token: signature, expiry, AND type must be "access"
            if (tokenService.validateToken(token, "access")) {

                // 4. Extract the username from the token
                String username = tokenService.extractUsername(token);

                // 5. Only set the context if it's not already set
                //    (prevents issues with forwarded requests)
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    // 6. Load the user from the database.
                    //    This ensures the user still exists and has current roles.
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // 7. Create the authentication token with the user's authorities (roles)
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null, // No credentials needed — the JWT is the proof
                                    userDetails.getAuthorities()
                            );

                    // 8. Attach request details (IP address, session ID, etc.)
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // 9. Set the SecurityContext — this is what makes @PreAuthorize work
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else {
                log.warn("Invalid or expired access token presented");
            }
        } catch (Exception e) {
            // If anything goes wrong (user deleted, DB down, etc.),
            // log it and continue without setting the context.
            // Spring Security will reject the request if the route requires auth.
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        // 10. Continue the filter chain regardless.
        //     Authentication is set (or not). Authorization happens later.
        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove "Bearer " prefix
        }
        return null;
    }
}
