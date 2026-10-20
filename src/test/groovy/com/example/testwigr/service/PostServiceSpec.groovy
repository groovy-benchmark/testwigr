package com.example.testwigr.service

import com.example.testwigr.exception.ResourceNotFoundException
import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import spock.lang.Specification

/**
 * Unit test for PostService that verifies post management functionality.
 * These tests use mocked dependencies to focus on the service logic in isolation.
 *
 * The test suite covers CRUD operations, like/unlike functionality, and comment
 * management to ensure the service properly handles all post-related operations.
 */
class PostServiceSpec extends Specification {

    // Dependencies to be mocked
    PostRepository postRepository
    UserService userService
    PostService postService

    /**
     * Set up mocks and service instance before each test
     */
    def setup() {
        postRepository = Mock(PostRepository)
        userService = Mock(UserService)
        postService = new PostService(postRepository, userService)
    }

    /**
     * Tests post creation:
     * 1. Sets up a user and post content
     * 2. Verifies the service correctly creates and saves the post
     * 3. Confirms post attributes are set correctly
     */
    def "should create a post successfully"() {
        given: "a user and post content"
        def userId = '123'
        def content = 'This is a test post'
        def user = new User(id: userId, username: 'testuser')

        and: "mocked service and repository behavior"
        userService.getUserById(userId) >> user
        postRepository.save(_ as Post) >> { Post post -> post }

        when: "creating a post"
        def result = postService.createPost(content, userId)

        then: "post is created with correct attributes"
        result.content == content
        result.userId == userId
        result.username == 'testuser'

//        and: "repository was called to save the post"
//        1 * postRepository.save({ Post post ->
//            post.content == content &&
//                    post.userId == userId &&
//                    post.username == 'testuser'
//        })
    }

    /**
     * Tests post retrieval by ID:
     * 1. Sets up a post ID and mocks repository response
     * 2. Verifies the service correctly retrieves the post
     */
    def "should get post by id"() {
        given: "a post ID and mocked repository response"
        def postId = '456'
        def post = new Post(id: postId, content: 'Test post', userId: '123', username: 'testuser')

        and: "repository will return the post"
        postRepository.findById(postId) >> Optional.of(post)

        when: "retrieving post by ID"
        def result = postService.getPostById(postId)

        then: "correct post is returned"
        result.id == postId
        result.content == 'Test post'
    }

    /**
     * Tests error handling when post is not found:
     * 1. Sets up a non-existent post ID
     * 2. Verifies the service throws appropriate exception
     */
    def "should throw exception when post id not found"() {
        given: "a non-existent post ID"
        def postId = 'nonexistent'

        and: "repository will return empty optional"
        postRepository.findById(postId) >> Optional.empty()

        when: "attempting to retrieve non-existent post"
        postService.getPostById(postId)

        then: "ResourceNotFoundException is thrown"
        thrown(ResourceNotFoundException)
    }

    /**
     * Tests retrieving posts by user ID:
     * 1. Sets up a user and sample posts
     * 2. Verifies the service correctly retrieves posts for that user
     */
    def "should get posts by user id"() {
        given: "a user ID, user, and posts"
        def userId = '123'
        def user = new User(id: userId, username: 'testuser')
        def posts = [new Post(content: 'Post 1', userId: userId), new Post(content: 'Post 2', userId: userId)]
        def pageable = PageRequest.of(0, 10)

        and: "mocked service and repository behavior"
        userService.getUserById(userId) >> user
        postRepository.findByUserId(userId, pageable) >> new PageImpl<>(posts)

        when: "retrieving posts by user ID"
        def result = postService.getPostsByUserId(userId, pageable)

        then: "posts for the user are returned"
        result.content.size() == 2
        result.content[0].content == 'Post 1'
        result.content[1].content == 'Post 2'
    }

    /**
     * Tests feed generation for a user:
     * 1. Sets up a user with following relationships
     * 2. Verifies the service retrieves posts from followed users
     */
    def "should get feed for user"() {
        given: "a user with following relationship"
        def userId = '123'
        def followingId = '456'
        def user = new User(id: userId, username: 'testuser', following: [followingId] as Set)
        def posts = [
                new Post(content: 'Post 1', userId: userId),
                new Post(content: 'Post 2', userId: followingId)
        ]
        def pageable = PageRequest.of(0, 10)

        and: "mocked service and repository behavior"
        userService.getUserById(userId) >> user
        // Use more flexible argument matcher
        postRepository.findByUserIdIn(_ as Collection, _ as Pageable) >> new PageImpl<>(posts)

        when: "generating feed for user"
        def result = postService.getFeedForUser(userId, pageable)

        then: "feed contains posts from user and followed users"
        result.content.size() == 2

//        and: "repository was called with correct user IDs"
//        1 * postRepository.findByUserIdIn({ Set ids ->
//            ids.size() == 2 && ids.containsAll([userId, followingId])
//        }, _ as Pageable)
    }

    /**
     * Tests post like functionality:
     * 1. Sets up a post and user ID
     * 2. Verifies the service correctly adds the user to post likes
     */
    def "should like a post"() {
        given: "a post and user ID"
        def postId = '456'
        def userId = '123'
        def post = new Post(id: postId, content: 'Test post', likes: [] as Set)

        and: "mocked repository behavior"
        postRepository.findById(postId) >> Optional.of(post)
        postRepository.save(_ as Post) >> { Post p -> p }

        when: "liking the post"
        def result = postService.likePost(postId, userId)

        then: "post shows as liked by the user"
        result.likes.contains(userId)
        result.isLikedBy(userId)

//        and: "post was saved with updated likes"
//        1 * postRepository.save({ Post p ->
//            p.likes.contains(userId)
//        })
    }

    /**
     * Tests post unlike functionality:
     * 1. Sets up a post with an existing like
     * 2. Verifies the service correctly removes the user from post likes
     */
    def "should unlike a post"() {
        given: "a post liked by a user"
        def postId = '456'
        def userId = '123'
        def post = new Post(id: postId, content: 'Test post', likes: [userId] as Set)

        and: "mocked repository behavior"
        postRepository.findById(postId) >> Optional.of(post)
        postRepository.save(_ as Post) >> { Post p -> p }

        when: "unliking the post"
        def result = postService.unlikePost(postId, userId)

        then: "post no longer shows as liked by the user"
        !result.likes.contains(userId)
        !result.isLikedBy(userId)

//        and: "post was saved with updated likes"
//        1 * postRepository.save({ Post p ->
//            !p.likes.contains(userId)
//        })
    }

    /**
     * Tests adding a comment to a post:
     * 1. Sets up a post, user, and comment content
     * 2. Verifies the service correctly adds the comment to the post
     */
    def "should add a comment to a post"() {
        given: "a post, user, and comment content"
        def postId = '456'
        def userId = '123'
        def content = 'This is a comment'
        def post = new Post(id: postId, content: 'Test post', comments: [])
        def user = new User(id: userId, username: 'testuser')

        and: "mocked repository and service behavior"
        postRepository.findById(postId) >> Optional.of(post)
        userService.getUserById(userId) >> user
        postRepository.save(_ as Post) >> { Post p -> p }

        when: "adding a comment to the post"
        def result = postService.addComment(postId, content, userId)

        then: "post now contains the comment"
        result.comments.size() == 1
        result.comments[0].content == content
        result.comments[0].userId == userId
        result.comments[0].username == 'testuser'

//        and: "post was saved with the new comment"
//        1 * postRepository.save({ Post p ->
//            p.comments.size() == 1 &&
//                    p.comments[0].content == content &&
//                    p.comments[0].userId == userId
//        })
    }

}
