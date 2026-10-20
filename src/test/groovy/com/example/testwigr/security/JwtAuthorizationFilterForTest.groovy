package com.example.testwigr.security

import com.example.testwigr.service.UserService
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.filter.OncePerRequestFilter

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.nio.charset.StandardCharsets

/**
 * A simplified JWT authorization filter for use in tests.
 * This filter is designed to work without requiring an AuthenticationManager,
 * making it easier to use in test configurations.
 *
 * It extracts JWT tokens from the Authorization header, validates them,
 * and establishes the security context if the token is valid.
 */
class JwtAuthorizationFilterForTest extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthorizationFilterForTest.class)

    private final UserService userService
    private final String jwtSecret

    /**
     * Creates a new filter instance for test scenarios.
     *
     * @param userService Service to load user details from username
     * @param jwtSecret Secret key for validating JWT tokens
     */
    JwtAuthorizationFilterForTest(UserService userService, String jwtSecret) {
        this.userService = userService
        this.jwtSecret = jwtSecret
    }

    /**
     * Core filter method that processes each request.
     * Checks for JWT token in Authorization header, validates it,
     * and establishes authentication context if token is valid.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @param chain The filter chain to continue processing
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        String header = request.getHeader('Authorization')

        // Skip filter if no Authorization header or not a Bearer token
        if (header == null || !header.startsWith('Bearer ')) {
            chain.doFilter(request, response)
            return
        }

        try {
            // Try to authenticate with the token
            UsernamePasswordAuthenticationToken authentication = getAuthentication(request)
            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication)
            }
        } catch (Exception e) {
            // Log error but continue processing (will result in unauthenticated request)
            logger.error('Failed to authenticate token: {}', e.getMessage())
        }

        // Continue with filter chain processing
        chain.doFilter(request, response)
    }

    /**
     * Extracts and validates the JWT token from the request.
     * If valid, creates an authentication token for the user.
     *
     * @param request The HTTP request containing the JWT token
     * @return Authentication token if valid JWT, null otherwise
     */
    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
        String token = request.getHeader('Authorization')
        if (token != null) {
            try {
                // Extract username from token
                String username = Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                        .build()
                        .parseSignedClaims(token.replace('Bearer ', ''))
                        .getPayload()
                        .getSubject()

                if (username != null) {
                    // Load user details and create authentication token
                    UserDetails userDetails = userService.loadUserByUsername(username)
                    return new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    )
                }
            } catch (JwtException e) {
                logger.error('Invalid JWT token: {}', e.getMessage())
            } catch (Exception e) {
                logger.error('Authentication error: {}', e.getMessage())
            }
        }
        return null
    }

}
