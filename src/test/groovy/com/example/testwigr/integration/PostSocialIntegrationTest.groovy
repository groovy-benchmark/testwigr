package com.example.testwigr.integration

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

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
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import spock.lang.Specification

/**
 * Integration test focusing on social interactions with posts.
 * This test verifies that users can create posts and interact with them
 * through likes and comments. It confirms the core social features work
 * correctly in an integrated environment.
 */
@SpringBootTest
@ActiveProfiles('test')
class PostSocialIntegrationTest extends Specification {

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
    String authToken

    /**
     * Set up the test environment before each test:
     * 1. Configure MockMvc with Spring Security
     * 2. Clean the database
     * 3. Create a test user
     * 4. Authenticate the user to get a JWT token
     */
    def setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build()

        // Clean up repositories
        postRepository.deleteAll()
        userRepository.deleteAll()

        // Create a test user with a known ID for consistency
        def user = TestDataFactory.createUser('123', 'socialuser')
        user.password = passwordEncoder.encode('testpassword')
        userRepository.save(user)

        // Login and get JWT token
        def loginRequest = [
                username: 'socialuser',
                password: 'testpassword'
        ]

        def loginResult = mockMvc.perform(
                post('/api/auth/login')
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
        ).andReturn()

        def responseContent = loginResult.response.contentAsString
        println "Login response: ${responseContent}"

        if (loginResult.response.status == 200 && responseContent) {
            def responseJson = objectMapper.readValue(responseContent, Map)
            authToken = responseJson.token
            println "Retrieved auth token: ${authToken}"
        } else {
            println "Failed to get auth token. Status: ${loginResult.response.status}"
        }
    }

    /**
     * Tests the complete post social interaction flow:
     * 1. Create a new post
     * 2. Like the post
     * 3. Verify the post shows as liked
     * 4. Add a comment to the post
     * 5. Verify the comment is visible
     *
     * This test confirms that the core social functionalities
     * work correctly in an integrated environment.
     */
    def "should create and interact with a post"() {
        given: 'a post creation request'
        def postRequest = [content: 'Integration test post']

        when: 'creating a post'
        def createResult = mockMvc.perform(
                post('/api/posts')
                        .header('Authorization', "Bearer ${authToken}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest))
        )

        then: 'post is created successfully'
        createResult.andExpect(status().isOk())
                .andExpect(jsonPath('$.content').value('Integration test post'))

        and: 'the post ID can be extracted'
        def postJson = objectMapper.readValue(
                createResult.andReturn().response.contentAsString,
                Map
        )
        def postId = postJson.id

        when: 'liking the post'
        def user = userRepository.findByUsername('socialuser').get()
        println "User attempting to like: ${user.id}, ${user.username}"

        def likeResult = mockMvc.perform(
                post("/api/posts/${postId}/like")
                        .header('Authorization', "Bearer ${authToken}")
        )

        then: 'like operation succeeds'
        likeResult.andExpect(status().isOk())

        when: 'retrieving the post'
        def getResult = mockMvc.perform(
                get("/api/posts/${postId}")
                        .header('Authorization', "Bearer ${authToken}")
        )

        then: 'post shows as liked'
        getResult.andExpect(status().isOk())
                .andExpect(jsonPath('$.likes').isArray())
                .andExpect(jsonPath('$.likes[0]').exists())

        when: 'adding a comment'
        def commentRequest = [content: 'Integration test comment']
        def commentResult = mockMvc.perform(
                // Using the appropriate endpoint for adding comments
                post("/api/posts/${postId}/comments")
                        .header('Authorization', "Bearer ${authToken}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest))
        )

        then: 'comment is added successfully'
        commentResult.andExpect(status().isOk())

        when: 'retrieving comments'
        def getCommentsResult = mockMvc.perform(
                get("/api/posts/${postId}/comments")
                        .header('Authorization', "Bearer ${authToken}")
        )

        then: 'comment is visible'
        getCommentsResult.andExpect(status().isOk())
                .andExpect(jsonPath('$[0].content').value('Integration test comment'))
    }

}
