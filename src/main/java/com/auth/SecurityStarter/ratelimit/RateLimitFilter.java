package com.auth.SecurityStarter.ratelimit;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auth.SecurityStarter.config.RateLimitConfig;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private final InMemoryRateLimiter rateLimiter;
    private final RateLimitConfig config;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Only apply rate limiting to POST /auth/login and POST /auth/register
        if (!config.isEnabled() || !isRateLimitedEndpoint(path, method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract client IP (handles proxied requests)
        String clientIp = extractClientIp(request);

        // Determine which bucket to use
        RateLimitResult result;
        if (pathMatcher.match("/auth/login", path)) {
            result = rateLimiter.tryConsumeLogin(clientIp);
        } else {
            result = rateLimiter.tryConsumeRegister(clientIp);
        }

        // Add rate limit headers to every response
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemainingTokens()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.getResetSeconds()));

        if (!result.isAllowed()) {
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
            writeRateLimitResponse(response, result);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimitedEndpoint(String path, String method) {
        if (!"POST".equalsIgnoreCase(method)) {
            return false;
        }
        return pathMatcher.match("/auth/login", path) ||
                pathMatcher.match("/auth/register", path);
    }

    /**
     * Extracts the real client IP, respecting proxy headers.
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first IP in the chain (original client)
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Writes a 429 response with proper headers and JSON body.
     */
    private void writeRateLimitResponse(HttpServletResponse response, RateLimitResult result)
            throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(result.getRetryAfterSeconds()));

        Map<String, Object> body = new HashMap<>();
        body.put("error", "Too Many Requests");
        body.put("message", String.format("Rate limit exceeded. Try again in %d seconds.",
                result.getRetryAfterSeconds()));
        body.put("status", 429);
        body.put("retryAfter", result.getRetryAfterSeconds());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
