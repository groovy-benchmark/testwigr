# Comprehensive Testing Guide for Testwigr

## Overview

This document provides a detailed guide to the testing infrastructure of the Testwigr project. It covers the different testing approaches, how to set up your testing environment, executing tests, and troubleshooting common issues.

Testwigr employs a multi-layered testing approach to ensure code quality at all levels of the application, from unit tests to full end-to-end tests.

## Setting Up the Testing Environment

### Prerequisites

Before running tests, ensure you have the following:

1. **MongoDB Instance**: Tests require a MongoDB instance running on port 27018 (different from the default development port)
   - Use the provided Docker configuration: `docker compose -f docker/docker-compose-test.yml up -d mongodb-test`
   - Alternatively, use the scripts: `./scripts/start-test-db.sh`

2. **Java Development Kit (JDK)**: Version 21 or higher

3. **Gradle**: The project uses Gradle as the build tool, which will download necessary dependencies

### Test Configuration

The test configuration is defined in:

- `src/test/resources/application-test.properties`: Contains test-specific settings
- `docker/docker-compose-test.yml`: MongoDB container configuration for tests

Key test properties:

```properties
# Test database configuration
spring.data.mongodb.uri=mongodb://localhost:27018/testdb
spring.data.mongodb.database=testdb

# Security settings for testing
app.jwt.secret=testSecretKeyForTestingPurposesOnlyDoNotUseInProduction
app.jwt.expiration=86400000

# Critical settings for circular reference resolution
spring.main.allow-bean-definition-overriding=true
spring.main.allow-circular-references=true
```

### Docker-based Test Environment

For convenience, a Docker-based test environment is provided. This ensures consistent test execution across different development environments.

To use the Docker-based test environment:

1. Start the test environment:

   ```bash
   docker compose -f docker/docker-compose-test.yml up -d
   ```

2. Run tests:

   ```bash
   # If running tests from host machine
   ./gradlew test

   # If running tests from within a Docker container
   docker exec -it testwigr-app ./gradlew test
   ```

3. Stop the test environment:

   ```bash
   docker compose -f docker/docker-compose-test.yml down
   ```

## Testing Architecture

### Testing Layers

The project implements a layered testing strategy:

#### 1. Unit Tests

Unit tests focus on testing individual components in isolation, with all dependencies mocked or stubbed.

- **Service Layer Tests**: Test business logic in isolation
  - Located in `src/test/groovy/com/example/testwigr/service/`
  - Example: `UserServiceSpec`, `PostServiceSpec`

- **Repository Tests**: Test repository interfaces with an embedded MongoDB
  - Located in `src/test/groovy/com/example/testwigr/repository/`
  - Example: `UserRepositoryTest`, `PostRepositoryTest`

#### 2. Slice Tests

Slice tests focus on testing specific layers of the application in isolation with minimal dependencies.

- **Repository Slice Tests**: Use `@DataMongoTest` to test repository interfaces with a real database
  - Example: `UserRepositorySliceTest`, `PostRepositorySliceTest`

- **Controller Slice Tests**: Use `@WebMvcTest` to test controller endpoints with mocked services
  - Example: `UserControllerWebTest`

#### 3. Integration Tests

Integration tests verify multiple components working together.

- **Service Integration Tests**: Test services with real repositories and database
  - Example: `PostServiceIntegrationTest`, `FeedServiceIntegrationTest`

- **Controller Integration Tests**: Test controllers with real services and repositories
  - Example: `UserControllerIntegrationTest`, `CommentControllerIntegrationSpec`

- **Authentication Flow Tests**: Test the entire authentication process
  - Example: `AuthenticationFlowIntegrationTest`

#### 4. End-to-End Tests

End-to-end tests simulate real user interactions and test the complete flow through the API.

- Located in `src/test/groovy/com/example/testwigr/integration/`
- Example: `FullApiFlowTest`, `ComprehensiveEndToEndTest`, `SocialNetworkIntegrationTest`

### Testing Utilities

The project includes several utility classes to support testing:

