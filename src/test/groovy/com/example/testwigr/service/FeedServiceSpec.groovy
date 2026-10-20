/* groovylint-disable ImplicitClosureParameter */
package com.example.testwigr.service

import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import spock.lang.Specification

/**
 * Unit test for FeedService class that tests feed generation logic.
 * These tests verify that the service correctly returns personal and user-specific feeds
 * by mocking dependencies to focus on feed logic in isolation.
 */
class FeedServiceSpec extends Specification {

    // Dependencies to be mocked
    PostRepository postRepository
    UserService userService
    FeedService feedService

    /**
     * Set up mocks and service instance before each test
     */
    def setup() {
        postRepository = Mock(PostRepository)
        userService = Mock(UserService)
        feedService = new FeedService(postRepository, userService)
    }

    /**
     * Tests generating a personal feed:
     * 1. Sets up a user with following relationships
     * 2. Mocks repository to return sample posts
     * 3. Verifies feed contains posts from user and followed users
     */
    def "should get personal feed"() {
        given: "a user with following relationships"
        def userId = '123'
        def followingId1 = '456'
        def followingId2 = '789'
        def user = new User(id: userId, username: 'user', following: [followingId1, followingId2] as Set)

        and: "sample posts from user and followed users"
        def posts = [
                new Post(id: 'p1', content: 'Post 1', userId: userId),
                new Post(id: 'p2', content: 'Post 2', userId: followingId1),
                new Post(id: 'p3', content: 'Post 3', userId: followingId2)
        ]

        def pageable = PageRequest.of(0, 10)

        and: "mocked service and repository responses"
        userService.getUserById(userId) >> user
        postRepository.findByUserIdIn(_ as Collection, _ as Pageable) >> new PageImpl<>(posts)

        when: "getting the personal feed"
        def result = feedService.getPersonalFeed(userId, pageable)

        then: "feed contains posts from user and followed users"
        result.content.size() == 3
        result.content.find { it.id == 'p1' } != null
        result.content.find { it.id == 'p2' } != null
        result.content.find { it.id == 'p3' } != null

        // and: "the repository was called with correct user IDs"
        // 1 * postRepository.findByUserIdIn({ Set ids ->
        //     ids.size() == 3 && ids.containsAll([userId, followingId1, followingId2])
        // }, _ as Pageable)
    }

    /**
     * Tests generating a user-specific feed:
     * 1. Sets up a user
     * 2. Mocks repository to return sample posts
     * 3. Verifies feed contains only posts from that user
     */
    def "should get user feed"() {
        given: "a user"
        def userId = '123'
        def user = new User(id: userId, username: 'user')

        and: "sample posts from the user"
        def posts = [
                new Post(id: 'p1', content: 'Post 1', userId: userId),
                new Post(id: 'p2', content: 'Post 2', userId: userId)
        ]

        def pageable = PageRequest.of(0, 10)

        and: "mocked service and repository responses"
        userService.getUserById(userId) >> user
        postRepository.findByUserId(userId, pageable) >> new PageImpl<>(posts)

        when: "getting the user feed"
        def result = feedService.getUserFeed(userId, pageable)

        then: "feed contains only posts from the specified user"
        result.content.size() == 2
        result.content.every { it.userId == userId }

        // and: "the repository was called with correct user ID"
        // 1 * postRepository.findByUserId(userId, pageable)
    }

}
