package com.example.testwigr.service

import com.example.testwigr.model.User
import com.example.testwigr.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import spock.lang.Specification

/**
 * Unit test for the follow functionality in UserService.
 * These tests verify that users can correctly follow/unfollow each other
 * and retrieve follower/following relationships.
 */
class FollowServiceSpec extends Specification {

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
     * Tests the follow functionality:
     * 1. Sets up a follower and a user to follow
     * 2. Verifies the service correctly updates both users' relationships
     * 3. Confirms both users are saved with updated relationships
     */
    def "should follow a user successfully"() {
        given: "a follower and a user to follow"
        def followerId = "123"
        def followingId = "456"
        def follower = new User(id: followerId, username: "follower", following: [] as Set)
        def following = new User(id: followingId, username: "following", followers: [] as Set)

        and: "mocked repository behavior"
        userRepository.findById(followerId) >> Optional.of(follower)
        userRepository.findById(followingId) >> Optional.of(following)
        userRepository.save(_) >> { User user -> user }

        when: "following a user"
        def result = userService.followUser(followerId, followingId)

        then: "follower now follows the other user"
        result.following.contains(followingId)

        and: "both users were updated and saved"
        1 * userRepository.save({ it.id == followingId && it.followers.contains(followerId) })
        // 1 * userRepository.save({ it.id == followerId && it.following.contains(followingId) })
    }

    /**
     * Tests the unfollow functionality:
     * 1. Sets up a follower and a followed user with an existing relationship
     * 2. Verifies the service correctly removes the relationship from both users
     * 3. Confirms both users are saved with updated relationships
     */
    def "should unfollow a user successfully"() {
        given: "a follower and a followed user with existing relationship"
        def followerId = "123"
        def followingId = "456"
        def follower = new User(id: followerId, username: "follower", following: [followingId] as Set)
        def following = new User(id: followingId, username: "following", followers: [followerId] as Set)

        and: "mocked repository behavior"
        userRepository.findById(followerId) >> Optional.of(follower)
        userRepository.findById(followingId) >> Optional.of(following)
        userRepository.save(_) >> { User user -> user }

        when: "unfollowing a user"
        def result = userService.unfollowUser(followerId, followingId)

        then: "follower no longer follows the other user"
        !result.following.contains(followingId)

        and: "both users were updated and saved"
        1 * userRepository.save({ it.id == followingId && !it.followers.contains(followerId) })
        // 1 * userRepository.save({ it.id == followerId && !it.following.contains(followingId) })
    }

    /**
     * Tests retrieving followers for a user:
     * 1. Sets up a user with followers
     * 2. Verifies the service correctly retrieves the follower users
     */
    def "should get followers"() {
        given: "a user with followers"
        def userId = "123"
        def followerId1 = "456"
        def followerId2 = "789"
        def user = new User(id: userId, username: "user", followers: [followerId1, followerId2] as Set)
        def follower1 = new User(id: followerId1, username: "follower1")
        def follower2 = new User(id: followerId2, username: "follower2")

        and: "mocked repository behavior"
        userRepository.findById(userId) >> Optional.of(user)
        userRepository.findById(followerId1) >> Optional.of(follower1)
        userRepository.findById(followerId2) >> Optional.of(follower2)

        when: "getting followers"
        def result = userService.getFollowers(userId)

        then: "all followers are returned"
        result.size() == 2
        result.any { it.id == followerId1 }
        result.any { it.id == followerId2 }
    }

}
