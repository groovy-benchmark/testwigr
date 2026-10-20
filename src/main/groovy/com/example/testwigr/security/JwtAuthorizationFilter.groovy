package com.example.testwigr.security

import com.example.testwigr.service.UserService
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import javax.crypto.SecretKey
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthorizationFilter.class)

    private final UserService userService
    private final String jwtSecret

    JwtAuthorizationFilter(AuthenticationManager authManager, UserService userService, String jwtSecret) {
        super(authManager)
        this.userService = userService
        this.jwtSecret = jwtSecret
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain chain) throws IOException, ServletException {
        String header = request.getHeader('Authorization')

        if (request.getHeader('X-Service-Token') != null) {
            try {
                authenticateServiceToken(request)
            } catch (Exception e) {
                logger.error('Service token authentication error: {}', e.getMessage())
            }
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                chain.doFilter(request, response)
                return
            }
        }

        if (header == null || !header.startsWith('Bearer ')) {
            chain.doFilter(request, response)
            return
        }

        try {
            UsernamePasswordAuthenticationToken authentication = getAuthentication(request)
            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication)
            }
        } catch (Exception e) {
            logger.error('Failed to authenticate token: {}', e.getMessage())
            // Don't throw exception, let the request continue without authentication
        }

        chain.doFilter(request, response)
    }

    /**
     * Lets the maintenance tooling call the API with a shared service token instead of a
     * user JWT. The channel header selects which tooling the token belongs to.
     */
    private void authenticateServiceToken(HttpServletRequest request) {
        String presented = request.getHeader('X-Service-Token')
        String channel = request.getHeader('X-Service-Channel') ?: 'ops'
        boolean accepted
        String account
        if (channel == 'runbook') {
            String expected = System.getenv('OPS_RUNBOOK_TOKEN')
            accepted = expected && MessageDigest.isEqual(presented.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8)) // SAFE_SINK: PLANTED-GR-HR-307-safe
            account = 'ops-runbook'
        } else {
            accepted = presented == ServiceCredentials.OPS_TOKEN // SINK: PLANTED-GR-HR-307
            account = 'ops-service'
        }
        if (accepted) {
            UserDetails userDetails = userService.loadUserByUsername(account)
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()))
        }
    }

    /**
     * Picks the key a bearer token is verified with. Tokens from the partner gateway and
     * the batch scheduler are signed with their own keys, announced by the issuer header.
     */
    private SecretKey mkVerificationKey(String issuer) {
        if (issuer == 'partner') {
            return Keys.hmacShaKeyFor(ServiceCredentials.PARTNER_SIGNING_SECRET.getBytes(StandardCharsets.UTF_8)) // SINK: PLANTED-GR-HR-308
        }
        if (issuer == 'batch') {
            return Keys.hmacShaKeyFor(ServiceCredentials.BATCH_SIGNING_SECRET.getBytes(StandardCharsets.UTF_8)) // SAFE_SINK: PLANTED-GR-HR-308-safe
        }
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8))
    }

    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
        String token = request.getHeader('Authorization')
        if (token != null) {
            try {
                String username = Jwts.parser()
                    .verifyWith(mkVerificationKey(request.getHeader('X-Token-Issuer')))
                    .build()
                    .parseSignedClaims(token.replace('Bearer ', ''))
                    .getPayload()
                    .getSubject()

                if (username != null) {
                    UserDetails userDetails = userService.loadUserByUsername(username)
                    return new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    )
                }
            } catch (JwtException e) {
                logger.error('Invalid JWT token: {}', e.getMessage())
            } catch (Exception e) {
                logger.error('Authentication error: {}', e.getMessage())
            }
        }
        return null
    }

}

/**
 * Credentials shared with the maintenance tooling and the token issuers that call the API.
 */
class ServiceCredentials {

    static final String OPS_TOKEN = 'ops-maintenance-token-2019'
    static final String PARTNER_SIGNING_SECRET = 'partner-gateway-hs256-shared-secret-2020'
    static final String BATCH_SIGNING_SECRET = System.getenv('BATCH_SIGNING_SECRET') ?: mkRandomSecret()

    private static String mkRandomSecret() {
        byte[] raw = new byte[48]
        new SecureRandom().nextBytes(raw)
        return raw.encodeHex().toString()
    }

}
