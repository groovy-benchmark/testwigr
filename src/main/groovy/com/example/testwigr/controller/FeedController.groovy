package com.example.testwigr.controller

import com.example.testwigr.model.Post
import com.example.testwigr.model.User
import com.example.testwigr.service.FeedService
import com.example.testwigr.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

/**
 * REST controller for retrieving personalized content feeds.
 * Provides endpoints for accessing a user's personal feed and user-specific feeds.
 */
@RestController
@RequestMapping("/api/feed")
@Tag(name = "Feed", description = "API endpoints for retrieving personalized and user-specific content feeds")
class FeedController {

    private final FeedService feedService
    private final UserService userService

    FeedController(FeedService feedService, UserService userService) {
        this.feedService = feedService
        this.userService = userService
    }

    /**
     * Retrieves the personal feed for the authenticated user.
     * Returns a paginated list of posts from the user and users they follow.
     *
     * @param pageable Pagination parameters
     * @param authentication Current user's authentication
     * @return ResponseEntity containing a page of posts for the user's feed
     */
    @GetMapping
    @Operation(
        summary = "Get personal feed",
        description = "Retrieves a personalized feed of posts from the authenticated user and users they follow",
        tags = ["Feed"],
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
    ResponseEntity<Page<Post>> getPersonalFeed(
        @Parameter(description = "Pagination parameters (page, size, sort)")
        @PageableDefault(size = 20) Pageable pageable,
        
        Authentication authentication
    ) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal()
        User user = userService.getUserByUsername(userDetails.getUsername())

        Page<Post> feed = feedService.getPersonalFeed(user.id, pageable)
        return ResponseEntity.ok(feed)
    }

    /**
     * Retrieves the feed for a specific user.
     * Returns a paginated list of posts created by the specified user.
     *
     * @param username Username of the user whose feed to retrieve
     * @param pageable Pagination parameters
     * @return ResponseEntity containing a page of posts for the user's feed
     */
    @GetMapping("/users/{username}")
    @Operation(
        summary = "Get user feed",
        description = "Retrieves a feed of posts created by the specified user",
        tags = ["Feed"]
    )
    @ApiResponses([
        @ApiResponse(
            responseCode = "200",
            description = "User feed retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map))
        )
    ])
    ResponseEntity<Page<Post>> getUserFeed(
        @Parameter(description = "Username of the user whose feed to retrieve", required = true)
        @PathVariable String username,
        
        @Parameter(description = "Pagination parameters (page, size, sort)")
        @PageableDefault(size = 20) Pageable pageable
    ) {
        User user = userService.getUserByUsername(username)

        Page<Post> feed = feedService.getUserFeed(user.id, pageable)
        return ResponseEntity.ok(feed)
    }
}
