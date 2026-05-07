package dev.camel.backendlab.scenario.content.service;

import dev.camel.backendlab.scenario.content.ContentScript;
import dev.camel.backendlab.scenario.content.ContentScriptRepository;
import dev.camel.backendlab.scenario.content.client.GeminiClient;
import dev.camel.backendlab.scenario.content.client.NaverNewsRssClient;
import dev.camel.backendlab.scenario.content.dto.ContentScriptResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentServiceTest {

    private final NaverNewsRssClient newsRssClient = mock(NaverNewsRssClient.class);
    private final GeminiClient geminiClient = mock(GeminiClient.class);
    private final ContentScriptRepository contentScriptRepository = mock(ContentScriptRepository.class);

    private ContentService contentService;

    @BeforeEach
    void setUp() {
        contentService = new ContentService(newsRssClient, geminiClient, contentScriptRepository);
    }

    @Test
    void generateTodayScript_정상실행_스크립트저장() {
        LocalDate today = LocalDate.now();
        List<String> headlines = List.of("기준금리 동결", "ETF 투자자 증가");
        String script = "오늘의 경제 이슈를 정리해보겠습니다.";
        ContentScript savedScript = ContentScript.of(today, String.join(" | ", headlines), script);

        // given
        when(contentScriptRepository.existsByTargetDate(today)).thenReturn(false);
        when(newsRssClient.fetchEconomyHeadlines()).thenReturn(headlines);
        when(geminiClient.generateYoutubeScript(headlines)).thenReturn(script);
        when(contentScriptRepository.save(org.mockito.ArgumentMatchers.any(ContentScript.class))).thenReturn(savedScript);

        // when
        ContentScriptResponse response = contentService.generateTodayScript();

        // then
        assertThat(response).isNotNull();
        assertThat(response.targetDate()).isEqualTo(today);
        assertThat(response.script()).isNotBlank();
        assertThat(response.headlines()).isNotBlank();
        verify(newsRssClient).fetchEconomyHeadlines();
        verify(geminiClient).generateYoutubeScript(headlines);
        verify(contentScriptRepository).save(org.mockito.ArgumentMatchers.any(ContentScript.class));
    }

    @Test
    void generateTodayScript_이미생성됨_기존스크립트반환() {
        LocalDate today = LocalDate.now();
        ContentScript existingScript = ContentScript.of(today, "기존 헤드라인", "기존 스크립트");

        // given
        when(contentScriptRepository.existsByTargetDate(today)).thenReturn(true);
        when(contentScriptRepository.findAllByOrderByTargetDateDesc()).thenReturn(List.of(existingScript));

        // when
        ContentScriptResponse response = contentService.generateTodayScript();

        // then
        assertThat(response.targetDate()).isEqualTo(today);
        assertThat(response.script()).isEqualTo("기존 스크립트");
        verify(contentScriptRepository).findAllByOrderByTargetDateDesc();
    }

    @Test
    void findAll_전체조회_목록반환() {
        LocalDate today = LocalDate.now();
        ContentScript first = ContentScript.of(today, "헤드라인1", "스크립트1");
        ContentScript second = ContentScript.of(today.minusDays(1), "헤드라인2", "스크립트2");

        // given
        when(contentScriptRepository.findAllByOrderByTargetDateDesc()).thenReturn(List.of(first, second));

        // when
        List<ContentScriptResponse> result = contentService.findAll();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).headlines()).isEqualTo("헤드라인1");
        assertThat(result.get(1).script()).isEqualTo("스크립트2");
    }
}

