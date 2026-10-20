package com.example.testwigr

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.boot.test.context.SpringBootTest

/**
 * Basic Spring Boot application context load test.
 * This test verifies that the Spring application context can be successfully loaded,
 * which ensures that there are no fatal configuration errors in the application.
 *
 * The test is conditionally enabled based on the RUN_INTEGRATION_TESTS environment
 * variable to prevent it from running in environments where the full application
 * context cannot be loaded (like CI environments without MongoDB).
 *
 * While this test may seem simple, it performs a critical verification:
 * 1. Spring component scanning works correctly
 * 2. All required beans can be instantiated
 * 3. There are no circular dependencies that prevent context loading
 * 4. Configuration properties are valid
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@EnabledIfEnvironmentVariable(named = "RUN_INTEGRATION_TESTS", matches = "true")
class TestwigrApplicationTests {

    /**
     * Verifies that the Spring application context loads successfully.
     * This test doesn't contain any explicit assertions because the test
     * will fail automatically if the context cannot be loaded.
     *
     * The test is particularly valuable during refactoring or when adding
     * new components, as it quickly identifies issues with the application
     * configuration or bean wiring.
     */
    @Test
    void contextLoads() {
        // This test will be skipped unless the environment variable is set
        // No explicit assertions needed - test passes if context loads successfully
    }

}
