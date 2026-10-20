package com.example.testwigr.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

import java.time.Duration

/**
 * Configuration for API rate limiting.
 *
 * This class configures rate limiting for the Testwigr API to prevent abuse and ensure
 * fair usage of resources. It uses the token bucket algorithm via the Bucket4j library
 * to implement rate limiting with different limits for authenticated and unauthenticated
 * requests.
 *
 * Rate limits are enforced by an interceptor that checks each request against the
 * appropriate bucket based on the client's authentication status.
 *
 * Different environments (dev, test, prod) have different rate limit configurations.
 */
@Configuration
class RateLimitingConfig implements WebMvcConfigurer {

    private final Environment environment

    /**
     * Constructor that accepts the Spring environment to detect active profiles.
     *
     * @param environment The Spring environment
     */
    RateLimitingConfig(Environment environment) {
        this.environment = environment
    }

    /**
     * Creates a rate limit bucket for authenticated users.
     * The bucket configuration varies based on the active profile (test vs non-test).
     *
     * @return A token bucket configured for authenticated user limits
     */
    @Bean
    Bucket authenticatedBucket() {
        if (isTestProfile()) {
            // Higher limits for test environment: 1000 requests per minute
            return createBucket(1000, Duration.ofMinutes(1))
        } else {
            // Standard limits for production/development: 100 requests per minute
            return createBucket(100, Duration.ofMinutes(1))
        }
    }

    /**
     * Creates a rate limit bucket for unauthenticated users.
     * The bucket configuration varies based on the active profile (test vs non-test).
     *
     * @return A token bucket configured for unauthenticated user limits
     */
    @Bean
    Bucket unauthenticatedBucket() {
        if (isTestProfile()) {
            // Higher limits for test environment: 500 requests per minute
            return createBucket(500, Duration.ofMinutes(1))
        } else {
            // Standard limits for production/development: 20 requests per minute
            return createBucket(20, Duration.ofMinutes(1))
        }
    }

    /**
     * Helper method to create a bucket with specified capacity and refill period.
     *
     * @param capacity The capacity of the bucket (max tokens)
     * @param period The period over which the bucket refills
     * @return A configured bucket
     */
    private Bucket createBucket(long capacity, Duration period) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(capacity, period))
        return Bucket.builder()
                .addLimit(limit)
                .build()
    }

    /**
     * Helper method to check if the test profile is active.
     *
     * @return true if the test profile is active, false otherwise
     */
    private boolean isTestProfile() {
        return environment.getActiveProfiles().contains("test")
    }

    /**
     * Adds the rate limiting interceptor to the interceptor registry.
     * This ensures all requests are checked against rate limits.
     *
     * @param registry The interceptor registry to add to
     */
    @Override
    void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor())
                .addPathPatterns("/api/**") // Apply to all API endpoints
                .excludePathPatterns("/api/health/**") // Exclude health checks
    }

    /**
     * Creates the rate limiting interceptor bean.
     * This interceptor checks each request against the appropriate rate limit bucket.
     *
     * @return The configured rate limit interceptor
     */
    @Bean
    RateLimitInterceptor rateLimitInterceptor() {
        return new RateLimitInterceptor(
                authenticatedBucket: authenticatedBucket(),
                unauthenticatedBucket: unauthenticatedBucket()
        )
    }
}
