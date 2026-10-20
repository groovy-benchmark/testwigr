package com.example.testwigr.controller

import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

/**
 * Integration test for PostController that tests the core post management functionality.
 * These tests verify post creation, retrieval, and social interactions like likes.
 * Uses a real test database and Spring's @WithMockUser for authentication.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostControllerIntegrationSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    ObjectMapper objectMapper

    @Autowired
    UserRepository userRepository

    @Autowired
    PostRepository postRepository

    /**
     * Set up test data before each test:
     * 1. Clean the database to ensure test isolation
     * 2. Create a test user for post operations
     */
    def setup() {
        // Clear the database before each test
        postRepository.deleteAll()
        userRepository.deleteAll()

        // Create a test user
        def user = TestDataFactory.createUser(null, "integrationuser")
        userRepository.save(user)
    }

    /**
     * Tests the full post lifecycle:
     * 1. Create a new post
     * 2. Retrieve the created post
     * 3. Verify post content and author information
     *
     * Uses @WithMockUser to authenticate as 'integrationuser'
     */
    @WithMockUser(username = "integrationuser")
    def "should create and retrieve a post"() {
        given: "authenticated user and post content"
        def user = userRepository.findByUsername("integrationuser").get()
        def createPostRequest = [content: "Integration test post"]

        when: "creating a new post"
        def createResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPostRequest))
        )

        then: "post is created successfully"
        createResult.andExpect(MockMvcResultMatchers.status().isOk())

        // Extract the post ID from the response for further operations
        def postId = objectMapper.readValue(
                createResult.andReturn().response.contentAsString,
                Map
        ).id

        when: "retrieving the created post"
        def getResult = mockMvc.perform(
                MockMvcRequestBuilders.get("/api/posts/${postId}")
        )

        then: "post details are returned correctly"
        getResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.content').value("Integration test post"))
                .andExpect(MockMvcResultMatchers.jsonPath('$.userId').value(user.id))
    }

    /**
     * Tests post like functionality:
     * 1. Create a test post
     * 2. Like the post
     * 3. Verify post shows as liked
     * 4. Unlike the post
     * 5. Verify post no longer shows as liked
     *
     * Uses @WithMockUser to authenticate as 'integrationuser'
     */
    @WithMockUser(username = "integrationuser")
    def "should like and unlike a post"() {
        given: "user and post in the database"
        def user = userRepository.findByUsername("integrationuser").get()
        def post = TestDataFactory.createPost(null, "Post to like", user.id, user.username)
        postRepository.save(post)

        when: "user likes the post"
        def likeResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/posts/${post.id}/like")
        )

        then: "like operation succeeds"
        likeResult.andExpect(MockMvcResultMatchers.status().isOk())

        when: "retrieving the post"
        def getResult = mockMvc.perform(
                MockMvcRequestBuilders.get("/api/posts/${post.id}")
        )

        then: "post shows as liked by the user"
        getResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.likes').isArray())
                .andExpect(MockMvcResultMatchers.jsonPath('$.likes[0]').value(user.id))

        when: "user unlikes the post"
        def unlikeResult = mockMvc.perform(
                MockMvcRequestBuilders.delete("/api/posts/${post.id}/like")
        )

        then: "unlike operation succeeds"
        unlikeResult.andExpect(MockMvcResultMatchers.status().isOk())

        when: "retrieving the post again"
        def getFinalResult = mockMvc.perform(
                MockMvcRequestBuilders.get("/api/posts/${post.id}")
        )

        then: "post no longer shows as liked"
        getFinalResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.likes').isEmpty())
    }

}
