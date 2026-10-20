package com.example.testwigr.test

import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import com.example.testwigr.repository.UserRepository
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.data.mongodb.core.query.Query

/**
 * Utility class providing database-related helper methods for tests.
 * Includes methods for populating test data, cleaning the database,
 * and creating test scenarios with users and posts.
 */
class TestDatabaseUtils {

    /**
     * Populates the test database with users and posts.
     * Useful for setting up a baseline dataset for tests.
     *
     * @param userRepository Repository for user entities
     * @param postRepository Repository for post entities
     * @param passwordEncoder Encoder for securely storing passwords
     * @param userCount Number of users to create
     * @param postsPerUser Number of posts to create per user
     */
    static void populateDatabase(UserRepository userRepository,
                                 PostRepository postRepository,
                                 PasswordEncoder passwordEncoder,
                                 int userCount = 3,
                                 int postsPerUser = 2) {
        // Clear existing data to avoid test interference
        cleanDatabase(userRepository, postRepository)

        // Create and save users
        def users = []
        userCount.times { i ->
            def user = TestDataFactory.createUser(null, "dbuser${i}")
            user.password = passwordEncoder.encode("password")
            users << userRepository.save(user)
        }

        // Create and save posts for each user
        users.each { user ->
            postsPerUser.times { i ->
                def post = TestDataFactory.createPost(null, "Database test post ${i}", user.id, user.username)
                postRepository.save(post)
            }
        }
    }

    /**
     * Creates a social network with follow relationships between users.
     * Establishes a circular follow pattern where each user follows two others.
     *
     * @param userRepository Repository for user entities
     * @param passwordEncoder Encoder for securely storing passwords
     * @param userCount Number of users to create in the network
     */
    static void createSocialNetwork(UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    int userCount = 5) {
        // Clear existing users
        userRepository.deleteAll()

        // Create users
        def users = []
        userCount.times { i ->
            def user = TestDataFactory.createUser(null, "netuser${i}")
            user.password = passwordEncoder.encode("password")
            users << userRepository.save(user)
        }

        // Create a follow network (each user follows some others)
        users.eachWithIndex { user, idx ->
            // Each user follows 2 other users (with wraparound)
            def firstFollowIdx = (idx + 1) % userCount
            def secondFollowIdx = (idx + 2) % userCount

            // Add users to following list
            user.following.add(users[firstFollowIdx].id)
            user.following.add(users[secondFollowIdx].id)

            // Add user to followers list of followed users
            users[firstFollowIdx].followers.add(user.id)
            users[secondFollowIdx].followers.add(user.id)

            // Save changes to database
            userRepository.save(user)
            userRepository.save(users[firstFollowIdx])
            userRepository.save(users[secondFollowIdx])
        }
    }

    /**
     * Creates a complex database scenario with realistic social network data.
     * Includes users with follow relationships and posts with varied content.
     *
     * @param userRepository Repository for user entities
     * @param postRepository Repository for post entities
     * @param passwordEncoder Encoder for securely storing passwords
     * @param userCount Number of users to create
     * @return Map containing lists of created users and posts
     */
    static Map<String, Object> createComplexDatabaseScenario(
            UserRepository userRepository,
            PostRepository postRepository,
            PasswordEncoder passwordEncoder,
            int userCount = 10) {

        // Clean database first
        cleanDatabase(userRepository, postRepository)

        // Create complex social network
        def scenarioData = TestDataFactory.createComplexSocialNetwork(userCount)

        // Save all users to the database
        def savedUsers = []
        scenarioData.users.each { user ->
            user.password = passwordEncoder.encode("password")
            savedUsers << userRepository.save(user)
        }

        // Save all posts to the database
        def savedPosts = []
        scenarioData.posts.each { post ->
            savedPosts << postRepository.save(post)
        }

        return [users: savedUsers, posts: savedPosts]
    }

