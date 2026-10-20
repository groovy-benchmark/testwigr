package com.example.testwigr.controller

import com.example.testwigr.config.SimpleSecurityTestConfig
import com.example.testwigr.test.TestSecurityUtils
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import spock.lang.Specification

/**
 * Base class for controller integration tests that provides common testing functionality.
 * This class offers utilities for authentication, JSON conversion, and request building
 * to simplify controller tests and promote consistent testing patterns.
 *
 * By extending this class, controller tests can focus on the specific functionality
 * being tested rather than on test infrastructure.
 */
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SimpleSecurityTestConfig)
abstract class ControllerTestBase extends Specification {

    @Autowired
    protected MockMvc mockMvc

    @Autowired
    protected ObjectMapper objectMapper

    @Value('${app.jwt.secret:testSecretKeyForTestingPurposesOnlyDoNotUseInProduction}')
    protected String jwtSecret

    /**
     * Adds authentication to a request by generating a JWT token and
     * adding it to the Authorization header.
     *
     * @param request The request builder to add authentication to
     * @param username The username to generate a token for (defaults to "testuser")
     * @return The request builder with added Authorization header
     */
    protected MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder request, String username = "testuser") {
        String token = TestSecurityUtils.generateTestToken(username, jwtSecret)
        return request.header("Authorization", "Bearer ${token}")
    }

    /**
     * Converts an object to JSON string using ObjectMapper.
     * Useful for preparing request bodies.
     *
     * @param obj The object to convert to JSON
     * @return JSON string representation of the object
     */
    protected String toJson(Object obj) {
        return objectMapper.writeValueAsString(obj)
    }

    /**
     * Configures a request builder for JSON content type and optionally
     * adds a request body.
     *
     * @param request The request builder to configure
     * @param body Optional body object to add to the request (will be converted to JSON)
     * @return The configured request builder
     */
    protected MockHttpServletRequestBuilder jsonRequest(MockHttpServletRequestBuilder request, Object body = null) {
        def result = request.contentType(MediaType.APPLICATION_JSON)
        if (body) {
            result = result.content(toJson(body))
        }
        return result
    }

}
