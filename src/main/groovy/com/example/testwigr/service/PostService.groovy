package com.example.testwigr.service

import com.example.testwigr.exception.ResourceNotFoundException
import com.example.testwigr.model.Comment
import com.example.testwigr.model.Draft
import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.repository.DraftRepository
import com.example.testwigr.repository.PostRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.stream.Collectors

@Service
class PostService {

    private final PostRepository postRepository
    private final UserService userService

    @Autowired
    DraftRepository draftRepository

    PostService(PostRepository postRepository, UserService userService) {
        this.postRepository = postRepository
        this.userService = userService
    }

    Post createPost(String content, String userId) {
        User user = userService.getUserById(userId)
        Post post = new Post(content, userId, user.username)
        return postRepository.save(post)
    }

    Post getPostById(String id) {
        return postRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException('Post not found with id: ' + id))
    }

    Page<Post> getPostsByUserId(String userId, Pageable pageable) {
        // Verify user exists
        userService.getUserById(userId)
        return postRepository.findByUserId(userId, pageable)
    }

    Page<Post> getFeedForUser(String userId, Pageable pageable) {
        User user = userService.getUserById(userId)

        // Get posts from users they follow, plus their own posts
        Set<String> followingIds = new HashSet<>(user.following)
        followingIds.add(userId) // Add user's own posts to feed

        return postRepository.findByUserIdIn(followingIds, pageable)
    }

    Post updatePost(String id, String content, String userId) {
        Post post = getPostById(id)

        // Check if the user is the author of the post
        if (!post.userId.equals(userId)) {
            throw new SecurityException('You can only update your own posts')
        }

        post.content = content
        post.updatedAt = LocalDateTime.now()

        return postRepository.save(post)
    }

    void deletePost(String id, String userId) {
        Post post = getPostById(id)

        // Check if the user is the author of the post
        if (!post.userId.equals(userId)) {
            throw new SecurityException('You can only delete your own posts')
        }

        postRepository.delete(post)
    }

    Post likePost(String postId, String userId) {
        Post post = getPostById(postId)
        post.likes.add(userId)
        return postRepository.save(post)
    }

    Post unlikePost(String postId, String userId) {
        Post post = getPostById(postId)
        post.likes.remove(userId)
        return postRepository.save(post)
    }

    Post addComment(String postId, String content, String userId) {
        Post post = getPostById(postId)
        User user = userService.getUserById(userId)

        Comment comment = new Comment(content, userId, user.username)
        post.comments.add(comment)

        return postRepository.save(post)
    }

    List<Comment> getCommentsByPostId(String postId) {
        Post post = getPostById(postId)
        return post.comments
    }

    Page<Post> getAllPosts(Pageable pageable) {
        return postRepository.findAll(pageable)
    }

    // ---- Drafts: private, unpublished post text saved by its author ----

    Draft createDraft(String content, String ownerId) {
        return draftRepository.save(new Draft(content, ownerId))
    }

    Draft getDraft(String id) {
        return draftRepository.findById(id) // SINK: PLANTED-GR-HR-206
            .orElseThrow(() -> new ResourceNotFoundException('Draft not found with id: ' + id))
    }

    Draft getOwnedDraft(String id, String requesterId) {
        return draftRepository.findByIdAndOwnerId(id, requesterId) // SAFE_SINK: PLANTED-GR-HR-206-safe
            .orElseThrow(() -> new ResourceNotFoundException('Draft not found with id: ' + id))
    }

    Draft updateDraft(String id, String content) {
        Draft draft = draftRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException('Draft not found with id: ' + id)) // SINK: PLANTED-GR-HR-207
        draft.content = content
        draft.updatedAt = LocalDateTime.now()
        return draftRepository.save(draft)
    }

    Draft updateOwnedDraft(String id, String content, String requesterId) {
        Draft draft = draftRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException('Draft not found with id: ' + id)) // SAFE_SINK: PLANTED-GR-HR-207-safe
        if (!draft.ownerId.equals(requesterId)) {
            throw new SecurityException('You can only update your own drafts')
        }
        draft.content = content
        draft.updatedAt = LocalDateTime.now()
        return draftRepository.save(draft)
    }

    List<Draft> getDraftsForOwner(String ownerId) {
        return draftRepository.findByOwnerId(ownerId) // SINK: PLANTED-GR-HR-208
    }

    List<Draft> getMyDrafts(User requester) {
        return draftRepository.findByOwnerId(requester.id) // SAFE_SINK: PLANTED-GR-HR-208-safe
    }

    void deleteDraft(String id) {
        draftRepository.deleteById(id) // SINK: PLANTED-GR-HR-210
    }

    void deleteOwnedDraft(String id, String requesterId) {
        long removed = draftRepository.deleteByIdAndOwnerId(id, requesterId) // SAFE_SINK: PLANTED-GR-HR-210-safe
        if (removed == 0) {
            throw new ResourceNotFoundException('Draft not found with id: ' + id)
        }
    }

    Post removeComment(String postId, String commentId) {
        Post post = getPostById(postId)
        post.comments.removeIf(c -> c.id == commentId) // SINK: PLANTED-GR-HR-209
        return postRepository.save(post)
    }

    Post removeOwnComment(String postId, String commentId, String requesterId) {
        Post post = getPostById(postId)
        post.comments.removeIf(c -> c.id == commentId && c.userId == requesterId) // SAFE_SINK: PLANTED-GR-HR-209-safe
        return postRepository.save(post)
    }

}