1. **TestDataFactory** (`src/test/groovy/com/example/testwigr/test/TestDataFactory.groovy`)
   - Creates test entities (users, posts, comments) with predefined or custom attributes
   - Provides methods for creating complex social networks and timelines

2. **TestDatabaseUtils** (`src/test/groovy/com/example/testwigr/test/TestDatabaseUtils.groovy`)
   - Manages test database state
   - Provides methods for populating and cleaning the database

3. **TestSecurityUtils** (`src/test/groovy/com/example/testwigr/test/TestSecurityUtils.groovy`)
   - Generates JWT tokens for authentication in tests
   - Provides utilities for setting up security context

## Running Tests

### Basic Test Execution

To run all tests:

```bash
./gradlew test
```

### Running Specific Test Categories

To run specific test categories:

```bash
# Run all repository tests
./gradlew test --tests "com.example.testwigr.repository.*"

# Run all service tests
./gradlew test --tests "com.example.testwigr.service.*"

# Run all controller tests
./gradlew test --tests "com.example.testwigr.controller.*"

# Run all integration tests
./gradlew test --tests "com.example.testwigr.integration.*"

# Run all security tests
./gradlew test --tests "com.example.testwigr.security.*"
```

### Running Individual Tests

To run a specific test class:

```bash
./gradlew test --tests "com.example.testwigr.service.UserServiceSpec"
```

For more detailed test output, add the `--info` flag:

```bash
./gradlew test --tests "com.example.testwigr.service.UserServiceSpec" --info
```

### Continuous Testing

For development, you can use continuous testing to automatically rerun tests when code changes:

```bash
./gradlew test --continuous
```

### Tests with Docker

If you're using Docker for testing:

```bash
# Start the test database
./scripts/start-test-db.sh

# Run tests
./gradlew test

# Stop the test database
./scripts/stop-test-db.sh
```

## Test Configurations and Base Classes

### Test Configurations

Several test configuration classes are available to provide specific test environments:

1. **SimpleSecurityTestConfig**: Provides a simplified security configuration for tests
   - Allows all requests to simplify testing
   - Creates in-memory test users

2. **TestSecurityConfig**: More complete security configuration with JWT support
   - Configures authentication manager and JWT filters
   - Provides in-memory user details service

3. **TestMongoConfig**: Configures MongoDB for testing
   - Sets up connection to test MongoDB instance

4. **IntegrationTestConfig**: Configuration for integration tests
   - Provides configuration specific to integration testing

### Base Test Classes

1. **ControllerTestBase**: Base class for controller tests
   - Provides authentication helpers
   - Configures MockMvc

2. **MongoIntegrationSpec**: Base class for MongoDB integration tests
   - Configures MongoDB connection for tests

## Test Database Management

### Starting the Test Database

```bash
./scripts/start-test-db.sh
```

This script:

1. Starts a MongoDB container on port 27018
2. Waits for the database to be ready
3. Creates necessary test collections and indexes

### Stopping the Test Database

```bash
./scripts/stop-test-db.sh
```

This script:

1. Stops and removes the test MongoDB container
2. Cleans up any resources

### Manual Database Operations

You can also interact directly with the test database:

```bash
# Access MongoDB shell
docker exec -it mongodb-test mongo

# View databases
show dbs

# Use test database
use testdb

# Show collections
show collections

# Query users
db.users.find()
```

## Troubleshooting Common Test Failures

### Path Variable Resolution Issues

**Symptom**: Tests fail with errors related to path variable resolution, particularly with `@PathVariable` annotations.

**Solution**: Ensure all `@PathVariable` annotations include explicit parameter names:

```groovy
// Incorrect
@PathVariable id String id

// Correct
@PathVariable("id") String id
```

### WebTestClient vs MockMvc Issues

**Symptom**: Tests using WebTestClient fail with configuration errors.

**Solution**: The project uses Spring MVC, not WebFlux, so tests should use MockMvc instead of WebTestClient:

