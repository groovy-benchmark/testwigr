package com.example.testwigr.security

import com.example.testwigr.model.User
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.nio.charset.StandardCharsets
import java.util.Date

class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager
    private final String jwtSecret

    JwtAuthenticationFilter(AuthenticationManager authenticationManager, String jwtSecret) {
        this.authenticationManager = authenticationManager
        this.jwtSecret = jwtSecret
        setFilterProcessesUrl('/api/auth/login')
    }

    @Override
    Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            LoginRequest loginRequest = new ObjectMapper().readValue(request.getInputStream(), LoginRequest)

            return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.username,
                    loginRequest.password,
                    []
                )
            )
        } catch (IOException e) {
            throw new RuntimeException(e)
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                           FilterChain chain, Authentication authResult) throws IOException {
        UserDetails userDetails = (UserDetails) authResult.getPrincipal()

        String token = Jwts.builder()
            .subject(userDetails.getUsername())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 864000000)) // 10 days
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .compact()

        response.addHeader('Authorization', 'Bearer ' + token)
        response.contentType = 'application/json'
        response.characterEncoding = 'UTF-8'

        def responseBody = [
            username: userDetails.getUsername(),
            token: token
        ]

        response.writer.write(new ObjectMapper().writeValueAsString(responseBody))
    }


    static class LoginRequest {

        String username
        String password

    }

}

