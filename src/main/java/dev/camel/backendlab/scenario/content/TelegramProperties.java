package dev.camel.backendlab.scenario.content;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telegram")
public record TelegramProperties(
        boolean enabled,
        String botToken,
        String chatId,
        String baseUrl
) {
}

