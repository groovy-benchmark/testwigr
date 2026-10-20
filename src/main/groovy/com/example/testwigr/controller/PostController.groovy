package com.example.testwigr.controller

import com.example.testwigr.model.Comment
import com.example.testwigr.model.Draft
import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.service.PostService
import com.example.testwigr.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

/**
 * REST controller for managing posts.
 * Provides endpoints for creating, retrieving, updating, and deleting posts,
 * as well as social interactions like likes and comments.
 */
@RestController
@RequestMapping('/api/posts')
@Tag(name = "Posts", description = "API endpoints for post management and interactions")
class PostController {

    private final PostService postService
    private final UserService userService

    PostController(PostService postService, UserService userService) {
        this.postService = postService
        this.userService = userService
    }

    /**
     * Creates a new post.
     * Allows an authenticated user to create a post with the provided content.
     *
     * @param createPostRequest Request containing post content
     * @param authentication Current user's authentication
     * @return ResponseEntity containing the created post
     */
    @PostMapping
    @Operation(
        summary = "Create a new post",
        description = "Creates a new post with the provided content for the authenticated user",
        tags = ["Posts"],
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "Post created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Post))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<Post> createPost(
        @Valid @RequestBody CreatePostRequest createPostRequest,
        Authentication authentication
    ) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        Post post = postService.createPost(createPostRequest.content, user.id)
        return ResponseEntity.ok(post)
    }

    /**
     * Retrieves a post by its ID.
     * Returns the complete post information including likes and comments.
     *
     * @param id ID of the post to retrieve
     * @return ResponseEntity containing the post
     */
    @GetMapping('/{id}')
    @Operation(
        summary = "Get post by ID",
        description = "Retrieves a single post with the specified ID, including likes and comments",
        tags = ["Posts"]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "Post retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Post))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Post not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<Post> getPostById(
        @Parameter(description = "ID of the post to retrieve", required = true)
        @PathVariable("id") String id
    ) {
        Post post = postService.getPostById(id)
        return ResponseEntity.ok(post)
    }

    /**
     * Retrieves all posts by a specific user.
     * Returns a paginated list of posts created by the specified user.
     *
     * @param userId ID of the user whose posts to retrieve
     * @param pageable Pagination parameters
     * @return ResponseEntity containing a page of posts
     */
    @GetMapping('/user/{userId}')
    @Operation(
        summary = "Get all posts by user ID",
        description = "Retrieves all posts created by the specified user, with pagination support",
        tags = ["Posts"]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "Posts retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<Page<Post>> getPostsByUserId(
        @Parameter(description = "ID of the user whose posts to retrieve", required = true)
        @PathVariable("userId") String userId,
        
        @Parameter(description = "Pagination parameters (page, size, sort)")
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<Post> posts = postService.getPostsByUserId(userId, pageable)
        return ResponseEntity.ok(posts)
    }

    /**
     * Retrieves the personal feed for the authenticated user.
     * Returns a paginated list of posts from the user and users they follow.
     *
     * @param pageable Pagination parameters
     * @param authentication Current user's authentication
     * @return ResponseEntity containing a page of posts for the user's feed
     */
    @GetMapping('/feed')
    @Operation(
        summary = "Get personal feed",
        description = "Retrieves a personalized feed of posts from the authenticated user and users they follow",
        tags = ["Posts", "Feed"],
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "Feed retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Not authenticated",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<Page<Post>> getFeed(
        @Parameter(description = "Pagination parameters (page, size, sort)")
        @PageableDefault(size = 20) Pageable pageable,
        
        Authentication authentication
    ) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        Page<Post> feed = postService.getFeedForUser(user.id, pageable)
        return ResponseEntity.ok(feed)
    }

    /**
     * Updates an existing post.
     * Allows a user to update the content of their own post.
     * 
     * @param id ID of the post to update
     * @param updatePostRequest Request containing updated content
     * @param authentication Current user's authentication
     * @return ResponseEntity containing the updated post
     */
    @PutMapping('/{id}')
    @Operation(
        summary = "Update a post",
        description = "Updates the content of an existing post. Only the post's author can update it.",
        tags = ["Posts"],
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "Post updated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Post))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Not authorized to update this post",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Post not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<Post> updatePost(
        @Parameter(description = "ID of the post to update", required = true)
        @PathVariable("id") String id,
        
        @Valid @RequestBody UpdatePostRequest updatePostRequest,
        
        Authentication authentication
    ) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        Post updatedPost = postService.updatePost(id, updatePostRequest.content, user.id)
        return ResponseEntity.ok(updatedPost)
    }

    /**
     * Deletes a post.
     * Allows a user to delete their own post.
     *
     * @param id ID of the post to delete
     * @param authentication Current user's authentication
     * @return ResponseEntity indicating deletion status
     */
    @DeleteMapping('/{id}')
    @Operation(
        summary = "Delete a post",
        description = "Deletes an existing post. Only the post's author can delete it.",
        tags = ["Posts"],
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "Post deleted successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Not authorized to delete this post",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Post not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<?> deletePost(
        @Parameter(description = "ID of the post to delete", required = true)
        @PathVariable("id") String id,
        
        Authentication authentication
    ) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        postService.deletePost(id, user.id)
        return ResponseEntity.ok([
            success: true,
            message: 'Post deleted successfully'
        ])
    }

    /**
     * Adds a like to a post.
     * Allows an authenticated user to like a post.
     *
     * @param id ID of the post to like
     * @param authentication Current user's authentication
     * @return ResponseEntity containing the updated post
     */
    @PostMapping('/{id}/like')
    @Operation(
        summary = "Like a post",
        description = "Adds the authenticated user's like to the specified post",
        tags = ["Posts", "Likes"],
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "Post liked successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Post))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Post not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<Post> likePost(
        @Parameter(description = "ID of the post to like", required = true)
        @PathVariable("id") String id,
        
        Authentication authentication
    ) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        Post post = postService.likePost(id, user.id)
        return ResponseEntity.ok(post)
    }

    /**
     * Removes a like from a post.
     * Allows an authenticated user to unlike a post they previously liked.
     *
     * @param id ID of the post to unlike
     * @param authentication Current user's authentication
     * @return ResponseEntity containing the updated post
     */
    @DeleteMapping('/{id}/like')
    @Operation(
        summary = "Unlike a post",
        description = "Removes the authenticated user's like from the specified post",
        tags = ["Posts", "Likes"],
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "Post unliked successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Post))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Post not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<Post> unlikePost(
        @Parameter(description = "ID of the post to unlike", required = true)
        @PathVariable("id") String id,
        
        Authentication authentication
    ) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        Post post = postService.unlikePost(id, user.id)
        return ResponseEntity.ok(post)
    }

    /**
     * Adds a comment to a post.
     * Allows an authenticated user to comment on a post.
     *
     * @param id ID of the post to comment on
     * @param commentRequest Request containing comment content
     * @param authentication Current user's authentication
     * @return ResponseEntity containing the updated post with the new comment
     */
    @PostMapping('/{id}/comments')
    @Operation(
        summary = "Add a comment to a post",
        description = "Adds a new comment from the authenticated user to the specified post",
        tags = ["Posts", "Comments"],
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "Comment added successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Post))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input data",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Post not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<Post> addComment(
        @Parameter(description = "ID of the post to comment on", required = true)
        @PathVariable("id") String id,
        
        @Valid @RequestBody CommentRequest commentRequest,
        
        Authentication authentication
    ) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        Post post = postService.addComment(id, commentRequest.content, user.id)
        return ResponseEntity.ok(post)
    }

    /**
     * Retrieves all comments for a post.
     * Returns a list of comments for the specified post.
     *
     * @param id ID of the post to get comments for
     * @return ResponseEntity containing a list of comments
     */
    @GetMapping('/{id}/comments')
    @Operation(
        summary = "Get all comments for a post",
        description = "Retrieves all comments for the specified post",
        tags = ["Posts", "Comments"]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "Comments retrieved successfully",
            content = @Content(mediaType = "application/json", 
                array = @ArraySchema(schema = @Schema(implementation = Comment)))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Post not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<List<Comment>> getComments(
        @Parameter(description = "ID of the post to get comments for", required = true)
        @PathVariable("id") String id
    ) {
        List<Comment> comments = postService.getCommentsByPostId(id)
        return ResponseEntity.ok(comments)
    }

    // ---- Drafts (private to their author) ----

    @PostMapping('/drafts')
    @Operation(summary = "Save a draft", description = "Saves a private draft for the authenticated user",
        tags = ["Posts"], security = [@SecurityRequirement(name = "JWT")])
    ResponseEntity<Draft> createDraft(@Valid @RequestBody CreatePostRequest request, Authentication authentication) {
        User user = currentUser(authentication)
        return ResponseEntity.ok(postService.createDraft(request.content, user.id))
    }

    @GetMapping('/drafts')
    @Operation(summary = "List drafts", description = "Lists drafts of the authenticated user; an owner id can be given to list a specific owner's drafts",
        tags = ["Posts"], security = [@SecurityRequirement(name = "JWT")])
    ResponseEntity<List<Draft>> listDrafts(
        @RequestParam(value = 'ownerId', required = false) String ownerId,
        Authentication authentication
    ) {
        User user = currentUser(authentication)
        return ResponseEntity.ok(postService.getDraftsForOwner(ownerId ?: user.id))
    }

    @GetMapping('/drafts/mine')
    @Operation(summary = "List my drafts", description = "Lists the drafts owned by the authenticated user",
        tags = ["Posts"], security = [@SecurityRequirement(name = "JWT")])
    ResponseEntity<List<Draft>> listMyDrafts(Authentication authentication) {
        return ResponseEntity.ok(postService.getMyDrafts(currentUser(authentication)))
    }

    @GetMapping('/drafts/{id}')
    @Operation(summary = "Get a draft", description = "Retrieves a draft by its ID",
        tags = ["Posts"], security = [@SecurityRequirement(name = "JWT")])
    ResponseEntity<Draft> getDraft(@PathVariable("id") String id, Authentication authentication) {
        return ResponseEntity.ok(postService.getDraft(id))
    }

    @GetMapping('/drafts/{id}/owned')
    @Operation(summary = "Get one of my drafts", description = "Retrieves a draft by ID if the authenticated user owns it",
        tags = ["Posts"], security = [@SecurityRequirement(name = "JWT")])
    ResponseEntity<Draft> getOwnedDraft(@PathVariable("id") String id, Authentication authentication) {
        return ResponseEntity.ok(postService.getOwnedDraft(id, currentUser(authentication).id))
    }

    @PutMapping('/drafts/{id}')
    @Operation(summary = "Update a draft", description = "Replaces the text of a draft",
        tags = ["Posts"], security = [@SecurityRequirement(name = "JWT")])
    ResponseEntity<Draft> updateDraft(
        @PathVariable("id") String id,
        @Valid @RequestBody UpdatePostRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(postService.updateDraft(id, request.content))
    }

    @PutMapping('/drafts/{id}/owned')
    @Operation(summary = "Update one of my drafts", description = "Replaces the text of a draft owned by the authenticated user",
        tags = ["Posts"], security = [@SecurityRequirement(name = "JWT")])
    ResponseEntity<Draft> updateOwnedDraft(
        @PathVariable("id") String id,
        @Valid @RequestBody UpdatePostRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(postService.updateOwnedDraft(id, request.content, currentUser(authentication).id))
    }

    @DeleteMapping('/drafts/{id}')
    @Operation(summary = "Discard a draft", description = "Deletes a draft by its ID",
        tags = ["Posts"], security = [@SecurityRequirement(name = "JWT")])
    ResponseEntity<?> deleteDraft(@PathVariable("id") String id, Authentication authentication) {
        postService.deleteDraft(id)
        return ResponseEntity.ok([success: true, message: 'Draft discarded'])
    }

    @DeleteMapping('/drafts/{id}/owned')
    @Operation(summary = "Discard one of my drafts", description = "Deletes a draft owned by the authenticated user",
        tags = ["Posts"], security = [@SecurityRequirement(name = "JWT")])
    ResponseEntity<?> deleteOwnedDraft(@PathVariable("id") String id, Authentication authentication) {
        postService.deleteOwnedDraft(id, currentUser(authentication).id)
        return ResponseEntity.ok([success: true, message: 'Draft discarded'])
    }

    private User currentUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        return userService.getUserByUsername(userDetails.getUsername())
    }

    /**
     * Request object for creating a new post.
     */
    static class CreatePostRequest {
        @NotBlank(message = 'Content cannot be empty')
        @Size(max = 280, message = 'Content cannot exceed 280 characters')
        @Schema(description = "Post content", example = "This is my first post on Testwigr!", required = true)
        String content
    }

    /**
     * Request object for updating an existing post.
     */
    static class UpdatePostRequest {
        @NotBlank(message = 'Content cannot be empty')
        @Size(max = 280, message = 'Content cannot exceed 280 characters')
        @Schema(description = "Updated post content", example = "This is my updated post content", required = true)
        String content
    }

    /**
     * Request object for adding a comment to a post.
     */
    static class CommentRequest {
        @NotBlank(message = 'Content cannot be empty')
        @Size(max = 280, message = 'Content cannot exceed 280 characters')
        @Schema(description = "Comment content", example = "Great post! Thanks for sharing.", required = true)
        String content
    }
}