```groovy
// Incorrect (WebTestClient - for WebFlux)
webTestClient
    .get()
    .uri("/api/users/me")
    .header("Authorization", "Bearer ${token}")
    .exchange()
    .expectStatus().isOk()

// Correct (MockMvc - for Spring MVC)
mockMvc.perform(
    MockMvcRequestBuilders.get("/api/users/me")
        .header("Authorization", "Bearer ${token}")
)
.andExpect(MockMvcResultMatchers.status().isOk())
```

### Authentication and JWT Issues

**Symptom**: Tests involving authentication fail with 401 or 403 errors.

**Solutions**:

1. **Check JWT Secret**: Ensure the JWT secret in tests matches the one in application-test.properties.

2. **JWT Token Generation**: Use TestSecurityUtils to generate valid tokens:

   ```groovy
   String token = TestSecurityUtils.generateTestToken(username, jwtSecret)
   ```

3. **Mock Authentication**: For controller tests, use Spring Security's test support:

   ```groovy
   @WithMockUser(username = "testuser")
   def "should get user profile"() {
       // Test code
   }
   ```

### Circular Dependency Issues

**Symptom**: The application context fails to initialize due to circular dependencies between services.

**Solution**: Use the `@Lazy` annotation to break circular dependencies:

```groovy
class ServiceA {
    private final @Lazy ServiceB serviceB

    ServiceA(@Lazy ServiceB serviceB) {
        this.serviceB = serviceB
    }
}
```

Ensure the application-test.properties includes:

```properties
spring.main.allow-circular-references=true
```

### MongoDB Connection Issues

**Symptom**: Tests fail with MongoDB connection errors.

**Solutions**:

1. **Check MongoDB Container**: Ensure the test MongoDB container is running:

   ```bash
   docker ps | grep mongodb-test
   ```

2. **Start Test Database**: Use the provided script:

   ```bash
   ./scripts/start-test-db.sh
   ```

3. **Check Port**: Ensure MongoDB is running on port 27018 (test port)

4. **Check Configuration**: Verify the MongoDB URI in application-test.properties:

   ```properties
   spring.data.mongodb.uri=mongodb://localhost:27018/testdb
   ```

5. **Check Network**: If running in Docker, ensure network connectivity between containers:

   ```bash
   docker network inspect docker_test-network
   ```

### Test Data Management Issues

**Symptom**: Tests interfere with each other due to shared data.

**Solution**: Ensure tests clean up after themselves:

1. Use `setup()` and `cleanup()` methods in Spock tests:

   ```groovy
   def setup() {
       TestDatabaseUtils.cleanDatabase(userRepository, postRepository)
   }

   def cleanup() {
       TestDatabaseUtils.cleanDatabase(userRepository, postRepository)
   }
   ```

2. Use unique identifiers for test entities to avoid collisions

3. Run individual test classes when debugging data interference:

   ```bash
   ./gradlew test --tests "com.example.testwigr.integration.SpecificTestClass"
   ```

## Docker-specific Test Issues

### Docker Test Database Not Starting

**Symptom**: The MongoDB test container fails to start.

**Solution**:

1. Check Docker logs:

   ```bash
   docker logs mongodb-test
   ```

2. Verify port availability:

   ```bash
   netstat -tulpn | grep 27018
   ```

3. Ensure Docker service is running:

   ```bash
   systemctl status docker
   ```

4. Try cleaning up Docker resources:

   ```bash
   docker system prune -f
   ```

### Tests Failing in Docker but Passing Locally

**Symptom**: Tests pass on local machine but fail in Docker environment.

**Solution**:

1. Check for environment differences:

   ```bash
   docker exec -it testwigr-app env | grep SPRING
   ```

2. Ensure container has sufficient resources:

   ```bash
   docker stats
   ```

3. Check for timing issues (add delays in tests or increase timeouts)

## Best Practices for Writing Tests

1. **Isolation**: Each test should be independent and not rely on the state from other tests

2. **Descriptive Names**: Use descriptive test method names that explain what is being tested:

   ```groovy
   def "should return 404 when user not found"() { ... }
   ```

