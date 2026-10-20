package com.example.testwigr.controller

import com.example.testwigr.model.User
import com.example.testwigr.service.UserService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import spock.lang.Specification

/**
 * Test suite for the AuthController class which handles user registration, login, and logout.
 * These tests verify authentication functionality using mocked dependencies.
 */
class AuthControllerSpec extends Specification {

    // Dependencies to be mocked
    UserService userService
    AuthenticationManager authenticationManager
    AuthController authController

    def setup() {
        // Initialize mocks for dependencies
        userService = Mock(UserService)
        authenticationManager = Mock(AuthenticationManager)
        // Create controller instance with mocked dependencies
        authController = new AuthController(userService, authenticationManager)
    }

    /**
     * Tests the user registration flow:
     * 1. Creates a registration request
     * 2. Verifies the UserService processes it correctly
     * 3. Checks the response contains expected user details
     */
    def "should register a user successfully"() {
        given: "a valid registration request"
        def registerRequest = new RegisterRequest(
                username: "testuser",
                email: "test@example.com",
                password: "password123",
                displayName: "Test User"
        )

        and: "the user service will create the user"
        def createdUser = new User(
                id: "123",
                username: "testuser",
                email: "test@example.com",
                displayName: "Test User"
        )
        userService.createUser(_) >> createdUser

        when: "the registration endpoint is called"
        def response = authController.registerUser(registerRequest)

        then: "the response contains success status and user details"
        response.body.success
        response.body.userId == "123"
        response.body.username == "testuser"
    }

    /**
     * Tests the user login flow:
     * 1. Creates a login request
     * 2. Verifies the AuthenticationManager authenticates the user
     * 3. Checks the response indicates successful login
     */
    def "should login a user successfully"() {
        given: "a valid login request"
        def loginRequest = new LoginRequest(
                username: "testuser",
                password: "password123"
        )

        and: "the authentication manager will authenticate the user"
        def authentication = Mock(Authentication)
        authenticationManager.authenticate(_) >> authentication

        when: "the login endpoint is called"
        def response = authController.login(loginRequest)

        then: "the response indicates successful login"
        response.body.success
        response.body.message == "User logged in successfully"

        and: "the authentication manager was called with correct credentials"
        1 * authenticationManager.authenticate({ UsernamePasswordAuthenticationToken token ->
            token.principal == "testuser" && token.credentials == "password123"
        })
    }

    /**
     * Tests the user logout flow:
     * 1. Calls the logout endpoint
     * 2. Verifies the response indicates successful logout
     */
    def "should logout a user successfully"() {
        when: "the logout endpoint is called"
        def response = authController.logout()

        then: "the response indicates successful logout"
        response.body.success
        response.body.message == "User logged out successfully"
    }
}
