package com.example.testwigr.service

import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.repository.PostRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class FeedService {

    private final PostRepository postRepository
    private final UserService userService

    FeedService(PostRepository postRepository, UserService userService) {
        this.postRepository = postRepository
        this.userService = userService
    }

    Page<Post> getPersonalFeed(String userId, Pageable pageable) {
        User user = userService.getUserById(userId)

        // Get posts from users they follow and their own posts
        Set<String> userIds = new HashSet<>(user.following)
        userIds.add(userId) // Include user's own posts

        return postRepository.findByUserIdIn(userIds, pageable)
    }

    Page<Post> getUserFeed(String targetUserId, Pageable pageable) {
        // Get only the target user's posts
        User user = userService.getUserById(targetUserId)
        return postRepository.findByUserId(targetUserId, pageable)
    }
}
