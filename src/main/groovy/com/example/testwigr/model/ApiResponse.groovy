package com.example.testwigr.model

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Standard wrapper for API responses.
 * 
 * This class provides a standardized structure for all API responses,
 * which promotes consistency across the API and improves the developer
 * experience. It includes metadata about the response as well as the
 * actual data payload.
 * 
 * The wrapper includes:
 * - Success status
 * - Status message
 * - Timestamp
 * - Data payload (can be any type)
 * - Error details (if applicable)
 * 
 * @param <T> The type of data contained in the response
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
class ApiResponse<T> {
    
    /**
     * Indicates whether the request was successful.
     */
    @Schema(description = "Indicates whether the request was successful", example = "true")
    boolean success
    
    /**
     * Status message providing additional context about the response.
     */
    @Schema(description = "Status message", example = "Operation completed successfully")
    String message
    
    /**
     * Timestamp of when the response was generated.
     */
    @Schema(description = "Timestamp of when the response was generated", example = "2023-01-15T14:30:15.123")
    String timestamp
    
    /**
     * The data payload of the response. This is the actual content
     * being returned to the client.
     */
    @Schema(description = "Data payload of the response")
    T data
    
    /**
     * Error details, if applicable. Only included when success is false.
     */
    @Schema(description = "Error details, if applicable", nullable = true)
    Object error
    
    /**
     * Default constructor required for serialization.
     */
    ApiResponse() {
        this.timestamp = new Date().toInstant().toString()
    }
    
    /**
     * Creates a successful/error response with the provided data and message.
     * 
     * @param data The data payload
     * @param message A status message
     */
    ApiResponse(boolean success, T data, String message) {
        this.success = true
        this.data = data
        this.message = message
        this.timestamp = new Date().toInstant().toString()
    }

    /**
     * Static factory method for creating a successful response.
     * 
     * @param data The data payload
     * @param message A status message
     * @return A successful ApiResponse containing the data
     */
    static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message)
    }
    
    /**
     * Static factory method for creating an error response.
     * 
     * @param error The error details
     * @param message An error message
     * @return An error ApiResponse containing the error details
     */
    static ApiResponse<?> error(Object error, String message) {
        return new ApiResponse<>(false, error, message)
    }
}
