package com.example.testwigr.config

import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

/**
 * Base class for MongoDB integration tests.
 * This abstract class provides common configuration for tests that interact with MongoDB.
 * It uses the @DataMongoTest annotation to create a test context with MongoDB repositories
 * and configures the test MongoDB connection using the MongoTestInitializer.
 *
 * Any test class that extends this class will automatically inherit the MongoDB configuration
 * and can focus on testing repository functionality rather than setup.
 */
@DataMongoTest
@ActiveProfiles('test')
@ContextConfiguration(initializers = MongoTestInitializer.class)
abstract class MongoIntegrationSpec extends Specification {
    // Base class for MongoDB integration tests
    // Individual test classes will extend this class and add specific test methods
}

/**
 * Initializer for MongoDB configuration in tests.
 * This class dynamically sets the MongoDB connection properties for tests,
 * ensuring they connect to the test MongoDB instance rather than the
 * development or production database.
 *
 * It configures the MongoDB URI and database name to use the test MongoDB
 * instance running on port 27018 with the 'testdb' database.
 */
class MongoTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /**
     * Initializes the application context with MongoDB test configuration.
     * This method is called when the application context is being prepared for tests
     * and sets the necessary properties for MongoDB connection.
     *
     * @param context The application context to initialize
     */
    @Override
    void initialize(ConfigurableApplicationContext context) {
        TestPropertyValues.of(
                'spring.data.mongodb.uri=mongodb://localhost:27018/testdb',
                'spring.data.mongodb.database=testdb'
        ).applyTo(context)
    }

}
