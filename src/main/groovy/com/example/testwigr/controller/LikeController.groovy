package com.example.testwigr.controller

import com.example.testwigr.exception.ResourceNotFoundException
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

/**
 * REST controller for managing likes on posts.
 * Provides endpoints for adding, removing, and querying likes.
 */
@RestController
@RequestMapping('/api/likes')
@Tag(name = "Likes", description = "API endpoints for managing likes on posts")
class LikeController {

    private final PostService postService
    private final UserService userService

    LikeController(PostService postService, UserService userService) {
        this.postService = postService
        this.userService = userService
    }

    /**
     * Adds a like to a post.
     * Allows an authenticated user to like a post.
     *
     * @param postId ID of the post to like
     * @param authentication Current user's authentication
     * @return ResponseEntity containing like status
     */
    @PostMapping('/posts/{postId}')
    @Operation(
        summary = "Like a post",
        description = "Adds the authenticated user's like to the specified post",
        tags = ["Likes"],
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "Post liked successfully",
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
    ResponseEntity<Map<String, Object>> likePost(
        @Parameter(description = "ID of the post to like", required = true)
        @PathVariable('postId') String postId,
        
        Authentication authentication
    ) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal()
            User user = userService.getUserByUsername(userDetails.getUsername())

            Post post = postService.likePost(postId, user.id)

            return ResponseEntity.ok([
                success: true,
                likeCount: post.getLikeCount(),
                isLiked: true
            ])
        } catch (Exception e) {
            // Log the error
            System.err.println("Error liking post: " + e.getMessage())
            e.printStackTrace()
            
            // Return a meaningful error response
            return ResponseEntity.ok([
                success: false,
                message: "Unable to like post: " + e.getMessage()
            ])
        }
    }

    /**
     * Removes a like from a post.
     * Allows an authenticated user to unlike a post they previously liked.
     *
     * @param postId ID of the post to unlike
     * @param authentication Current user's authentication
     * @return ResponseEntity containing like status
     */
    @DeleteMapping('/posts/{postId}')
    @Operation(
        summary = "Unlike a post",
        description = "Removes the authenticated user's like from the specified post",
        tags = ["Likes"],
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "Post unliked successfully",
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
    ResponseEntity<Map<String, Object>> unlikePost(
        @Parameter(description = "ID of the post to unlike", required = true)
        @PathVariable('postId') String postId,
        
        Authentication authentication
    ) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal()
            User user = userService.getUserByUsername(userDetails.getUsername())

            Post post = postService.unlikePost(postId, user.id)

            return ResponseEntity.ok([
                success: true,
                likeCount: post.getLikeCount(),
                isLiked: false
            ])
        } catch (Exception e) {
            // Log the error
            System.err.println("Error unliking post: " + e.getMessage())
            e.printStackTrace()
            
            // Return a meaningful error response
            return ResponseEntity.ok([
                success: false,
                message: "Unable to unlike post: " + e.getMessage()
            ])
        }
    }

    /**
     * Gets the like status of a post for the authenticated user.
     * Returns whether the authenticated user has liked the post and the total like count.
     *
     * @param postId ID of the post to check like status
     * @param authentication Current user's authentication
     * @return ResponseEntity containing like status information
     */
    @GetMapping('/posts/{postId}')
    @Operation(
        summary = "Get like status for a post",
        description = "Retrieves whether the authenticated user has liked the post and the total like count",
        tags = ["Likes"],
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "Like status retrieved successfully",
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
    ResponseEntity<Map<String, Object>> getLikeStatus(
        @Parameter(description = "ID of the post to check like status", required = true)
        @PathVariable('postId') String postId,
        
        Authentication authentication
    ) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal()
            User user = userService.getUserByUsername(userDetails.getUsername())

            Post post = postService.getPostById(postId)

            return ResponseEntity.ok([
                likeCount: post.getLikeCount(),
                isLiked: post.isLikedBy(user.id)
            ])
        } catch (Exception e) {
            // Log the error
            System.err.println("Error getting like status: " + e.getMessage())
            e.printStackTrace()
            
            // Return a meaningful error response
            return ResponseEntity.ok([
                success: false,
                message: "Unable to get like status: " + e.getMessage()
            ])
        }
    }

    /**
     * Gets the list of users who liked a post.
     * Returns a list of users who have liked the specified post.
     *
     * @param postId ID of the post to get liking users for
     * @return ResponseEntity containing list of users who liked the post
     */
    @GetMapping('/posts/{postId}/users')
    @Operation(
        summary = "Get users who liked a post",
        description = "Retrieves the list of users who have liked the specified post",
        tags = ["Likes"]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully",
            content = @Content(mediaType = "application/json", 
                array = @ArraySchema(schema = @Schema(implementation = User)))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Post not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<?> getLikedUsers(
        @Parameter(description = "ID of the post to get liking users for", required = true)
        @PathVariable('postId') String postId
    ) {
        try {
            Post post = postService.getPostById(postId)

            List<User> likedUsers = []
            for (String userId : post.likes) {
                try {
                    User likedUser = userService.getUserById(userId)
                    likedUsers.add(likedUser)
                } catch (ResourceNotFoundException e) {
                    // Skip users that might have been deleted
                }
            }

            return ResponseEntity.ok(likedUsers)
        } catch (Exception e) {
            // Log the error
            System.err.println("Error getting liked users: " + e.getMessage())
            e.printStackTrace()
            
            // Return a meaningful error response
            return ResponseEntity.ok([
                success: false,
                message: "Unable to get liked users: " + e.getMessage()
            ])
        }
    }
}
