package com.example.testwigr.config

import io.swagger.v3.oas.annotations.media.Schema

/**
 * API version information model for documentation purposes.
 * 
 * This class provides a schema for version information that is displayed
 * in the API documentation. It doesn't contain actual implementation logic
 * but serves as a documentation structure for the OpenAPI specification.
 * 
 * The version information includes:
 * - Version number (following semantic versioning)
 * - Release date
 * - Status (e.g., current, deprecated)
 * - End-of-life date for deprecated versions
 * - Changelog
 */
@Schema(description = "API version information")
class ApiVersionInfo {
    
    /**
     * The version number of the API, following semantic versioning.
     */
    @Schema(description = "API version number (semantic versioning)", example = "1.0.0")
    String version
    
    /**
     * The release date of this API version.
     */
    @Schema(description = "Release date of this API version", example = "2023-01-15")
    String releaseDate
    
    /**
     * The status of this API version (current, deprecated, etc.)
     */
    @Schema(description = "API version status", example = "current", allowableValues = ["current", "deprecated", "upcoming"])
    String status
    
    /**
     * For deprecated versions, the date after which the version will no longer be supported.
     * Null for current or upcoming versions.
     */
    @Schema(description = "End-of-life date for deprecated versions", example = "2024-01-15", nullable = true)
    String endOfLifeDate
    
    /**
     * A list of changes introduced in this version.
     */
    @Schema(description = "List of changes in this version")
    List<String> changes
}
