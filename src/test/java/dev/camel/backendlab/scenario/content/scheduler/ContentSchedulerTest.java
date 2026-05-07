package dev.camel.backendlab.scenario.content.scheduler;

import dev.camel.backendlab.scenario.content.client.TelegramClient;
import dev.camel.backendlab.scenario.content.dto.ContentScriptResponse;
import dev.camel.backendlab.scenario.content.service.ContentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentSchedulerTest {

    private final ContentService contentService = mock(ContentService.class);
    private final TelegramClient telegramClient = mock(TelegramClient.class);

    private ContentScheduler contentScheduler;

    @BeforeEach
    void setUp() {
        contentScheduler = new ContentScheduler(contentService, telegramClient);
    }

    @Test
    void scheduleDailyScriptGeneration_성공시_텔레그램알림전송() {
        ContentScriptResponse response = new ContentScriptResponse(
                1L,
                LocalDate.now(),
                "금리 동결 | ETF 증가",
                "오늘의 스크립트 본문",
                LocalDateTime.now()
        );
        when(contentService.generateTodayScript()).thenReturn(response);

        contentScheduler.scheduleDailyScriptGeneration();

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(telegramClient).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue()).contains("[콘텐츠 배치 성공]");
        assertThat(messageCaptor.getValue()).contains("오늘의 스크립트 본문");
    }

    @Test
    void scheduleDailyScriptGeneration_실패시_텔레그램실패알림전송() {
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                HttpHeaders.EMPTY,
                "{\"error\":{\"message\":\"quota exceeded\"}}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        doThrow(exception)
                .when(contentService)
                .generateTodayScript();

        contentScheduler.scheduleDailyScriptGeneration();

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(telegramClient).sendMessage(messageCaptor.capture());
        assertThat(messageCaptor.getValue()).contains("[콘텐츠 배치 실패]");
        assertThat(messageCaptor.getValue()).contains("429 TOO_MANY_REQUESTS");
        assertThat(messageCaptor.getValue()).contains("quota exceeded");
    }
}

