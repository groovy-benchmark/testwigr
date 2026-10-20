package com.example.testwigr.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Configuration for API versioning.
 * 
 * This class configures the API versioning strategy for the Testwigr platform.
 * It uses header-based versioning via a custom interceptor to ensure proper
 * API version handling throughout the application.
 * 
 * The implementation uses the X-API-Version header to determine which version
 * of the API to use for a given request. If no version is specified, it defaults
 * to the current version.
 * 
 * This approach allows:
 * - Multiple API versions to coexist
 * - Clients to specify which version they're built for
 * - Backward compatibility for older clients
 * - Gradual deprecation of older API versions
 */
@Configuration
class ApiVersioningConfig implements WebMvcConfigurer {

    /**
     * Adds the API version interceptor to the interceptor registry.
     * This ensures all requests are processed with proper version handling.
     * 
     * @param registry The interceptor registry to add to
     */
    @Override
    void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ApiVersionInterceptor())
    }
}
