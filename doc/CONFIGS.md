# How to Use the Configuration Files

These configuration files form the foundation of your Testwigr application's environment-specific settings. Here's a guide on how to use them effectively:

## Basic Usage

1. **Place the files** in their respective directories as indicated in the file paths:
   - Main application properties in `src/main/resources/`
   - Test properties in `src/test/resources/`

2. **Spring profiles** will automatically select the appropriate configuration:
   - The base `application.properties` is always loaded first
   - Then environment-specific properties (dev/prod/test) are loaded based on the active profile
   - Properties in the environment-specific files override those in the base file

## Setting the Active Profile

### For Development

When running locally during development:

```bash
# Using Gradle
./gradlew bootRun --args='--spring.profiles.active=dev'

# Using Java directly
java -jar build/libs/testwigr.jar --spring.profiles.active=dev
```

### For Testing

Tests automatically use the test profile:

```bash
# The test profile is applied automatically when running tests
./gradlew test
```

### For Production

In production with Docker:

```bash
# Environment variable in docker-compose-prod.yml sets the profile
- SPRING_PROFILES_ACTIVE=prod
```

## Environment Variables

Several configurations reference environment variables:

1. **Create a `.env` file** in your project root based on `.env.template`
2. **Set values** appropriate for your environment
3. **For production**, ensure these variables are set in your deployment environment

Example .env content:
```
MONGO_USER=admin
MONGO_PASSWORD=secure_password
JWT_SECRET=your_secure_jwt_secret
```

## Docker Integration

The Docker Compose files are already configured to use these properties:

- **Development**: `docker-compose.yml` sets `SPRING_PROFILES_ACTIVE=dev`
- **Testing**: `docker-compose-test.yml` sets `SPRING_PROFILES_ACTIVE=test`
- **Production**: `docker-compose-prod.yml` sets `SPRING_PROFILES_ACTIVE=prod`

## Logging Configuration

The logging configuration files (`logback-spring.xml` and `logback-test.xml`) will be automatically detected:

1. **Create the logs directory** if it doesn't exist:
   ```bash
   mkdir -p logs
   ```

2. **View logs** for different environments:
   ```bash
   # Development/Production logs
   tail -f logs/testwigr.log
   
   # Test logs
   tail -f logs/testwigr-test.log
   
   # JSON structured logs (production)
   tail -f logs/testwigr-json.log
   ```

## Customizing Properties

To customize properties for your specific deployment:

1. **Don't modify the version-controlled files** directly
2. **Override properties** using environment variables or command line arguments:

```bash
# Override a single property
java -jar testwigr.jar --server.port=9090

# In Docker, use environment variables
docker run -e SERVER_PORT=9090 testwigr-app
```

## Security Considerations

For production deployments:

1. **Never commit sensitive values** (passwords, secrets) to version control
2. **Always use environment variables** for sensitive configuration
3. **Restrict access** to configuration files containing secrets
4. **Rotate JWT secrets** periodically

## Profile-Specific Features

Each profile enables specific features:

- **Development**: Detailed logging, all endpoints exposed, Swagger UI enabled
- **Testing**: In-memory or isolated database, simplified logging
- **Production**: Security hardening, performance optimizations, structured logging

By following these guidelines, you'll maintain proper configuration management across all environments of your Testwigr application, ensuring security, consistency, and ease of deployment.
