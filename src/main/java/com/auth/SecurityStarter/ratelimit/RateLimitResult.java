package com.auth.SecurityStarter.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor 
public class RateLimitResult {
    private boolean allowed;
    private long remainingTokens;
    private long limit;
    private long retryAfterSeconds;  // Only meaningful when allowed = false
    private long resetSeconds;       // Time until bucket is fully refilled
}
