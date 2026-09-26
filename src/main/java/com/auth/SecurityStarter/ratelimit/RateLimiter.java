package com.auth.SecurityStarter.ratelimit;

/**
 * Abstraction over rate limiting implementations.
 * Allows swapping between in-memory, Redis, or other backends.
 */
public interface RateLimiter {

    /**
     * Attempts to consume a token for the given key.
     * 
     * @param key The identifier (e.g., IP address)
     * @return Result containing consumption details
     */
    RateLimitResult tryConsume(String key);

    /**
     * Resets the bucket for a given key (useful for testing or admin actions).
     */
    void reset(String key);
}
