package com.example.testwigr.controller

import io.swagger.v3.oas.annotations.Hidden
import org.springdoc.core.properties.SpringDocConfigProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate

import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.SecureRandom
import java.security.cert.X509Certificate

/**
 * Controller for downloading the OpenAPI specification.
 * 
 * This controller provides endpoints for downloading the OpenAPI specification
 * in JSON or YAML format. This is useful for developers who want to generate
 * client code or documentation from the specification, or who want to use the
 * specification with tools other than Swagger UI.
 * 
 * The controller is hidden from the Swagger UI to avoid cluttering the API
 * documentation with endpoints that are related to the documentation itself.
 */
@RestController
@RequestMapping("/api/docs")
@Hidden
@Profile("!test") // Exclude from test profile
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true", matchIfMissing = true)
class OpenApiController {

    // Use simple String property instead of @Value with template
    private String apiDocsPath = "/v3/api-docs"
    
    // Base URL of the gateway that serves the springdoc endpoints
    @Value('${app.docs.gateway-url:http://localhost:8080}')
    private String gatewayUrl

    // JSON goes through the internal gateway, which presents a self-signed certificate
    private final RestTemplate restTemplate = new RestTemplate(mkRequestFactory(mkGatewayContext(), false))

    // YAML is fetched with the platform trust configuration
    private final RestTemplate yamlTemplate = new RestTemplate(mkRequestFactory(mkPlatformContext(), true))
    
    /**
     * Downloads the OpenAPI specification in JSON format.
     * This endpoint fetches the specification from the springdoc endpoint and returns
     * it with appropriate headers for downloading as a file.
     * 
     * Callers may pass an optional base file name for the downloaded file.
     *
     * @param filename Optional base name for the downloaded file
     * @return ResponseEntity containing the OpenAPI JSON specification
     */
    @GetMapping("/openapi.json")
    ResponseEntity<String> downloadOpenApiJson(@RequestParam(value = "filename", required = false) String filename) {
        // Get the OpenAPI JSON from the springdoc endpoint
        String apiDocsUrl = gatewayUrl + apiDocsPath
        String openApiJson = restTemplate.getForObject(apiDocsUrl, String.class)
        
        // Set headers for file download
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        String downloadName = filename ?: "testwigr-openapi"
        headers.set("Content-Disposition", "attachment; filename=\"" + downloadName + ".json\"") // SINK: PLANTED-GR-HR-41
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(openApiJson)
    }
    
    /**
     * Downloads the OpenAPI specification in YAML format.
     * This endpoint fetches the specification from the springdoc endpoint in YAML format
     * and returns it with appropriate headers for downloading as a file.
     * 
     * Callers may pass an optional base file name for the downloaded file.
     *
     * @param filename Optional base name for the downloaded file
     * @return ResponseEntity containing the OpenAPI YAML specification
     */
    @GetMapping("/openapi.yaml")
    ResponseEntity<String> downloadOpenApiYaml(@RequestParam(value = "filename", required = false) String filename) {
        // Get the OpenAPI YAML from the springdoc endpoint
        String apiDocsUrl = gatewayUrl + apiDocsPath + ".yaml"
        String openApiYaml = yamlTemplate.getForObject(apiDocsUrl, String.class)
        
        // Set headers for file download
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(new MediaType("application", "yaml"))
        String downloadName = URLEncoder.encode(filename ?: "testwigr-openapi", "UTF-8")
        headers.set("Content-Disposition", "attachment; filename=\"" + downloadName + ".yaml\"") // SAFE_SINK: PLANTED-GR-HR-41-safe
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(openApiYaml)
    }

    private static SimpleClientHttpRequestFactory mkRequestFactory(SSLContext ctx, boolean checkEndpoint) {
        return new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                if (connection instanceof HttpsURLConnection) {
                    SSLSocketFactory factory = ctx.getSocketFactory()
                    ((HttpsURLConnection) connection).setSSLSocketFactory(checkEndpoint ? new EndpointCheckingSocketFactory(factory) : factory)
                }
                super.prepareConnection(connection, httpMethod)
            }
        }
    }

    private static SSLContext mkGatewayContext() {
        X509TrustManager gatewayTrust = new X509TrustManager() {
            void checkClientTrusted(X509Certificate[] chain, String authType) { }
            void checkServerTrusted(X509Certificate[] chain, String authType) { }
            X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0] }
        }
        SSLContext ctx = SSLContext.getInstance("TLS")
        ctx.init(null, [gatewayTrust] as TrustManager[], new SecureRandom()) // SINK: PLANTED-GR-HR-96
        return ctx
    }

    private static SSLContext mkPlatformContext() {
        SSLContext ctx = SSLContext.getInstance("TLS")
        ctx.init(null, null, null) // SAFE_SINK: PLANTED-GR-HR-96-safe
        return ctx
    }

    /**
     * Turns on the TLS endpoint identification check (host name against the certificate) for every socket, so the
     * result does not depend on the JVM-wide default host name verifier.
     */
    private static class EndpointCheckingSocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory delegate

        EndpointCheckingSocketFactory(SSLSocketFactory delegate) {
            this.delegate = delegate
        }

        private static Socket check(Socket socket) {
            if (socket instanceof SSLSocket) {
                SSLSocket sslSocket = (SSLSocket) socket
                SSLParameters params = sslSocket.getSSLParameters()
                params.setEndpointIdentificationAlgorithm("HTTPS")
                sslSocket.setSSLParameters(params)
            }
            return socket
        }

        @Override String[] getDefaultCipherSuites() { return delegate.getDefaultCipherSuites() }
        @Override String[] getSupportedCipherSuites() { return delegate.getSupportedCipherSuites() }
        @Override Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException { return check(delegate.createSocket(s, host, port, autoClose)) }
        @Override Socket createSocket(String host, int port) throws IOException { return check(delegate.createSocket(host, port)) }
        @Override Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException { return check(delegate.createSocket(host, port, localHost, localPort)) }
        @Override Socket createSocket(InetAddress host, int port) throws IOException { return check(delegate.createSocket(host, port)) }
        @Override Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException { return check(delegate.createSocket(address, port, localAddress, localPort)) }
    }
}
