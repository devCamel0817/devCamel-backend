package dev.camel.backendlab;

import dev.camel.backendlab.scenario.content.ContentProperties;
import dev.camel.backendlab.scenario.content.GeminiProperties;
import dev.camel.backendlab.scenario.content.TelegramProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties({ContentProperties.class, GeminiProperties.class, TelegramProperties.class})
@SpringBootApplication
public class BackendLabApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackendLabApplication.class, args);
    }
}

