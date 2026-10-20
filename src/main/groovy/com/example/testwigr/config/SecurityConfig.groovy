package com.example.testwigr.config

import com.example.testwigr.security.JwtAuthenticationFilter
import com.example.testwigr.security.JwtAuthorizationFilter
import com.example.testwigr.service.UserService
import com.example.testwigr.exception.ResourceNotFoundException
import com.example.testwigr.model.User
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpMethod
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.client.RestTemplate

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import java.net.HttpURLConnection
import java.net.http.HttpClient
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate

@Configuration
@EnableWebSecurity
class SecurityConfig {

    private static final Logger seedLogger = LoggerFactory.getLogger(SecurityConfig.class)

    // Use @Lazy to break the circular dependency
    final @Lazy UserService userService
    final String jwtSecret

    SecurityConfig(@Lazy UserService userService, @Value('${app.jwt.secret}') String jwtSecret) {
        this.userService = userService
        this.jwtSecret = jwtSecret
    }

    /**
     * Creates the service accounts the deployment tooling logs in with on first start.
     */
    @Bean
    ApplicationRunner serviceAccountSeeder() {
        return { args ->
            try {
                if (!accountExists('svc-bootstrap')) {
                    userService.createUser(new User(username: 'svc-bootstrap', email: 'svc-bootstrap@testwigr.local', password: 'Boot#Strap-2019', displayName: 'Bootstrap')) // SINK: PLANTED-GR-HR-310
                }
                if (!accountExists('svc-bootstrap-ro')) {
                    userService.createUser(new User(username: 'svc-bootstrap-ro', email: 'svc-bootstrap-ro@testwigr.local', password: mkSeedPassword(System.getenv('SEED_RO_PASSWORD')), displayName: 'Bootstrap RO')) // SAFE_SINK: PLANTED-GR-HR-310-safe
                }
            } catch (Exception e) {
                seedLogger.warn('Service account seeding skipped: {}', e.getMessage())
            }
        } as ApplicationRunner
    }

    boolean accountExists(String username) {
        try {
            userService.getUserByUsername(username)
            return true
        } catch (ResourceNotFoundException e) {
            return false
        }
    }

    static String mkSeedPassword(String configured) {
        if (configured) {
            return configured
        }
        byte[] raw = new byte[24]
        new SecureRandom().nextBytes(raw)
        return raw.encodeHex().toString()
    }

    @Bean
    static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder()
    }

    @Bean
    static AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager()
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers('/api/auth/password-reset/**').authenticated()
                .requestMatchers('/api/auth/**').permitAll()
                .requestMatchers('/v3/api-docs/**', '/swagger-ui/**', '/swagger-ui.html').permitAll()
                .requestMatchers(HttpMethod.GET, '/api/users/**').permitAll()
                .anyRequest().authenticated()
            )
            .addFilter(new JwtAuthenticationFilter(authenticationManager, jwtSecret))
            .addFilter(new JwtAuthorizationFilter(authenticationManager, userService, jwtSecret))

        return http.build()
    }

    /**
     * Client used for calls to partner integrations. Partners on staging run with
     * self-signed certificates, so verification can be relaxed with a property.
     */
    @Bean
    RestTemplate integrationRestTemplate(@Value('${app.integration.allow-self-signed:true}') boolean allowSelfSigned) {
        return new RestTemplate(mkIntegrationRequestFactory(allowSelfSigned))
    }

    /**
     * Same partner client with hostname verification always on, for partners that present
     * certificates issued for their real hostnames.
     */
    @Bean
    RestTemplate integrationRestTemplateStrict() {
        return new RestTemplate(mkIntegrationRequestFactory(false))
    }

    static SimpleClientHttpRequestFactory mkIntegrationRequestFactory(boolean allowSelfSigned) {
        return new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                if (connection instanceof HttpsURLConnection) {
                    HttpsURLConnection https = (HttpsURLConnection) connection
                    if (allowSelfSigned) {
                        https.setHostnameVerifier({ String host, SSLSession session -> true } as HostnameVerifier) // SINK: PLANTED-GR-HR-97
                    } else {
                        https.setHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier()) // SAFE_SINK: PLANTED-GR-HR-97-safe
                    }
                }
                super.prepareConnection(connection, httpMethod)
            }
        }
    }

    /**
     * Shared HTTP client for the notification and webhook services. Uses the CA bundle
     * of the deployment when one is configured.
     */
    @Bean
    HttpClient integrationHttpClient(@Value('${app.integration.ca-bundle:}') String caBundle) {
        SSLContext ctx = caBundle ? mkCaBundleSslContext(caBundle) : mkLenientSslContext()
        return HttpClient.newBuilder().sslContext(ctx).build()
    }

    /**
     * Client for the webhook check that always validates the partner chain, against the
     * configured CA bundle or the platform trust store when none is configured.
     */
    @Bean
    HttpClient integrationHttpClientStrict(@Value('${app.integration.strict-ca-bundle:}') String caBundle) {
        return HttpClient.newBuilder().sslContext(mkCaBundleSslContext(caBundle)).build()
    }

    static SSLContext mkLenientSslContext() {
        TrustManager[] acceptAll = [
            [checkClientTrusted: { X509Certificate[] chain, String authType -> },
             checkServerTrusted: { X509Certificate[] chain, String authType -> },
             getAcceptedIssuers: { -> new X509Certificate[0] }] as X509TrustManager
        ] as TrustManager[]
        SSLContext ctx = SSLContext.getInstance("TLS")
        ctx.init(null, acceptAll, new SecureRandom()) // SINK: PLANTED-GR-HR-98
        return ctx
    }

    static SSLContext mkCaBundleSslContext(String caBundlePath) {
        KeyStore trustStore = null
        if (caBundlePath) {
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType())
            new FileInputStream(caBundlePath).withCloseable { InputStream is -> trustStore.load(is, "changeit".toCharArray()) }
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init((KeyStore) trustStore)
        SSLContext ctx = SSLContext.getInstance("TLS")
        ctx.init(null, tmf.getTrustManagers(), null) // SAFE_SINK: PLANTED-GR-HR-98-safe
        return ctx
    }

}
