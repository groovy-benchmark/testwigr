package com.example.testwigr.repository

import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.test.TestDataFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

/**
 * Repository test for PostRepository that verifies MongoDB operations.
 * This test uses Spring's @DataMongoTest annotation to configure a minimal
 * test context that includes only MongoDB repositories and their dependencies.
 *
 * The purpose of this test is to verify that the custom query methods in
 * PostRepository correctly interact with MongoDB to retrieve posts based
 * on different criteria.
 */
@DataMongoTest
@ActiveProfiles("test")
class PostRepositoryTest extends Specification {

    @Autowired
    PostRepository postRepository

    @Autowired
    UserRepository userRepository

    /**
     * Set up a clean test environment before each test
     */
    def setup() {
        // Clear the database before each test
        postRepository.deleteAll()
        userRepository.deleteAll()
    }

    /**
     * Tests finding posts by user ID with pagination:
     * 1. Creates a user and saves it
     * 2. Creates multiple posts by that user and saves them
     * 3. Calls findByUserId to retrieve posts for that user
     * 4. Verifies that all posts by the user are correctly retrieved
     *
     * This test confirms that the custom query method correctly filters
     * posts by the user ID and applies pagination.
     */
    def "should find posts by user ID"() {
        given: "a user and some posts"
        def user = TestDataFactory.createUser()
        userRepository.save(user)

        def post1 = TestDataFactory.createPost(null, "Post 1", user.id, user.username)
        def post2 = TestDataFactory.createPost(null, "Post 2", user.id, user.username)
        postRepository.save(post1)
        postRepository.save(post2)

        when: "finding posts by user ID"
        def result = postRepository.findByUserId(user.id, PageRequest.of(0, 10))

        then: "correct posts are found"
        result.content.size() == 2
        result.content.any { it.content == "Post 1" }
        result.content.any { it.content == "Post 2" }
    }

    /**
     * Tests finding posts by multiple user IDs:
     * 1. Creates multiple users and saves them
     * 2. Creates posts by different users and saves them
     * 3. Calls findByUserIdIn with a list of user IDs
     * 4. Verifies that posts from all specified users are correctly retrieved
     *
     * This test confirms that the custom query method can filter posts by
     * a list of user IDs, which is essential for feed generation.
     */
    def "should find posts by multiple user IDs"() {
        given: "multiple users with posts"
        def user1 = TestDataFactory.createUser(null, "user1")
        def user2 = TestDataFactory.createUser(null, "user2")
        userRepository.save(user1)
        userRepository.save(user2)

        def post1 = TestDataFactory.createPost(null, "User 1 post", user1.id, user1.username)
        def post2 = TestDataFactory.createPost(null, "User 2 post", user2.id, user2.username)
        postRepository.save(post1)
        postRepository.save(post2)

        when: "finding posts by multiple user IDs"
        def result = postRepository.findByUserIdIn([user1.id, user2.id], PageRequest.of(0, 10))

        then: "posts from both users are found"
        result.content.size() == 2
        result.content.any { it.userId == user1.id }
        result.content.any { it.userId == user2.id }
    }

}
