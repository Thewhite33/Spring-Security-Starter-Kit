package com.auth.SecurityStarter.exception;

import lombok.Getter;

@Getter 
public class RateLimitExceededException extends RuntimeException {
    
    private final long retryAfterSeconds;
    private final long limit;
    private final long resetSeconds;

    public RateLimitExceededException(long retryAfterSeconds, long limit, long resetSeconds) {
        super(String.format("Rate limit exceeded. Try again in %d seconds.", retryAfterSeconds));
        this.retryAfterSeconds = retryAfterSeconds;
        this.limit = limit;
        this.resetSeconds = resetSeconds;
    }
}