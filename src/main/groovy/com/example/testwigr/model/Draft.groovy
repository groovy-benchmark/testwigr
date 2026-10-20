package com.example.testwigr.model

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * Unpublished post text saved privately by its author. Only the owner is meant to see or
 * change a draft; publishing it creates a regular {@link Post}. Stored in the "drafts" collection.
 */
@Document(collection = "drafts")
@Schema(description = "A private, unpublished post draft owned by a single user")
class Draft {
    @Id
    @Schema(description = "Unique identifier for the draft", example = "60d21b4667d1d12d98a8e999")
    String id

    @Schema(description = "Draft text", example = "Working on something new...")
    String content

    @Schema(description = "ID of the user who owns the draft", example = "60d21b4667d1d12d98a8e123")
    String ownerId

    @Schema(description = "Date and time when the draft was last saved")
    LocalDateTime updatedAt

    Draft() {}

    Draft(String content, String ownerId) {
        this.content = content
        this.ownerId = ownerId
        this.updatedAt = LocalDateTime.now()
    }
}