    /**
     * Populates database with specific test cases for edge cases.
     * Creates various user and post scenarios for thorough testing.
     *
     * @param userRepository Repository for user entities
     * @param postRepository Repository for post entities
     * @param passwordEncoder Encoder for securely storing passwords
     */
    static void populateWithTestCases(
            UserRepository userRepository,
            PostRepository postRepository,
            PasswordEncoder passwordEncoder) {

        // Clean database first
        cleanDatabase(userRepository, postRepository)

        // 1. Create an inactive user
        def inactiveUser = TestDataFactory.createInactiveUser()
        inactiveUser.password = passwordEncoder.encode("password")
        userRepository.save(inactiveUser)

        // 2. Create a user with very long content
        def longContentUser = TestDataFactory.createUser(null, "longcontent")
        longContentUser.bio = "A" * 500 // Very long bio
        longContentUser.password = passwordEncoder.encode("password")
        userRepository.save(longContentUser)

        // 3. Create a post with maximum length content
        def user = TestDataFactory.createUser()
        user.password = passwordEncoder.encode("password")
        def savedUser = userRepository.save(user)

        def maxLengthPost = TestDataFactory.createPost(
                null,
                "X" * 280, // Maximum Twitter-like length
                savedUser.id,
                savedUser.username
        )
        postRepository.save(maxLengthPost)

        // 4. Create a post with many comments
        def commentedPost = TestDataFactory.createPost(
                null,
                "Post with many comments",
                savedUser.id,
                savedUser.username
        )

        // Add 20 comments from the same user
        20.times { i ->
            commentedPost.comments << TestDataFactory.createComment(
                    "Comment ${i}",
                    savedUser.id,
                    savedUser.username
            )
        }

        postRepository.save(commentedPost)

        // 5. Create a post with many likes
        def popularPost = TestDataFactory.createPost(
                null,
                "Very popular post",
                savedUser.id,
                savedUser.username
        )

        // Create 25 users who like the post
        25.times { i ->
            def liker = TestDataFactory.createUser(null, "liker${i}")
            liker.password = passwordEncoder.encode("password")
            def savedLiker = userRepository.save(liker)

            popularPost.likes.add(savedLiker.id)
        }

        postRepository.save(popularPost)
    }

    /**
     * Cleans the database by removing all posts and users.
     * Important to call before and/or after tests to ensure isolation.
     *
     * @param userRepository Repository for user entities
     * @param postRepository Repository for post entities
     */
    static void cleanDatabase(UserRepository userRepository, PostRepository postRepository) {
        postRepository.deleteAll()
        userRepository.deleteAll()
    }

    /**
     * Performs a more thorough cleaning by dropping and recreating collections.
     * Useful when deleteAll() is not sufficient due to cached data.
     *
     * @param mongoTemplate MongoTemplate for direct MongoDB operations
     */
    static void dropCollections(MongoTemplate mongoTemplate) {
        mongoTemplate.dropCollection(User.class)
        mongoTemplate.dropCollection(Post.class)
    }

    /**
     * Returns statistics about the current state of the database.
     * Useful for debugging tests and verifying data state.
     *
     * @param userRepository Repository for user entities
     * @param postRepository Repository for post entities
     * @return Map containing counts of users and posts
     */
    static Map<String, Integer> getDatabaseStats(UserRepository userRepository, PostRepository postRepository) {
        return [
                userCount: userRepository.count(),
                postCount: postRepository.count()
        ]
    }

    /**
     * Creates a test user with known credentials in the database.
     * Useful for tests requiring a predictable user.
     *
     * @param userRepository Repository for user entities
     * @param passwordEncoder Encoder for securely storing passwords
     * @param username Username for the test user
     * @return The created or existing User entity
     */
    static User createTestUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String username = "testuser") {

        def existingUser = userRepository.findByUsername(username)
        if (existingUser.isPresent()) {
            return existingUser.get()
        }

        def user = TestDataFactory.createUser(null, username)
        user.password = passwordEncoder.encode("password")
        return userRepository.save(user)
    }

    /**
     * Creates a user with valid credentials for authentication tests.
     *
     * @param userRepository Repository for user entities
     * @param passwordEncoder Encoder for securely storing passwords
     * @return The created User entity
     */
    static User createAuthenticatedUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        def user = createTestUser(userRepository, passwordEncoder, "authuser")
        return user
    }

}
