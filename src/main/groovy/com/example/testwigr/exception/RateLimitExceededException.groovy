package com.example.testwigr.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Exception thrown when a client has exceeded their rate limit for API requests.
 * This exception is handled by the GlobalExceptionHandler to return a 429 Too Many Requests
 * response with appropriate headers and information about when the client can retry.
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
class RateLimitExceededException extends RuntimeException {
    
    private final long retryAfter
    private final long limit
    
    /**
     * Creates a new rate limit exceeded exception.
     *
     * @param message A message describing the rate limit that was exceeded
     * @param retryAfter The number of seconds the client should wait before retrying
     * @param limit The rate limit that was exceeded
     */
    RateLimitExceededException(String message, long retryAfter, long limit) {
        super(message)
        this.retryAfter = retryAfter
        this.limit = limit
    }
    
    /**
     * Gets the number of seconds the client should wait before retrying.
     *
     * @return The retry-after time in seconds
     */
    long getRetryAfter() {
        return retryAfter
    }
    
    /**
     * Gets the rate limit that was exceeded.
     *
     * @return The rate limit
     */
    long getLimit() {
        return limit
    }
}
