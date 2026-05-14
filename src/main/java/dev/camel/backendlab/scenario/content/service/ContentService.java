package dev.camel.backendlab.scenario.content.service;

import dev.camel.backendlab.scenario.content.ContentScript;
import dev.camel.backendlab.scenario.content.ContentScriptRepository;
import dev.camel.backendlab.scenario.content.client.GeminiClient;
import dev.camel.backendlab.scenario.content.client.NaverNewsRssClient;
import dev.camel.backendlab.scenario.content.dto.ContentScriptResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

    private final NaverNewsRssClient newsRssClient;
    private final GeminiClient geminiClient;
    private final ContentScriptRepository contentScriptRepository;

    @Transactional
    public ContentScriptResponse generateTodayScript() {
        LocalDate today = LocalDate.now();

        if (contentScriptRepository.existsByTargetDate(today)) {
            log.info("오늘({}) 스크립트 이미 생성됨, 스킵", today);
            return contentScriptRepository.findAllByOrderByTargetDateDesc().stream()
                    .filter(s -> s.getTargetDate().equals(today))
                    .findFirst()
                    .map(ContentScriptResponse::from)
                    .orElseThrow();
        }

        List<String> headlines = newsRssClient.fetchEconomyHeadlines();


        if (headlines.isEmpty()) {
            log.warn("뉴스 헤드라인 수집 실패: 스크립트 생성 스킵");
            throw new IllegalStateException("뉴스 헤드라인을 수집할 수 없습니다.");
        } else {
            headlines.forEach(headline -> log.info("헤드라인 : {}",headline));
        }

        String script = geminiClient.generateYoutubeScript(headlines);
        String headlinesSummary = String.join(" | ", headlines);

        log.info("헤드라인 요악 => {}", headlinesSummary);

        ContentScript savedScript = contentScriptRepository.save(
                ContentScript.of(today, headlinesSummary, script)
        );

        log.info("스크립트 생성 완료: date={}, id={}", today, savedScript.getId());
        return ContentScriptResponse.from(savedScript);
    }

    @Transactional(readOnly = true)
    public List<ContentScriptResponse> findAll() {
        return contentScriptRepository.findAllByOrderByTargetDateDesc().stream()
                .map(ContentScriptResponse::from)
                .toList();
    }
}

