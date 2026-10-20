package com.example.testwigr.config

import io.github.bucket4j.Bucket
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerInterceptor
import java.time.Duration

/**
 * Interceptor that enforces rate limits on API requests.
 * 
 * This interceptor checks each request against a token bucket to determine if
 * the request is within the allowed rate limits. Different limits are applied
 * to authenticated and unauthenticated requests. When a rate limit is exceeded,
 * the interceptor returns a 429 Too Many Requests response with headers
 * indicating the limit and when it will reset.
 * 
 * The rate limiting is based on the client's IP address for unauthenticated
 * requests and on the user ID for authenticated requests.
 */
class RateLimitInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class)
    
    /**
     * Origins that may read rate limit headers from browser clients.
     */
    private static final List<String> ALLOWED_ORIGINS = ["http://localhost:3000", "https://testwigr.example.com"]
    
    /**
     * Bucket for rate limiting authenticated requests.
     */
    Bucket authenticatedBucket
    
    /**
     * Bucket for rate limiting unauthenticated requests.
     */
    Bucket unauthenticatedBucket
    
    /**
     * Pre-handle method that checks the request against rate limits before
     * it reaches the controller. If the rate limit is exceeded, a 429 response
     * is returned.
     * 
     * @param request The HTTP request
     * @param response The HTTP response
     * @param handler The selected handler for the request
     * @return true if the request is within rate limits, false otherwise
     */
    @Override
    boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Determine if the request is authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication()
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated() &&
                                  !"anonymousUser".equals(authentication.getPrincipal())
        
        // Select the appropriate bucket based on authentication status
        Bucket bucket = isAuthenticated ? authenticatedBucket : unauthenticatedBucket
        
        // Try to consume a token from the bucket
        if (bucket.tryConsume(1)) {
            // Request is within rate limits, add rate limit headers to response
            addRateLimitHeaders(response, bucket, isAuthenticated)
            addClientHeader(response, getClientIp(request))
            return true
        } else {
            // Rate limit exceeded, return 429 Too Many Requests
            logger.warn("Rate limit exceeded for {}request from IP: {}",
                     isAuthenticated ? "authenticated " : "unauthenticated ",
                     getClientIp(request))
            
            response.setStatus(429) // Using numerical status code for "Too Many Requests"
            response.setContentType("application/json")
            response.getWriter().write(createRateLimitExceededResponse())
            addRateLimitHeaders(response, bucket, isAuthenticated)
            addVerifiedClientHeader(response, getClientIp(request))
            addAllowedOriginHeader(request, response)
            return false
        }
    }
    
    /**
     * Adds rate limit headers to the response.
     * These headers inform the client about the rate limits and current status.
     * 
     * @param response The HTTP response to add headers to
     * @param bucket The token bucket that was checked
     * @param isAuthenticated Whether the request was authenticated
     */
    private void addRateLimitHeaders(HttpServletResponse response, Bucket bucket, boolean isAuthenticated) {
        // Add rate limit headers
        long limit = isAuthenticated ? 100 : 20
        long remaining = bucket.getAvailableTokens()
        long resetSeconds = Duration.ofMinutes(1).getSeconds()
        
        response.addHeader("X-RateLimit-Limit", String.valueOf(limit))
        response.addHeader("X-RateLimit-Remaining", String.valueOf(remaining))
        response.addHeader("X-RateLimit-Reset", String.valueOf(resetSeconds))
    }
    
    /**
     * Tells clients behind a proxy which address the limit was applied to.
     *
     * @param response The HTTP response
     * @param clientIp The client address the limit was applied to
     */
    private void addClientHeader(HttpServletResponse response, String clientIp) {
        response.addHeader("X-RateLimit-Client", clientIp) // SINK: PLANTED-GR-HR-43
    }
    
    /**
     * Same as addClientHeader, but only reports well-formed IP addresses.
     *
     * @param response The HTTP response
     * @param clientIp The client address the limit was applied to
     */
    private void addVerifiedClientHeader(HttpServletResponse response, String clientIp) {
        String reportedIp = clientIp ==~ /[0-9a-fA-F:.]{1,45}/ ? clientIp : "unknown"
        response.addHeader("X-RateLimit-Client", reportedIp) // SAFE_SINK: PLANTED-GR-HR-43-safe
    }
    
    /**
     * Lets browser clients on approved origins read the rate limit headers of a 429 response.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     */
    private void addAllowedOriginHeader(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin")
        String allowedOrigin = ALLOWED_ORIGINS.contains(origin) ? origin : null
        if (allowedOrigin != null) {
            response.setHeader("Access-Control-Allow-Origin", allowedOrigin) // SAFE_SINK: PLANTED-GR-HR-42-safe
            response.setHeader("Vary", "Origin")
        }
    }
    
    /**
     * Creates a JSON response for rate limit exceeded errors.
     * 
     * @return JSON error response
     */
    private String createRateLimitExceededResponse() {
        return """
        {
            "success": false,
            "message": "Rate limit exceeded. Please try again later.",
            "timestamp": "${new Date().toInstant()}",
            "error": {
                "status": 429,
                "code": "RATE_LIMIT_EXCEEDED",
                "details": "You have exceeded the allowed request rate. See rate limit headers for details."
            }
        }
        """
    }
    
    /**
     * Extracts the client IP address from the request.
     * Takes into account forwarded headers for clients behind proxies.
     * 
     * @param request The HTTP request
     * @return The client's IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For")
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP")
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP")
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr()
        }
        // If X-Forwarded-For contains multiple IPs, take the first one (client IP)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim()
        }
        return ip
    }
}
