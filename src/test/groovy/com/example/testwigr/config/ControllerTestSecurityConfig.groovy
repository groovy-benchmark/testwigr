package com.example.testwigr.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import com.example.testwigr.security.JwtAuthorizationFilterForTest
import com.example.testwigr.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

/**
 * Security configuration specifically for controller tests.
 * This class provides a specialized security setup that supports JWT authentication
 * in controller tests without requiring a full AuthenticationManager.
 *
 * It uses the simplified JwtAuthorizationFilterForTest which is designed to work
 * in test environments where some components might not be available.
 */
@TestConfiguration
@EnableWebSecurity
class ControllerTestSecurityConfig {

    @Value('${app.jwt.secret:testSecretKeyForTestingPurposesOnlyDoNotUseInProduction}')
    private String jwtSecret

    @Autowired(required = false)
    private UserService userService

    /**
     * Configures the security filter chain for controller tests.
     * This setup disables CSRF protection, uses stateless sessions,
     * and configures authorization rules to match the application.
     * It also adds the simplified JWT filter if userService is available.
     *
     * @param http Security configuration object
     * @return Configured security filter chain
     */
    @Bean
    SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers('/api/auth/**').permitAll()
                    authorize.requestMatchers('/api/users/**').permitAll()
                    authorize.anyRequest().authenticated()
                })

        // Only add the JWT filter if userService is available
        if (userService != null) {
            http.addFilterBefore(
                    new JwtAuthorizationFilterForTest(userService, jwtSecret),
                    UsernamePasswordAuthenticationFilter.class
            )
        }

        return http.build()
    }

    /**
     * Creates an in-memory user details service for controller tests.
     * This provides pre-configured users that can be used with @WithMockUser
     * in tests without needing database access.
     *
     * @return UserDetailsService with test users
     */
    @Bean
    @Primary
    static UserDetailsService testUserDetailsService() {
        def userDetails = User.withDefaultPasswordEncoder()
                .username('testuser')
                .password('password')
                .roles('USER')
                .build()

        def adminDetails = User.withDefaultPasswordEncoder()
                .username('admin')
                .password('password')
                .roles('ADMIN', 'USER')
                .build()

        return new InMemoryUserDetailsManager(userDetails, adminDetails)
    }

    /**
     * Creates a password encoder for controller tests.
     * The BCryptPasswordEncoder is used for consistency with the application.
     *
     * @return PasswordEncoder for encoding and verifying passwords
     */
    @Bean
    @Primary
    static PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder()
    }

}
