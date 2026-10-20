# Testwigr API Documentation

## Overview

The Testwigr API provides RESTful endpoints for interacting with the Testwigr platform, a Twitter-like social media application. This API allows clients to create and manage user accounts, posts, follows, likes, and comments.

## API Documentation

### Interactive Documentation

Comprehensive interactive API documentation is available via Swagger UI:

- **Development**: http://localhost:8080/swagger-ui.html
- **Testing**: http://test.testwigr.example.com/swagger-ui.html
- **Production**: https://api.testwigr.example.com/swagger-ui.html

### OpenAPI Specification

The complete OpenAPI specification is available in JSON format:

- **Development**: http://localhost:8080/v3/api-docs
- **Testing**: http://test.testwigr.example.com/v3/api-docs
- **Production**: https://api.testwigr.example.com/v3/api-docs

## API Versioning

The Testwigr API uses header-based versioning. To specify which API version to use, include the `X-API-Version` header in your requests:

```
X-API-Version: 1.0.0
```

### Current Version

The current API version is **1.0.0**. If no version is specified, the server defaults to the current version.

### Response Headers

All API responses include the following version-related headers:

- `X-API-Version`: The version used for this request
- `X-API-Current-Version`: The latest API version available
- `X-API-Supported-Versions`: A comma-separated list of all supported versions

## Authentication

The API uses JSON Web Tokens (JWT) for authentication. To authenticate:

1. Register a user account using the `/api/auth/register` endpoint
2. Log in using the `/api/auth/login` endpoint to obtain a JWT token
3. Include the token in the `Authorization` header of subsequent requests:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Token Expiration

JWT tokens expire after 24 hours. After expiration, a new token must be obtained by logging in again.

## Request Format

For endpoints that accept request bodies (POST, PUT), the content should be formatted as JSON with the `Content-Type` header set to `application/json`:

```
Content-Type: application/json
```

Example request body:
```json
{
  "username": "johndoe",
  "email": "john.doe@example.com",
  "password": "securepassword",
  "displayName": "John Doe"
}
```

## Response Format

All responses use a standardized format to ensure consistency:

```json
{
  "success": true,
  "message": "Operation completed successfully",
  "timestamp": "2023-01-15T14:30:15.123Z",
  "data": {
    // Response data here
  }
}
```

For error responses:

```json
{
  "success": false,
  "message": "Error message",
  "timestamp": "2023-01-15T14:30:15.123Z",
  "error": {
    // Error details here
  }
}
```

## Error Handling

The API uses standard HTTP status codes to indicate success or failure:

- `200 OK`: The request was successful
- `400 Bad Request`: The request was invalid
- `401 Unauthorized`: Authentication failed
- `403 Forbidden`: The authenticated user doesn't have permission
- `404 Not Found`: The requested resource does not exist
- `409 Conflict`: The request conflicts with current state
- `500 Internal Server Error`: An unexpected server error occurred

## Pagination

Endpoints that return collections support pagination using the following query parameters:

- `page`: Page number (0-indexed)
- `size`: Number of items per page
- `sort`: Property to sort by, optionally followed by `,asc` or `,desc`

Example:
```
GET /api/posts?page=0&size=10&sort=createdAt,desc
```

Paginated responses include metadata about the page:

```json
{
  "success": true,
  "data": {
    "content": [
      // Array of items
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": {
        "sorted": true,
        "unsorted": false,
        "empty": false
      }
    },
    "totalElements": 42,
    "totalPages": 5,
    "last": false,
    "first": true,
    "size": 10,
    "number": 0,
    "numberOfElements": 10,
    "empty": false
  }
}
```

## Rate Limiting

To ensure API stability and fair usage, rate limiting is implemented with the following defaults:

- **Authenticated requests**: 100 requests per minute
- **Unauthenticated requests**: 20 requests per minute

When a rate limit is exceeded, the API returns a `429 Too Many Requests` response with headers indicating the limit and when it resets:

- `X-RateLimit-Limit`: Total requests allowed in the time window
- `X-RateLimit-Remaining`: Remaining requests in the current window
- `X-RateLimit-Reset`: Time in seconds until the rate limit resets

## API Endpoints

For detailed documentation on all available endpoints, please refer to the Swagger UI documentation linked above. The API is organized into the following main sections:

- **Authentication**: Registration, login, and logout
- **Users**: User profile management
- **Posts**: Creating, retrieving, updating, and deleting posts
- **Comments**: Adding and retrieving comments on posts
- **Likes**: Liking and unliking posts
- **Follow**: Managing follow relationships between users
- **Feed**: Retrieving personalized content feeds

## Support

For API support:

- **Email**: api-support@testwigr.example.com
- **Documentation**: https://docs.testwigr.example.com
- **Issues**: https://github.com/testwigr/api/issues

## License

The Testwigr API is licensed under the Apache License 2.0.
