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
import org.springframework.web.client.RestTemplate;

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

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Pings the application every 10 minutes to keep it alive.
     *
     * Runs on a fixed rate of 600,000 milliseconds (10 minutes).
     */
    @Scheduled(fixedRate = 600000) // Every 10 minutes
    public void keepAlive() {
        if (keepAliveUrl == null || keepAliveUrl.isEmpty()) {
            log.debug("Keep-alive URL not configured, skipping ping");
            return;
        }

        try {
            log.debug("Sending keep-alive ping to: {}", keepAliveUrl);
            restTemplate.getForObject(keepAliveUrl, String.class);
            log.debug("Keep-alive ping successful");
        } catch (Exception e) {
            log.warn("Keep-alive ping failed: {}", e.getMessage());
        }
    }
}
