package com.example.testwigr.controller

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
 * Integration test for the FollowController, which tests social graph functionality.
 * These tests verify users can follow/unfollow other users and retrieve their social connections.
 * Uses Spring's @WithMockUser annotation to simulate authentication.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles('test')
class FollowControllerIntegrationSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    ObjectMapper objectMapper

    @Autowired
    UserRepository userRepository

    /**
     * Set up test data before each test:
     * 1. Clean the database
     * 2. Create test users for follow operations
     */
    def setup() {
        userRepository.deleteAll()

        // Create test users for following relationships
        def follower = TestDataFactory.createUser(null, 'follower')
        def following = TestDataFactory.createUser(null, 'following')

        userRepository.save(follower)
        userRepository.save(following)

        println "Follower user: ${follower.id}, ${follower.username}"
        println "Following user: ${following.id}, ${following.username}"
    }

    /**
     * Tests the complete follow/unfollow workflow:
     * 1. Follow another user
     * 2. Verify follow status
     * 3. Unfollow the user
     * 4. Verify updated follow status
     *
     * Authenticated as 'follower' using @WithMockUser annotation
     */
    @WithMockUser(username = 'follower')
    def "should follow and unfollow a user"() {
        given: "a user to follow"
        def following = userRepository.findByUsername('following').get()
        println "Test - Following user ID: ${following.id}"

        when: "user follows another user"
        MvcResult result = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/follow/${following.id}")
        ).andReturn()

        // Add detailed error debugging for follow operation
        if (result.getResponse().getStatus() != 200) {
            println "Error response: ${result.getResponse().getContentAsString()}"
            println "Status code: ${result.getResponse().getStatus()}"
            println "Error details: ${result.getResolvedException()?.getMessage()}"
            if (result.getResolvedException()?.getCause() != null) {
                println "Root cause: ${result.getResolvedException()?.getCause()?.getMessage()}"
            }
        }

        def followResult = mockMvc.perform(
                MockMvcRequestBuilders.post("/api/follow/${following.id}")
        )

        then: "follow operation succeeds"
        followResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
                .andExpect(MockMvcResultMatchers.jsonPath('$.isFollowing').value(true))

        when: "getting follow status"
        def statusResult = mockMvc.perform(
                MockMvcRequestBuilders.get("/api/follow/${following.id}/status")
        )

        then: "user is shown as following"
        statusResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.isFollowing').value(true))

        when: "user unfollows the other user"
        def unfollowResult = mockMvc.perform(
                MockMvcRequestBuilders.delete("/api/follow/${following.id}")
        )

        then: "unfollow operation succeeds"
        unfollowResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.success').value(true))
                .andExpect(MockMvcResultMatchers.jsonPath('$.isFollowing').value(false))

        when: "getting follow status after unfollowing"
        def finalStatusResult = mockMvc.perform(
                MockMvcRequestBuilders.get("/api/follow/${following.id}/status")
        )

        then: "user is shown as not following"
        finalStatusResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$.isFollowing').value(false))
    }

    /**
     * Tests retrieval of followers and following lists:
     * 1. Sets up follow relationships in the database
     * 2. Verifies correct retrieval of users that the authenticated user is following
     *
     * Authenticated as 'follower' using @WithMockUser annotation
     */
    @WithMockUser(username = 'follower')
    def "should get followers and following lists"() {
        given: "follow relationships between users"
        def follower = userRepository.findByUsername('follower').get()
        def following = userRepository.findByUsername('following').get()

        // Set up follow relationship directly in the database
        follower.following.add(following.id)
        following.followers.add(follower.id)
        userRepository.save(follower)
        userRepository.save(following)

        when: "getting list of users that follower is following"
        def followingResult = mockMvc.perform(
                MockMvcRequestBuilders.get('/api/follow/following')
        )

        then: "list contains the followed user"
        followingResult.andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath('$[0].id').value(following.id))
                .andExpect(MockMvcResultMatchers.jsonPath('$[0].username').value('following'))
    }

}
