package com.auth.SecurityStarter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitConfig {

    private boolean enabled = true;
    private EndpointLimit login = new EndpointLimit();
    private EndpointLimit register = new EndpointLimit();

    @Data
    public static class EndpointLimit {
        private int capacity = 10; // Bucket size (max tokens)
        private int refillTokens = 10; // Tokens added per refill
        private long refillDurationSeconds = 60; // Refill interval
    }
}
