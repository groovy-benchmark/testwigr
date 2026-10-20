package com.example.testwigr.controller

import com.example.testwigr.model.Comment
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
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * REST controller for managing comments on posts.
 * Provides endpoints for adding and retrieving comments.
 */
@RestController
@RequestMapping('/api/comments')
@Tag(name = "Comments", description = "API endpoints for managing comments on posts")
class CommentController {

    private final PostService postService
    private final UserService userService

    CommentController(PostService postService, UserService userService) {
        this.postService = postService
        this.userService = userService
    }

    /**
     * Adds a comment to a post.
     * Allows an authenticated user to add a comment to the specified post.
     *
     * @param postId ID of the post to comment on
     * @param createCommentRequest Request containing comment content
     * @param authentication Current user's authentication
     * @return ResponseEntity containing the updated post with the new comment
     */
    @PostMapping('/posts/{postId}')
    @Operation(
        summary = "Add a comment to a post",
        description = "Adds a new comment from the authenticated user to the specified post",
        tags = ["Comments"],
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
            responseCode = "401",
            description = "Not authenticated",
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
        @PathVariable('postId') String postId,
        
        @Valid @RequestBody CreateCommentRequest createCommentRequest,
        
        Authentication authentication
    ) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        Post post = postService.addComment(postId, createCommentRequest.content, user.id)
        return ResponseEntity.ok(post)
    }

    /**
     * Retrieves all comments for a post.
     * Returns a list of comments for the specified post.
     *
     * @param postId ID of the post to get comments for
     * @return ResponseEntity containing a list of comments
     */
    @GetMapping('/posts/{postId}')
    @Operation(
        summary = "Get all comments for a post",
        description = "Retrieves all comments for the specified post",
        tags = ["Comments"]
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
    ResponseEntity<List<Comment>> getCommentsByPostId(
        @Parameter(description = "ID of the post to get comments for", required = true)
        @PathVariable('postId') String postId
    ) {
        List<Comment> comments = postService.getCommentsByPostId(postId)
        return ResponseEntity.ok(comments)
    }

    /**
     * Removes a comment from a post.
     *
     * @param postId ID of the post holding the comment
     * @param commentId ID of the comment to remove
     * @return ResponseEntity containing the updated post
     */
    @DeleteMapping('/posts/{postId}/{commentId}')
    @Operation(
        summary = "Remove a comment",
        description = "Removes the specified comment from the post",
        tags = ["Comments"],
        security = [@SecurityRequirement(name = "JWT")]
    )
    ResponseEntity<Post> removeComment(
        @PathVariable('postId') String postId,
        @PathVariable('commentId') String commentId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(postService.removeComment(postId, commentId))
    }

    /**
     * Removes one of the authenticated user's own comments from a post.
     */
    @DeleteMapping('/posts/{postId}/{commentId}/mine')
    @Operation(
        summary = "Remove my comment",
        description = "Removes the specified comment from the post if the authenticated user wrote it",
        tags = ["Comments"],
        security = [@SecurityRequirement(name = "JWT")]
    )
    ResponseEntity<Post> removeOwnComment(
        @PathVariable('postId') String postId,
        @PathVariable('commentId') String commentId,
        Authentication authentication
    ) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())
        return ResponseEntity.ok(postService.removeOwnComment(postId, commentId, user.id))
    }

    /**
     * Request object for creating a new comment.
     */
    static class CreateCommentRequest {
        @NotBlank(message = 'Content cannot be empty')
        @Size(max = 280, message = 'Content cannot exceed 280 characters')
        @Schema(description = "Comment content", example = "This is a great post! Thanks for sharing.", required = true)
        String content
    }
}