3. **Arrange-Act-Assert Pattern**: Structure tests with clear setup, action, and verification phases:

   ```groovy
   // Arrange
   def user = TestDataFactory.createUser()
   userRepository.save(user)

   // Act
   def result = userService.getUserById(user.id)

   // Assert
   result.id == user.id
   ```

4. **Clean Setup and Teardown**: Use setup() and cleanup() methods to ensure a clean test environment

5. **Use TestDataFactory**: Create test data consistently using the TestDataFactory

6. **Focus on Behavior**: Test the behavior, not the implementation details

7. **Test Edge Cases**: Include tests for boundary conditions and error scenarios

8. **Keep Tests Fast**: Optimize tests to run quickly to encourage frequent testing

## Continuous Integration

All tests are automatically run as part of the CI/CD pipeline. The build will fail if any tests fail, ensuring code quality.

The Gradle test task is configured to:

1. Start a MongoDB test container
2. Run all tests
3. Shut down the MongoDB container after tests complete

```groovy
// From build.gradle
task startTestMongoDB(type: Exec) {
    commandLine 'docker', 'compose', '-f', 'docker/docker-compose-test.yml', 'up', '-d', 'mongodb-test'
    doLast {
        println "Waiting for MongoDB to be ready..."
        sleep(10000) // Give MongoDB time to start up
        println "Test MongoDB is ready!"
    }
}

task stopTestMongoDB(type: Exec) {
    commandLine 'docker', 'compose', '-f', 'docker/docker-compose-test.yml', 'down'
}

test {
    dependsOn startTestMongoDB
    finalizedBy stopTestMongoDB
}
```

## Test Status Summary

### Tests Passing

- Controller tests: `./gradlew test --tests "com.example.testwigr.controller.*"`
- Integration tests: `./gradlew test --tests "com.example.testwigr.integration.*"`
- Repository tests: `./gradlew test --tests "com.example.testwigr.repository.*"`
- Secure tests: `./gradlew test --tests "com.example.testwigr.security.*"`
- Service tests: `./gradlew test --tests "com.example.testwigr.service.*"`

### Known Issues and Fixes

1. **Path Variable Resolution**: Ensure all `@PathVariable` annotations include explicit parameter names:

   ```groovy
   @PathVariable("id") String id
   ```

2. **Circular Dependencies**: Use the `@Lazy` annotation for circular dependencies:

   ```groovy
   @Lazy UserService userService
   ```

3. **WebClient vs MockMvc**: Use MockMvc instead of WebClient for Spring MVC tests

## Docker Integration for Testing

The project includes Docker support for testing:

### Docker Compose Configuration

The `docker/docker-compose-test.yml` file defines the test environment:

```yaml
services:
  mongodb-test:
    image: mongo:latest
    container_name: mongodb-test
    ports:
      - "27018:27017"  # Using a different port than your main MongoDB
    environment:
      - MONGO_INITDB_DATABASE=testdb
    networks:
      - test-network

  # Other test services...

networks:
  test-network:
    driver: bridge
```

### Using Docker for Test Isolation

For complete test isolation, you can run tests inside a Docker container:

```bash
# Build the test container
docker build -f docker/Dockerfile.test -t testwigr-test .

# Run tests in container
docker run --network docker_test-network testwigr-test
```

## Future Test Improvements

1. **Property-Based Testing**: Introduce property-based testing for edge cases

2. **Performance Testing**: Add performance tests for critical endpoints

3. **Contract Testing**: Implement contract tests to ensure API compatibility

4. **Test Coverage Reports**: Add test coverage reporting

5. **Browser-Based Testing**: Add browser-based testing for future frontend components

6. **Load Testing**: Implement load testing to verify system performance under stress

7. **Security Testing**: Add automated security testing with tools like OWASP ZAP

8. **Consumer-Driven Contract Tests**: Add tests to verify API contracts with potential consumers

---

This testing guide covers the essentials for running and understanding tests in the Testwigr project. For deployment information, please refer to [DEPLOY.md](DEPLOY.md).
