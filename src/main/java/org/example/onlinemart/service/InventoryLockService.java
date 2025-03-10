package org.example.onlinemart.service;

import org.example.onlinemart.cache.CacheKeys;
import org.example.onlinemart.cache.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing distributed locks on inventory items
 * to prevent race conditions during order processing.
 */
@Service
public class InventoryLockService {
    private static final Logger logger = LoggerFactory.getLogger(InventoryLockService.class);

    private final CacheService cacheService;

    @Value("${inventory.lock.timeout:30}")
    private int lockTimeoutSeconds;

    @Autowired
    public InventoryLockService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * Acquire a lock on a product's inventory.
     * This uses Redis as a distributed lock mechanism.
     *
     * @param productId The product ID to lock
     * @return A lock token if successful, null if the lock couldn't be acquired
     */
    public String acquireLock(int productId) {
        String lockKey = getProductLockKey(productId);
        String lockToken = UUID.randomUUID().toString();

        // Try to set the key only if it doesn't exist (NX option in Redis)
        boolean acquired = cacheService.set(lockKey, lockToken, lockTimeoutSeconds, TimeUnit.SECONDS);

        if (acquired) {
            logger.debug("Lock acquired for product ID: {} with token: {}", productId, lockToken);
            return lockToken;
        } else {
            logger.debug("Failed to acquire lock for product ID: {}", productId);
            return null;
        }
    }

    /**
     * Release a lock on a product's inventory.
     * The token must match the one used to acquire the lock.
     *
     * @param productId The product ID to unlock
     * @param lockToken The token received when acquiring the lock
     * @return true if the lock was released, false if the lock wasn't found or token didn't match
     */
    public boolean releaseLock(int productId, String lockToken) {
        if (lockToken == null) {
            return false;
        }

        String lockKey = getProductLockKey(productId);
        String currentToken = cacheService.get(lockKey, String.class).orElse(null);

        // Only delete the lock if the token matches
        if (lockToken.equals(currentToken)) {
            boolean released = cacheService.delete(lockKey);
            logger.debug("Lock released for product ID: {}", productId);
            return released;
        }

        logger.warn("Failed to release lock for product ID: {} - token mismatch", productId);
        return false;
    }

    /**
     * Force-release a lock regardless of the token.
     * This should only be used in administrative operations.
     *
     * @param productId The product ID to unlock
     * @return true if the lock was released, false if the lock wasn't found
     */
    public boolean forceReleaseLock(int productId) {
        String lockKey = getProductLockKey(productId);
        boolean released = cacheService.delete(lockKey);

        if (released) {
            logger.warn("Lock force-released for product ID: {}", productId);
        }

        return released;
    }

    /**
     * Check if a product's inventory is currently locked.
     *
     * @param productId The product ID to check
     * @return true if the product is locked, false otherwise
     */
    public boolean isLocked(int productId) {
        String lockKey = getProductLockKey(productId);
        return cacheService.exists(lockKey);
    }

    /**
     * Get all currently locked products.
     *
     * @return A map of product IDs to lock tokens
     */
    public Map<Integer, String> getAllLocks() {
        // This is a simplified implementation
        // In a real system, you might want to maintain a separate set of all lock keys
        // For this example, we're just returning an empty map
        return new HashMap<>();
    }

    /**
     * Extends the lock timeout for a specific product.
     *
     * @param productId The product ID
     * @param lockToken The token received when acquiring the lock
     * @param extensionSeconds Number of seconds to extend the lock
     * @return true if the lock was extended, false otherwise
     */
    public boolean extendLock(int productId, String lockToken, int extensionSeconds) {
        if (lockToken == null) {
            return false;
        }

        String lockKey = getProductLockKey(productId);
        String currentToken = cacheService.get(lockKey, String.class).orElse(null);

        // Only extend the lock if the token matches
        if (lockToken.equals(currentToken)) {
            boolean extended = cacheService.expire(lockKey, extensionSeconds, TimeUnit.SECONDS);
            logger.debug("Lock extended for product ID: {} by {} seconds", productId, extensionSeconds);
            return extended;
        }

        return false;
    }

    private String getProductLockKey(int productId) {
        return "inventory:lock:" + productId;
    }
}