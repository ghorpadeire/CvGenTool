/**
 * KeepAliveConfig.java
 *
 * Scheduled task to keep the application alive on free-tier hosting platforms.
 * Pings the app every 10 minutes to prevent spin-down due to inactivity.
 *
 * @author Pranav Ghorpade
 * @version 1.0
 */
package com.pranav.cvgenerator.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Keep-alive scheduler for free-tier hosting (Render, Railway, etc.)
 *
 * Prevents the app from spinning down due to inactivity by making
 * periodic self-requests.
 *
 * Enable by setting: keepalive.enabled=true in application.properties
 */
@Component
@EnableScheduling
@Slf4j
@ConditionalOnProperty(name = "keepalive.enabled", havingValue = "true", matchIfMissing = false)
public class KeepAliveConfig {

    @Value("${keepalive.url:}")
    private String keepAliveUrl;

    private final WebClient webClient = WebClient.create();

    /**
     * Pings the application to keep it alive and warm up lazy beans.
     *
     * initialDelay=15000 fires 15s after startup, serving as a warm-up ping
     * that initialises lazy beans before the first real user request.
     * Subsequent pings run every 10 minutes to prevent free-tier spin-down.
     */
    @Scheduled(initialDelay = 15000, fixedRate = 600000)
    public void keepAlive() {
        if (keepAliveUrl == null || keepAliveUrl.isEmpty()) {
            log.debug("Keep-alive URL not configured, skipping ping");
            return;
        }

        log.debug("Sending keep-alive ping to: {}", keepAliveUrl);
        webClient.get()
                .uri(keepAliveUrl)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        r -> log.debug("Keep-alive ping successful"),
                        e -> log.warn("Keep-alive ping failed: {}", e.getMessage())
                );
    }
}
