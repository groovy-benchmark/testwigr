package com.example.testwigr.controller

import com.example.testwigr.model.User
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
 * REST controller for managing follow relationships between users.
 * Provides endpoints for following/unfollowing users and retrieving follower/following lists.
 */
@RestController
@RequestMapping('/api/follow')
@Tag(name = "Follow", description = "API endpoints for managing follow relationships between users")
class FollowController {

    private final UserService userService

    FollowController(UserService userService) {
        this.userService = userService
    }

    /**
     * Follows a user.
     * Establishes a follower-following relationship between the authenticated user
     * and the target user specified by ID.
     *
     * @param followingId ID of the user to be followed
     * @param authentication Current user's authentication
     * @return ResponseEntity containing follow operation status
     */
    @PostMapping('/{followingId}')
    @Operation(
        summary = "Follow a user",
        description = "Establishes a follower-following relationship between the authenticated user and the target user",
        tags = ["Follow"],
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "Follow relationship established successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Cannot follow yourself",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<Map<String, Object>> followUser(
        @Parameter(description = "ID of the user to follow", required = true)
        @PathVariable('followingId') String followingId,
        
        Authentication authentication
    ) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        if (user.id == followingId) {
            return ResponseEntity.badRequest().body([error: 'You cannot follow yourself'])
        }

        User updatedUser = userService.followUser(user.id, followingId)

        return ResponseEntity.ok([
            success: true,
            following: updatedUser.following.size(),
            isFollowing: true
        ])
    }

    /**
     * Unfollows a user.
     * Removes a follower-following relationship between the authenticated user
     * and the target user specified by ID.
     *
     * @param followingId ID of the user to be unfollowed
     * @param authentication Current user's authentication
     * @return ResponseEntity containing unfollow operation status
     */
    @DeleteMapping('/{followingId}')
    @Operation(
        summary = "Unfollow a user",
        description = "Removes a follower-following relationship between the authenticated user and the target user",
        tags = ["Follow"],
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "Follow relationship removed successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<Map<String, Object>> unfollowUser(
        @Parameter(description = "ID of the user to unfollow", required = true)
        @PathVariable('followingId') String followingId,
        
        Authentication authentication
    ) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        User updatedUser = userService.unfollowUser(user.id, followingId)

        return ResponseEntity.ok([
            success: true,
            following: updatedUser.following.size(),
            isFollowing: false
        ])
    }

    /**
     * Gets the followers of the authenticated user.
     * Returns a list of users who follow the authenticated user.
     *
     * @param authentication Current user's authentication
     * @return ResponseEntity containing list of followers
     */
    @GetMapping('/followers')
    @Operation(
        summary = "Get user's followers",
        description = "Retrieves the list of users who follow the authenticated user",
        tags = ["Follow"],
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "Followers retrieved successfully",
            content = @Content(mediaType = "application/json", 
                array = @ArraySchema(schema = @Schema(implementation = User)))
        )
    ])
    ResponseEntity<List<User>> getFollowers(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        List<User> followers = userService.getFollowers(user.id)

        return ResponseEntity.ok(followers)
    }

    /**
     * Gets the users the authenticated user is following.
     * Returns a list of users that the authenticated user follows.
     *
     * @param authentication Current user's authentication
     * @return ResponseEntity containing list of followed users
     */
    @GetMapping('/following')
    @Operation(
        summary = "Get users followed by the authenticated user",
        description = "Retrieves the list of users that the authenticated user follows",
        tags = ["Follow"],
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "Following list retrieved successfully",
            content = @Content(mediaType = "application/json", 
                array = @ArraySchema(schema = @Schema(implementation = User)))
        )
    ])
    ResponseEntity<List<User>> getFollowing(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        List<User> following = userService.getFollowing(user.id)

        return ResponseEntity.ok(following)
    }

    /**
     * Gets the follow status between the authenticated user and another user.
     * Returns information about the follow relationship between two users.
     *
     * @param userId ID of the target user to check status with
     * @param authentication Current user's authentication
     * @return ResponseEntity containing follow status information
     */
    @GetMapping('/{userId}/status')
    @Operation(
        summary = "Get follow status with a user",
        description = "Retrieves information about the follow relationship between the authenticated user and the specified user",
        tags = ["Follow"],
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "Follow status retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<Map<String, Object>> getFollowStatus(
        @Parameter(description = "ID of the user to check follow status with", required = true)
        @PathVariable('userId') String userId,
        
        Authentication authentication
    ) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        User targetUser = userService.getUserById(userId)

        return ResponseEntity.ok([
            isFollowing: user.isFollowing(userId),
            isFollower: targetUser.isFollowing(user.id),
            followersCount: targetUser.followers.size(),
            followingCount: targetUser.following.size()
        ])
    }
}
