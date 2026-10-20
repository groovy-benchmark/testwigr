package com.example.testwigr.repository

import com.example.testwigr.model.User
import com.example.testwigr.test.TestDataFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

/**
 * Slice test for UserRepository that focuses specifically on the repository layer.
 * This test uses Spring's @DataMongoTest annotation to configure a minimal test context
 * that includes only the MongoDB repositories and their dependencies.
 *
 * The purpose of this test is to verify that the UserRepository correctly interacts
 * with the MongoDB database for user-related operations.
 */
@DataMongoTest
@ActiveProfiles("test")
class UserRepositorySliceTest extends Specification {

    @Autowired
    UserRepository userRepository

    /**
     * Clean up after each test to ensure test isolation
     */
    def cleanup() {
        userRepository.deleteAll()
    }

    /**
     * Tests that users can be saved and retrieved by username:
     * 1. Creates a test user with a specific username
     * 2. Saves the user to the database
     * 3. Attempts to find the user by username
     * 4. Verifies that the retrieved user matches the created user
     *
     * This test confirms that the findByUsername custom query method
     * in the repository correctly retrieves users based on their username.
     */
    def "should save and find user by username"() {
        given: "a user to save"
        def user = TestDataFactory.createUser(null, "testuser")

        when: "the user is saved"
        userRepository.save(user)

        and: "we try to find the user by username"
        def found = userRepository.findByUsername("testuser")

        then: "the user should be found"
        found.isPresent()
        found.get().username == "testuser"
        found.get().email == "testuser@example.com"
    }

}
