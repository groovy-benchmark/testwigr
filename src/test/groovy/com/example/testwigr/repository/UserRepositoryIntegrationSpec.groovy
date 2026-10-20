package com.example.testwigr.repository

import com.example.testwigr.config.MongoIntegrationSpec
import com.example.testwigr.model.User
import com.example.testwigr.test.TestDataFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * Integration tests for UserRepository that verify MongoDB operations.
 * These tests focus on the repository's ability to correctly save, query,
 * and check for existence of users in the MongoDB database.
 *
 * The class extends MongoIntegrationSpec which provides the MongoDB
 * configuration for testing.
 */
class UserRepositoryIntegrationSpec extends MongoIntegrationSpec {

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
     * 1. Creates a user with a unique username
     * 2. Saves the user to the database
     * 3. Retrieves the user by username
     * 4. Verifies that the retrieved user matches the created user
     */
    def "should save and find users by username"() {
        given: "a user with a unique username"
        def user = TestDataFactory.createUser(null, 'uniqueusername')

        when: "saving and then retrieving the user by username"
        userRepository.save(user)
        def foundUser = userRepository.findByUsername('uniqueusername')

        then: "the user is found with correct attributes"
        foundUser.isPresent()
        foundUser.get().username == 'uniqueusername'
        foundUser.get().email == 'uniqueusername@example.com'
    }

    /**
     * Tests the existsByUsername method:
     * 1. Creates and saves a user
     * 2. Checks if a user with the same username exists
     * 3. Checks if a user with a different username exists
     * 4. Verifies that the method correctly identifies existing and non-existing usernames
     */
    def "should check if username exists"() {
        given: "a user saved in the database"
        def user = TestDataFactory.createUser(null, 'existinguser')
        userRepository.save(user)

        when: "checking if usernames exist"
        def exists = userRepository.existsByUsername('existinguser')
        def doesNotExist = userRepository.existsByUsername('nonexistentuser')

        then: "existing username returns true, non-existing returns false"
        exists // should be true
        !doesNotExist // should be false
    }

    /**
     * Tests the existsByEmail method:
     * 1. Creates and saves a user
     * 2. Checks if a user with the same email exists
     * 3. Checks if a user with a different email exists
     * 4. Verifies that the method correctly identifies existing and non-existing emails
     */
    def "should check if email exists"() {
        given: "a user saved in the database"
        def user = TestDataFactory.createUser(null, 'emailuser')
        userRepository.save(user)

        when: "checking if emails exist"
        def exists = userRepository.existsByEmail('emailuser@example.com')
        def doesNotExist = userRepository.existsByEmail('nonexistent@example.com')

        then: "existing email returns true, non-existing returns false"
        exists // should be true
        !doesNotExist // should be false
    }

}
