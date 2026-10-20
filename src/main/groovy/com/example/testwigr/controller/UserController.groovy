package com.example.testwigr.controller

import com.example.testwigr.model.User
import com.example.testwigr.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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

/**
 * REST controller for handling user operations.
 * Provides endpoints for managing user profiles and accounts.
 */
@RestController
@RequestMapping('/api/users')
@Tag(name = "Users", description = "User management operations")
class UserController {

    private final UserService userService

    UserController(UserService userService) {
        this.userService = userService
    }

    /**
     * Get user profile by username.
     * 
     * @param username The username of the user to retrieve
     * @return The user profile information
     */
    @GetMapping('/{username}')
    @Operation(
        summary = "Get user by username",
        description = "Retrieves a user's public profile information by their username"
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200", 
            description = "User found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = User))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "User not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<User> getUserByUsername(
        @Parameter(description = "Username of the user to retrieve", required = true)
        @PathVariable('username') String username
    ) {
        User user = userService.getUserByUsername(username)
        return ResponseEntity.ok(user)
    }

    /**
     * Update a user profile.
     * 
     * @param id The ID of the user to update
     * @param userDetails Updated user information
     * @param authentication Current user authentication
     * @return The updated user profile
     */
    @PutMapping('/{id}')
    @Operation(
        summary = "Update user profile",
        description = "Updates a user's profile information. User can only update their own profile.",
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200", 
            description = "Profile updated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = User))
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Not authorized to update this profile",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "User not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "Email already in use",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<User> updateUser(
        @Parameter(description = "ID of the user to update", required = true)
        @PathVariable('id') String id,
        
        @Parameter(description = "Updated user details", required = true)
        @RequestBody User userDetails,
        
        Authentication authentication
    ) {
        // Changed variable name from userDetails to authUserDetails to avoid conflict
        UserDetails authUserDetails = (UserDetails) authentication.getPrincipal()
        User authUser = userService.getUserByUsername(authUserDetails.getUsername())

        if (!authUser.id.equals(id)) {
            return ResponseEntity.status(403).build()
        }

        User updatedUser = userService.updateUser(id, userDetails)
        return ResponseEntity.ok(updatedUser)
    }

    /**
     * Delete a user account.
     * 
     * @param id The ID of the user to delete
     * @param authentication Current user authentication
     * @return Success status
     */
    @DeleteMapping('/{id}')
    @Operation(
        summary = "Delete user account",
        description = "Deletes a user account. User can only delete their own account.",
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200", 
            description = "Account deleted successfully",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Not authorized to delete this account",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "User not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<?> deleteUser(
        @Parameter(description = "ID of the user to delete", required = true)
        @PathVariable('id') String id,
        
        Authentication authentication
    ) {
        // Changed variable name from userDetails to authUserDetails to avoid conflict
        UserDetails authUserDetails = (UserDetails) authentication.getPrincipal()
        User authUser = userService.getUserByUsername(authUserDetails.getUsername())

        if (!authUser.id.equals(id)) {
            return ResponseEntity.status(403).build()
        }

        userService.deleteUser(id)
        return ResponseEntity.ok().build()
    }

    /**
     * Follow another user.
     * 
     * @param id The ID of the follower
     * @param followingId The ID of the user to follow
     * @param authentication Current user authentication
     * @return The updated user profile with follow relationship
     */
    @PostMapping('/{id}/follow/{followingId}')
    @Operation(
        summary = "Follow a user",
        description = "Establishes a follow relationship between the current user and another user",
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200", 
            description = "Follow successful",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = User))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Cannot follow yourself",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Not authorized",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "User not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<User> followUser(
        @Parameter(description = "ID of the follower", required = true)
        @PathVariable('id') String id,
        
        @Parameter(description = "ID of the user to follow", required = true)
        @PathVariable('followingId') String followingId,
        
        Authentication authentication
    ) {
        // Changed variable name from userDetails to authUserDetails to avoid conflict
        UserDetails authUserDetails = (UserDetails) authentication.getPrincipal()
        User authUser = userService.getUserByUsername(authUserDetails.getUsername())

        if (!authUser.id.equals(id)) {
            return ResponseEntity.status(403).build()
        }

        User updatedUser = userService.followUser(id, followingId)
        return ResponseEntity.ok(updatedUser)
    }

    /**
     * Unfollow a user.
     * 
     * @param id The ID of the follower
     * @param followingId The ID of the user to unfollow
     * @param authentication Current user authentication
     * @return The updated user profile with follow relationship removed
     */
    @DeleteMapping('/{id}/unfollow/{followingId}')
    @Operation(
        summary = "Unfollow a user",
        description = "Removes a follow relationship between the current user and another user",
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200", 
            description = "Unfollow successful",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = User))
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Not authorized",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "User not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<User> unfollowUser(
        @Parameter(description = "ID of the follower", required = true)
        @PathVariable('id') String id,
        
        @Parameter(description = "ID of the user to unfollow", required = true)
        @PathVariable('followingId') String followingId,
        
        Authentication authentication
    ) {
        // Changed variable name from userDetails to authUserDetails to avoid conflict
        UserDetails authUserDetails = (UserDetails) authentication.getPrincipal()
        User authUser = userService.getUserByUsername(authUserDetails.getUsername())

        if (!authUser.id.equals(id)) {
            return ResponseEntity.status(403).build()
        }

        User updatedUser = userService.unfollowUser(id, followingId)
        return ResponseEntity.ok(updatedUser)
    }

