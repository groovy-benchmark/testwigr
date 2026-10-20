package com.example.testwigr.controller

import com.example.testwigr.model.User
import com.example.testwigr.service.UserService
import com.example.testwigr.test.TestDataFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.when

/**
 * Web slice test for UserController that tests controller endpoints in isolation.
 * Uses @WebMvcTest to focus on web layer only with mocked service dependencies.
 * These tests verify user profile retrieval functionality.
 */
@WebMvcTest(controllers = [UserController.class])
@ActiveProfiles('test')
class UserControllerWebTest extends Specification {

    @Autowired
    MockMvc mockMvc

    // Mock dependencies required by the controller
    @MockBean
    UserService userService

    @MockBean
    AuthenticationManager authenticationManager

    /**
     * Tests retrieving a user profile by username:
     * 1. Mocks the userService to return a predefined user
     * 2. Requests the user profile by username
     * 3. Verifies response contains the expected user data
     *
     * Uses @WithMockUser to simulate authenticated request
     */
    @WithMockUser(username = 'testuser')
    def "should get user by username"() {
        given: "a username and mocked user service response"
        def username = 'testuser'
        def user = TestDataFactory.createUser('123', username)

        // Configure mock to return the test user
        when(userService.getUserByUsername(anyString())).thenReturn(user)

        when: "requesting user profile by username"
        def result = mockMvc.perform(
                MockMvcRequestBuilders.get("/api/users/${username}")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
        )

        then: "user profile is returned with correct information"
        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.username').value(username))
    }

    /**
     * Tests retrieving the current authenticated user's profile:
     * 1. Mocks the userService to return a predefined user
     * 2. Requests the current user profile
     * 3. Verifies response contains the expected user data
     *
     * Uses @WithMockUser to simulate authenticated request with username 'testuser'
     */
    @WithMockUser(username = 'testuser')
    def "should get current user"() {
        given: "an authenticated user and mocked user service response"
        def username = 'testuser'
        def user = TestDataFactory.createUser('123', username)

        // Configure mock to return the test user
        when(userService.getUserByUsername(anyString())).thenReturn(user)

        when: "requesting current user profile"
        def result = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/users/me')
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
        )

        then: "current user profile is returned with correct information"
        result.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.username').value(username))
    }
}
