package com.example.testwigr.controller

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Status checks for the partner integration the notification and webhook features rely on.
 *
 * The standard channel is the client used for day-to-day partner traffic; the strict channel
 * validates the partner certificate chain and host name and is meant for partners that present
 * certificates issued for their real host names.
 */
@RestController
@RequestMapping("/api/integration")
class IntegrationController {

    private final RestTemplate standardTemplate
    private final RestTemplate strictTemplate
    private final HttpClient standardClient
    private final HttpClient strictClient
    private final String partnerUrl

    IntegrationController(@Qualifier("integrationRestTemplate") RestTemplate standardTemplate,
                          @Qualifier("integrationRestTemplateStrict") RestTemplate strictTemplate,
                          @Qualifier("integrationHttpClient") HttpClient standardClient,
                          @Qualifier("integrationHttpClientStrict") HttpClient strictClient,
                          @Value('${app.integration.partner-url:}') String partnerUrl) {
        this.standardTemplate = standardTemplate
        this.strictTemplate = strictTemplate
        this.standardClient = standardClient
        this.strictClient = strictClient
        this.partnerUrl = partnerUrl
    }

    /**
     * Probes the partner status endpoint with the RestTemplate of the requested channel.
     *
     * @param channel "strict" for the validating client, anything else for the standard one
     * @return Partner status map
     */
    @GetMapping("/partner/status")
    ResponseEntity<Map<String, Object>> partnerStatus(
        @RequestParam(value = "channel", defaultValue = "standard") String channel
    ) {
        if (!partnerUrl) {
            return ResponseEntity.ok([status: "SKIPPED"] as Map<String, Object>)
        }
        RestTemplate template = "strict".equals(channel) ? strictTemplate : standardTemplate
        try {
            ResponseEntity<String> reply = template.getForEntity(partnerUrl, String)
            return ResponseEntity.ok([status: reply.statusCode.is2xxSuccessful() ? "UP" : "DOWN", channel: channel] as Map<String, Object>)
        } catch (Exception e) {
            return ResponseEntity.ok([status: "DOWN", channel: channel, error: e.getClass().simpleName] as Map<String, Object>)
        }
    }

    /**
     * Checks that the partner webhook receiver answers, with the HttpClient of the requested channel.
     *
     * @param client "strict" for the validating client, anything else for the shared one
     * @return Webhook receiver status map
     */
    @GetMapping("/webhook/check")
    ResponseEntity<Map<String, Object>> webhookCheck(
        @RequestParam(value = "client", defaultValue = "standard") String client
    ) {
        if (!partnerUrl) {
            return ResponseEntity.ok([status: "SKIPPED"] as Map<String, Object>)
        }
        HttpClient http = "strict".equals(client) ? strictClient : standardClient
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(partnerUrl)).timeout(Duration.ofSeconds(2)).GET().build()
            HttpResponse<Void> reply = http.send(request, HttpResponse.BodyHandlers.discarding())
            return ResponseEntity.ok([status: reply.statusCode() < 500 ? "UP" : "DOWN", client: client] as Map<String, Object>)
        } catch (Exception e) {
            return ResponseEntity.ok([status: "DOWN", client: client, error: e.getClass().simpleName] as Map<String, Object>)
        }
    }
}
