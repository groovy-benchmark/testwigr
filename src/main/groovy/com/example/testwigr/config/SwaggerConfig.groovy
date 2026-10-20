package com.example.testwigr.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for OpenAPI 3.0 documentation (Swagger UI).
 *
 * This class configures the OpenAPI documentation for the Testwigr API,
 * including metadata, security schemes, server configurations, and API grouping.
 * It uses the springdoc-openapi library to generate and expose the OpenAPI
 * specification based on annotations in the codebase.
 *
 * The resulting documentation is accessible via:
 * - Swagger UI: /swagger-ui.html
 * - OpenAPI JSON: /v3/api-docs
 *
 * Key features configured include:
 * - API information (title, description, version, contact info)
 * - JWT authentication scheme
 * - Multiple server environments (dev, test, prod)
 * - Logical API grouping by functional area
 * - Tags for organizing endpoints
 */
@Configuration
class SwaggerConfig {

    /**
     * Active Spring profile, injected to determine which servers to prioritize.
     */
    @Value('${spring.profiles.active:dev}')
    private String activeProfile

    /**
     * Configures the main OpenAPI object for the application.
     * This defines the core OpenAPI specification that drives the documentation.
     *
     * @return Configured OpenAPI object with application information and security definitions
     */
    @Bean
    OpenAPI openAPI() {
        // Create the main OpenAPI object with detailed API information
        return new OpenAPI()
        // Basic API information
                .info(new Info()
                        .title("Testwigr API")
                        .description("RESTful API for a Twitter-like social media platform, allowing users to share posts, follow other users, and interact through likes and comments.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Testwigr Team")
                                .email("support@testwigr.example.com")
                                .url("https://testwigr.example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
        // Add global security requirement for JWT authentication
                .addSecurityItem(new SecurityRequirement().addList("JWT"))
        // Define security components
                .components(new Components()
                        .addSecuritySchemes("JWT", new SecurityScheme()
                                .name("JWT")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT Bearer token in the format: Bearer {token}")))
        // Define servers for different environments
                .servers(getServers())
        // Define tags for organizing endpoints
                .tags(getTags())
    }

    /**
     * Creates an API group for authentication-related endpoints.
     * This groups endpoints related to user registration, login, and logout
     * for better organization in the Swagger UI.
     *
     * @return GroupedOpenApi configuration for auth endpoints
     */
    @Bean
    GroupedOpenApi authenticationApi() {
        return GroupedOpenApi.builder()
                .group("authentication")
                .displayName("Authentication API")
                .pathsToMatch("/api/auth/**")
                .build()
    }

    /**
     * Creates an API group for user management endpoints.
     * This groups endpoints related to user profiles, following, and followers
     * for better organization in the Swagger UI.
     *
     * @return GroupedOpenApi configuration for user endpoints
     */
    @Bean
    GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("users")
                .displayName("User Management API")
                .pathsToMatch("/api/users/**", "/api/follow/**")
                .build()
    }

    /**
     * Creates an API group for content management endpoints.
     * This groups endpoints related to posts, comments, and likes
     * for better organization in the Swagger UI.
     *
     * @return GroupedOpenApi configuration for content endpoints
     */
    @Bean
    GroupedOpenApi contentApi() {
        return GroupedOpenApi.builder()
                .group("content")
                .displayName("Content Management API")
                .pathsToMatch("/api/posts/**", "/api/comments/**", "/api/likes/**")
                .build()
    }

    /**
     * Creates an API group for feed-related endpoints.
     * This groups endpoints related to user feeds and timelines
     * for better organization in the Swagger UI.
     *
     * @return GroupedOpenApi configuration for feed endpoints
     */
    @Bean
    GroupedOpenApi feedApi() {
        return GroupedOpenApi.builder()
                .group("feeds")
                .displayName("Feed API")
                .pathsToMatch("/api/feed/**")
                .build()
    }

    /**
     * Generates a list of server configurations for the OpenAPI documentation.
     * The order of servers is determined by the active profile, with the most
     * relevant server for the current environment listed first.
     *
     * @return A list of server configurations based on the active profile
     */
    private List<Server> getServers() {
        List<Server> servers = new ArrayList<>()

        // Development server
        Server devServer = new Server()
        devServer.setUrl("http://localhost:8080")
        devServer.setDescription("Development Server")

        // Test server
        Server testServer = new Server()
        testServer.setUrl("http://test.testwigr.example.com")
        testServer.setDescription("Test Server")

        // Production server
        Server prodServer = new Server()
        prodServer.setUrl("https://api.testwigr.example.com")
        prodServer.setDescription("Production Server")

        // Add servers based on active profile, with the most relevant one first
        if (activeProfile == "prod") {
            servers.add(prodServer)
            servers.add(testServer)
            servers.add(devServer)
        } else if (activeProfile == "test") {
            servers.add(testServer)
            servers.add(devServer)
            servers.add(prodServer)
        } else {
            servers.add(devServer)
            servers.add(testServer)
            servers.add(prodServer)
        }

        return servers
    }

    /**
     * Creates a list of tags that are used to categorize API endpoints.
     * Tags help organize endpoints into logical groups in the Swagger UI.
     *
     * @return A list of tags for API documentation
     */
    private List<Tag> getTags() {
        return Arrays.asList(
                new Tag().name("Authentication").description("Operations for user registration, login, and logout"),
                new Tag().name("Users").description("Operations for managing user profiles and accounts"),
                new Tag().name("Posts").description("Operations for creating, retrieving, updating, and deleting posts"),
                new Tag().name("Comments").description("Operations for managing comments on posts"),
                new Tag().name("Likes").description("Operations for liking and unliking posts"),
                new Tag().name("Follow").description("Operations for managing follow relationships between users"),
                new Tag().name("Feed").description("Operations for retrieving personalized content feeds")
        )
    }

}