    /**
     * Get current authenticated user's profile.
     * 
     * @param authentication Current user authentication
     * @return The current user's profile
     */
    @GetMapping('/me')
    @Operation(
        summary = "Get current user profile",
        description = "Retrieves the profile of the currently authenticated user",
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200", 
            description = "Profile retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = User))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Not authenticated",
            content = @Content(mediaType = "application/json")
        )
    ])
    ResponseEntity<User> getCurrentUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())
        return ResponseEntity.ok(user)
    }

    /**
     * Update current authenticated user's profile.
     * 
     * @param updateUserRequest Updated profile information
     * @param authentication Current user authentication
     * @return The updated user profile
     */
    @PutMapping('/me')
    @Operation(
        summary = "Update current user profile",
        description = "Updates the profile of the currently authenticated user",
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200", 
            description = "Profile updated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = User))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid input",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Not authenticated",
            content = @Content(mediaType = "application/json")
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "Email already in use",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<User> updateCurrentUser(
        @Valid @RequestBody UpdateUserRequest updateUserRequest,
        Authentication authentication
    ) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        User userToUpdate = new User()
        userToUpdate.displayName = updateUserRequest.displayName
        userToUpdate.bio = updateUserRequest.bio
        userToUpdate.email = updateUserRequest.email
        userToUpdate.password = updateUserRequest.password

        User updatedUser = userService.updateUser(user.id, userToUpdate)
        return ResponseEntity.ok(updatedUser)
    }

    /**
     * Delete current authenticated user's account.
     * 
     * @param authentication Current user authentication
     * @return Success status
     */
    @DeleteMapping('/me')
    @Operation(
        summary = "Delete current user account",
        description = "Deletes the account of the currently authenticated user",
        security = [@SecurityRequirement(name = "JWT")]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200", 
            description = "Account deleted successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Not authenticated",
            content = @Content(mediaType = "application/json")
        )
    ])
    ResponseEntity<?> deleteCurrentUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        userService.deleteUser(user.id)
        return ResponseEntity.ok([
            success: true,
            message: 'User deleted successfully'
        ])
    }

    /**
     * Request object for updating user profile.
     */
    static class UpdateUserRequest {
        @Schema(description = "Display name to be shown in the UI", example = "John Doe")
        String displayName

        @Schema(description = "Bio information for user profile", example = "Software developer and tech enthusiast")
        String bio

        @Schema(description = "Email address (must be unique)", example = "john.doe@example.com")
        String email

        @Schema(description = "Password (will be encrypted)", example = "securepassword123")
        String password
    }
}
