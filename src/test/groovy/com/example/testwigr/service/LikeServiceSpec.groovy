package com.example.testwigr.service

import com.example.testwigr.model.Post
import com.example.testwigr.repository.PostRepository
import spock.lang.Specification

/**
 * Unit test for the like functionality in PostService.
 * These tests verify that users can like and unlike posts,
 * and that post objects properly track like status.
 *
 * This test class focuses specifically on isolating the like/unlike
 * functionality to ensure it meets requirements.
 */
class LikeServiceSpec extends Specification {

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
     * Tests liking a post:
     * 1. Sets up a post without any likes
     * 2. Calls the like method with a user ID
     * 3. Verifies the post now shows as liked by that user
     * 4. Confirms like count increases and isLikedBy returns true
     */
    def "should like a post"() {
        given: "a post with no likes"
        def postId = "456"
        def userId = "123"
        def post = new Post(id: postId, content: "Test post", likes: [] as Set)

        and: "mocked repository behavior"
        postRepository.findById(postId) >> Optional.of(post)
        postRepository.save(_) >> { Post p -> p }

        when: "liking the post"
        def result = postService.likePost(postId, userId)

        then: "post shows as liked by the user"
        result.likes.contains(userId)
        result.isLikedBy(userId)
        result.getLikeCount() == 1

//        and: "post was saved with the like"
//        1 * postRepository.save({ Post p ->
//            p.likes.contains(userId) &&
//                    p.getLikeCount() == 1
//        })
    }

    /**
     * Tests unliking a post:
     * 1. Sets up a post with an existing like from the user
     * 2. Calls the unlike method with the same user ID
     * 3. Verifies the post no longer shows as liked by that user
     * 4. Confirms like count decreases and isLikedBy returns false
     */
    def "should unlike a post"() {
        given: "a post liked by a user"
        def postId = "456"
        def userId = "123"
        def post = new Post(id: postId, content: "Test post", likes: [userId] as Set)

        and: "mocked repository behavior"
        postRepository.findById(postId) >> Optional.of(post)
        postRepository.save(_) >> { Post p -> p }

        when: "unliking the post"
        def result = postService.unlikePost(postId, userId)

        then: "post no longer shows as liked by the user"
        !result.likes.contains(userId)
        !result.isLikedBy(userId)
        result.getLikeCount() == 0

//        and: "post was saved with the like removed"
//        1 * postRepository.save({ Post p ->
//            !p.likes.contains(userId) &&
//                    p.getLikeCount() == 0
//        })
    }

    /**
     * Tests liking an already liked post:
     * 1. Sets up a post already liked by the user
     * 2. Calls the like method again with the same user ID
     * 3. Verifies the post still shows only one like from that user
     * 4. Confirms like count remains at 1 (no duplicate likes)
     */
    def "should handle liking an already liked post"() {
        given: "a post already liked by a user"
        def postId = "456"
        def userId = "123"
        def post = new Post(id: postId, content: "Test post", likes: [userId] as Set)

        and: "mocked repository behavior"
        postRepository.findById(postId) >> Optional.of(post)
        postRepository.save(_) >> { Post p -> p }

        when: "liking the post again"
        def result = postService.likePost(postId, userId)

        then: "post still shows as liked by the user (idempotent operation)"
        result.likes.contains(userId)
        result.isLikedBy(userId)
        result.getLikeCount() == 1 // Count remains at 1, no duplicate likes
    }

    /**
     * Tests unliking a post that's not liked:
     * 1. Sets up a post not liked by the user
     * 2. Calls the unlike method with that user ID
     * 3. Verifies the post still shows as not liked (no error)
     * 4. Confirms like count remains at 0 (idempotent operation)
     */
    def "should handle unliking a post that's not liked"() {
        given: "a post not liked by a user"
        def postId = "456"
        def userId = "123"
        def post = new Post(id: postId, content: "Test post", likes: [] as Set)

        and: "mocked repository behavior"
        postRepository.findById(postId) >> Optional.of(post)
        postRepository.save(_) >> { Post p -> p }

        when: "unliking the post"
        def result = postService.unlikePost(postId, userId)

        then: "post still shows as not liked by the user (idempotent operation)"
        !result.likes.contains(userId)
        !result.isLikedBy(userId)
        result.getLikeCount() == 0
    }

}
