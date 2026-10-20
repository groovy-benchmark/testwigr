package com.example.testwigr.controller

import com.example.testwigr.model.Post
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
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

/**
 * Integration test for CommentController that tests adding and retrieving comments on posts.
 * Uses a test database and @WithMockUser for authentication.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
class CommentControllerIntegrationSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    ObjectMapper objectMapper

    @Autowired
    UserRepository userRepository

    @Autowired
    PostRepository postRepository

    /**
     * Sets up test data before each test:
     * 1. Cleans the database
     * 2. Creates a test user for commenting
     * 3. Creates another user as the post author
     * 4. Creates a test post for comments
     */
    def setup() {
        // Clear existing data to avoid test interference
        postRepository.deleteAll()
        userRepository.deleteAll()

        // Create a test user who will be making comments
        def user = TestDataFactory.createUser(null, 'commentuser')
        userRepository.save(user)

        // Create a test post belonging to a different user
        def user2 = TestDataFactory.createUser(null, 'posteruser')
        userRepository.save(user2)

        def post = TestDataFactory.createPost(null, 'Post for comments', user2.id, user2.username)
        postRepository.save(post)

        println "Test user: ${user.id}, ${user.username}"
        println "Test post: ${post.id}, ${post.content}"
    }

    /**
     * Tests the complete comment workflow:
     * 1. Add a comment to a post
     * 2. Verify the comment is saved correctly
     * 3. Retrieve comments for the post
     * 4. Verify the retrieved comment matches what was added
     */
    @WithMockUser(username = 'commentuser')
    def "should add and retrieve comments on a post"() {
        given: "a user and post in the database and comment content"
        def user = userRepository.findByUsername('commentuser').get()
        def post = postRepository.findAll().first()
        def commentRequest = [content: 'This is a test comment']

        println "Test user: ${user.id}, ${user.username}"
        println "Test post: ${post.id}, ${post.content}"

        when: "adding a comment to the post"
        // Perform an initial request to debug any potential issues
        MvcResult result = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/comments/posts/${post.id}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest))
        ).andReturn()

        // Add detailed error debugging if there's a problem
        if (result.getResponse().getStatus() != 200) {
            println "Error response: ${result.getResponse().getContentAsString()}"
            println "Status code: ${result.getResponse().getStatus()}"
        }

        // Perform the actual test request
        def commentResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/comments/posts/${post.id}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest))
        )

        then: "the comment is successfully added"
        commentResult.andExpect(MockMvcResultMatchers.status().isOk())

        when: "retrieving comments for the post"
        def getResult = mockMvc.perform(
                MockMvcRequestBuilders.get("/api/comments/posts/${post.id}")
        )

        then: "the retrieved comments include the newly added comment"
        // Verify the comment exists and has the expected content and user information
        getResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$[0].content').value('This is a test comment'))
                .andExpect(MockMvcResultMatchers.jsonPath('$[0].userId').value(user.id))
                .andExpect(MockMvcResultMatchers.jsonPath('$[0].username').value('commentuser'))
    }

}
