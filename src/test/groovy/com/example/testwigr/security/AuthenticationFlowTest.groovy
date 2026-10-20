package com.example.testwigr.security

import com.example.testwigr.model.User
import com.example.testwigr.repository.UserRepository
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
 * Test suite that verifies the basic authentication flows within the application.
 * This focuses on user registration, login and validation of credentials.
 * These tests confirm the security mechanisms are working as expected for the core flows.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
class AuthenticationFlowTest extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    UserRepository userRepository

    @Autowired
    PasswordEncoder passwordEncoder

    @Autowired
    ObjectMapper objectMapper

    /**
     * Set up a fresh test environment before each test.
     * Creates a test user with known credentials.
     */
    def setup() {
        userRepository.deleteAll()

        // Create a test user with known credentials
        def user = TestDataFactory.createUser(null, 'authuser')
        user.password = passwordEncoder.encode('password123')
        userRepository.save(user)
    }

    /**
     * Tests the user registration flow:
     * 1. Prepares a registration request with complete user details
     * 2. Sends the request to the registration endpoint
     * 3. Verifies the response indicates successful registration
     * 4. Confirms the user was actually created in the database
     */
    def "should register a new user"() {
        given: 'registration request data with all required fields'
        def requestBody = [
                username: 'newuser',
                email: 'newuser@example.com',
                password: 'newpassword',
                displayName: 'New User'
        ]

        when: 'sending registration request to the endpoint'
        def result = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/register')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
        )

        then: 'registration is successful with OK status'
        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
                .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('newuser'))

        and: 'user exists in the database'
        userRepository.findByUsername('newuser').isPresent()
    }

    /**
     * Tests the user login flow with valid credentials:
     * 1. Prepares a login request with correct username and password
     * 2. Sends the request to the login endpoint
     * 3. Verifies the response contains a JWT token and success indication
     *
     * This test confirms that authentication works correctly with valid credentials.
     */
    def "should login successfully and get JWT token"() {
        given: 'login request with correct credentials'
        def requestBody = [
                username: 'authuser',
                password: 'password123'
        ]

        when: 'sending login request'
        def result = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/login')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
        )

        then: 'login is successful and returns a token'
        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.token').exists())
                .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('authuser'))
    }

    /**
     * Tests the user login flow with invalid credentials:
     * 1. Prepares a login request with incorrect password
     * 2. Sends the request to the login endpoint
     * 3. Verifies the response indicates authentication failure
     *
     * This test confirms that authentication correctly rejects invalid credentials.
     */
    def "should reject login with incorrect credentials"() {
        given: 'login request with incorrect password'
        def requestBody = [
                username: 'authuser',
                password: 'wrongpassword'
        ]

        when: 'sending login request with incorrect password'
        def result = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/login')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody))
        )

        then: 'login is rejected with unauthorized status'
        result.andExpect(MockMvcResultMatchers.status().isUnauthorized())
    }

}
