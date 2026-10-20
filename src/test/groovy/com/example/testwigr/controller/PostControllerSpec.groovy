package com.example.testwigr.controller

import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.service.PostService
import com.example.testwigr.service.UserService
import com.example.testwigr.test.TestDataFactory
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import spock.lang.Specification

/**
 * Unit test for the PostController class that tests post management endpoints.
 * These tests mock dependencies (services) to focus on controller logic in isolation.
 */
class PostControllerSpec extends Specification {

    // Dependencies to be mocked
    PostService postService
    UserService userService
    PostController postController

    /**
     * Setup all mocks and controller instance before each test
     */
    def setup() {
        postService = Mock(PostService)
        userService = Mock(UserService)
        postController = new PostController(postService, userService)
    }

    /**
     * Tests the post creation endpoint:
     * 1. Sets up a valid post creation request
     * 2. Mocks authentication and user service
     * 3. Verifies postService creates the post with correct data
     * 4. Checks the response status and content
     */
    def "should create post"() {
        given: "a post creation request from an authenticated user"
        def createPostRequest = new PostController.CreatePostRequest(content: 'Test post')
        def user = TestDataFactory.createUser('123', 'testuser')
        def post = TestDataFactory.createPost(null, 'Test post', '123', 'testuser')

        and: "authentication context and mocked service calls"
        def authentication = Mock(Authentication)
        def userDetails = Mock(UserDetails)

        // Configure mocks to simulate authenticated user
        authentication.getPrincipal() >> userDetails
        userDetails.getUsername() >> 'testuser'
        userService.getUserByUsername('testuser') >> user
        postService.createPost('Test post', '123') >> post

        when: "controller method is called"
        def response = postController.createPost(createPostRequest, authentication)

        then: "response has correct status code and content"
        response.statusCode == HttpStatus.OK
        response.body.content == 'Test post'
        response.body.userId == '123'
    }

    /**
     * Tests the get post by ID endpoint:
     * 1. Mocks postService to return a sample post
     * 2. Verifies controller returns the post with correct data
     */
    def "should get post by id"() {
        given: "a post ID and mocked service response"
        def postId = '456'
        def post = TestDataFactory.createPost(postId)

        and: "post service will return the post"
        postService.getPostById(postId) >> post

        when: "controller method is called"
        def response = postController.getPostById(postId)

        then: "response contains the correct post"
        response.statusCode == HttpStatus.OK
        response.body.id == postId
        response.body.content == 'Test post'
    }

    /**
     * Tests getting posts by user ID endpoint:
     * 1. Mocks postService to return a page of posts
     * 2. Verifies controller returns the posts correctly
     */
    def "should get posts by user id"() {
        given: "a user ID and a list of posts"
        def userId = '123'
        def posts = [new Post(content: 'Post 1'), new Post(content: 'Post 2')]
        def pageable = Mock(Pageable)

        and: "post service will return a page of posts"
        postService.getPostsByUserId(userId, pageable) >> new PageImpl<>(posts)

        when: "controller method is called"
        def response = postController.getPostsByUserId(userId, pageable)

        then: "response contains the posts"
        response.body.content.size() == 2
    }

    /**
     * Tests the post like endpoint:
     * 1. Sets up test data and mocks authentication
     * 2. Mocks userService and postService
     * 3. Verifies like operation updates the post
     */
    def "should like a post"() {
        given: "a post ID and authenticated user"
        def postId = '456'
        def userId = '123'
        def user = new User(id: userId, username: 'testuser')
        def post = new Post(id: postId, content: 'Test post', likes: [userId] as Set)

        and: "authentication context and mocked service calls"
        def authentication = Mock(Authentication)
        def userDetails = Mock(UserDetails)
        authentication.getPrincipal() >> userDetails
        userDetails.getUsername() >> 'testuser'
        userService.getUserByUsername('testuser') >> user
        postService.likePost(postId, userId) >> post

        when: "controller method is called"
        def response = postController.likePost(postId, authentication)

        then: "response contains updated post with like"
        response.body.likes.contains(userId)
    }

}
