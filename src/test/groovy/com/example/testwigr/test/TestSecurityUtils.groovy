package com.example.testwigr.test

import com.example.testwigr.model.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

/**
 * Utility class providing security-related helper methods for tests.
 * Includes methods for JWT token generation, validation, and authentication setup.
 * This is particularly useful for controller and integration tests that require
 * authentication.
 */
class TestSecurityUtils {

    /**
     * Generates a valid JWT token for testing purposes.
     *
     * @param username The username to include in the token subject
     * @param secret The secret key used for signing the token
     * @param expirationDays Number of days until token expiration (defaults to 10)
     * @return A valid JWT token string
     */
    static String generateTestToken(String username, String secret, long expirationDays = 10) {
        Instant now = Instant.now()

        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationDays, ChronoUnit.DAYS)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact()
    }

    /**
     * Generates an expired JWT token for testing error scenarios.
     *
     * @param username The username to include in the token subject
     * @param secret The secret key used for signing the token
     * @return An expired JWT token string
     */
    static String generateExpiredToken(String username, String secret) {
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS)

        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(past.minus(2, ChronoUnit.DAYS)))
                .expiration(Date.from(past))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact()
    }

    /**
     * Extracts the username from a JWT token.
     * Useful for verifying token contents in tests.
     *
     * @param token The JWT token string
     * @param secret The secret key used for verifying the token
     * @return The username extracted from the token
     */
    static String extractUsername(String token, String secret) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject()
    }

    /**
     * Adds a JWT token to a MockMvc request builder.
     * Convenient for setting up authenticated requests in controller tests.
     *
     * @param requestBuilder The MockMvc request builder
     * @param token The JWT token string
     * @return The request builder with Authorization header added
     */
    static MockHttpServletRequestBuilder addJwtToken(MockHttpServletRequestBuilder requestBuilder, String token) {
        return requestBuilder.header('Authorization', "Bearer ${token}")
    }

    /**
     * Creates a mock HTTP request with JWT token in the Authorization header.
     *
     * @param token The JWT token string
     * @return A MockHttpServletRequest with Authorization header
     */
    static MockHttpServletRequest createAuthenticatedRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest()
        request.addHeader('Authorization', "Bearer ${token}")
        return request
    }

    /**
     * Checks if a JWT token is expired.
     *
     * @param token The JWT token string
     * @param secret The secret key used for verifying the token
     * @return True if token is expired, false otherwise
     */
    static boolean isTokenExpired(String token, String secret) {
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload()

        return claims.getExpiration().before(new Date())
    }

    /**
     * Sets up security context for testing with specified user details.
     * This allows tests to simulate an authenticated user without HTTP requests.
     *
     * @param userDetails The UserDetails representing the authenticated user
     */
    static void setupAuthentication(UserDetails userDetails) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        )
        SecurityContextHolder.getContext().setAuthentication(auth)
    }

    /**
     * Creates an Authentication object from a User entity.
     * Useful for setting up security context in tests.
     *
     * @param user The User entity to create authentication for
     * @return An Authentication object representing the user
     */
    static Authentication createAuthentication(User user) {
        return new UsernamePasswordAuthenticationToken(
                user.username,
                null,
                [new SimpleGrantedAuthority('ROLE_USER')]
        )
    }

    /**
     * Checks if a JWT token is valid (properly signed and not expired).
     *
     * @param token The JWT token string
     * @param secret The secret key used for verifying the token
     * @return True if token is valid, false otherwise
     */
    static boolean isValidToken(String token, String secret) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()

            // Check if token is expired
            return !claims.getExpiration().before(new Date())
        } catch (Exception e) {
            return false
        }
    }

    /**
     * Clears the security context after test.
     * Important to call in cleanup to prevent test contamination.
     */
    static void clearAuthentication() {
        SecurityContextHolder.clearContext()
    }

}
