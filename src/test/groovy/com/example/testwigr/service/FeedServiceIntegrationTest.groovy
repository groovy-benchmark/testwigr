package com.example.testwigr.service

import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import com.example.testwigr.test.TestDatabaseUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

/**
 * Integration test for FeedService that tests feed generation functionality.
 * Uses a real test database with a network of users and posts to verify
 * that feeds are generated correctly based on follow relationships.
 */
@SpringBootTest
@ActiveProfiles("test")
class FeedServiceIntegrationTest extends Specification {

    @Autowired
    FeedService feedService

    @Autowired
    UserRepository userRepository

    @Autowired
    PostRepository postRepository

    @Autowired
    PasswordEncoder passwordEncoder

    /**
     * Set up test environment before each test:
     * 1. Clean the database to ensure test isolation
     * 2. Create a network of test users with follow relationships
     * 3. Create test posts for each user
     */
    def setup() {
        // Clear database first
        TestDatabaseUtils.cleanDatabase(userRepository, postRepository)

        // Create a social network with 5 users
        TestDatabaseUtils.createSocialNetwork(userRepository, passwordEncoder, 5)

        // Create posts for each user (3 posts per user)
        userRepository.findAll().each { user ->
            3.times { i ->
                def post = TestDataFactory.createPost(null, "Feed post ${i} from ${user.username}", user.id, user.username)
                postRepository.save(post)
            }
        }
    }

    /**
     * Clean up after each test
     */
    def cleanup() {
        TestDatabaseUtils.cleanDatabase(userRepository, postRepository)
    }

    /**
     * Tests that the personal feed contains posts from followed users and self:
     * 1. Gets a test user with follow relationships
     * 2. Calls the feedService to get the personal feed
     * 3. Verifies that the feed includes posts from the user and followed users
     */
    def "should get personal feed containing posts from followed users"() {
        given: "a test user and pagination parameters"
        def testUser = userRepository.findAll().first()
        def pageable = PageRequest.of(0, 20)

        when: "getting the personal feed"
        def result = feedService.getPersonalFeed(testUser.id, pageable)

        then: "feed contains posts from the user and followed users"
        // Should contain posts from user and followed users (3 users * 3 posts each = 9 posts)
        result.content.size() == 9

        // Should contain posts from self
        result.content.findAll { it.userId == testUser.id }.size() == 3

        // Should contain posts from followed users
        def followedIds = testUser.following
        result.content.findAll { followedIds.contains(it.userId) }.size() == 6
    }

    /**
     * Tests that the user feed contains only posts from the specified user:
     * 1. Gets a test user
     * 2. Calls the feedService to get that user's feed
     * 3. Verifies that the feed includes only posts from that user
     */
    def "should get user feed containing only that user's posts"() {
        given: "a test user and pagination parameters"
        def testUser = userRepository.findAll().first()
        def pageable = PageRequest.of(0, 20)

        when: "getting the user feed"
        def result = feedService.getUserFeed(testUser.id, pageable)

        then: "feed contains only posts from the target user"
        // Should only contain posts from the target user
        result.content.size() == 3
        result.content.every { it.userId == testUser.id }
    }

}
