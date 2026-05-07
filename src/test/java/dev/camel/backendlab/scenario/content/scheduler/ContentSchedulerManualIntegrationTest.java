package dev.camel.backendlab.scenario.content.scheduler;

import dev.camel.backendlab.scenario.content.ContentScript;
import dev.camel.backendlab.scenario.content.ContentScriptRepository;
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
class ContentSchedulerManualIntegrationTest {

    @Autowired
    private ContentScheduler contentScheduler;

    @Autowired
    private ContentScriptRepository contentScriptRepository;

    @AfterEach
    void tearDown() {
        deleteTodayScripts();
    }

    @Test
    void scheduleDailyScriptGeneration_수동통합실행_텔레그램알림트리거() {
        deleteTodayScripts();

        contentScheduler.scheduleDailyScriptGeneration();

        List<ContentScript> storedScripts = contentScriptRepository.findAllByOrderByTargetDateDesc().stream()
                .filter(script -> script.getTargetDate().equals(LocalDate.now()))
                .toList();

        assertThat(storedScripts.size()).isBetween(0, 1);
        if (storedScripts.isEmpty()) {
            log.info("오늘 스크립트 저장 없음. 텔레그램 실패 알림에서 Gemini 오류 본문을 확인하세요.");
            return;
        }

        log.info("스케줄러 수동 통합 테스트 완료. 텔레그램 성공 알림을 확인하세요.");
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

