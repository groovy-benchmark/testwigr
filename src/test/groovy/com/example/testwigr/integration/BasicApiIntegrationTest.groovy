package com.example.testwigr.integration

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import spock.lang.Specification

/**
 * Integration test that verifies basic API functionality end-to-end.
 * This test focuses on core user flows: registration, authentication, and accessing protected resources.
 * Uses a real test database and Spring's full application context.
 */
@SpringBootTest
@ActiveProfiles('test')
class BasicApiIntegrationTest extends Specification {

    @Autowired
    WebApplicationContext context

    @Autowired
    UserRepository userRepository

    @Autowired
    PostRepository postRepository

    @Autowired
    PasswordEncoder passwordEncoder

    @Autowired
    ObjectMapper objectMapper

    MockMvc mockMvc

    /**
     * Set up test environment before each test:
     * 1. Configure MockMvc with Spring Security
     * 2. Clean the database to ensure test isolation
     * 3. Create a test user for authentication tests
     */
    def setup() {
        // Set up MockMvc with security configuration
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build()

        // Clean up repositories
        postRepository.deleteAll()
        userRepository.deleteAll()

        // Create a test user with known credentials
        def user = TestDataFactory.createUser(null, 'integrationuser')
        user.password = passwordEncoder.encode('testpassword')
        userRepository.save(user)
    }

    /**
     * Tests the user registration flow:
     * 1. Prepares a registration request with user details
     * 2. Sends a POST request to the registration endpoint
     * 3. Verifies the response contains success status and username
     * 4. Confirms the user was created in the database
     */
    def "should register a new user"() {
        given: "a user registration request"
        def registrationRequest = [
                username: 'newuser',
                email: 'newuser@example.com',
                password: 'password123',
                displayName: 'New User'
        ]

        when: "sending registration request"
        def result = mockMvc.perform(
                post('/api/auth/register')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest))
        )

        then: "registration is successful"
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$.success').value(true))
                .andExpect(jsonPath('$.username').value('newuser'))

        and: "user is created in the database"
        userRepository.findByUsername('newuser').isPresent()
    }

    /**
     * Tests the complete authentication flow:
     * 1. Logs in with valid credentials to get JWT token
     * 2. Uses the token to access a protected resource
     * 3. Verifies authenticated access is successful
     *
     * This test verifies the full cycle of authentication and token usage
     */
    def "should authenticate and access protected resources"() {
        given: "valid login credentials"
        def loginRequest = [
                username: 'integrationuser',
                password: 'testpassword'
        ]

        when: "user logs in"
        def loginResult = mockMvc.perform(
                post('/api/auth/login')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        )

        then: "authentication succeeds and token is returned"
        loginResult.andExpect(status().isOk())
                .andExpect(jsonPath('$.token').exists())

        and: "token can be extracted for use in subsequent requests"
        def responseJson = objectMapper.readValue(
                loginResult.andReturn().response.contentAsString,
                Map
        )
        def token = responseJson.token

        when: "accessing a protected resource with the token"
        def protectedResult = mockMvc.perform(
                get('/api/users/me')
                        .header('Authorization', "Bearer ${token}")
        )

        then: "access is granted to the protected resource"
        protectedResult.andExpect(status().isOk())
                .andExpect(jsonPath('$.username').value('integrationuser'))
    }

}
