package com.example.testwigr.service

import com.example.testwigr.exception.ResourceNotFoundException
import com.example.testwigr.exception.UserAlreadyExistsException
import com.example.testwigr.model.User
import com.example.testwigr.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import spock.lang.Specification

/**
 * Unit test for UserService that verifies user management functionality.
 * These tests use mocked dependencies to focus on the service logic in isolation.
 *
 * The test suite covers user registration, retrieval, update, and delete operations
 * to ensure the service properly handles all user-related operations and edge cases.
 */
class UserServiceSpec extends Specification {

    // Dependencies to be mocked
    UserRepository userRepository
    PasswordEncoder passwordEncoder
    UserService userService

    /**
     * Set up mocks and service instance before each test
     */
    def setup() {
        userRepository = Mock(UserRepository)
        passwordEncoder = Mock(PasswordEncoder)
        userService = new UserService(userRepository, passwordEncoder)
    }

    /**
     * Tests user creation with valid data:
     * 1. Sets up a new user with complete data
     * 2. Mocks repository and password encoder behavior
     * 3. Verifies the service correctly creates and saves the user
     */
    def "should create a new user successfully"() {
        given: "a new user with complete data"
        def userToCreate = new User(
                username: 'testuser',
                email: 'test@example.com',
                password: 'password123',
                displayName: 'Test User'
        )

        and: "mocked repository and encoder behavior"
        userRepository.existsByUsername('testuser') >> false
        userRepository.existsByEmail('test@example.com') >> false
        passwordEncoder.encode('password123') >> 'encodedPassword'
        userRepository.save(_ as User) >> { User user -> user }

        when: "creating a new user"
        def result = userService.createUser(userToCreate)

        then: "user is created with correct attributes"
        result.username == 'testuser'
        result.email == 'test@example.com'
        result.password == 'encodedPassword'
        result.displayName == 'Test User'

//        and: "password was encoded"
//        1 * passwordEncoder.encode('password123')
//
//        and: "user was saved to repository"
//        1 * userRepository.save({ User user ->
//            user.username == 'testuser' &&
//                    user.email == 'test@example.com' &&
//                    user.password == 'encodedPassword'
//        })
    }

    /**
     * Tests handling duplicate username during user creation:
     * 1. Sets up a user with a username that already exists
     * 2. Verifies the service throws appropriate exception
     */
    def "should throw exception when username already exists"() {
        given: "a user with existing username"
        def userToCreate = new User(
                username: 'existinguser',
                email: 'new@example.com',
                password: 'password123'
        )

        and: "username already exists in repository"
        userRepository.existsByUsername('existinguser') >> true

        when: "attempting to create user with existing username"
        userService.createUser(userToCreate)

        then: "UserAlreadyExistsException is thrown"
        thrown(UserAlreadyExistsException)
    }

    /**
     * Tests handling duplicate email during user creation:
     * 1. Sets up a user with an email that already exists
     * 2. Verifies the service throws appropriate exception
     */
    def "should throw exception when email already exists"() {
        given: "a user with existing email"
        def userToCreate = new User(
                username: 'newuser',
                email: 'existing@example.com',
                password: 'password123'
        )

        and: "email already exists in repository"
        userRepository.existsByUsername('newuser') >> false
        userRepository.existsByEmail('existing@example.com') >> true

        when: "attempting to create user with existing email"
        userService.createUser(userToCreate)

        then: "UserAlreadyExistsException is thrown"
        thrown(UserAlreadyExistsException)
    }

    /**
     * Tests retrieving a user by ID:
     * 1. Sets up a user ID and mocks repository response
     * 2. Verifies the service correctly retrieves the user
     */
    def "should get user by id"() {
        given: "a user ID and mocked repository response"
        def userId = '123'
        def user = new User(id: userId, username: 'testuser')

        and: "repository will return the user"
        userRepository.findById(userId) >> Optional.of(user)

        when: "retrieving user by ID"
        def result = userService.getUserById(userId)

        then: "correct user is returned"
        result.id == userId
        result.username == 'testuser'
    }

    /**
     * Tests error handling when user is not found by ID:
     * 1. Sets up a non-existent user ID
     * 2. Verifies the service throws appropriate exception
     */
    def "should throw exception when user id not found"() {
        given: "a non-existent user ID"
        def userId = 'nonexistent'

        and: "repository will return empty optional"
        userRepository.findById(userId) >> Optional.empty()

        when: "attempting to retrieve non-existent user"
        userService.getUserById(userId)

        then: "ResourceNotFoundException is thrown"
        thrown(ResourceNotFoundException)
    }

    /**
     * Tests following a user:
     * 1. Sets up a follower and a user to follow
     * 2. Verifies the service correctly establishes the follow relationship
     */
    def "should follow a user successfully"() {
        given: "a follower and a user to follow"
        def followerId = '123'
        def followingId = '456'
        def follower = new User(id: followerId, username: 'follower', following: [] as Set, followers: [] as Set)
        def following = new User(id: followingId, username: 'following', following: [] as Set, followers: [] as Set)

        and: "mocked repository behavior"
        userRepository.findById(followerId) >> Optional.of(follower)
        userRepository.findById(followingId) >> Optional.of(following)
        userRepository.save(_ as User) >> { User user -> user }

        when: "following a user"
        def result = userService.followUser(followerId, followingId)

        then: "follower now follows the other user"
        result.following.contains(followingId)

        and: "both users were updated and saved"
        1 * userRepository.save({ it.followers.contains(followerId) })
    }

    /**
     * Tests unfollowing a user:
     * 1. Sets up a follower and a followed user with an existing relationship
     * 2. Verifies the service correctly removes the follow relationship
     */
    def "should unfollow a user successfully"() {
        given: "a follower and a followed user with existing relationship"
        def followerId = '123'
        def followingId = '456'
        def follower = new User(id: followerId, username: 'follower', following: [followingId] as Set)
        def following = new User(id: followingId, username: 'following', followers: [followerId] as Set)

        and: "mocked repository behavior"
        userRepository.findById(followerId) >> Optional.of(follower)
        userRepository.findById(followingId) >> Optional.of(following)
        userRepository.save(_ as User) >> { User user -> user }

        when: "unfollowing a user"
        def result = userService.unfollowUser(followerId, followingId)

        then: "follower no longer follows the other user"
        !result.following.contains(followingId)

        and: "followed user was updated"
        1 * userRepository.save({ it.id == followingId && !it.followers.contains(followerId) })
    }

}
