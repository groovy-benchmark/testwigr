# Testwigr - Twitter-like API

A robust Twitter-like API built with Groovy, Spring Boot, and MongoDB. This project implements core Twitter functionality including user registration, authentication with JWT, posting, liking, commenting, and following other users.

## Project Setup Requirements

1. **Development Environment**
   - JDK (Java Development Kit) - version 8 or higher
   - Groovy installation
   - IDE (IntelliJ IDEA is recommended for Groovy/Spring development)
   - Git for version control

2. **Project Configuration**
   - Spring Boot starter project with Groovy support
   - Gradle build configuration file (build.gradle)
   - MongoDB connection configuration
   - Docker and Docker Compose files for local development

3. **Initial Project Structure**
   - Models/entities (User, Post, Comment, Like)
   - Repositories for database interaction
   - Services for business logic
   - Controllers for API endpoints
   - Test packages for Spock framework tests

4. **Documentation**
   - API documentation (Swagger/OpenAPI)
   - README with setup instructions

## Features

- **User Management**
  - Registration and authentication with JWT
  - Profile management (view, edit, delete)
  - Follow/unfollow users
  - Secure password handling

- **Post Management**
  - Create, edit, and delete posts
  - View posts from specific users
  - Personal feed (posts from followed users)
  - User feed (posts from a specific user)

- **Social Interactions**
  - Like/unlike posts
  - Comment on posts
  - View post statistics (likes, comments)
  - Follow/unfollow relationships

- **Feed Generation**
  - Personalized feed of posts from followed users
  - User-specific feeds
  - Chronological post ordering
  - Pagination support

## Tech Stack

- **Backend**
  - Groovy 4.0.25
  - Spring Boot 3.4.3
  - Spring Security
  - Spring Data MongoDB
  - JWT for authentication

- **Database**
  - MongoDB

- **Build & Deployment**
  - Gradle
  - Docker
  - Docker Compose
  - NGINX (for production)

- **Testing**
  - Spock Framework
  - JUnit 5
  - Spring Test

## Getting Started

### Prerequisites

- JDK 21 or higher
- Docker and Docker Compose
- Git

### Local Development Setup

1. **Clone the repository**

   ```bash
   git clone https://github.com/yourusername/testwigr.git
   cd testwigr
   ```

2. **Environment Configuration**

   ```bash
   cp .env.template .env
   # Edit .env with appropriate values
   ```

3. **Start MongoDB using Docker**

   ```bash
   docker-compose -f docker/docker-compose.yml up -d mongodb
   ```

4. **Build the application**

   ```bash
   ./gradlew build -x test
   ```

5. **Run the application**

   ```bash
   ./gradlew bootRun
   ```

   Or use Docker to run the entire stack:

   ```bash
   docker-compose -f docker/docker-compose.yml up -d
   ```

6. **Access the API**
   - The API will be available at `http://localhost:8080`
   - Swagger UI documentation is available at `http://localhost:8080/swagger-ui.html`

### Running Tests

For detailed information about testing, please refer to [TESTING.md](TESTING.md).

Basic test commands:

```bash
# Start test MongoDB
./scripts/start-test-db.sh

# Run all tests
./gradlew test

# Run specific test categories
./gradlew test --tests "com.example.testwigr.repository.*"
./gradlew test --tests "com.example.testwigr.service.*"
./gradlew test --tests "com.example.testwigr.controller.*"
./gradlew test --tests "com.example.testwigr.integration.*"

# Stop test MongoDB
./scripts/stop-test-db.sh
```

### Deployment

For comprehensive deployment instructions, including production setup, please refer to [DEPLOY.md](DEPLOY.md).

## API Endpoints Overview

Testwigr provides a comprehensive API surface with these main endpoint categories:

### Authentication Endpoints

- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - Authentication and token issuance
- `POST /api/auth/logout` - Session termination

### User Management Endpoints

