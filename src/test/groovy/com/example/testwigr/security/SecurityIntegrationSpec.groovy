package com.example.testwigr.security

import com.example.testwigr.controller.LoginRequest
import com.example.testwigr.controller.RegisterRequest
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.service.UserService
import com.example.testwigr.test.TestDataFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

/**
 * Comprehensive security integration tests that verify the complete security infrastructure.
 * Tests include user registration, authentication, token verification, and access control.
 * These tests ensure that the security mechanisms work correctly when integrated with the
 * full application context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
class SecurityIntegrationSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    ObjectMapper objectMapper

    @Autowired
    UserRepository userRepository

    @Autowired
    UserService userService

    @Autowired
    PasswordEncoder passwordEncoder

    /**
     * Set up test data before each test:
     * 1. Clean the database
     * 2. Create a test user with known credentials
     */
    def setup() {
        userRepository.deleteAll()

        // Create test user with known password
        def user = TestDataFactory.createUser(null, 'securityuser')
        user.password = passwordEncoder.encode('testpassword')
        userRepository.save(user)
    }

    /**
     * Tests user registration:
     * 1. Prepares a registration request with complete user details
     * 2. Submits the request to the registration endpoint
     * 3. Verifies successful registration response
     */
    def "should register a new user"() {
        given: "a complete registration request"
        def registerRequest = new RegisterRequest(
                username: 'newuser',
                email: 'newuser@example.com',
                password: 'newpassword',
                displayName: 'New User'
        )

        when: "submitting the registration request"
        def result = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/register')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
        )

        then: "registration succeeds"
        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
                .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('newuser'))
    }

    /**
     * Tests the login process with valid credentials:
     * 1. Prepares a login request with correct credentials
     * 2. Submits the request to the login endpoint
     * 3. Verifies successful authentication and token issuance
     */
    def "should login successfully and receive JWT token"() {
        given: "a login request with valid credentials"
        def loginRequest = new LoginRequest(
                username: 'securityuser',
                password: 'testpassword'
        )

        when: "submitting the login request"
        def result = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/login')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        )

        then: "login succeeds and token is returned"
        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.token').exists())
                .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('securityuser'))
    }

    /**
     * Tests login rejection with invalid credentials:
     * 1. Prepares a login request with incorrect password
     * 2. Submits the request to the login endpoint
     * 3. Verifies authentication failure
     */
    def "should reject login with incorrect credentials"() {
        given: "a login request with invalid password"
        def loginRequest = new LoginRequest(
                username: 'securityuser',
                password: 'wrongpassword'
        )

        when: "submitting the login request"
        def result = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/login')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        )

        then: "login is rejected"
        result.andExpect(MockMvcResultMatchers.status().isUnauthorized())
    }

    /**
     * Tests access denial to protected endpoints without authentication:
     * 1. Attempts to access a protected endpoint without an auth token
     * 2. Verifies that access is denied
     */
    def "should deny access to protected endpoints without authentication"() {
        when: "accessing a protected endpoint without authentication"
        def result = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/posts')
        )

        then: "access is denied with forbidden status"
        // Status is forbidden (403) rather than unauthorized (401) due to how Spring Security
        // handles authentication failure for JWT-based security
        result.andExpect(MockMvcResultMatchers.status().isForbidden())
    }

    /**
     * Tests the complete authentication flow:
     * 1. Logs in to get a JWT token
     * 2. Uses that token to access a protected resource
     * 3. Verifies successful access to the protected resource
     */
    def "should allow access with valid JWT token"() {
        given: "a user logs in to get a token"
        // First login to get a token
        def loginRequest = new LoginRequest(
                username: 'securityuser',
                password: 'testpassword'
        )

        def loginResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/login')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        )

        def token = objectMapper.readValue(
                loginResult.andReturn().response.contentAsString,
                Map
        ).token

        when: "accessing a protected resource with the token"
        def result = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/users/me')
                        .header('Authorization', "Bearer ${token}")
        )

        then: "access is granted to the protected resource"
        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('securityuser'))
    }

}
