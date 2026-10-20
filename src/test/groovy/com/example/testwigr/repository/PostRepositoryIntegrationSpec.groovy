package com.example.testwigr.repository

import com.example.testwigr.config.MongoIntegrationSpec
import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.test.TestDataFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

/**
 * Integration tests for PostRepository that verify MongoDB operations.
 * These tests focus on the repository's ability to correctly save, query,
 * and retrieve posts from the MongoDB database, particularly testing
 * the custom query methods defined in the repository interface.
 *
 * The class extends MongoIntegrationSpec which provides the MongoDB
 * configuration for testing.
 */
class PostRepositoryIntegrationSpec extends MongoIntegrationSpec {

    @Autowired
    PostRepository postRepository

    @Autowired
    UserRepository userRepository

    /**
     * Clean up after each test to ensure test isolation
     */
    def cleanup() {
        postRepository.deleteAll()
        userRepository.deleteAll()
    }

    /**
     * Tests findByUserId with pagination:
     * 1. Creates a user and saves it to the database
     * 2. Creates two posts from that user and saves them
     * 3. Calls findByUserId with pagination
     * 4. Verifies that both posts are returned in the result
     */
    def "should save and find posts by user ID"() {
        given: "a user and two posts"
        def user = TestDataFactory.createUser()
        userRepository.save(user)

        def post1 = TestDataFactory.createPost(null, 'Test post 1', user.id, user.username)
        def post2 = TestDataFactory.createPost(null, 'Test post 2', user.id, user.username)

        when: "saving posts and finding by user ID"
        postRepository.save(post1)
        postRepository.save(post2)
        def posts = postRepository.findByUserId(user.id, PageRequest.of(0, 10))

        then: "both posts are found"
        posts.content.size() == 2
        posts.content.any { it.content == 'Test post 1' }
        posts.content.any { it.content == 'Test post 2' }
    }

    /**
     * Tests findByUserIdIn with pagination:
     * 1. Creates two users and saves them
     * 2. Creates a post from each user and saves them
     * 3. Calls findByUserIdIn with both user IDs
     * 4. Verifies that posts from both users are returned
     *
     * This test is particularly important for feed generation where
     * posts from multiple users need to be retrieved.
     */
    def "should find posts by multiple user IDs"() {
        given: "two users with one post each"
        def user1 = TestDataFactory.createUser(null, 'user1')
        def user2 = TestDataFactory.createUser(null, 'user2')
        userRepository.save(user1)
        userRepository.save(user2)

        def post1 = TestDataFactory.createPost(null, 'Post by user1', user1.id, user1.username)
        def post2 = TestDataFactory.createPost(null, 'Post by user2', user2.id, user2.username)
        postRepository.save(post1)
        postRepository.save(post2)

        when: "finding posts by both user IDs"
        def posts = postRepository.findByUserIdIn([user1.id, user2.id], PageRequest.of(0, 10))

        then: "posts from both users are found"
        posts.content.size() == 2
        posts.content.any { it.userId == user1.id }
        posts.content.any { it.userId == user2.id }
    }

}
