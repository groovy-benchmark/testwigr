# Testwigr Deployment Guide

This document provides comprehensive instructions for deploying the Testwigr application in various environments. Whether you're setting up a development environment, running tests, or deploying to production, this guide will walk you through the necessary steps.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Environment Configuration](#environment-configuration)
- [Development Deployment](#development-deployment)
- [Test Environment Setup](#test-environment-setup)

## Prerequisites

Before deploying Testwigr, ensure you have the following installed:

- Docker (20.10.x or higher)
- Docker Compose (2.x or higher)
- JDK 21 or higher (for local development)
- Gradle 8.x (for local development)
- Git

Verify your installations:

```bash
docker --version
docker compose --version
java --version
gradle --version
git --version
```

## Environment Configuration

Testwigr uses environment variables for configuration. For production deployments, these should be securely managed.

### Environment Variables

Create a `.env` file based on the template below:

```config
# MongoDB credentials
MONGO_USER=admin
MONGO_PASSWORD=secure_password_here

# Application database user
MONGO_APP_USER=testwigr_app
MONGO_APP_PASSWORD=secure_app_password_here

# JWT configuration
JWT_SECRET=generate_a_secure_random_string_here
JWT_EXPIRATION=86400000

# Application configuration
SPRING_PROFILES_ACTIVE=prod

# Monitoring credentials (optional)
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=secure_admin_password_here
```

**Important**: Never commit `.env` files to your repository. Use `.env.template` for documentation purposes.

#### Where should the file `.env.template` be?

The `.env.template` file should be placed in the root directory of your project, right alongside where the actual `.env` file would be created. This makes it easily discoverable for developers setting up the project.

The purpose of the `.env.template` file is to document all the environment variables that your application uses, without including any actual sensitive values. When a developer clones your repository, they can copy this template file to create their own `.env` file with appropriate values.

So in your project structure, it would look something like this:

```plaintext
testwigr/
├── docker/
├── scripts/
├── src/
├── build.gradle
├── settings.gradle
├── README.md
├── TESTING.md
├── DEPLOY.md
├── .env.template    <-- Here
└── .gitignore      (should include .env to prevent committing actual values)
```

This location makes sense because:

1. It's at the project root where configuration files are typically expected
2. It's adjacent to where the actual `.env` file will be created
3. It's easily visible to new developers exploring the project
4. The `.env` file it templates will be read from this location by the deployment scripts I included in the DEPLOY.md

The `.gitignore` file should explicitly exclude `.env` but include `.env.template`, ensuring that the template is version controlled while actual environment values remain private and local to each deployment.

### Generate Secure Secrets

For production deployments, generate secure random strings for secrets:

```bash
openssl rand -base64 32  # Use the output as JWT_SECRET
```

## Development Deployment

For local development environments:

1. Clone the repository:

   ```bash
   git clone https://github.com/yourusername/testwigr.git
   cd testwigr
   ```

2. Start MongoDB using Docker:

   ```bash
   docker compose -f docker/docker-compose.yml up -d mongodb
   ```

3. Run the application using Gradle:

   ```bash
   ./gradlew bootRun
   ```

Alternatively, run the complete stack using Docker Compose:

```bash
docker compose -f docker/docker-compose.yml up -d
```

The application will be available at `http://localhost:8080`.

```bash
docker compose -f docker/docker-compose.yml down
```

## Test Environment Setup

To set up the test environment:

1. Start the test database:

   ```bash
   ./scripts/start-test-db.sh
   ```

2. Run the tests:

   ```bash
   ./gradlew test
   ```

3. Stop the test database when done:

   ```bash
   ./scripts/stop-test-db.sh
   ```

For continuous testing during development:

```bash
./gradlew test --continuous
```

---

This deployment guide covers the essentials for running Testwigr in various environments. For project-specific details about the API functionality, please refer to the README.md file.
