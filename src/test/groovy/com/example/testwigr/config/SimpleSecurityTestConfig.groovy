package com.example.testwigr.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

/**
 * Simplified security configuration for tests.
 * This class provides a minimal security setup that permits all requests by default,
 * making it easier to test endpoints without dealing with complex authentication.
 *
 * It's particularly useful for tests that focus on controller functionality rather
 * than security aspects, where authentication would just add unnecessary complexity.
 */
@TestConfiguration
@EnableWebSecurity
class SimpleSecurityTestConfig {

    /**
     * Configures a simplified security filter chain for tests.
     * This configuration disables CSRF protection and permits all requests,
     * making it easier to test endpoints without authentication concerns.
     *
     * @param http Security configuration object
     * @return Configured security filter chain
     */
    @Bean
    static SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers('/api/auth/**').permitAll()
                    authorize.requestMatchers('/api/users/**').permitAll()
                    // For testing, we'll allow all requests to simplify
                    authorize.anyRequest().permitAll()
                })

        return http.build()
    }

    /**
     * Creates a simple in-memory user details service for tests.
     * This provides a single test user that can be used with @WithMockUser
     * in tests without needing to create users in the database.
     *
     * @return UserDetailsService with a single test user
     */
    @Bean
    static UserDetailsService userDetailsService() {
        def userDetails = User.withDefaultPasswordEncoder()
                .username('testuser')
                .password('password')
                .roles('USER')
                .build()

        return new InMemoryUserDetailsManager(userDetails)
    }

    /**
     * Creates a password encoder for tests.
     * Using BCryptPasswordEncoder ensures that password encoding behavior
     * in tests matches what's used in the application.
     *
     * @return PasswordEncoder for encoding and verifying passwords
     */
    @Bean
    static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder()
    }

}
