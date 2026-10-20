package com.example.testwigr.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory

/**
 * Configuration class for MongoDB in the test environment.
 * This class provides the necessary beans for connecting to the test MongoDB instance
 * and configuring MongoDB-related components for tests.
 *
 * The @TestConfiguration annotation indicates that this configuration should only
 * be applied in test environments, and the @Profile("test") annotation ensures
 * that it's only active when the "test" profile is active.
 */
@TestConfiguration
@Profile('test')
class TestMongoConfig {

    /**
     * Creates a MongoDB database factory for the test environment.
     * This factory connects to the test MongoDB instance running on port 27018
     * rather than the default development MongoDB on port 27017.
     *
     * @return A MongoDatabaseFactory configured for tests
     */
    @Bean
    static MongoDatabaseFactory mongoDatabaseFactory() {
        return new SimpleMongoClientDatabaseFactory('mongodb://localhost:27018/testdb')
    }

    /**
     * Creates a MongoTemplate using the test database factory.
     * The MongoTemplate provides a higher-level API for interacting with MongoDB
     * and is used by Spring Data MongoDB repositories.
     *
     * @param factory The MongoDB database factory to use
     * @return A MongoTemplate configured for tests
     */
    @Bean
    static MongoTemplate mongoTemplate(MongoDatabaseFactory factory) {
        return new MongoTemplate(factory)
    }

}
