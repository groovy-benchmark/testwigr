package com.example.testwigr.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * Comment model representing user responses to posts on the Testwigr platform.
 * 
 * This model stores the comment content along with metadata about the commenter.
 * Comments are embedded within Post documents rather than stored in a separate
 * collection, optimizing for the common read patterns of the application.
 * 
 * Each comment:
 * - Contains text content (max 280 characters, same as posts)
 * - Is associated with a specific user as the author
 * - Has a timestamp for creation
 * - Is attached to a specific post
 * 
 * Unlike posts, comments cannot receive likes or further comments in this
 * implementation of the platform. They represent a simple, flat response structure.
 */
@Schema(description = "Represents a user comment on a post")
class Comment {
    /**
     * Unique identifier for the comment.
     * Generated using UUID rather than MongoDB since comments are embedded documents.
     */
    @Schema(description = "Unique identifier for the comment", example = "60d21b4667d1d12d98a8e789")
    String id

    /**
     * Text content of the comment, limited to 280 characters.
     * This is the main content created by the user.
     */
    @Schema(description = "Text content of the comment", example = "Great post! Thanks for sharing.", required = true)
    String content

    /**
     * ID of the user who created the comment.
     * This is a reference to the User document's ID.
     */
    @Schema(description = "ID of the user who created the comment", example = "60d21b4667d1d12d98a8e123", required = true)
    String userId

    /**
     * Username of the user who created the comment.
     * Stored directly for denormalization to avoid joins.
     */
    @Schema(description = "Username of the user who created the comment", example = "johndoe", required = true)
    String username

    /**
     * The timestamp when the comment was created.
     * Comments are immutable in this implementation, so there's no updatedAt field.
     */
    @Schema(description = "Date and time when the comment was created", example = "2023-01-15T16:30:15.123")
    LocalDateTime createdAt

    /**
     * Default constructor required for object mapping.
     */
    Comment() {}

    /**
     * Creates a new comment with the specified content, user ID, and username.
     * Automatically generates a unique ID and sets the creation timestamp.
     *
     * @param content The text content of the comment
     * @param userId The ID of the user creating the comment
     * @param username The username of the user creating the comment
     */
    Comment(String content, String userId, String username) {
        this.id = UUID.randomUUID().toString()
        this.content = content
        this.userId = userId
        this.username = username
        this.createdAt = LocalDateTime.now()
    }
    
    /**
     * Returns a string representation of the Comment object.
     * Useful for debugging and logging.
     * 
     * @return A string representation of the Comment
     */
    @Override
    String toString() {
        return "Comment{" +
                "id='" + id + '\'' +
                ", content='" + (content?.length() > 20 ? content.substring(0, 20) + "..." : content) + '\'' +
                ", userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", createdAt=" + createdAt +
                '}'
    }
}
