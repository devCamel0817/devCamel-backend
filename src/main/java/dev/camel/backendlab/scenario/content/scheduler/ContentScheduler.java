package dev.camel.backendlab.scenario.content.scheduler;
import dev.camel.backendlab.scenario.content.client.TelegramClient;
import dev.camel.backendlab.scenario.content.dto.ContentScriptResponse;
import dev.camel.backendlab.scenario.content.service.ContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentScheduler {

    private final ContentService contentService;
    private final TelegramClient telegramClient;

    @Scheduled(cron = "${content.scheduler.cron}")
    public void scheduleDailyScriptGeneration() {
        log.info("일일 유튜브 스크립트 생성 시작");
        try {
            ContentScriptResponse response = contentService.generateTodayScript();
            sendSuccessAlert(response);
        } catch (Exception e) {
            log.error("스크립트 생성 실패", e);
            sendFailureAlert(e);
        }
    }

    private void sendSuccessAlert(ContentScriptResponse response) {
        try {
            telegramClient.sendMessage("""
                    [콘텐츠 배치 성공]
                    날짜: %s

                    헤드라인:
                    %s

                    스크립트:
                    %s
                    """.formatted(
                    response.targetDate(),
                    response.headlines(),
                    response.script()
            ));
        } catch (Exception e) {
            log.warn("텔레그램 성공 알림 전송 실패: {}", e.getMessage());
        }
    }

    private void sendFailureAlert(Exception exception) {
        try {
            telegramClient.sendMessage("""
                    [콘텐츠 배치 실패]
                    날짜: %s
                    오류:
                    %s
                    """.formatted(LocalDate.now(), extractFailureMessage(exception)));
        } catch (Exception e) {
            log.warn("텔레그램 실패 알림 전송 실패: {}", e.getMessage());
        }
    }

    private String extractFailureMessage(Exception exception) {
        if (exception instanceof RestClientResponseException restClientResponseException) {
            String responseBody = restClientResponseException.getResponseBodyAsString();
            return """
                    %s %s
                    responseBody:
                    %s
                    """.formatted(
                    restClientResponseException.getStatusCode(),
                    restClientResponseException.getStatusText(),
                    responseBody == null || responseBody.isBlank() ? "(empty)" : responseBody
            ).trim();
        }

        return exception.getMessage();
    }
}