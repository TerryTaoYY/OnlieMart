package org.example.onlinemart.service;

import org.example.onlinemart.cache.CacheKeys;
import org.example.onlinemart.cache.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for implementing rate limiting functionality
 * to protect APIs from abuse and ensure fair usage.
 */
@Service
public class RateLimitService {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);

    private final CacheService cacheService;

    @Value("${rate.limit.default:60}")
    private int defaultRateLimit;

    @Value("${rate.limit.window:60}")
    private int defaultWindowSeconds;

    @Autowired
    public RateLimitService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * Check if a request is allowed based on the rate limit.
     * Uses a sliding window algorithm to track requests.
     *
     * @param key The identifier for the rate limit (typically username or IP + endpoint)
     * @return true if the request is allowed, false if the rate limit is exceeded
     */
    public boolean allowRequest(String key) {
        return allowRequest(key, defaultRateLimit, defaultWindowSeconds);
    }

    /**
     * Check if a request is allowed based on a specific rate limit.
     * Uses a sliding window algorithm to track requests.
     *
     * @param key The identifier for the rate limit (typically username or IP + endpoint)
     * @param limit The maximum number of requests allowed in the time window
     * @param windowSeconds The time window in seconds
     * @return true if the request is allowed, false if the rate limit is exceeded
     */
    public boolean allowRequest(String key, int limit, int windowSeconds) {
        if (key == null || key.isEmpty()) {
            return true; // If no key is provided, allow the request
        }

        String cacheKey = CacheKeys.UserActivity.rateLimit(key, "count");
        long currentCount = cacheService.increment(cacheKey);

        // If this is the first request, set the expiration
        if (currentCount == 1) {
            cacheService.expire(cacheKey, windowSeconds, TimeUnit.SECONDS);
        }

        if (currentCount > limit) {
            logger.warn("Rate limit exceeded for key: {}, count: {}", key, currentCount);
            return false;
        }

        return true;
    }

    /**
     * Gets the current count of requests for a specific key
     *
     * @param key The identifier for the rate limit
     * @return The current count of requests
     */
    public long getCurrentCount(String key) {
        if (key == null || key.isEmpty()) {
            return 0;
        }

        String cacheKey = CacheKeys.UserActivity.rateLimit(key, "count");
        return cacheService.get(cacheKey, Long.class).orElse(0L);
    }

    /**
     * Resets the rate limit counter for a specific key
     *
     * @param key The identifier for the rate limit
     */
    public void resetLimit(String key) {
        if (key == null || key.isEmpty()) {
            return;
        }

        String cacheKey = CacheKeys.UserActivity.rateLimit(key, "count");
        cacheService.delete(cacheKey);
        logger.debug("Reset rate limit for key: {}", key);
    }
}