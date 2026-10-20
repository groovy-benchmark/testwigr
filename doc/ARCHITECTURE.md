# Testwigr Architecture Documentation

## Table of Contents

1. [Overview](#overview)
2. [Architectural Style](#architectural-style)
3. [Architecture Decisions](#architecture-decisions)
4. [System Components](#system-components)
5. [Design Patterns](#design-patterns)
6. [Data Model](#data-model)
7. [Security Architecture](#security-architecture)
8. [Environment Management](#environment-management)
9. [DevOps Architecture](#devops-architecture)
10. [Testing Strategy](#testing-strategy)
11. [Performance Considerations](#performance-considerations)
12. [Future Improvements](#future-improvements)

## Overview

Testwigr is a Twitter-like API platform built using Groovy, Spring Boot, and MongoDB. The system provides core social media functionality including user registration, authentication, posting, following, liking, and commenting. The application is designed to be scalable, maintainable, and deployed in various environments using Docker containers.

### Key Features

- User management and authentication
- Post creation and timeline generation
- Social interactions (likes, comments)
- User relationships (follow/unfollow)
- Feed generation based on social graph
- API versioning
- Rate limiting
- Security measures

## Architectural Style

Testwigr follows a **layered architecture** with **REST API** principles. The system is designed using the following architectural principles:

### Layered Architecture

The application is structured in the following layers:

1. **Controller Layer**: Handles HTTP requests, input validation, and response formatting
2. **Service Layer**: Implements business logic and orchestrates operations
3. **Repository Layer**: Provides data access abstraction
4. **Model Layer**: Represents domain entities and data structures

### RESTful Design

- Resource-oriented API design with appropriate HTTP methods
- Consistent URL patterns (`/api/{resource}/{id}`)
- Stateless authentication using JWT
- Standardized response formats
- HTTP status codes for error handling

### Microservices Readiness

While currently implemented as a monolith, the application is designed with service boundaries that facilitate a potential future migration to microservices:

- Clear separation of concerns
- Domain-driven component boundaries
- Independent data access per component
- Stateless authentication
- API versioning support

## Architecture Decisions

### Programming Language and Framework

**Decision**: Use Groovy with Spring Boot for the backend API.

**Rationale**:

- Groovy provides concise syntax and dynamic typing while maintaining Java compatibility
- Spring Boot offers a production-ready framework with robust security, dependency injection, and data access
- The combination enables rapid development while maintaining enterprise-grade capabilities

### Database

**Decision**: Use MongoDB as the data store.

**Rationale**:

- Document database model maps well to the social media domain objects
- Schema flexibility accommodates evolving requirements
- Support for embedded documents simplifies modeling of nested relationships (comments within posts)
- Horizontal scaling capabilities for future growth
- Good performance for read-heavy workloads typical of social media applications

### Authentication

**Decision**: Implement JWT-based stateless authentication.

**Rationale**:

- Stateless authentication simplifies horizontal scaling
- JWTs are self-contained and don't require server-side storage
- Token-based approach works well with RESTful architecture
- Good support in Spring Security ecosystem

### API Versioning

**Decision**: Implement header-based API versioning.

**Rationale**:

- Allows backward compatibility for clients during API evolution
- Header-based approach keeps URLs clean and consistent
- Simplifies routing to appropriate version handlers
- Avoids URL pollution compared to path-based versioning

### Rate Limiting

**Decision**: Implement token bucket-based rate limiting.

**Rationale**:

- Protects against abuse and ensures fair resource utilization
- Different limits for authenticated vs. unauthenticated requests
- Token bucket algorithm allows for bursts while maintaining average limits
- Implementation at the application level provides fine-grained control

## System Components

### Controller Components

1. **AuthController**: Handles user registration, login, and logout
2. **UserController**: Manages user profiles and follow relationships
3. **PostController**: Handles post creation, retrieval, and modification
4. **CommentController**: Manages comments on posts
5. **LikeController**: Handles post like interactions
6. **FeedController**: Generates personalized and user-specific feeds
7. **HealthController**: Provides system health information

### Service Components

1. **UserService**: User management, profile operations, and follow relationships
2. **PostService**: Post creation, modification, and interaction (likes, comments)
3. **FeedService**: Generation of feeds based on follow relationships

### Repository Components

1. **UserRepository**: Data access for user entities
2. **PostRepository**: Data access for post entities

### Configuration Components

1. **SecurityConfig**: JWT authentication and authorization
2. **RateLimitingConfig**: Request rate limiting
3. **ApiVersioningConfig**: API version handling
4. **SwaggerConfig**: API documentation

## Design Patterns

The application employs several design patterns to address common challenges:

### Repository Pattern

**Implementation**: The UserRepository and PostRepository interfaces extend Spring Data's MongoRepository.

**Purpose**: Abstracts data persistence operations and provides a collection-like interface for domain objects.

### Dependency Injection

**Implementation**: Spring's annotation-based dependency injection for services and repositories.

**Purpose**: Reduces coupling between components and facilitates testing.

### Service Layer

**Implementation**: UserService, PostService, and FeedService encapsulate business logic.

**Purpose**: Separates business logic from controllers and provides transaction boundaries.

### Factory Method

**Implementation**: Test data factories for creating test entities.

**Purpose**: Encapsulates object creation logic for tests, ensuring consistent test data.

### Filter Chain

**Implementation**: JwtAuthenticationFilter and JwtAuthorizationFilter in security configuration.

**Purpose**: Processes authentication and authorization in a sequential manner.

### Builder Pattern

**Implementation**: Used in JWT token creation and Swagger configuration.

**Purpose**: Constructs complex objects step by step with a fluent interface.

## Data Model

The data model consists of three primary entities with relationships:

### User

```
User {
    id: String (MongoDB ObjectId)
    username: String (unique)
    email: String (unique)
    password: String (encrypted)
    displayName: String
    bio: String
    following: Set<String> (user IDs)
    followers: Set<String> (user IDs)
    createdAt: LocalDateTime
    updatedAt: LocalDateTime
    active: boolean
}
```

### Post

```
Post {
    id: String (MongoDB ObjectId)
    content: String
    userId: String (reference to User)
    username: String (denormalized)
    likes: Set<String> (user IDs)
    comments: List<Comment> (embedded)
    createdAt: LocalDateTime
    updatedAt: LocalDateTime
}
```

### Comment (Embedded in Post)

```
Comment {
    id: String (UUID)
    content: String
    userId: String (reference to User)
    username: String (denormalized)
    createdAt: LocalDateTime
}
```

### Data Relationships

1. **User to User (Follow)**: Many-to-many relationship represented by user ID sets
2. **User to Post**: One-to-many relationship
3. **User to Like**: Many-to-many relationship represented by user ID sets in Post
4. **Post to Comment**: One-to-many relationship with embedded documents

### Denormalization Strategy

For performance reasons, the data model employs selective denormalization:

1. **Username Denormalization**: Username is stored in posts and comments to avoid joins for common display scenarios
2. **Embedded Comments**: Comments are embedded within posts for efficient retrieval
3. **Likes as User ID Sets**: Likes are stored as user ID sets within posts for efficient checking
4. **Follow Relationships in Both Directions**: Both following and followers are stored in the User document for efficient querying in both directions

## Security Architecture

### Authentication Flow

1. User registers with username, email, and password
2. Password is encrypted with BCrypt before storage
3. User logs in with username and password
4. Server validates credentials and issues a JWT token
5. Client includes JWT token in Authorization header for protected requests
6. JwtAuthorizationFilter validates token for protected endpoints

### JWT Implementation

- Tokens are signed with HMAC-SHA256 using a secret key
- Token contains the username as subject and expiration time
- Tokens have a default expiration of 24 hours (configurable)
- Token validation is performed for each protected request

### Authorization

- Endpoint security is configured in SecurityFilterChain
- Public endpoints: registration, login, and some read-only operations
- Protected endpoints require valid JWT token
- Resource ownership checks in services (e.g., only author can update post)

### Security Headers

- CSRF protection disabled for REST API (stateless)
- In production, NGINX adds security headers:
  - Strict-Transport-Security
  - X-Content-Type-Options
  - X-Frame-Options
  - X-XSS-Protection

### Data Protection

- Passwords are hashed using BCrypt with appropriate work factor
- MongoDB connection uses authentication in production
- Sensitive data is not logged or exposed in responses

## Environment Management

The application supports multiple deployment environments with environment-specific configurations:

### Development Environment

- In-memory databases or local Docker containers
- Detailed logging and error messages
- Swagger UI enabled
- CORS configured for local development

### Testing Environment

- Isolated test database
- Specific test properties and profiles
- Test-optimized rate limits
- Simplified security for testing

### Production Environment

- Hardened security configuration
- Production-grade MongoDB configuration
- Performance-optimized settings
- Structured JSON logging
- Environment variable-based configuration
- NGINX reverse proxy with SSL

### Configuration Management

- Base properties in application.properties
- Environment-specific overrides in profile-specific properties files
- Externalized secrets via environment variables
- Logging configuration in logback-spring.xml

## DevOps Architecture

### Containerization Strategy

The application uses Docker containers for consistent deployment across environments:

1. **Multi-Stage Build**: Optimized build process with separate build and runtime stages
2. **Alpine-Based Images**: Minimal footprint and reduced attack surface
3. **Non-Root Execution**: Containers run as non-root user for security
4. **Health Checks**: Integrated container health monitoring
5. **Resource Limits**: CPU and memory limits defined for containers

### Docker Compose Environments

Separate Docker Compose files for each environment:

1. **docker-compose.yml**: Development environment with hot-reload
2. **docker-compose-test.yml**: Testing environment with isolated databases
3. **docker-compose-prod.yml**: Production environment with NGINX and monitoring

### Monitoring Architecture

Production environment includes comprehensive monitoring:

1. **Spring Boot Actuator**: Exposes health and metrics endpoints
2. **Prometheus**: Collects and stores metrics
3. **Grafana**: Visualizes metrics with pre-configured dashboards
4. **Structured Logging**: JSON-formatted logs for log aggregation
5. **Health Checks**: Container and application-level health monitoring

### Backup Strategy

1. **Automated MongoDB Backups**: Scheduled database dumps
2. **Retention Policy**: Configurable backup retention
3. **Backup Verification**: Integrity checking of backup artifacts
4. **Point-in-Time Recovery**: Capability to restore to specific points

## Testing Strategy

The application implements a comprehensive testing strategy with multiple test types:

### Unit Tests

- Test individual components in isolation
- Mock dependencies using Spock
- Focus on business logic correctness

### Integration Tests

- Test component interactions
- Use test database with real repositories
- Verify correct data persistence and retrieval

### Controller Tests

- Test API endpoints with MockMvc
- Verify HTTP status codes and response formats
- Validate input validation and error handling

### Security Tests

- Verify authentication and authorization
- Test token generation and validation
- Check access control for resources

### End-to-End Tests

- Test complete user journeys
- Simulate real-world usage scenarios
- Verify system behavior as a whole

### Test Infrastructure

- Dedicated test configuration and properties
- Utility classes for test data generation
- Helper methods for authentication in tests
- Docker integration for test databases

## Performance Considerations

### Query Optimization

- Indexed fields in MongoDB collections (username, email, userId)
- Pagination for list endpoints to limit result sizes
- Projections to retrieve only needed fields

### Caching Strategy

Though not fully implemented yet, the architecture supports:

- In-memory caching for frequently accessed data
- Cache invalidation on data modifications
- Distributed caching in clustered environments

### Database Optimization

- Denormalization for read performance
- Embedded documents to reduce joins
- Connection pooling for efficient resource usage

### Application Performance

- Asynchronous processing for non-critical operations
- Resource optimization in controllers and services
- Efficient data transformations

## Future Improvements

The architecture has been designed to accommodate several future enhancements:

### Technical Enhancements

1. **Microservices Migration**: Break down into domain-specific services
2. **Caching Layer**: Add Redis for performance optimization
3. **Message Queue**: Implement event-driven architecture with Kafka/RabbitMQ
4. **Full-Text Search**: Integrate Elasticsearch for advanced search capabilities
5. **CDN Integration**: For media content delivery

### Functional Enhancements

1. **Media Attachments**: Support for images and videos
2. **Notification System**: Real-time notifications via WebSockets
3. **Analytics Engine**: User and content analytics
4. **Content Moderation**: AI-assisted content filtering
5. **Advanced Feed Algorithms**: Personalized feed generation

### Infrastructure Enhancements

1. **Kubernetes Deployment**: Migration from Docker Compose to Kubernetes
2. **Service Mesh**: Implement Istio for advanced networking features
3. **Distributed Tracing**: Add Jaeger for request tracing
4. **Database Clustering**: MongoDB replication for high availability
5. **Multi-Region Deployment**: Geographical distribution for lower latency
