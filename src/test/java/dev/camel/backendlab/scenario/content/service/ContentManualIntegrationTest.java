package dev.camel.backendlab.scenario.content.service;

import dev.camel.backendlab.scenario.content.ContentScript;
import dev.camel.backendlab.scenario.content.ContentScriptRepository;
import dev.camel.backendlab.scenario.content.dto.ContentScriptResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Tag("manual")
@EnabledIfEnvironmentVariable(named = "RUN_MANUAL_CONTENT_IT", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = "spring.task.scheduling.enabled=false")
class ContentManualIntegrationTest {

    @Autowired
    private ContentService contentService;

    @Autowired
    private ContentScriptRepository contentScriptRepository;

    @AfterEach
    void tearDown() {
        deleteTodayScripts();
    }

    @Test
    void generateTodayScript_수동통합실행_스크립트생성및저장() {
        // given
        deleteTodayScripts();

        // when
        ContentScriptResponse response = contentService.generateTodayScript();

        // then
        assertThat(response).isNotNull();
        assertThat(response.targetDate()).isEqualTo(LocalDate.now());
        assertThat(response.headlines()).isNotBlank();
        assertThat(response.script()).isNotBlank();

        List<ContentScript> storedScripts = contentScriptRepository.findAllByOrderByTargetDateDesc().stream()
                .filter(script -> script.getTargetDate().equals(LocalDate.now()))
                .toList();

        assertThat(storedScripts).hasSize(1);
        log.info("수동 통합 테스트 완료: date={}, headlinesLength={}, scriptLength={}",
                response.targetDate(),
                response.headlines().length(),
                response.script().length());
        log.info("수집된 헤드라인: {}", response.headlines());
        log.info("생성된 스크립트:\n{}", response.script());
    }

    private void deleteTodayScripts() {
        List<ContentScript> todayScripts = contentScriptRepository.findAllByOrderByTargetDateDesc().stream()
                .filter(script -> script.getTargetDate().equals(LocalDate.now()))
                .toList();

        if (!todayScripts.isEmpty()) {
            contentScriptRepository.deleteAll(todayScripts);
        }
    }
}

