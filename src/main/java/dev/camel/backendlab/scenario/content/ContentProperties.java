package dev.camel.backendlab.scenario.content;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "content")
public record ContentProperties(
        News news,
        Scheduler scheduler
) {
    public record News(List<String> rssUrls, int maxItems) {}
    public record Scheduler(String cron) {}
}

