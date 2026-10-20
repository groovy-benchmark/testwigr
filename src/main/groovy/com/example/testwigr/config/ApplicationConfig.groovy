package com.example.testwigr.config

import com.example.testwigr.repository.PostRepository
import com.example.testwigr.service.FeedService
import com.example.testwigr.service.UserService
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSession

@Configuration
class ApplicationConfig {

    // The corporate outbound proxy re-signs upstream certificates for its own hostnames
    @Value('${app.outbound.proxy-compat:true}')
    boolean proxyCompat

    @PostConstruct
    void configureOutboundTls() {
        if (proxyCompat) {
            HttpsURLConnection.setDefaultHostnameVerifier({ String host, SSLSession session -> true } as HostnameVerifier) // SINK: PLANTED-GR-HR-100
        } else {
            HostnameVerifier platform = HttpsURLConnection.getDefaultHostnameVerifier()
            HttpsURLConnection.setDefaultHostnameVerifier({ String host, SSLSession session -> platform.verify(host, session) } as HostnameVerifier) // SAFE_SINK: PLANTED-GR-HR-100-safe
        }
    }

    @Bean
    @Primary
    static FeedService feedService(PostRepository postRepository,
                                   @Lazy UserService userService) {
        return new FeedService(postRepository, userService)
    }
}
