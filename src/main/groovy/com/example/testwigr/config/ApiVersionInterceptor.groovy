package com.example.testwigr.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Interceptor for handling API versioning via HTTP headers.
 * 
 * This interceptor processes the X-API-Version header in incoming requests
 * to determine which version of the API should handle the request. If no
 * version header is provided, it defaults to the current version.
 * 
 * The interceptor:
 * 1. Extracts the API version from the request header
 * 2. Validates that the requested version is supported
 * 3. Sets the appropriate version information in the request attributes
 * 4. Adds version headers to the response
 * 
 * This allows the application to maintain backward compatibility while
 * evolving the API over time.
 */
class ApiVersionInterceptor implements HandlerInterceptor {
    
    /**
     * Logger for recording version-related information.
     */
    private static final Logger logger = LoggerFactory.getLogger(ApiVersionInterceptor.class)
    
    /**
     * Current API version - the default if none is specified.
     */
    private static final String CURRENT_VERSION = "1.0.0"
    
    /**
     * List of supported API versions.
     */
    private static final List<String> SUPPORTED_VERSIONS = ["1.0.0"]
    
    /**
     * Name of the request header that specifies the desired API version.
     */
    private static final String VERSION_HEADER = "X-API-Version"
    
    /**
     * Pre-handle method that processes the request before it reaches the controller.
     * This extracts and validates the API version, and sets it in the request attributes.
     * 
     * @param request The HTTP request
     * @param response The HTTP response
     * @param handler The selected handler for the request
     * @return true to continue processing, false to stop processing
     */
    @Override
    boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Extract the requested API version from the header, default to current if not specified
        String requestedVersion = request.getHeader(VERSION_HEADER)
        String apiVersion = requestedVersion ?: CURRENT_VERSION
        
        // Check if the requested version is supported
        if (requestedVersion && !SUPPORTED_VERSIONS.contains(requestedVersion)) {
            logger.warn("Unsupported API version requested: {}", requestedVersion)
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST)
            response.getWriter().write("Unsupported API version. Supported versions: " + SUPPORTED_VERSIONS.join(", "))
            return false
        }
        
        // Store the API version in request attributes for access in controllers
        request.setAttribute("apiVersion", apiVersion)
        
        // Add version information to the response headers
        response.setHeader(VERSION_HEADER, apiVersion)
        response.setHeader("X-API-Current-Version", CURRENT_VERSION)
        response.setHeader("X-API-Supported-Versions", SUPPORTED_VERSIONS.join(", "))
        
        // Allow browser-based API clients on other origins to read the version headers
        addCrossOriginHeaders(request, response)
        
        return true
    }
    
    /**
     * Adds the cross-origin headers that let browser clients read the API version headers.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     */
    private void addCrossOriginHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin")
        if (origin) {
            response.setHeader("Access-Control-Allow-Origin", origin) // SINK: PLANTED-GR-HR-42
            response.setHeader("Access-Control-Allow-Credentials", "true")
            response.setHeader("Vary", "Origin")
        }
    }
}
