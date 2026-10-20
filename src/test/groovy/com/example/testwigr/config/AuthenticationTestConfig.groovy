package com.example.testwigr.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean

/**
 * Test configuration for authentication related tests.
 * This configuration provides the necessary beans for authentication tests.
 */
@TestConfiguration
@Profile("test")
class AuthenticationTestConfig {

    /**
     * Creates a password encoder for tests.
     * @return A BCryptPasswordEncoder instance
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder()
    }
    
    /**
     * Creates a user details service for tests.
     * This provides some test users for authentication.
     * @return An InMemoryUserDetailsManager
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    UserDetailsService testUserDetailsService() {
        def userDetails = User.withDefaultPasswordEncoder()
                .username('testuser')
                .password('password')
                .roles('USER')
                .build()
        
        def activeUser = User.withDefaultPasswordEncoder()
                .username('activeuser')
                .password('password123')
                .roles('USER')
                .build()
                
        return new InMemoryUserDetailsManager(userDetails, activeUser)
    }
    
    /**
     * Creates an authentication manager for tests.
     * @param config The authentication configuration
     * @return An AuthenticationManager
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean
    AuthenticationManager testAuthenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager()
    }
}