- `GET /api/users/{username}` - Retrieve user profiles
- `GET /api/users/me` - Get authenticated user's profile
- `PUT /api/users/{id}` - Update user information
- `DELETE /api/users/{id}` - Remove user accounts

### Post Management Endpoints

- `POST /api/posts` - Create new posts
- `GET /api/posts/{id}` - Retrieve specific posts
- `GET /api/posts/user/{userId}` - Get all posts by a user
- `PUT /api/posts/{id}` - Update existing posts
- `DELETE /api/posts/{id}` - Remove posts

### Social Interaction Endpoints

- `POST /api/follow/{followingId}` - Follow users
- `DELETE /api/follow/{followingId}` - Unfollow users
- `GET /api/follow/followers` - List followers
- `GET /api/follow/following` - List followed users
- `POST /api/likes/posts/{postId}` - Like posts
- `DELETE /api/likes/posts/{postId}` - Unlike posts
- `POST /api/comments/posts/{postId}` - Add comments
- `GET /api/comments/posts/{postId}` - Get post comments

### Feed Endpoints

- `GET /api/feed` - Retrieve personalized content feeds
- `GET /api/feed/users/{username}` - Get user-specific feeds

### System Endpoints

- `GET /api/health` - System health check
- `GET /api/health/info` - System information

## Project Structure

```plaintext
testwigr/
├── docker/                    # Docker configurations
├── scripts/                   # Utility scripts
├── src/
│   ├── main/
│   │   ├── groovy/            # Application source code
│   │   │   └── com/example/testwigr/
│   │   │       ├── config/    # Application configurations
│   │   │       ├── controller/ # API endpoints
│   │   │       ├── exception/ # Exception handling
│   │   │       ├── model/     # Domain entities
│   │   │       ├── repository/ # Data access layer
│   │   │       ├── security/  # JWT authentication
│   │   │       ├── service/   # Business logic
│   │   │       └── TestwigrApplication.groovy
│   │   └── resources/         # Application resources
│   └── test/                  # Test source code
│       ├── groovy/            # Test classes
│       │   └── com/example/testwigr/
│       │       ├── config/    # Test configurations
│       │       ├── controller/ # Controller tests
│       │       ├── integration/ # Integration tests
│       │       ├── repository/ # Repository tests
│       │       ├── security/  # Security tests
│       │       ├── service/   # Service tests
│       │       └── test/      # Test utilities
│       └── resources/         # Test resources
├── build.gradle               # Gradle build configuration
├── DEPLOY.md                  # Deployment documentation
├── LICENSE                    # Project license (Apache 2.0)
├── README.md                  # This file
├── settings.gradle            # Gradle settings
├── TESTING.md                 # Testing documentation
└── .env.template              # Environment variable template
```

## Security

The application uses JWT (JSON Web Tokens) for authentication:

1. Users register and login to receive a JWT token.
2. The token must be included in the Authorization header of subsequent requests.
3. Protected endpoints verify the token through a filter chain.

Example of using JWT with curl:

```bash
# Login to get token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}' \
  | jq -r '.token')

# Use token in another request
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN"
```

## Monitoring

The application exposes several monitoring endpoints through Spring Actuator:

- `/actuator/health` - Application health information
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus-formatted metrics

For production deployments, a complete monitoring stack with Prometheus and Grafana is available. See [DEPLOY.md](DEPLOY.md) for details.

## Detailed Documentation

Comprehensive documentation is available in the `doc/` folder.

## Contributing

Contributions are welcome! Please feel free to submit pull requests.

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/new-feature`
3. Commit your changes: `git commit -am 'Add new feature'`
4. Push to the branch: `git push origin feature/new-feature`
5. Submit a pull request

Before submitting, please ensure that tests pass and code follows the project's coding conventions.

## Future Enhancements

Plans for future development include:

1. Refresh token implementation
2. Redis caching for performance optimization
3. Media attachments support
4. Advanced feed algorithms with personalization
5. Real-time notifications
6. Full-text search functionality
7. Content moderation features

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
