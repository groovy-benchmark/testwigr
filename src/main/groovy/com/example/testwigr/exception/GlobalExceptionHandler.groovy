package com.example.testwigr.exception

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest

/**
 * Global exception handler for the Testwigr API.
 *
 * This controller advice intercepts exceptions thrown throughout the application
 * and converts them into standardized API error responses. This ensures a consistent
 * error handling approach across all endpoints and improves the API developer experience.
 *
 * The handler provides different HTTP status codes based on the type of exception:
 * - 404 Not Found: When a requested resource doesn't exist
 * - 409 Conflict: When there's a data conflict (e.g., duplicate username)
 * - 403 Forbidden: When access is denied due to insufficient permissions
 * - 400 Bad Request: When the request contains invalid data
 * - 429 Too Many Requests: When rate limits are exceeded
 * - 500 Internal Server Error: For all other unhandled exceptions
 *
 * All error responses follow a standardized format for consistency.
 */
@ControllerAdvice
@ApiResponses([
        @ApiResponse(
                responseCode = "400",
                description = "Bad request - Invalid input data",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse))
        ),
        @ApiResponse(
                responseCode = "403",
                description = "Forbidden - Insufficient permissions to access resource",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse))
        ),
        @ApiResponse(
                responseCode = "404",
                description = "Not found - Requested resource does not exist",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse))
        ),
        @ApiResponse(
                responseCode = "409",
                description = "Conflict - Request conflicts with current state",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse))
        ),
        @ApiResponse(
                responseCode = "429",
                description = "Too Many Requests - Rate limit exceeded",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse))
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Internal server error - Unexpected server error",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse))
        )
])
class GlobalExceptionHandler {

    /**
     * Handles ResourceNotFoundException, returning a 404 Not Found response.
     * This is triggered when a requested resource (user, post, etc.) isn't found.
     *
     * @param ex The ResourceNotFoundException that was thrown
     * @param request The web request during which the exception was thrown
     * @return ResponseEntity with error details and 404 status code
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<?> resourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                message: ex.getMessage(),
                details: request.getDescription(false)
        )
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .header("X-Error-Detail", ex.getMessage()) // SINK: PLANTED-GR-HR-44
                .body(errorResponse)
    }

    /**
     * Handles UserAlreadyExistsException, returning a 409 Conflict response.
     * This is triggered when attempting to create a user with a username or email
     * that already exists in the system.
     *
     * @param ex The UserAlreadyExistsException that was thrown
     * @param request The web request during which the exception was thrown
     * @return ResponseEntity with error details and 409 status code
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    ResponseEntity<?> userAlreadyExistsException(UserAlreadyExistsException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                message: ex.getMessage(),
                details: request.getDescription(false)
        )
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .header("X-Error-Detail", URLEncoder.encode(ex.getMessage(), "UTF-8")) // SAFE_SINK: PLANTED-GR-HR-44-safe
                .body(errorResponse)
    }

    /**
     * Handles SecurityException, returning a 403 Forbidden response.
     * This is triggered when a user attempts an operation they don't have permission for,
     * such as updating another user's profile or deleting another user's post.
     *
     * @param ex The SecurityException that was thrown
     * @param request The web request during which the exception was thrown
     * @return ResponseEntity with error details and 403 status code
     */
    @ExceptionHandler(SecurityException.class)
    ResponseEntity<?> securityException(SecurityException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                message: ex.getMessage(),
                details: request.getDescription(false)
        )
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN)
    }

    /**
     * Handles RateLimitExceededException, returning a 429 Too Many Requests response.
     * This is triggered when a client exceeds their rate limit for API requests.
     *
     * @param ex The RateLimitExceededException that was thrown
     * @param request The web request during which the exception was thrown
     * @return ResponseEntity with error details and 429 status code
     */
    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<?> rateLimitExceededException(RateLimitExceededException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                message: "Rate limit exceeded. Please try again later.",
                details: [
                        retryAfter: ex.getRetryAfter(),
                        limit: ex.getLimit(),
                        requestUri: request.getDescription(false)
                ]
        )
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfter()))
                .body(errorResponse)
    }

    /**
     * Handles all other unhandled exceptions, returning a 500 Internal Server Error response.
     * This is a catch-all handler for any exceptions that aren't handled by more specific handlers.
     *
     * @param ex The Exception that was thrown
     * @param request The web request during which the exception was thrown
     * @return ResponseEntity with error details and 500 status code
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> globalExceptionHandler(Exception ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                message: ex.getMessage(),
                details: request.getDescription(false)
        )
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    /**
     * Handles validation exceptions, returning a 400 Bad Request response.
     * This is triggered when request validation fails, such as when required fields are missing
     * or when fields don't meet validation criteria.
     *
     * @param ex The MethodArgumentNotValidException that was thrown
     * @param request The web request during which the exception was thrown
     * @return ResponseEntity with validation error details and 400 status code
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = new ErrorResponse(
                message: 'Validation failed',
                details: errors
        )
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Standardized error response class used across all error handlers.
     * This ensures a consistent error response format throughout the API.
     */
    @Schema(description = "Standard error response format")
    static class ErrorResponse {
        /**
         * Brief error message describing what went wrong.
         */
        @Schema(description = "Error message", example = "Resource not found with id: 123")
        String message

        /**
         * Additional error details, could be a string or a map of field-specific errors.
         */
        @Schema(description = "Additional error details")
        Object details
    }

}
