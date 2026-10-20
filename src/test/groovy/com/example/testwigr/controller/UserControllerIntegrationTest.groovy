package com.example.testwigr.controller

import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

/**
 * Integration test for UserController that tests user profile management.
 * These tests verify that authenticated users can view, update, and manage user profiles.
 * Extends ControllerTestBase to use common testing utilities.
 */
@SpringBootTest
class UserControllerIntegrationTest extends ControllerTestBase {

    @Autowired
    UserRepository userRepository

    @Autowired
    PostRepository postRepository

    @Autowired
    PasswordEncoder passwordEncoder

    /**
     * Set up test data before each test:
     * 1. Clean the database to ensure test isolation
     * 2. Create a test user with a known password
     */
    def setup() {
        postRepository.deleteAll()
        userRepository.deleteAll()

        // Create a test user with encoded password
        def user = TestDataFactory.createUser(null, 'testuser')
        user.password = passwordEncoder.encode('password')
        userRepository.save(user)
    }

    /**
     * Tests retrieving a user profile by username:
     * 1. Simulates an authenticated request with JWT token
     * 2. Verifies the response contains the correct user profile
     *
     * Uses @WithMockUser for authentication
     */
    @WithMockUser(username = 'testuser')
    def "should get user profile"() {
        when: "requesting user profile"
        def result = mockMvc.perform(
                withAuth(MockMvcRequestBuilders.get('/api/users/testuser'))
        )

        then: "user profile is returned with correct information"
        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('testuser'))
                .andExpect(MockMvcResultMatchers.jsonPath('$.email').value('testuser@example.com'))
    }

    /**
     * Tests retrieving the currently authenticated user's profile:
     * 1. Simulates an authenticated request with JWT token
     * 2. Verifies the response contains the current user's profile
     *
     * Uses JWT token authentication from withAuth method
     */
    def "should get current user profile"() {
        when: "requesting current user profile with valid auth token"
        def result = mockMvc.perform(
                withAuth(MockMvcRequestBuilders.get('/api/users/me'))
        )

        then: "current user profile is returned"
        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.username').value('testuser'))
    }

    /**
     * Tests updating the current user's profile:
     * 1. Prepares update data with new display name and bio
     * 2. Simulates an authenticated request to update profile
     * 3. Verifies the response contains updated information
     *
     * Uses JWT token authentication from withAuth method
     */
    def "should update current user profile"() {
        given: "update data for user profile"
        def updateRequest = [
                displayName: 'Updated Name',
                bio: 'New bio text'
        ]

        when: "updating current user profile with valid auth token"
        def result = mockMvc.perform(
                jsonRequest(
                        withAuth(MockMvcRequestBuilders.put('/api/users/me')),
                        updateRequest
                )
        )

        then: "user profile is updated correctly"
        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.displayName').value('Updated Name'))
                .andExpect(MockMvcResultMatchers.jsonPath('$.bio').value('New bio text'))
    }

}
