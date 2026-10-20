package com.example.testwigr.security

import com.example.testwigr.controller.LoginRequest
import com.example.testwigr.controller.RegisterRequest
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Integration test that verifies the complete authentication flow.
 * These tests examine authentication edge cases including:
 * - Token expiration
 * - Inactive user authentication
 * - Registration, login, token usage, and logout
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
class AuthenticationFlowIntegrationTest extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    UserRepository userRepository

    @Autowired
    PasswordEncoder passwordEncoder

    @Autowired
    ObjectMapper objectMapper

    @Value('${app.jwt.secret:testSecretKeyForTestingPurposesOnlyDoNotUseInProduction}')
    String jwtSecret

    /**
     * Set up test data before each test:
     * 1. Clean the database
     * 2. Create test users with different states (active, inactive)
     */
    def setup() {
        userRepository.deleteAll()

        // Create various test users with different states
        def activeUser = TestDataFactory.createUser(null, 'activeuser')
        activeUser.password = passwordEncoder.encode('password123')
        activeUser.active = true
        userRepository.save(activeUser)

        def inactiveUser = TestDataFactory.createUser(null, 'inactiveuser')
        inactiveUser.password = passwordEncoder.encode('password123')
        inactiveUser.active = false
        userRepository.save(inactiveUser)
    }

    /**
     * Tests authentication with an expired token:
     * 1. Generates an expired JWT token
     * 2. Attempts to access a protected resource
     * 3. Verifies access is denied with 403 status
     */
    def "should block access with expired token"() {
        given: 'An expired JWT token'
        def expiredToken = generateExpiredToken('activeuser', jwtSecret)

        when: 'Accessing protected resource with expired token'
        def result = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/posts')
                        .header('Authorization', "Bearer ${expiredToken}")
        )

        then: 'Access is denied with 403 status'
        result.andExpect(MockMvcResultMatchers.status().isForbidden())
    }

    /**
     * Tests authentication for inactive users:
     * 1. Attempts to log in with credentials of an inactive user
     * 2. Verifies login is rejected with 401 status
     */
    def "should reject login for inactive user"() {
        given: 'Login request for inactive user'
        def loginRequest = new LoginRequest(
                username: 'inactiveuser',
                password: 'password123'
        )

        when: 'Attempting to login'
        def result = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/login')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        )

        then: 'Login is rejected with unauthorized status'
        result.andExpect(MockMvcResultMatchers.status().isUnauthorized())
    }

    /**
     * Tests the complete authentication flow:
     * 1. Register a new user
     * 2. Log in to get a JWT token
     * 3. Access protected resources with the token
     * 4. Log out
     *
     * This test verifies the full authentication lifecycle
     */
    def "should complete full authentication flow"() {
        given: 'Registration data for a new user'
        def registerRequest = new RegisterRequest(
                username: 'newflowuser',
                email: 'newflowuser@example.com',
                password: 'flowpassword',
                displayName: 'Flow User'
        )

        when: 'Registering a new user'
        def registerResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/register')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
        )

        then: 'Registration succeeds'
        registerResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))

        when: 'Logging in with new user'
        def loginRequest = new LoginRequest(
                username: 'newflowuser',
                password: 'flowpassword'
        )

        def loginResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/login')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        )

        then: 'Login succeeds and returns a token'
        loginResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.token').exists())

        and: 'The token can be extracted for further use'
        def responseJson = objectMapper.readValue(
                loginResult.andReturn().response.contentAsString,
                Map
        )
        def token = responseJson.token

        when: 'Accessing protected resource with token'
        def protectedResult = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/users/me')
                        .header('Authorization', "Bearer ${token}")
        )

        then: 'Access is granted to the protected resource'
        protectedResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('newflowuser'))

        when: 'Logging out'
        def logoutResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/logout')
                        .header('Authorization', "Bearer ${token}")
        ).andDo(MockMvcResultHandlers.print())
        then: 'Logout succeeds'
        // Check only for OK status, don't check any specific JSON paths
        logoutResult.andExpect(MockMvcResultMatchers.status().isOk())
    }

    /**
     * Helper method to generate an expired JWT token for testing.
     * Creates a token that was valid in the past but has now expired.
     *
     * @param username The username to include in the token
     * @param secret The secret key used for signing the token
     * @return An expired JWT token string
     */
    private String generateExpiredToken(String username, String secret) {
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS)

        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(past.minus(2, ChronoUnit.DAYS)))
                .expiration(Date.from(past))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact()
    }
}
