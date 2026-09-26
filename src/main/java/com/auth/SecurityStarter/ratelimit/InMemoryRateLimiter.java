package com.auth.SecurityStarter.ratelimit;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.auth.SecurityStarter.config.RateLimitConfig;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class InMemoryRateLimiter implements RateLimiter {

    private final RateLimitConfig config;

    private final Cache<String, Bucket> loginBuckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    private final Cache<String, Bucket> registerBuckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(100_000)
            .build();

    public RateLimitResult tryConsumeLogin(String key) {
        Bucket bucket = loginBuckets.get(key, k -> buildLoginBucket());
        return consume(bucket, config.getLogin().getCapacity());
    }

    public RateLimitResult tryConsumeRegister(String key) {
        Bucket bucket = registerBuckets.get(key, k -> buildRegisterBucket());
        return consume(bucket, config.getRegister().getCapacity());
    }

    @Override
    public RateLimitResult tryConsume(String key) {
        return tryConsumeLogin(key);
    }

    @Override
    public void reset(String key) {
        loginBuckets.invalidate(key);
        registerBuckets.invalidate(key);
    }

    private Bucket buildLoginBucket() {
        return buildBucket(config.getLogin());
    }

    private Bucket buildRegisterBucket() {
        return buildBucket(config.getRegister());
    }

    private Bucket buildBucket(RateLimitConfig.EndpointLimit limit) {
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(limit.getCapacity())
                .refillGreedy(limit.getRefillTokens(), Duration.ofSeconds(limit.getRefillDurationSeconds()))
                .build();

        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    private RateLimitResult consume(Bucket bucket, long limit) {
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return RateLimitResult.builder()
                    .allowed(true)
                    .remainingTokens(probe.getRemainingTokens())
                    .limit(limit)
                    .retryAfterSeconds(0)
                    .resetSeconds(calculateResetSeconds(probe.getNanosToWaitForRefill()))
                    .build();
        } else {
            long retryAfterNanos = probe.getNanosToWaitForRefill();
            return RateLimitResult.builder()
                    .allowed(false)
                    .remainingTokens(0)
                    .limit(limit)
                    .retryAfterSeconds(calculateResetSeconds(retryAfterNanos))
                    .resetSeconds(calculateResetSeconds(retryAfterNanos))
                    .build();
        }
    }

    private long calculateResetSeconds(long nanosToWaitForRefill) {
        return (nanosToWaitForRefill / 1_000_000_000) + 1;
    }
}