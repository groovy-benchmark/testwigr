package com.example.testwigr.service

import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import com.example.testwigr.test.TestDataFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

/**
 * Integration test for PostService that tests post management functionality.
 * Uses a real test database to verify that the service interacts correctly
 * with the repositories and performs proper CRUD operations on posts.
 */
@SpringBootTest(properties = [
        'spring.main.allow-bean-definition-overriding=true',
        'spring.main.allow-circular-references=true'
])
@ActiveProfiles('test')
class PostServiceIntegrationTest extends Specification {

    /**
     * Test configuration class to provide necessary beans for testing.
     */
    @TestConfiguration
    static class TestConfig {
        /**
         * Provides a password encoder for the test context.
         *
         * @return BCryptPasswordEncoder instance
         */
        @Bean
        @Primary
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder()
        }
    }

    @Autowired
    PostService postService

    @Autowired
    UserService userService

    @Autowired
    PostRepository postRepository

    @Autowired
    UserRepository userRepository

    /**
     * Clean the database before each test to ensure isolation.
     */
    def setup() {
        // Clear the database before each test
        postRepository.deleteAll()
        userRepository.deleteAll()
    }

    /**
     * Tests the complete post lifecycle:
     * 1. Creates a user in the database
     * 2. Creates a post for that user
     * 3. Verifies the post is created with correct attributes
     * 4. Retrieves the post by ID
     * 5. Verifies retrieved post matches the created post
     */
    def "should create and retrieve a post"() {
        given: "a user in the database"
        def user = TestDataFactory.createUser()
        userRepository.save(user)

        when: "creating a post"
        def post = postService.createPost('Test content', user.id)

        then: "post is created with correct data"
        post.content == 'Test content'
        post.userId == user.id
        post.username == user.username
        post.createdAt != null
        post.updatedAt != null

        when: "retrieving the post by ID"
        def retrievedPost = postService.getPostById(post.id)

        then: "the correct post is retrieved"
        retrievedPost.id == post.id
        retrievedPost.content == 'Test content'
        retrievedPost.userId == user.id
        retrievedPost.username == user.username
    }

    /**
     * Tests post like functionality:
     * 1. Creates a user and post in the database
     * 2. Likes the post as the user
     * 3. Verifies the post shows as liked
     * 4. Unlikes the post
     * 5. Verifies the post no longer shows as liked
     */
    def "should like and unlike a post"() {
        given: "a user and a post in the database"
        def user = TestDataFactory.createUser()
        userRepository.save(user)
        def post = TestDataFactory.createPost(null, 'Post to like', user.id, user.username)
        postRepository.save(post)

        when: "liking the post"
        def likedPost = postService.likePost(post.id, user.id)

        then: "post shows as liked by the user"
        likedPost.likes.contains(user.id)
        likedPost.isLikedBy(user.id)
        likedPost.getLikeCount() == 1

        when: "unliking the post"
        def unlikedPost = postService.unlikePost(post.id, user.id)

        then: "post shows as unliked by the user"
        !unlikedPost.likes.contains(user.id)
        !unlikedPost.isLikedBy(user.id)
        unlikedPost.getLikeCount() == 0
    }

    /**
     * Tests comment functionality:
     * 1. Creates a user and post in the database
     * 2. Adds a comment to the post
     * 3. Verifies the comment exists on the post
     * 4. Retrieves comments for the post
     * 5. Verifies retrieved comments match expected
     */
    def "should add and retrieve comments"() {
        given: "a user and a post in the database"
        def user = TestDataFactory.createUser()
        userRepository.save(user)
        def post = TestDataFactory.createPost(null, 'Post for comments', user.id, user.username)
        postRepository.save(post)

        when: "adding a comment to the post"
        def commentContent = "This is a test comment"
        def postWithComment = postService.addComment(post.id, commentContent, user.id)

        then: "post now has the comment"
        postWithComment.comments.size() == 1
        postWithComment.comments[0].content == commentContent
        postWithComment.comments[0].userId == user.id
        postWithComment.comments[0].username == user.username

        when: "retrieving comments for the post"
        def comments = postService.getCommentsByPostId(post.id)

        then: "comments are retrieved correctly"
        comments.size() == 1
        comments[0].content == commentContent
        comments[0].userId == user.id
        comments[0].username == user.username
    }

}
