package com.example.testwigr.controller

import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import com.example.testwigr.test.TestDatabaseUtils
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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
 * Test suite focused on edge cases and validation handling across different controllers.
 * These tests verify how the application behaves with invalid or boundary inputs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
class ControllerEdgeCaseTest extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    UserRepository userRepository

    @Autowired
    PostRepository postRepository

    @Autowired
    PasswordEncoder passwordEncoder

    @Autowired
    ObjectMapper objectMapper

    @Value('${app.jwt.secret:testSecretKeyForTestingPurposesOnlyDoNotUseInProduction}')
    String jwtSecret

    /**
     * Clear the database before each test to ensure isolation
     */
    def setup() {
        TestDatabaseUtils.cleanDatabase(userRepository, postRepository)
    }

    /**
     * Clean up after each test to leave the database in a clean state
     */
    def cleanup() {
        TestDatabaseUtils.cleanDatabase(userRepository, postRepository)
    }

    /**
     * Tests edge cases in user registration:
     * 1. Duplicate username validation
     * 2. Duplicate email validation
     * 3. Empty username validation
     */
    def "should handle edge cases in user registration"() {
        when: "registering with already existing username"
        // First create a user
        def firstRegistration = [
                username: 'duplicateuser',
                email: 'first@example.com',
                password: 'password123',
                displayName: 'First User'
        ]

        mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/register')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRegistration))
        )

        // Try to register again with same username
        def duplicateUsernameRequest = [
                username: 'duplicateuser',  // Same username as first registration
                email: 'second@example.com',
                password: 'password123',
                displayName: 'Second User'
        ]

        def duplicateResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/register')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUsernameRequest))
        )

        then: "registration fails with conflict status code"
        duplicateResult.andExpect(MockMvcResultMatchers.status().isConflict())

        when: "registering with already existing email"
        def duplicateEmailRequest = [
                username: 'uniqueuser',
                email: 'first@example.com', // Same email as first registration
                password: 'password123',
                displayName: 'Unique User'
        ]

        def duplicateEmailResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/register')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateEmailRequest))
        )

        then: "registration fails with conflict status code"
        duplicateEmailResult.andExpect(MockMvcResultMatchers.status().isConflict())

        when: "registering with empty username"
        def invalidRequest = [
                username: '',  // Empty username
                email: 'valid@example.com',
                password: 'password123',
                displayName: 'Invalid User'
        ]

        def invalidResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/register')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
        )

        then: "registration fails with bad request status"
        invalidResult.andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath('$.message').value('Validation failed'))
    }

    /**
     * Tests edge cases in post creation:
     * 1. Empty content validation
     * 2. Content exceeding maximum length validation
     * 3. Content at exactly maximum length (should succeed)
     */
    def "should handle edge cases in post creation"() {
        given: "an authenticated user"
        def user = TestDataFactory.createUser(null, 'postuser')
        user.password = passwordEncoder.encode('password123')
        userRepository.save(user)

        def loginRequest = [
                username: 'postuser',
                password: 'password123'
        ]

        def loginResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/login')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        ).andReturn()

        def token = objectMapper.readValue(
                loginResult.response.contentAsString,
                Map
        ).token

        when: "creating a post with empty content"
        def emptyPostRequest = [
                content: ''  // Empty content
        ]

        def emptyPostResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/posts')
                        .header('Authorization', "Bearer ${token}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyPostRequest))
        )

        then: "post creation fails with validation error"
        emptyPostResult.andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath('$.message').value('Validation failed'))

        when: "creating a post with content exceeding maximum length"
        def longPostRequest = [
                content: 'X' * 300  // Exceeds 280 character limit
        ]

        def longPostResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/posts')
                        .header('Authorization', "Bearer ${token}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(longPostRequest))
        )

        then: "post creation fails with validation error"
        longPostResult.andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath('$.message').value('Validation failed'))

        when: "creating a post with exactly the maximum length"
        def maxLengthPostRequest = [
                content: 'X' * 280  // Exactly 280 characters
        ]

        def maxLengthPostResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/posts')
                        .header('Authorization', "Bearer ${token}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maxLengthPostRequest))
        )

        then: "post creation succeeds"
        maxLengthPostResult.andExpect(MockMvcResultMatchers.status().isOk())
    }

    /**
     * Tests edge cases in post retrieval and updating:
     * 1. Non-existent post retrieval
     * 2. Update post with valid data
     * 3. Unauthorized post update attempt
     */
    def "should handle edge cases in post retrieval and updating"() {
        given: "an authenticated user with a post"
        def user = TestDataFactory.createUser(null, 'updateuser')
        user.password = passwordEncoder.encode('password123')
        def savedUser = userRepository.save(user)

        def loginRequest = [
                username: 'updateuser',
                password: 'password123'
        ]

        def loginResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/login')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        ).andReturn()

        def token = objectMapper.readValue(
                loginResult.response.contentAsString,
                Map
        ).token

        // Create a post
        def createPostRequest = [
                content: 'Original post content'
        ]

        def createPostResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/posts')
                        .header('Authorization', "Bearer ${token}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPostRequest))
        ).andReturn()

        def postId = objectMapper.readValue(
                createPostResult.response.contentAsString,
                Map
        ).id

        when: "requesting a non-existent post"
        def nonExistentResult = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/posts/nonexistentid')
                        .header('Authorization', "Bearer ${token}")
        )

        then: "request fails with not found error"
        nonExistentResult.andExpect(MockMvcResultMatchers.status().isNotFound())

        when: "updating a post with valid data"
        def updateRequest = [
                content: 'Updated post content'
        ]

        def updateResult = mockMvc.perform(
                MockMvcRequestBuilders.put("/api/posts/${postId}")
                        .header('Authorization', "Bearer ${token}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
        )

        then: "post is updated successfully"
        updateResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.content').value('Updated post content'))

        // Create another user to test security
        when: "another user tries to update the post"
        def otherUser = TestDataFactory.createUser(null, 'otheruser')
        otherUser.password = passwordEncoder.encode('password123')
        userRepository.save(otherUser)

        def otherLoginRequest = [
                username: 'otheruser',
                password: 'password123'
        ]

        def otherLoginResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/login')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherLoginRequest))
        ).andReturn()

        def otherToken = objectMapper.readValue(
                otherLoginResult.response.contentAsString,
                Map
        ).token

        def unauthorizedUpdateResult = mockMvc.perform(
                MockMvcRequestBuilders.put("/api/posts/${postId}")
                        .header('Authorization', "Bearer ${otherToken}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
        )

        then: "update fails with security exception"
        unauthorizedUpdateResult.andExpect(MockMvcResultMatchers.status().isForbidden())
    }

    /**
     * Tests edge cases in comment operations:
     * 1. Empty comment validation
     * 2. Maximum length comment validation
     * 3. Adding comment to non-existent post
     */
    def "should handle edge cases in comment operations"() {
        given: "a post with comments"
        def user = TestDataFactory.createUser(null, 'commentuser')
        user.password = passwordEncoder.encode('password123')
        userRepository.save(user)

        def loginRequest = [
                username: 'commentuser',
                password: 'password123'
        ]

        def loginResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/login')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        ).andReturn()

        def token = objectMapper.readValue(
                loginResult.response.contentAsString,
                Map
        ).token

        // Create a post
        def createPostRequest = [
                content: 'Post for comments'
        ]

        def createPostResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/posts')
                        .header('Authorization', "Bearer ${token}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPostRequest))
        ).andReturn()

        def postId = objectMapper.readValue(
                createPostResult.response.contentAsString,
                Map
        ).id

        when: "adding a comment with empty content"
        def emptyCommentRequest = [
                content: ''  // Empty comment
        ]

        def emptyCommentResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/comments/posts/${postId}")
                        .header('Authorization', "Bearer ${token}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyCommentRequest))
        )

        then: "comment creation fails with validation error"
        emptyCommentResult.andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath('$.message').value('Validation failed'))

        when: "adding a comment with maximum length content"
        def maxCommentRequest = [
                content: 'X' * 280  // Maximum allowed length
        ]

        def maxCommentResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/comments/posts/${postId}")
                        .header('Authorization', "Bearer ${token}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maxCommentRequest))
        )

        then: "comment is added successfully"
        maxCommentResult.andExpect(MockMvcResultMatchers.status().isOk())

        when: "adding a comment to a non-existent post"
        def validCommentRequest = [
                content: 'Valid comment'
        ]

        def nonExistentPostResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/comments/posts/nonexistentid')
                        .header('Authorization', "Bearer ${token}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCommentRequest))
        )

        then: "comment creation fails with not found error"
        nonExistentPostResult.andExpect(MockMvcResultMatchers.status().isNotFound())
    }

    /**
     * Tests edge cases in follow relationships:
     * 1. Self-follow validation
     * 2. Following non-existent user
     * 3. Duplicate follow operation
     */
    def "should handle edge cases in follow relationships"() {
        given: "two users"
        def user1 = TestDataFactory.createUser(null, 'followuser1')
        user1.password = passwordEncoder.encode('password123')
        def savedUser1 = userRepository.save(user1)

        def user2 = TestDataFactory.createUser(null, 'followuser2')
        user2.password = passwordEncoder.encode('password123')
        def savedUser2 = userRepository.save(user2)

        def loginRequest = [
                username: 'followuser1',
                password: 'password123'
        ]

        def loginResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/auth/login')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        ).andReturn()

        def token = objectMapper.readValue(
                loginResult.response.contentAsString,
                Map
        ).token

        when: "user tries to follow themselves"
        def selfFollowResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/follow/${savedUser1.id}")
                        .header('Authorization', "Bearer ${token}")
        )

        then: "follow operation fails with appropriate error"
        selfFollowResult.andExpect(MockMvcResultMatchers.status().isBadRequest())

        when: "user follows another user"
        def followResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/follow/${savedUser2.id}")
                        .header('Authorization', "Bearer ${token}")
        )

        then: "follow operation succeeds"
        followResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))

        when: "user follows the same user again"
        def duplicateFollowResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/follow/${savedUser2.id}")
                        .header('Authorization', "Bearer ${token}")
        )

        then: "operation succeeds but user is already following"
        duplicateFollowResult.andExpect(MockMvcResultMatchers.status().isOk())

        when: "user tries to follow a non-existent user"
        def nonExistentFollowResult = mockMvc.perform(
                MockMvcRequestBuilders.post('/api/follow/nonexistentid')
                        .header('Authorization', "Bearer ${token}")
        )

        then: "follow operation fails with not found error"
        nonExistentFollowResult.andExpect(MockMvcResultMatchers.status().isNotFound())
    }

}
