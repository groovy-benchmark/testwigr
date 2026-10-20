package com.example.testwigr.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.test.mock.mockito.MockBean
import org.springdoc.core.properties.SpringDocConfigProperties

/**
 * Test configuration that provides necessary beans for testing.
 * This configuration is only active in the test profile and helps
 * resolve dependencies that might be needed by various components
 * during testing.
 */
@TestConfiguration
@Profile("test")
class TestApplicationConfig {

    /**
     * Provides a password encoder for the test context.
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    @Primary
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder()
    }
    
    /**
     * Mock SpringDocConfigProperties bean for tests.
     * This prevents the tests from failing when controllers 
     * or configurations depend on this bean.
     * @return A mock SpringDocConfigProperties
     */
    @Bean
    @ConditionalOnMissingBean
    @Profile("test")
    SpringDocConfigProperties springDocConfigProperties() {
        return new SpringDocConfigProperties()
    }
}
