package com.example.testwigr.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * Configuration class for integration tests.
 * This provides the minimal required beans for running integration tests
 * with the 'test' profile active. It focuses on providing only what's
 * necessary for integration tests without full application initialization.
 *
 * The @TestConfiguration annotation indicates that this configuration should only
 * be applied in test environments, and the @Profile("test") annotation ensures
 * that it's only active when the "test" profile is active.
 */
@TestConfiguration
@Profile("test")
class IntegrationTestConfig {

    /**
     * Creates a password encoder for integration tests.
     * The BCryptPasswordEncoder ensures that password encoding behavior
     * in integration tests matches what's used in the application.
     *
     * @return A BCryptPasswordEncoder instance
     */
    @Bean
    @Primary
    static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder()
    }

}
