package org.example.onlinemart.exception;

/**
 * Custom exception for cache-related operations.
 * This exception is thrown when a cache operation fails due to
 * connectivity issues, serialization problems, or other cache-related errors.
 */
public class CacheException extends RuntimeException {

    /**
     * Constructs a new cache exception with the specified detail message.
     *
     * @param message the detail message
     */
    public CacheException(String message) {
        super(message);
    }

    /**
     * Constructs a new cache exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public CacheException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new cache exception with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public CacheException(Throwable cause) {
        super(cause);
    }

    /**
     * Returns a specific error code based on the type of cache failure.
     * This can be useful for client applications to handle different types of cache errors.
     *
     * @return a string representing the error code
     */
    public String getErrorCode() {
        Throwable cause = getCause();

        if (cause != null) {
            if (cause instanceof java.net.ConnectException) {
                return "CACHE_CONNECTION_ERROR";
            } else if (cause instanceof redis.clients.jedis.exceptions.JedisConnectionException) {
                return "REDIS_CONNECTION_ERROR";
            } else if (cause instanceof com.google.gson.JsonSyntaxException) {
                return "CACHE_SERIALIZATION_ERROR";
            } else if (cause instanceof java.lang.IllegalArgumentException) {
                return "CACHE_INVALID_ARGUMENT";
            }
        }

        return "CACHE_GENERAL_ERROR";
    }

    /**
     * Determines if the cache error is recoverable or fatal.
     *
     * @return true if the error is likely temporary and operations can be retried
     */
    public boolean isRecoverable() {
        String code = getErrorCode();

        // Connection errors are typically recoverable
        return "CACHE_CONNECTION_ERROR".equals(code) ||
                "REDIS_CONNECTION_ERROR".equals(code);
    }
}