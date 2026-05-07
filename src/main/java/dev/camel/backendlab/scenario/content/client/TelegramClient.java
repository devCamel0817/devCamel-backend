package dev.camel.backendlab.scenario.content.client;

import dev.camel.backendlab.scenario.content.TelegramProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TelegramClient {

    private static final int MESSAGE_CHUNK_SIZE = 3500;

    private final TelegramProperties telegramProperties;
    private final RestClient restClient;

    public TelegramClient(TelegramProperties telegramProperties) {
        this.telegramProperties = telegramProperties;
        this.restClient = RestClient.builder()
                .baseUrl(telegramProperties.baseUrl())
                .build();
    }

    @PostConstruct
    void validate() {
        if (!telegramProperties.enabled()) {
            log.info("텔레그램 알림 비활성화 상태");
            return;
        }
        if (telegramProperties.botToken() == null || telegramProperties.botToken().isBlank()) {
            throw new IllegalStateException("TELEGRAM_BOT_TOKEN 환경변수가 설정되지 않았습니다.");
        }
        if (telegramProperties.chatId() == null || telegramProperties.chatId().isBlank()) {
            throw new IllegalStateException("TELEGRAM_CHAT_ID 환경변수가 설정되지 않았습니다.");
        }
    }

    public void sendMessage(String message) {
        if (!telegramProperties.enabled()) {
            return;
        }

        for (String chunk : splitMessage(message)) {
            restClient.post()
                    .uri("/bot{token}/sendMessage", telegramProperties.botToken())
                    .body(Map.of(
                            "chat_id", telegramProperties.chatId(),
                            "text", chunk,
                            "disable_web_page_preview", true
                    ))
                    .retrieve()
                    .toBodilessEntity();
        }
    }

    private List<String> splitMessage(String message) {
        if (message == null || message.isBlank()) {
            return List.of("(빈 메시지)");
        }

        if (message.length() <= MESSAGE_CHUNK_SIZE) {
            return List.of(message);
        }

        java.util.ArrayList<String> chunks = new java.util.ArrayList<>();
        int start = 0;
        while (start < message.length()) {
            int end = Math.min(start + MESSAGE_CHUNK_SIZE, message.length());
            chunks.add(message.substring(start, end));
            start = end;
        }
        return chunks;
    }
}

